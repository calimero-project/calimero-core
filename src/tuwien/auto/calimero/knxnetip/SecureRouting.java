/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2021 B. Malinowsky

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    Linking this library statically or dynamically with other modules is
    making a combined work based on this library. Thus, the terms and
    conditions of the GNU General Public License cover the whole
    combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent
    modules, and to copy and distribute the resulting executable under terms
    of your choice, provided that you also meet, for each linked independent
    module, the terms and conditions of the license of that module. An
    independent module is a module which is not derived from or based on
    this library. If you modify this library, you may extend this exception
    to your version of the library, but you are not obligated to do so. If
    you do not wish to do so, delete this exception statement from your
    version.
*/

package tuwien.auto.calimero.knxnetip;

import static tuwien.auto.calimero.DataUnitBuilder.toHex;
import static tuwien.auto.calimero.knxnetip.Net.hostPort;
import static tuwien.auto.calimero.knxnetip.SecureConnection.securityInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.SerialNumber;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.log.LogService.LogLevel;
import tuwien.auto.calimero.secure.KnxSecureException;

final class SecureRouting extends KNXnetIPRouting {

	private static final int SecureGroupSync = 0x0955;

	private final SerialNumber sno;
	private final Key secretKey;

	private final int mcastLatencyTolerance; // [ms]
	private final int syncLatencyTolerance; // [ms]

	private final AtomicInteger routingCount = new AtomicInteger();

	// bookkeeping for multicast group timestamp sync

	// offset to adjust local clock upon receiving a valid secure packet or group sync response
	private long timestampOffset = -System.nanoTime() / 1_000_000L; // [ms]

	private volatile boolean syncedWithGroup;
	private volatile int sentGroupSyncTag;

	private static final int syncQueryInterval = 10_000; // [ms]
	private static final int minDelayTimeKeeperUpdateNotify = 100; // [ms]

	private int minDelayUpdateNotify;
	private int maxDelayUpdateNotify;
	private int minDelayPeriodicNotify;
	private int maxDelayPeriodicNotify;
	private volatile boolean periodicSchedule = true;

	// info for scheduled outgoing group sync
	private SerialNumber timerNotifySN;
	private int timerNotifyTag;

	// assign dummy to have it initialized
	private Future<?> groupSync = CompletableFuture.completedFuture(Void.TYPE);

	private static final ScheduledThreadPoolExecutor groupSyncSender = new ScheduledThreadPoolExecutor(1, r -> {
		final Thread t = new Thread(r);
		t.setName("KNX/IP secure group sync");
		t.setDaemon(true);
		return t;
	});

	static {
		// remove idle threads after a while
		groupSyncSender.setKeepAliveTime(30, TimeUnit.SECONDS);
		groupSyncSender.allowCoreThreadTimeOut(true);
		groupSyncSender.setRemoveOnCancelPolicy(true);
	}

	SecureRouting(final NetworkInterface netif, final InetAddress mcGroup, final byte[] groupKey,
			final Duration latencyTolerance) throws KNXException {
		super(mcGroup);

		sno = deriveSerialNumber(netif);
		secretKey = SecureConnection.createSecretKey(groupKey);
		mcastLatencyTolerance = (int) latencyTolerance.toMillis();
		syncLatencyTolerance = mcastLatencyTolerance / 10;

		init(netif, true, true);
		// we don't randomize initial delay [0..10] seconds to minimize uncertainty window of eventual group sync
		scheduleGroupSync(0);
		try {
			awaitGroupSync();
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static SerialNumber deriveSerialNumber(final NetworkInterface netif) {
		try {
			if (netif != null) {
				final byte[] hardwareAddress = netif.getHardwareAddress();
				if (hardwareAddress != null)
					return SerialNumber.from(Arrays.copyOf(hardwareAddress, 6));
			}
		}
		catch (final SocketException ignore) {}
		return SerialNumber.Zero;
	}

	@Override
	public String name() {
		return "KNX/IP " + SecureConnection.secureSymbol + " Routing " + ctrlEndpt.getAddress().getHostAddress();
	}

	@Override
	public String toString() {
		return name();
	}

	@Override
	protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
		final int tag = routingCount.getAndIncrement() % 0x10000;
		final byte[] wrapped = newSecurePacket(timestamp(), tag, packet);
		channel().send(ByteBuffer.wrap(wrapped), dst);
		scheduleGroupSync(periodicNotifyDelay());
	}

	@Override
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset,
		final InetAddress src, final int port) throws KNXFormatException, IOException {
		final int svc = h.getServiceType();
		if (!h.isSecure()) {
			logger.trace("received insecure service type 0x{} - ignore", Integer.toHexString(svc));
			return true;
		}

		if (svc == SecureGroupSync) {
			try {
				final Object[] fields = newGroupSync(h, data, offset);
				onGroupSync(src, (long) fields[0], true, (SerialNumber) fields[1], (int) fields[2]);
			}
			catch (final KnxSecureException e) {
				logger.debug("group sync {}", e.getMessage());
				return true;
			}
		}
		else if (svc == SecureConnection.SecureSvc) {
			final Object[] fields = unwrap(h, data, offset);
			final long timestamp = (long) fields[1];
			if (!withinTolerance(src, timestamp, (SerialNumber) fields[2], (int) fields[3])) {
				final var source = new InetSocketAddress(src, port);
				logger.warn("{} timestamp {} outside latency tolerance of {} ms (local {}) - ignore", hostPort(source),
						timestamp, mcastLatencyTolerance, timestamp());
				return true;
			}

			final byte[] packet = (byte[]) fields[4];
			final KNXnetIPHeader containedHeader = new KNXnetIPHeader(packet, 0);

			// let base class handle contained in decrypted knxip packet
			return super.handleServiceType(containedHeader, packet, containedHeader.getStructLength(), src, port);
		}
		else
			logger.warn("received unsupported secure service type 0x{} - ignore", Integer.toHexString(svc));

		return true;
	}

	@Override
	protected void close(final int initiator, final String reason, final LogLevel level, final Throwable t) {
		groupSync.cancel(true);
		super.close(initiator, reason, level, t);
	}

	private boolean withinTolerance(final InetAddress src, final long timestamp, final SerialNumber sn, final int tag) {
		onGroupSync(src, timestamp, false, sn, tag);
		final long diff = timestamp() - timestamp;
		return diff <= mcastLatencyTolerance;
	}

	private void onGroupSync(final InetAddress src, final long timestamp, final boolean byTimerNotify,
			final SerialNumber sn, final int tag) {
		final long local = timestamp();
		if (timestamp > local) {
			logger.debug("sync timestamp +{} ms", timestamp - local);
			timestampOffset += timestamp - local;
			syncedWithGroup(byTimerNotify, sn, tag);
		}
		else if (timestamp > (local - syncLatencyTolerance)) {
			// only consider sync messages sent by other nodes
			if (tag != sentGroupSyncTag || !isLocalIpAddress(src))
				syncedWithGroup(byTimerNotify, sn, tag);
		}
//		else if (timestamp > (local - mcastLatencyTolerance)) {
			// received old timestamp within tolerance, do nothing
//		}
		else if (timestamp <= (local - mcastLatencyTolerance)) {
			// received outdated timestamp, schedule group sync if we haven't done so already ...
			if (periodicSchedule) {
				timerNotifySN = sn;
				timerNotifyTag = tag;
				periodicSchedule = false;
				scheduleGroupSync(randomClosedRange(minDelayUpdateNotify, maxDelayUpdateNotify));
			}
		}
	}

	private synchronized void becomeTimeFollower() {
		final int maxDelayTimeKeeperUpdateNotify = minDelayTimeKeeperUpdateNotify + 1 * syncLatencyTolerance;
		final int minDelayTimeKeeperPeriodicNotify = syncQueryInterval;
		final int maxDelayTimeKeeperPeriodicNotify = minDelayTimeKeeperPeriodicNotify + 3 * syncLatencyTolerance;

		final int minDelayTimeFollowerUpdateNotify = maxDelayTimeKeeperUpdateNotify + 1 * syncLatencyTolerance;
		final int maxDelayTimeFollowerUpdateNotify = minDelayTimeFollowerUpdateNotify + 10 * syncLatencyTolerance;
		final int minDelayTimeFollowerPeriodicNotify = maxDelayTimeKeeperPeriodicNotify + 1 * syncLatencyTolerance;
		final int maxDelayTimeFollowerPeriodicNotify = minDelayTimeFollowerPeriodicNotify + 10 * syncLatencyTolerance;

		minDelayUpdateNotify = minDelayTimeFollowerUpdateNotify;
		maxDelayUpdateNotify = maxDelayTimeFollowerUpdateNotify;
		minDelayPeriodicNotify = minDelayTimeFollowerPeriodicNotify;
		maxDelayPeriodicNotify = maxDelayTimeFollowerPeriodicNotify;
	}

	private synchronized void becomeTimeKeeper() {
		final int maxDelayTimeKeeperUpdateNotify = minDelayTimeKeeperUpdateNotify + 1 * syncLatencyTolerance;
		final int minDelayTimeKeeperPeriodicNotify = syncQueryInterval;
		final int maxDelayTimeKeeperPeriodicNotify = minDelayTimeKeeperPeriodicNotify + 3 * syncLatencyTolerance;

		minDelayUpdateNotify = minDelayTimeKeeperUpdateNotify;
		maxDelayUpdateNotify = maxDelayTimeKeeperUpdateNotify;
		minDelayPeriodicNotify = minDelayTimeKeeperPeriodicNotify;
		maxDelayPeriodicNotify = maxDelayTimeKeeperPeriodicNotify;
	}

	private void syncedWithGroup(final boolean byTimerNotify, final SerialNumber sn, final int tag) {
		if (byTimerNotify)
			becomeTimeFollower();

		scheduleGroupSync(periodicNotifyDelay());
		if (!syncedWithGroup && tag == sentGroupSyncTag && sno.equals(sn)) {
			logger.info("synchronized with group {}", getRemoteAddress().getAddress().getHostAddress());
			syncedWithGroup = true;
			synchronized (this) {
				notifyAll();
			}
		}
	}

	private void awaitGroupSync() throws InterruptedException {
		// max. waiting time = 2 * latency tolerance + maxDelayTimeFollowerUpdateNotify
		final long wait = 2 * mcastLatencyTolerance + 100 + 12 * syncLatencyTolerance;
		final long end = System.nanoTime() / 1_000_000 + wait;
		long remaining = wait;
		while (remaining > 0 && !syncedWithGroup) {
			synchronized (this) {
				wait(remaining);
			}
			remaining = end - System.nanoTime() / 1_000_000;
		}
		syncedWithGroup = true;
		logger.trace("waited {} ms for group sync", wait - remaining);
	}

	private boolean isLocalIpAddress(final InetAddress addr) {
		Stream<NetworkInterface> netifs = Stream.empty();
		try {
			final var local = ((InetSocketAddress) channel().getLocalAddress()).getAddress();
			if (addr.equals(local))
				return true;
			final NetworkInterface ni = channel().getOption(StandardSocketOptions.IP_MULTICAST_IF);
			final boolean noneSet = ni == null || ni.getInetAddresses().nextElement().isAnyLocalAddress();
			netifs = noneSet ? NetworkInterface.networkInterfaces() : Stream.of(ni);
		}
		catch (final IOException e) {}
		return netifs.flatMap(ni -> ni.inetAddresses()).anyMatch(addr::equals);
	}

	private void scheduleGroupSync(final long initialDelay) {
		logger.trace("schedule group sync (initial delay {} ms)", initialDelay);
		groupSync.cancel(true);
		groupSync = groupSyncSender.scheduleWithFixedDelay(this::sendGroupSync, initialDelay, syncQueryInterval,
				TimeUnit.MILLISECONDS);
	}

	private void sendGroupSync() {
		try {
			final long timestamp = timestamp();
			final byte[] sync = newGroupSync(timestamp);
			logger.debug("sending group sync timestamp {} ms (S/N {}, tag {})", timestamp,
					periodicSchedule ? sno : timerNotifySN,
					periodicSchedule ? sentGroupSyncTag : timerNotifyTag);

			// schedule next sync before send to maintain happens-before with sync rcv
			becomeTimeKeeper();
			scheduleGroupSync(periodicNotifyDelay());
			channel().send(ByteBuffer.wrap(sync), dataEndpt);
		}
		catch (IOException | RuntimeException e) {
			if (!channel().isOpen()) {
				groupSync.cancel(true);
				throw new CancellationException("stop group sync for " + this);
			}
			logger.warn("sending group sync failed", e);
		}
	}

	private long timestamp() {
		final long now = System.nanoTime() / 1000_000L;
		return now + timestampOffset;
	}

	private synchronized int periodicNotifyDelay() {
		periodicSchedule = true;
		return randomClosedRange(minDelayPeriodicNotify, maxDelayPeriodicNotify);
	}

	private static int randomClosedRange(final int min, final int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	private byte[] newSecurePacket(final long seq, final int msgTag, final byte[] knxipPacket) {
		return SecureConnection.newSecurePacket(0, seq, sno, msgTag, knxipPacket, secretKey);
	}

	private Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		return unwrap(h, data, offset, 0, secretKey);
	}

	private Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset, final int sessionId,
			final Key secretKey) throws KNXFormatException {
		final Object[] fields = SecureConnection.unwrap(h, data, offset, secretKey);

		final int sid = (int) fields[0];
		if (sid != 0)
			throw new KnxSecureException("secure session mismatch: received ID " + sid + ", expected 0");

		final long seq = (long) fields[1];

		final var sn = (SerialNumber) fields[2];
		final int tag = (int) fields[3];
		final byte[] knxipPacket = (byte[]) fields[4];
		logger.trace("received {} (session {} seq {} S/N {} tag {})", toHex(knxipPacket, " "), sid, seq, sn, tag);
		return new Object[] { fields[0], fields[1], sn, fields[3], fields[4] };
	}

	private byte[] newGroupSync(final long timestamp) {
		if (timestamp < 0 || timestamp > 0xffff_ffff_ffffL)
			throw new KNXIllegalArgumentException("timestamp " + timestamp + " out of range [0..0xffffffffffff]");

		final KNXnetIPHeader header = new KNXnetIPHeader(SecureGroupSync, 0x1e);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());

		buffer.putShort((short) (timestamp >> 32)).putInt((int) timestamp);
		if (periodicSchedule) {
			sentGroupSyncTag = randomClosedRange(1, 0xffff);
			buffer.put(sno.array()).putShort((short) sentGroupSyncTag);
		}
		else
			buffer.put(timerNotifySN.array()).putShort((short) timerNotifyTag);

		final byte[] mac = cbcMac(buffer.array(), 0, header.getStructLength() + 6 + 6 + 2, securityInfo(buffer.array(), 6, 0));
		final byte[] secInfo = securityInfo(buffer.array(), 6, 0xff00);
		encrypt(mac, 0, secInfo);
		buffer.put(mac);
		return buffer.array();
	}

	private Object[] newGroupSync(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		if (h.getTotalLength() != 0x24)
			throw new KNXFormatException("invalid length " + data.length + " for a secure group sync");

		final ByteBuffer buffer = ByteBuffer.wrap(data, offset, h.getTotalLength() - h.getStructLength());

		final long timestamp = uint48(buffer);
		final byte[] sn = new byte[6];
		buffer.get(sn);
		final int msgTag = buffer.getShort() & 0xffff;

		final ByteBuffer mac = decrypt(buffer, securityInfo(data, offset, 0xff00));

		final byte[] secInfo = securityInfo(buffer.array(), 6, 0);
		SecureConnection.cbcMacVerify(data, offset - h.getStructLength(), h.getTotalLength() - SecureConnection.macSize,
				secretKey, secInfo, mac.array());
		logger.trace("received group sync timestamp {} ms (S/N {}, tag {})", timestamp, toHex(sn, ""), msgTag);
		return new Object[] { timestamp, SerialNumber.from(sn), msgTag };
	}

	private void encrypt(final byte[] data, final int offset, final byte[] secInfo) {
		SecureConnection.encrypt(data, offset, secretKey, secInfo);
	}

	private ByteBuffer decrypt(final ByteBuffer buffer, final byte[] secInfo) {
		return SecureConnection.decrypt(buffer, secretKey, secInfo);
	}

	private byte[] cbcMac(final byte[] data, final int offset, final int length, final byte[] secInfo) {
		return SecureConnection.cbcMac(data, offset, length, secretKey, secInfo);
	}

	private static long uint48(final ByteBuffer buffer) {
		long l = (buffer.getShort() & 0xffffL) << 32;
		l |= buffer.getInt() & 0xffffffffL;
		return l;
	}
}

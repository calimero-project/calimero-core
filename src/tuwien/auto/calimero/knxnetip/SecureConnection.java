/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018 B. Malinowsky

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.rfc7748.X25519;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogService.LogLevel;

/**
 * Provides KNX IP Secure routing and tunneling connections (experimental).
 */
public final class SecureConnection extends KNXnetIPRouting {

	private static final int SecureSvc = 0x0950;
	private static final int SecureSessionResponse = 0x0952;
	private static final int SecureSessionAuth = 0x0953;
	private static final int SecureSessionStatus = 0x0954;
	private static final int SecureGroupSync = 0x0955;

	private static final int macSize = 16; // [bytes]
	private static final int keyLength = 32; // [bytes]


	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private final byte[] sno;
	private Key secretKey;
	private int sessionId;

	// tunneling connection setup

	private static Key deviceAuthKey = createSecretKey(new byte[16]);

	private final byte[] privateKey = new byte[keyLength];
	private final byte[] publicKey = new byte[keyLength];

	// timeout channel.req -> channel.res
	private static final int sessionSetupTimeout = 30_000; // [ms]

	private static final int SessionSetup = -1;
	private volatile int sessionStatus = SessionSetup;

	private DatagramSocket localSocket;
	private ReceiverLoop setupLoop;
	private int seq;
	private int rcvSeq;

	// routing connection setup

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
	}

	// assign dummy to have it initialized
	private Future<?> groupSync = CompletableFuture.completedFuture(Void.TYPE);
	private volatile boolean syncedWithGroup;

	// multicast timestamp sync
	private static final Duration timestampExpiresAfter = Duration.ofMinutes(60 + new Random().nextInt(11));
	private long timestampExpiresAt = 0 + timestampExpiresAfter.toMillis();
	// offset to adjust local clock upon receiving a valid secure packet or group sync response
	private long timestampOffset = -System.nanoTime() / 1_000_000L; // [ms]
	private static final long queryInterval = 10_000; // [ms]

	private final int mcastLatencyTolerance; // [ms]
	private final int syncLatencyTolerance; // [ms]

	private final AtomicInteger routingCount = new AtomicInteger();


	public static KNXnetIPConnection newTunneling(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat) throws KNXException, InterruptedException {
		return new SecureConnection(knxLayer, localEP, serverCtrlEP, useNat);
	}

	public static KNXnetIPConnection newRouting(final NetworkInterface netIf, final InetAddress mcGroup, final byte[] groupKey,
		final Duration latencyTolerance) throws KNXException {
		return new SecureConnection(netIf, mcGroup, groupKey, latencyTolerance);
	}

	private SecureConnection(final NetworkInterface netif, final InetAddress mcGroup, final byte[] groupKey,
		final Duration latencyTolerance) throws KNXException {
		super(netif, mcGroup);

		sno = deriveSerialNumber(netif);
		secretKey = createSecretKey(groupKey);
		mcastLatencyTolerance = (int) latencyTolerance.toMillis();
		syncLatencyTolerance = mcastLatencyTolerance / 10;
		// we don't randomize initial delay [0..10] seconds to minimize uncertainty window of eventual group sync
		scheduleGroupSync(0);
	}

	private SecureConnection(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat) throws KNXException {
		super(null);

		sno = deriveSerialNumber(localEP);
		setupSecureSession(localEP, serverCtrlEP);
		// NYI tcp, secure wrapper

		mcastLatencyTolerance = 0;
		syncLatencyTolerance = 0;
	}

	private static byte[] deriveSerialNumber(final InetSocketAddress localEP) {
		try {
			return deriveSerialNumber(NetworkInterface.getByInetAddress(localEP.getAddress()));
		}
		catch (final SocketException ignore) {}
		return new byte[6];
	}

	private static byte[] deriveSerialNumber(final NetworkInterface netif) {
		try {
			if (netif != null) {
				final byte[] hardwareAddress = netif.getHardwareAddress();
				if (hardwareAddress != null)
					return Arrays.copyOf(hardwareAddress, 6);
			}
		}
		catch (final SocketException ignore) {}
		return new byte[6];
	}

	@Override
	protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
		if (!syncedWithGroup)
			logger.warn("sending while not yet synchronized with group {}", dst.getAddress().getHostAddress());
		final int tag = routingCount.getAndIncrement() % 0x10000;
		final byte[] wrapped = newSecurePacket(timestamp(), tag, packet);
		final DatagramPacket p = new DatagramPacket(wrapped, wrapped.length, dst);
		socket.send(p);
	}

	@Override
	public String getName() {
		final String lock = new String(Character.toChars(0x1F512));
		return "KNX/IP " + lock + " Routing " + ctrlEndpt.getAddress().getHostAddress();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset,
		final InetAddress src, final int port) throws KNXFormatException, IOException {
		final int svc = h.getServiceType();

		if (!h.isSecure()) {
			logger.trace("received insecure service type 0x{} - ignore", Integer.toHexString(svc));
			return true;
		}

		if (svc == SecureSessionResponse) {
			try {
				final Object[] res = newSessionResponse(h, data, offset);

				final int authContext = 2;
				final byte[] serverPublicKey = (byte[]) res[1];
				final byte[] auth = newSessionAuth(authContext, serverPublicKey);
				final byte[] packet = newSecurePacket(seq++, 0, auth);
				logger.debug("requesting user access {}", authContext);
				localSocket.send(new DatagramPacket(packet, packet.length, src, port));
			}
			catch (final RuntimeException e) {
				setupLoop.quit();
				logger.error("negotiating session key failed", e);
			}
		}
		else if (svc == SecureGroupSync)
			onGroupSync(src, newGroupSync(h, data, offset));
		else if (svc == SecureSvc) {
			final Object[] fields = unwrap(h, data, offset);
			final long timestamp = (long) fields[1];
			if (sessionId == 0 && !withinTolerance(src, timestamp)) {
				logger.warn("{}:{} timestamp {} outside latency tolerance of {} ms (local {}) - ignore", src,
						port, timestamp, mcastLatencyTolerance, timestamp());
				return true;
			}

			final byte[] packet = (byte[]) fields[4];
			final KNXnetIPHeader containedHeader = new KNXnetIPHeader(packet, 0);

			if (containedHeader.getServiceType() == SecureSessionStatus) {
				final int status = newChannelStatus(containedHeader, packet, containedHeader.getStructLength());
				LogService.log(logger, status == 0 ? LogLevel.TRACE : LogLevel.ERROR, "secure session {}",
						statusMsg(status));
				setupLoop.quit();
				sessionStatus = status;
				if (status != 0) // XXX do we need this throw, its swallowed by the loop anyway?
					throw new KnxSecureException("secure session " + statusMsg(status));
			}
			else {
				// let base class handle contained in decrypted knxip packet
				return super.handleServiceType(containedHeader, packet, containedHeader.getStructLength(), src, port);
			}
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

	// unicast session
	// session.req -> session.res -> auth.req -> session-status
	private void setupSecureSession(final InetSocketAddress localEP, final InetSocketAddress serverCtrlEP) throws KNXException {

		final String lock = new String(Character.toChars(0x1F512));
		logger = LoggerFactory.getLogger("calimero.knxnetip.KNX/IP " + lock + " Session " + serverCtrlEP.getAddress().getHostAddress() + ":"
				+ serverCtrlEP.getPort());

		logger.debug("setup secure session with {}", serverCtrlEP);
		generateKeyPair(privateKey, publicKey);
		try (DatagramSocket local = new DatagramSocket(localEP)) {
			localSocket = local;
			final HPAI hpai = new HPAI(HPAI.IPV4_UDP, useNat ? null : (InetSocketAddress) local.getLocalSocketAddress());
			final byte[] sessionReq = PacketHelper.newChannelRequest(hpai, publicKey);
			local.send(new DatagramPacket(sessionReq, sessionReq.length, serverCtrlEP));

			setupLoop = new ReceiverLoop(this, local, 512, 0, sessionSetupTimeout);
			setupLoop.run();
			if (sessionStatus == SessionSetup)
				throw new KNXTimeoutException("timeout establishing secure session with " + serverCtrlEP);
			if (sessionStatus != 0)
				throw new KnxSecureException("secure session " + statusMsg(sessionStatus));
		}
		catch (final IOException e) {
			throw new KNXException("I/O error establishing secure session with " + serverCtrlEP, e);
		}
		finally {
			Arrays.fill(privateKey, (byte) 0);
			Arrays.fill(publicKey, (byte) 0);
		}
	}

	private boolean withinTolerance(final InetAddress src, final long timestamp) {
		onGroupSync(src, timestamp);
		final long diff = timestamp() - timestamp;
		return diff <= mcastLatencyTolerance;
	}

	// upon receiving a group sync, we answer within [0..5] seconds using a single sync, but only iff we
	// didn't receive another sync with a greater timestamp in between
	private void onGroupSync(final InetAddress src, final long timestamp) {
		final long local = timestamp();
		if (timestamp > local) {
			logger.debug("sync timestamp +{} ms", timestamp - local);
			timestampOffset += timestamp - local;
			final long abs = local - timestampOffset;
			timestampExpiresAt = abs + timestampExpiresAfter.toMillis();
			groupSync.cancel(false);
		}

		if (timestamp >= (local - mcastLatencyTolerance)) {
			// we only consider messages sent by other nodes
			if (!syncedWithGroup && !isLocalIpAddress(src)) {
				logger.info("synchronized with group {}", getRemoteAddress().getAddress().getHostAddress());
				syncedWithGroup = true;
				groupSync.cancel(false);
			}
		}
		else if (timestamp < (local - mcastLatencyTolerance)) {
			// received old timestamp, schedule group sync
			scheduleGroupSync((int) (Math.random() / Math.nextDown(1.0) * 5_000));
		}
	}

	private boolean isLocalIpAddress(final InetAddress addr) {
		Stream<NetworkInterface> netifs = Stream.empty();
		try {
			final NetworkInterface ni = ((MulticastSocket) socket).getNetworkInterface();
			final boolean noneSet = ni.getInetAddresses().nextElement().isAnyLocalAddress();
			netifs = noneSet ? NetworkInterface.networkInterfaces() : Stream.of(ni);
		}
		catch (final SocketException e) {}
		return netifs.flatMap(ni -> ni.inetAddresses()).anyMatch(addr::equals);
	}

	private void scheduleGroupSync(final long initialDelay) {
		logger.trace("schedule group sync (initial delay {} ms)", initialDelay);
		groupSync.cancel(false);
		groupSync = groupSyncSender.scheduleWithFixedDelay(this::sendGroupSync, initialDelay, queryInterval,
				TimeUnit.MILLISECONDS);
	}

	private void sendGroupSync() {
		try {
			final long timestamp = timestamp();
			final byte[] sync = newGroupSync(timestamp);
			logger.debug("sending group sync timestamp {} ms", timestamp);
			socket.send(new DatagramPacket(sync, sync.length, dataEndpt.getAddress(), dataEndpt.getPort()));
		}
		catch (IOException | RuntimeException e) {
			logger.warn("sending group sync failed", e);
		}
	}

	private long timestamp() {
		final long now = System.nanoTime() / 1000_000L;
		return now + timestampOffset;
	}

	// for multicast, the session index = 0
	// seq: for unicast connections: monotonically increasing counter of sender.
	// seq: for multicasts, timestamp [ms]
	// msg tag: for unicasts, tag is 0
	private byte[] newSecurePacket(final long seq, final int msgTag, final byte[] knxipPacket) {
		if (seq < 0 || seq > 0xffff_ffff_ffffL)
			throw new KNXIllegalArgumentException(
					"sequence / group counter " + seq + " out of range [0..0xffffffffffff]");
		if (msgTag < 0 || msgTag > 0xffff)
			throw new KNXIllegalArgumentException("message tag " + msgTag + " out of range [0..0xffff]");

		final int svcLength = 2 + 6 + 6 + 2 + knxipPacket.length + macSize;
		final KNXnetIPHeader header = new KNXnetIPHeader(KNXnetIPHeader.SecureWrapper, svcLength);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.putShort((short) sessionId);
		buffer.putShort((short) (seq >> 32));
		buffer.putInt((int) seq);
		buffer.put(sno);
		buffer.putShort((short) msgTag);
		buffer.put(knxipPacket);

		final byte[] secInfo = securityInfo(buffer.array(), header.getStructLength() + 2, knxipPacket.length);
		final byte[] mac = cbcMac(buffer.array(), 0, buffer.position(), secInfo);
		buffer.put(mac);
		encrypt(buffer.array(), header.getStructLength() + 2 + 6 + 6 + 2, securityInfo(buffer.array(), 8, 0xff00));
		return buffer.array();
	}

	public static byte[] newSecurePacket(final long sessionId, final long seq, final byte[] sno, final int msgTag,
		final byte[] knxipPacket, final Key secretKey) {
		if (seq < 0 || seq > 0xffff_ffff_ffffL)
			throw new KNXIllegalArgumentException(
					"sequence / group counter " + seq + " out of range [0..0xffffffffffff]");
		if (msgTag < 0 || msgTag > 0xffff)
			throw new KNXIllegalArgumentException("message tag " + msgTag + " out of range [0..0xffff]");

		final int svcLength = 2 + 6 + 6 + 2 + knxipPacket.length + macSize;
		final KNXnetIPHeader header = new KNXnetIPHeader(KNXnetIPHeader.SecureWrapper, svcLength);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.putShort((short) sessionId);
		buffer.putShort((short) (seq >> 32));
		buffer.putInt((int) seq);
		buffer.put(sno);
		buffer.putShort((short) msgTag);
		buffer.put(knxipPacket);

		final byte[] secInfo = securityInfo(buffer.array(), header.getStructLength() + 2, knxipPacket.length);
		final byte[] mac = cbcMac(buffer.array(), 0, buffer.position(), secretKey, secInfo);
		buffer.put(mac);
		encrypt(buffer.array(), header.getStructLength() + 2 + 6 + 6 + 2, secretKey,
				securityInfo(buffer.array(), 8, 0xff00));
		return buffer.array();
	}

	private Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		final Object[] fields = unwrap(h, data, offset, secretKey);

		final int sid = (int) fields[0];
		if (sid != sessionId)
			throw new KnxSecureException("secure session mismatch: received ID " + sid + ", expected " + sessionId);

		final long seq = (long) fields[1];
		if (sessionId != 0 && seq < rcvSeq)
			throw new KnxSecureException("received secure packet with sequence " + seq + " < expected " + rcvSeq);
		++rcvSeq;

		final long sno = (long) fields[2];
		final int tag = (int) fields[3];
		final byte[] knxipPacket = (byte[]) fields[4];
		logger.trace("received {} (session {} seq {} S/N {} tag {})", toHex(knxipPacket, " "), sid, seq, sno, tag);
		return fields;
	}

	public static Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset, final Key secretKey)
		throws KNXFormatException {
		if ((h.getServiceType() & SecureSvc) != SecureSvc)
			throw new KNXIllegalArgumentException("not a secure service type");

		final int total = h.getTotalLength();
		final int hdrLength = h.getStructLength();
		final int minLength = hdrLength + 2 + 6 + 6 + 2 + hdrLength + macSize;
		if (total < minLength)
			throw new KNXFormatException("secure packet length < required minimum length " + minLength, total);

		final ByteBuffer buffer = ByteBuffer.wrap(data, offset, total - hdrLength);
		final int sid = buffer.getShort() & 0xffff;

		final long seq = uint48(buffer);
		final long sno = uint48(buffer);
		final int tag = buffer.getShort() & 0xffff;

		final ByteBuffer dec = decrypt(buffer, secretKey, securityInfo(data, offset + 2, 0xff00));

		final byte[] knxipPacket = new byte[total - minLength + hdrLength];
		dec.get(knxipPacket);
		final byte[] mac = new byte[macSize];
		dec.get(mac);

		final byte[] frame = Arrays.copyOfRange(data, offset - hdrLength, offset + total - hdrLength);
		System.arraycopy(knxipPacket, 0, frame, hdrLength + 2 + 6 + 6 + 2, knxipPacket.length);
		cbcMacVerify(frame, 0, total - macSize, secretKey, securityInfo(data, offset + 2, knxipPacket.length), mac);

		return new Object[] { sid, seq, sno, tag, knxipPacket };
	}

	private Object[] newSessionResponse(final KNXnetIPHeader h, final byte[] data, final int offset)
		throws KNXFormatException {

		if (h.getServiceType() != SecureSessionResponse)
			throw new IllegalArgumentException("no secure channel response");
		if (h.getTotalLength() != 0x38 && h.getTotalLength() != 0x08)
			throw new KNXFormatException("invalid length " + data.length + " for a secure channel response");

		final ByteBuffer buffer = ByteBuffer.wrap(data, offset, h.getTotalLength() - h.getStructLength());

		sessionId = buffer.getShort() & 0xffff;
		if (sessionId == 0)
			throw new KnxSecureException("no more free secure channels / remote endpoint busy");

		final byte[] serverPublicKey = new byte[keyLength];
		buffer.get(serverPublicKey);

		final byte[] sessionKey = sessionKey(keyAgreement(serverPublicKey));
		secretKey = createSecretKey(sessionKey);
		logger.trace("*** security sensitive information! using session key {} ***", toHex(sessionKey, ""));

		final ByteBuffer mac = decrypt(buffer, securityInfo(new byte[16], 0, 0xff00));
		final byte[] xor = xor(serverPublicKey, 0, publicKey, 0, keyLength);
		final byte[] verifyAgainst = cbcMacSimple(xor, 0, keyLength);
		final boolean authenticated = Arrays.equals(mac.array(), verifyAgainst);
		if (!authenticated) {
			final String packet = toHex(Arrays.copyOfRange(data, offset - 6, offset - 6 + 0x38), " ");
			throw new KnxSecureException("authentication failed for session response " + packet);
		}

		return new Object[] { sessionId, serverPublicKey };
	}

	private byte[] newSessionAuth(final int authContext, final byte[] serverPublicKey) {
		final KNXnetIPHeader header = new KNXnetIPHeader(SecureSessionAuth, 2 + macSize);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.putShort((short) authContext);

		final byte[] xor = xor(serverPublicKey, 0, publicKey, 0, keyLength);
		final byte[] mac = cbcMacSimple(xor, 0, keyLength);
		buffer.put(mac);

		return buffer.array();
	}

	private int newChannelStatus(final KNXnetIPHeader h, final byte[] data, final int offset)
		throws KNXFormatException {

		if (h.getServiceType() != SecureSessionStatus)
			throw new KNXIllegalArgumentException("no secure channel status");
		if (h.getTotalLength() != 8)
			throw new KNXFormatException("invalid length " + h.getTotalLength() + " for a secure channel status");

		// 0: auth success
		// 1: auth failed
		// 2: error unauthorized
		// 3: timeout
		final int status = data[offset] & 0xff;
		return status;
	}

	private byte[] newGroupSync(final long timestamp) {
		if (timestamp < 0 || timestamp > 0xffff_ffff_ffffL)
			throw new KNXIllegalArgumentException("timestamp " + timestamp + " out of range [0..0xffffffffffff]");

		final KNXnetIPHeader header = new KNXnetIPHeader(SecureGroupSync, 0x1e);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());

		buffer.putShort((short) (timestamp >> 32));
		buffer.putInt((int) timestamp);
		buffer.put(sno);
		final int tag = 0;
		buffer.putShort((short) tag);

		final byte[] mac = cbcMac(buffer.array(), 0, header.getStructLength() + 6 + 6 + 2, securityInfo(buffer.array(), 6, 0));
		final byte[] secInfo = securityInfo(buffer.array(), 6, 0xff00);
		encrypt(mac, 0, secInfo);
		buffer.put(mac);
		return buffer.array();
	}

	private long newGroupSync(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		if (h.getTotalLength() != 0x24)
			throw new KNXFormatException("invalid length " + data.length + " for a secure group sync");

		final ByteBuffer buffer = ByteBuffer.wrap(data, offset, h.getTotalLength() - h.getStructLength());

		final long timestamp = uint48(buffer);
		final long sno = uint48(buffer);
		final int msgTag = buffer.getShort() & 0xffff;

		final ByteBuffer mac = decrypt(buffer, securityInfo(data, offset, 0xff00));

		final byte[] secInfo = securityInfo(buffer.array(), 6, 0);
		cbcMacVerify(data, offset - h.getStructLength(), h.getTotalLength() - macSize, secretKey, secInfo, mac.array());
		logger.trace("received group sync timestamp {} ms (S/N {}, tag {})", timestamp, Long.toHexString(sno), msgTag);
		return timestamp;
	}

	private void encrypt(final byte[] data, final int offset, final byte[] secInfo) {
		encrypt(data, offset, secretKey, secInfo);
	}

	public static void encrypt(final byte[] data, final int offset, final Key secretKey, final byte[] secInfo) {
		try {
			final ByteBuffer encrypt = ByteBuffer.wrap(data, offset, data.length - offset);
			final ByteBuffer result = cipher(encrypt, secretKey, secInfo);
			System.arraycopy(result.array(), 0, data, offset, result.remaining());
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("encrypting error", e);
		}
	}

	private ByteBuffer decrypt(final ByteBuffer buffer, final byte[] secInfo) {
		return decrypt(buffer, secretKey, secInfo);
	}

	private static ByteBuffer decrypt(final ByteBuffer buffer, final Key secretKey, final byte[] secInfo) {
		try {
			return cipher(buffer, secretKey, secInfo);
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("decrypting error", e);
		}
	}

	private static ByteBuffer cipher(final ByteBuffer buffer, final Key secretKey, final byte[] secInfo)
		throws GeneralSecurityException {
		final int blocks = (buffer.remaining() + 0xf) >> 4;
		final byte[] cipher = cipherStream(blocks, secretKey, secInfo);

		final ByteBuffer result = ByteBuffer.allocate(buffer.remaining());
		if (blocks > 1) {
			for (int i = 0; result.remaining() > macSize; i++)
				result.put((byte) (buffer.get() ^ cipher[macSize + i]));
		}
		for (int i = 0; result.hasRemaining(); i++)
			result.put((byte) (buffer.get() ^ cipher[i]));
		return result.flip();
	}

	private static byte[] cipherStream(final int blocks, final Key secretKey, final byte[] secInfo)
		throws GeneralSecurityException {
		final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		final int blockSize = 16;
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i = 0; i < blocks; i++) {
			final byte[] output = cipher.update(secInfo);
			baos.write(output, 0, blockSize);
			++secInfo[15];
		}
		return baos.toByteArray();
	}

	private static void cbcMacVerify(final byte[] data, final int offset, final int length, final Key secretKey,
		final byte[] secInfo, final byte[] verifyAgainst) {
		final byte[] mac = cbcMac(data, offset, length, secretKey, secInfo);
		final boolean authenticated = Arrays.equals(mac, verifyAgainst);
		if (!authenticated) {
			final String packet = toHex(Arrays.copyOfRange(data, offset, offset + length), " ");
			throw new KnxSecureException("authentication failed for " + packet);
		}
	}

	private byte[] cbcMac(final byte[] data, final int offset, final int length, final byte[] secInfo) {
		final byte[] log = Arrays.copyOfRange(data, offset, offset + length);
		logger.trace("authenticating (length {}): {}", length, toHex(log, " "));

		final byte[] hdr = Arrays.copyOfRange(data, offset, offset + 6);

		final int packetOffset = hdr.length + 2 + 6 + 6 + 2;
		byte[] session = new byte[0];
		byte[] frame = new byte[0];
		if (length > packetOffset) {
			session = Arrays.copyOfRange(data, offset + 6, offset + 8);
			frame = Arrays.copyOfRange(data, offset + packetOffset, offset + length);
		}

		try {
			final Cipher cipher = Cipher.getInstance("AES/CBC/ZeroBytePadding");
			final IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			cipher.update(secInfo);

			final byte[] lenBuf = new byte[] { 0, (byte) (hdr.length + session.length) };
			cipher.update(lenBuf);
			cipher.update(hdr);
			cipher.update(session);

			final byte[] result = cipher.doFinal(frame);
			final byte[] mac = Arrays.copyOfRange(result, result.length - macSize, result.length);
			return mac;
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("calculating CBC-MAC of " + toHex(log, " "), e);
		}
	}

	private static byte[] cbcMac(final byte[] data, final int offset, final int length, final Key secretKey,
		final byte[] secInfo) {
		final byte[] log = Arrays.copyOfRange(data, offset, offset + length);

		final byte[] hdr = Arrays.copyOfRange(data, offset, offset + 6);

		final int packetOffset = hdr.length + 2 + 6 + 6 + 2;
		byte[] session = new byte[0];
		byte[] frame = new byte[0];
		if (length > packetOffset) {
			session = Arrays.copyOfRange(data, offset + 6, offset + 8);
			frame = Arrays.copyOfRange(data, offset + packetOffset, offset + length);
		}

		try {
			final Cipher cipher = Cipher.getInstance("AES/CBC/ZeroBytePadding");
			final IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			cipher.update(secInfo);

			final byte[] lenBuf = new byte[] { 0, (byte) (hdr.length + session.length) };
			cipher.update(lenBuf);
			cipher.update(hdr);
			cipher.update(session);

			final byte[] result = cipher.doFinal(frame);
			final byte[] mac = Arrays.copyOfRange(result, result.length - macSize, result.length);
			return mac;
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("calculating CBC-MAC of " + toHex(log, " "), e);
		}
	}

	private byte[] cbcMacSimple(final byte[] data, final int offset, final int length) {
		final byte[] log = Arrays.copyOfRange(data, offset, offset + length);
		logger.trace("authenticating (length {}): {}", length, toHex(log, " "));

		try {
			final Cipher cipher = Cipher.getInstance("AES/CBC/ZeroBytePadding");
			final IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
			cipher.init(Cipher.ENCRYPT_MODE, deviceAuthKey, ivSpec);

			final byte[] result = cipher.doFinal(data, offset, length);
			final byte[] mac = Arrays.copyOfRange(result, result.length - macSize, result.length);
			return mac;
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("calculating CBC-MAC of " + toHex(log, " "), e);
		}
	}

	private static byte[] securityInfo(final byte[] data, final int offset, final int lengthInfo) {
		final byte[] secInfo = Arrays.copyOfRange(data, offset, offset + 16);
		secInfo[14] = (byte) (lengthInfo >> 8);
		secInfo[15] = (byte) lengthInfo;
		return secInfo;
	}

	private static String statusMsg(final int status) {
		final String[] msg = { "authorization success", "authorization failed", "unauthorized", "timeout" };
		if (status > 3)
			return "unknown status " + status;
		return msg[status];
	}

	private static Key createSecretKey(final byte[] key) {
		if (key.length != 16)
			throw new KNXIllegalArgumentException("KNX group key has to be " + macSize + " bytes in length");
		return new SecretKeySpec(key, 0, key.length, "AES");
	}

	private static void generateKeyPair(final byte[] privateKey, final byte[] publicKey) {
		new SecureRandom().nextBytes(privateKey);
		X25519.scalarMultBase(privateKey, 0, publicKey, 0);
	}

	private byte[] keyAgreement(final byte[] serverPublicKey) {
		final byte[] sharedSecret = new byte[keyLength];
		X25519.scalarMult(privateKey, 0, serverPublicKey, 0, sharedSecret, 0);
		return sharedSecret;
	}

	private static byte[] sessionKey(final byte[] sharedSecret) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(sharedSecret);
			return Arrays.copyOfRange(hash, 0, 16);
		}
		catch (final NoSuchAlgorithmException e) {
			// every platform is required to support SHA-256
			throw new KnxSecureException("platform does not support SHA-256 algorithm", e);
		}
	}

	private static byte[] xor(final byte[] a, final int offsetA, final byte[] b, final int offsetB, final int len) {
		if (a.length - len < offsetA || b.length - len < offsetB)
			throw new KNXIllegalArgumentException("illegal offset or length");
		final byte[] res = new byte[len];
		for (int i = 0; i < len; i++)
			res[i] = (byte) (a[i + offsetA] ^ b[i + offsetB]);
		return res;
	}

	private static long uint48(final ByteBuffer buffer) {
		long l = (buffer.getShort() & 0xffffL) << 32;
		l |= buffer.getInt() & 0xffffffffL;
		return l;
	}
}

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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogService.LogLevel;

final class SecureConnection extends KNXnetIPRouting {

	private static final int SecureSvc = 0x0950;
	private static final int SecureSessionResponse = 0x0952;
	private static final int SecureSessionStatus = 0x0954;
	private static final int SecureGroupSync = 0x0955;

	static final int aesKeySize = 128; // [bits]
	private static final int macSize = 16; // [bytes]
	private static final int keyLength = 36; // [bytes]

	private final byte[] ecdhPublicKey;


	// tunneling connection setup

	// timeout channel.req -> channel.res
	private static final int channelSetupTimeout = 30; // [s]

	private byte[] serverPublicKey;
	private int sessionId;


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

	private byte[] groupKey;
	private final Key secretKey;

	// multicast timestamp sync
	private static final Duration timestampExpiresAfter = Duration.ofMinutes(60 + new Random().nextInt(11));
	private long timestampExpiresAt = 0 + timestampExpiresAfter.toMillis();
	// offset to adjust local clock upon receiving a valid secure packet or group sync response
	private long timestampOffset = -System.nanoTime() / 1_000_000L; // [ms]
	private static final long queryInterval = 10_000; // [ms]

	private final int mcastLatencyTolerance = 3000; // [ms]



	public static KNXnetIPConnection newTunneling(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat) {
		return null;
	};

	public static KNXnetIPConnection newRouting(final NetworkInterface netIf, final InetAddress mcGroup)
		throws KNXException {
		return new SecureConnection(netIf, mcGroup);
	};

	private SecureConnection(final NetworkInterface netIf, final InetAddress mcGroup) throws KNXException {
		super(netIf, mcGroup);

		ecdhPublicKey = null;
//		final byte[] key = fromHex("374708fff7719dd5979ec875d56cd228");
		final byte[] key = new byte[16];
		secretKey = createSecretKey(key);
		scheduleGroupSync(new Random().nextInt(10_000));
	}

	@Override
	public void send(final CEMI frame, final BlockingMode mode) throws KNXConnectionClosedException {
		final long seq = 0;
		final int tag = 0;

		final byte[] knxip = PacketHelper.toPacket(new ServiceRequest(serviceRequest, channelId, getSeqSend(), frame));
		final byte[] wrapped = newSecurePacket(seq, tag, knxip);

		// XXX we have to forward to standard send, not this one
		send(wrapped);
	}

	@Override
	public String getName() {
		final String lock = new String(Character.toChars(0x1F512));
		return "KNX/IP " + lock + " Routing " + ctrlEndpt.getAddress().getHostAddress() + ":" + ctrlEndpt.getPort();
	}

	@Override
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset,
		final InetAddress src, final int port) throws KNXFormatException, IOException {
		final int svc = h.getServiceType();

		if (!PacketHelper.isKnxSecure(h)) {
			logger.trace("received insecure service type 0x{} - ignore", Integer.toHexString(svc));
			return false;
		}

		if (svc == SecureSessionResponse) {
			try {
				final Object[] res = newChannelResponse(h, data, offset);
				sessionId = (int) res[0];
				serverPublicKey = (byte[]) res[1];

				final int authContext = 2;
				final byte[] mac = null;
				final byte[] auth = PacketHelper.newChannelAuth(sessionId, authContext, mac);
				final byte[] packet = newSecurePacket(1, 0, auth);
				// NYI send auth
			}
			catch (final KNXRemoteException e) {
				// TODO wrap this exception at origin because we don't support it in our method signature
				logger.error("server session response error", e);
			}
		}
		else if (svc == SecureGroupSync)
			onGroupSync(newGroupSync(h, data, offset));
		else if (svc == SecureSvc) {
			// NYI verify fields
			final Object[] fields = unwrap(h, data, offset);
			final long timestamp = (long) fields[1];
			if (!withinTolerance(timestamp)) {
				logger.warn("{}:{} timestamp {} outside latency tolerance of {} ms (local {}) - ignore", src,
						port, timestamp, mcastLatencyTolerance, timestamp());
				return true;
			}

			final byte[] packet = (byte[]) fields[4];
			final KNXnetIPHeader containedHeader = new KNXnetIPHeader(packet, 0);

			if (containedHeader.getServiceType() == SecureSessionStatus) {
				final int status = newChannelStatus(h, data, offset);
				LogService.log(logger, status == 0 ? LogLevel.TRACE : LogLevel.ERROR, "secure session {}",
						statusMsg(status));
			}
			else {
				// let base class handle contained in decrypted knxip packet
				return super.handleServiceType(containedHeader, packet, containedHeader.getStructLength(), src,
						port);
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
	private void setupSecureChannel() {
		// session.req -> session.res -> auth.req -> session-status

		// send
		final HPAI hpai = null;
		sessionRequest(hpai);
		// waitForStateChange
	}

	private byte[] sessionRequest(final HPAI hpai)  {
		return PacketHelper.newChannelRequest(hpai, ecdhPublicKey);
	}

	private boolean withinTolerance(final long timestamp) {
		onGroupSync(timestamp);
		final long diff = timestamp() - timestamp;
		return diff <= mcastLatencyTolerance;
	}

	// upon receiving a group sync, we answer within [0..5] seconds using a single sync, but only iff we
	// didn't receive another sync with a greater timestamp in between
	private void onGroupSync(final long timestamp) {
		final long local = timestamp();
		if (timestamp > local) {
			logger.debug("sync timestamp +{} ms", timestamp - local);
			timestampOffset += timestamp - local;
			final long abs = local - timestampOffset;
			timestampExpiresAt = abs + timestampExpiresAfter.toMillis();
			groupSync.cancel(false);
		}
		else if (timestamp < (local - mcastLatencyTolerance)) {
			// received old timestamp, schedule group sync
			scheduleGroupSync(new Random().nextInt(5_000));
		}
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

	// for multicast, the channel index = 0
	// seq: for unicast connections: monotonically increasing counter of sender.
	// seq: for multicasts, time stamp [ms]
	// domainMask: for unicasts, mask is 0
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
		final long sno = 0;
		buffer.putShort((short) (sno >> 32));
		buffer.putInt((int) sno);
		buffer.putShort((short) msgTag);
		buffer.put(knxipPacket);

		final byte[] mac = cbcMac(buffer.array(), 0, buffer.position(), seq);
		buffer.put(mac);
		encrypt(buffer.array(), header.getStructLength() + 2 + 6 + 6 + 2);
		return buffer.array();
	}

	private Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		if ((h.getServiceType() & SecureSvc) != SecureSvc)
			throw new KNXIllegalArgumentException("not a secure service type");

		final int total = h.getTotalLength();
		final int hdrLength = h.getStructLength();
		final int minLength = hdrLength + 2 + 6 + 6 + 2 + hdrLength + macSize;
		if (total < minLength)
			throw new KNXFormatException("secure packet length < required minimum length " + minLength, total);

		final ByteBuffer buffer = ByteBuffer.wrap(data, offset, total - hdrLength);
		final int sid = buffer.getShort() & 0xffff;
		if (sid != sessionId)
			throw new KNXFormatException("secure session mismatch (expected ID " + sid + ")", "" + sid);

		final long seq = uint48(buffer);
		final long sno = uint48(buffer);
		final int tag = buffer.getShort() & 0xffff;
		final ByteBuffer dec = decrypt(buffer, seq);

		final byte[] knxipPacket = new byte[total - minLength + hdrLength];
		logger.trace("unwrap lengths: total {}, min {}, remaining {}, packet {}", total, minLength, dec.remaining(),
				knxipPacket.length);
		dec.get(knxipPacket);
		final byte[] mac = new byte[macSize];
		dec.get(mac);
		// NYI check mac

		logger.trace("received {} (session {} seq {} S/N {} tag {})", DataUnitBuilder.toHex(knxipPacket, " "), sid, seq,
				sno, tag);

		return new Object[] { sid, seq, sno, tag, knxipPacket };
	}

	private Object[] newChannelResponse(final KNXnetIPHeader h, final byte[] data, final int offset)
		throws KNXFormatException, KNXRemoteException {

		if (h.getServiceType() != SecureSessionResponse)
			throw new IllegalArgumentException("no secure channel response");
		if (h.getTotalLength() != 0x3c && h.getTotalLength() != 0x08)
			throw new KNXFormatException("invalid length " + data.length + " for a secure channel response");

		final int start = offset + h.getStructLength();
		final ByteBuffer buffer = ByteBuffer.wrap(data, start, data.length - start);

		sessionId = buffer.getShort() & 0xffff;
		if (sessionId == 0)
			throw new KNXRemoteException("no more free secure channels / remote endpoint busy");

		serverPublicKey = new byte[keyLength];
		buffer.get(serverPublicKey);
		final byte[] aes128 = new byte[macSize];
		buffer.get(aes128);

		// NYI
		final byte[] clientPublicKey = ecdhPublicKey;
		final byte[] sessionKey = new byte[16];

		final byte[] xor = xor(serverPublicKey, 0, clientPublicKey, 0, serverPublicKey.length);
		final byte[] mac = cbcMac(xor, 0, serverPublicKey.length, 0);

		// TODO compare encryped mac

		return new Object[] { sessionId, serverPublicKey };
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
		final int status = data[offset + h.getStructLength()] & 0xff;
//		if (status > 3)
//			; // log warning or throw with unknown status
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

		final long sno = 0;
		buffer.putShort((short) (sno >> 32));
		buffer.putInt((int) sno);

		final int tag = 0;
		buffer.putShort((short) tag);

		final byte[] mac = cbcMac(buffer.array(), 0, header.getStructLength() + 6 + 6 + 2, timestamp);
		encrypt(mac, 0);
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
		final ByteBuffer mac = decrypt(buffer, timestamp);

		// NYI check mac
		final byte[] cmp = cbcMac(data, offset - h.getStructLength(), h.getTotalLength() - macSize, timestamp);
		logger.trace("MAC\n\trcv {}\n\tcmp {}", DataUnitBuilder.toHex(mac.array(), ""), DataUnitBuilder.toHex(cmp, ""));

		logger.trace("received group sync timestamp {} ms (S/N {}, tag {})", timestamp, Long.toHexString(sno), msgTag);
		return timestamp;
	}

	private static Key createSecretKey(final byte[] key) {
		return new SecretKeySpec(key, 0, key.length, "AES");
	}

	private void encrypt(final byte[] data, final int offset) {
		// NYI
		try {
			final ByteBuffer ts = ByteBuffer.allocate(8);
			ts.putLong(timestamp()).flip().position(2);
			final byte[] tsdata = new byte[6];
			ts.get(tsdata);

			final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			final byte[] t0 = Arrays.copyOf(tsdata, 16);
			final int counter = 0;
			t0[15] = counter;
			final IvParameterSpec ivSpec = new IvParameterSpec(t0);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			final byte[] cipherText = cipher.doFinal(data, offset, data.length - offset);
			System.arraycopy(cipherText, 0, data, offset, cipherText.length);
		}
		catch (final GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ByteBuffer decrypt(final ByteBuffer buffer, final long seq) {
		final ByteBuffer plain = ByteBuffer.allocate(buffer.remaining());

		try {
			final ByteBuffer ts = ByteBuffer.allocate(8);
			ts.putLong(seq).flip().position(2);
			final byte[] tsdata = new byte[6];
			ts.get(tsdata);

			final byte[] t0 = Arrays.copyOf(tsdata, 16);

			final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			final IvParameterSpec ivSpec = new IvParameterSpec(t0);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

			logger.trace("decrypt length {}", buffer.remaining());
			cipher.doFinal(buffer, plain);
			plain.flip();
		}
		catch (final GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return plain;
	}

	// calculated using session (for unicast) or group key (for multicast)
	private byte[] cbcMac(final byte[] data, final int offset, final int length, final long seq) {
		// NYI
		logger.trace("calculating CBC-MAC for (length {}): {}", length,
				DataUnitBuilder.toHex(Arrays.copyOfRange(data, offset, offset + length), " "));
		try {
			final ByteBuffer ts = ByteBuffer.allocate(8);
			ts.putLong(seq).flip().position(2);
			final byte[] tsdata = new byte[6];
			ts.get(tsdata);

			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

			final byte[] t0 = new byte[16];
			final IvParameterSpec ivSpec = new IvParameterSpec(t0);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			final byte[] mac = new byte[macSize];
			return mac;
		}
		catch (final GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static String statusMsg(final int status) {
		final String[] msg = { "authorization success", "authorization failed", "unauthorized", "timeout" };
		if (status > 3)
			return "unknown status " + status;
		return msg[status];
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

	private static byte[] fromHex(final String s) {
		final byte[] data = new byte[s.length() / 2];
		for (int i = 0; i < s.length(); i += 2)
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		return data;
	}
}

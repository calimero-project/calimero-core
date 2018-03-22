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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogService.LogLevel;

final class SecureConnection {

	private static final int SecureSvc = 0xaa00;

	private static final int SecureChannelResponse = 0xaa02;
	private static final int SecureChannelStatus = 0xaa04;
	private static final int SecureGroupSyncRequest = 0xaa06;
	private static final int SecureGroupSyncResponse = 0xaa07;

	private static final int aesKeySize = 128; // [bits]
	private static final int macSize = 16; // [bytes]
	private static final int keyLength = 36; // [bytes]

	private final Logger logger = LoggerFactory.getLogger("[temp logger] Knx-Secure");

	private final byte[] ecdhPublicKey;


	// tunneling connection setup

	// timeout channel.req -> channel.res
	private static final int channelSetupTimeout = 30; // [s]

	private byte[] serverPublicKey;
	private int channelIdx;


	// routing connection setup

	private byte[] groupKey;

	// multicast timestamp sync
	private static final Duration timestampExpiresAfter = Duration.ofMinutes(60 + new Random().nextInt(11));
	private long timestampExpiresAt = 0 + timestampExpiresAfter.toMillis();
	// offset to adjust local clock upon receiving a valid secure packet or group sync response
	private long timestampOffset = -System.nanoTime() / 1_000_000L; // [ms]

	private final int mcastLatencyTolerance = 3000; // [ms]


	public static KNXnetIPConnection newTunneling(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat) {
		return null;
	};

	public static KNXnetIPConnection newRouting(final NetworkInterface netIf, final InetAddress mcGroup) {
		return null;
	};

	private SecureConnection() {
		ecdhPublicKey = null;
	}

	private void setupSecureChannel() {
		// channel.req -> channel.res -> auth.req -> channel.status

		// send connectRequest(hpai);
		// waitForStateChange
	}

	private byte[] connectRequest(final HPAI hpai)  {
		return PacketHelper.newChannelRequest(hpai, ecdhPublicKey);
	}

//	@Override
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset,
		final InetAddress src, final int port) throws KNXFormatException, KNXRemoteException {
		final int svc = h.getServiceType();

		if (!PacketHelper.isKnxSecure(h)) {
			logger.trace("received insecure service type 0x{} - ignore", Integer.toHexString(svc));
			return false;
		}

		if (svc == SecureChannelResponse) {
			final Object[] res = newChannelResponse(h, data, offset);
			channelIdx = (int) res[0];
			serverPublicKey = (byte[]) res[1];

			final int authContext = 2;
			final byte[] mac = null;
			final byte[] auth = PacketHelper.newChannelAuth(channelIdx, authContext, mac);
			final byte[] packet = newSecurePacket(channelIdx, 1, 0, auth);

			// NYI send auth
		}
		else if (svc == SecureGroupSyncRequest)
			onGroupSync(true, newGroupSync(h, data, offset));
		else if (svc == SecureGroupSyncResponse)
			onGroupSync(false, newGroupSync(h, data, offset));
		else if (svc == KNXnetIPHeader.SecureWrapper) {
			// NYI verify fields
			final Object[] fields = unwrap(h, data, offset);
			final byte[] knxipPacket = (byte[]) fields[3];
			final KNXnetIPHeader containedHeader = new KNXnetIPHeader(knxipPacket, 0);

			if (containedHeader.getServiceType() == SecureChannelStatus) {
				final int status = newChannelStatus(h, data, offset);
				LogService.log(logger, status == 0 ? LogLevel.TRACE : LogLevel.ERROR, "secure channel {}",
						statusMsg(status));
			}
			else {
				// let base class handle contained decrypted knxip packet
//				return super.handleServiceType(containedHeader, knxipPacket, containedHeader.getStructLength(), src, port);
			}
		}
		else
			logger.warn("received unsupported secure service type 0x{} - ignore", Integer.toHexString(svc));

		return false;
	}

	// upon receiving a sync request, we answer within [0..5] seconds using a single sync response, only iff we
	// didn't receive another response with a greater timestamp in between
	private void onGroupSync(final boolean request, final long timestamp) {
		boolean scheduleResponse = request;

		final long now = System.nanoTime() / 1000_000L;
		if (timestamp > now + timestampOffset) {
			timestampOffset = timestamp - now;
			timestampExpiresAt = now + timestampExpiresAfter.toMillis();
			if (!request)
				scheduleResponse = false;
		}
	}

	private static String statusMsg(final int status) {
		final String[] msg = { "authorization success", "authorization failed", "unauthorized", "timeout" };
		if (status > 3)
			return "unknown status " + status;
		return msg[status];
	}

	// for multicast, the channel index = 0
	// seq: for unicast connections: monotonically increasing counter of sender.
	// seq: for multicasts, time stamp [ms]
	// domainMask: for unicasts, mask is 0
	private byte[] newSecurePacket(final int channelIdx, final long seq, final int domainMask,
		final byte[] knxipPacket) {
		if (channelIdx < 0 || channelIdx > 0xffff)
			throw new KNXIllegalArgumentException("secure channel index " + channelIdx + " out of range [0..0xffff]");
		if (seq < 0 || seq > 0xffff_ffff_ffffL)
			throw new KNXIllegalArgumentException(
					"sequence / group counter " + seq + " out of range [0..0xffffffffffff]");
		if (domainMask < 0 || domainMask > 0xffff)
			throw new KNXIllegalArgumentException("security domain mask " + domainMask + " out of range [0..0xffff]");

		final int svcLength = 2 + 6 + 2 + knxipPacket.length + macSize;
		final KNXnetIPHeader header = new KNXnetIPHeader(KNXnetIPHeader.SecureWrapper, svcLength);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.putShort((short) channelIdx);
		buffer.putShort((short) (seq >> 32));
		buffer.putInt((int) seq);
		buffer.putShort((short) domainMask);
		buffer.put(knxipPacket);

		final int offset = header.getStructLength() + 2 + 6;
		final byte[] mac = cbcMac(buffer.array(), offset, 2 + knxipPacket.length);
		buffer.put(mac);
		encrypt(buffer.array(), header.getStructLength() + 2 + 6);
		return buffer.array();
	}

	private Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		if ((h.getServiceType() & SecureSvc) != SecureSvc)
			throw new KNXIllegalArgumentException("not a secure service type");

		final int minLength = h.getStructLength() + 2 + 6 + 2 + h.getStructLength() + macSize;
		if (h.getTotalLength() < minLength)
			throw new KNXFormatException("secure packet length < required minimum length " + minLength,
					h.getTotalLength());

		final int length = 0;
		final ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
		final int channelIdx = buffer.getShort() & 0xffff;
		long seq = (buffer.getShort() & 0xffff) << 32;
		seq |= buffer.getInt() & 0xffffffffL;
		final ByteBuffer dec = decrypt(buffer);

		final int domainMask = dec.getShort() & 0xffff;
		final byte[] knxipPacket = new byte[h.getTotalLength() - minLength + h.getStructLength()];
		dec.get(knxipPacket);
		final byte[] mac = new byte[macSize];
		dec.get(mac);
		// NYI check mac

		return new Object[] { channelIdx, seq, domainMask, knxipPacket };
	}

	private Object[] newChannelResponse(final KNXnetIPHeader h, final byte[] data, final int offset)
		throws KNXFormatException, KNXRemoteException {

		if (h.getServiceType() != SecureChannelResponse)
			throw new IllegalArgumentException("no secure channel response");
		if (h.getTotalLength() != 0x3c && h.getTotalLength() != 0x08)
			throw new KNXFormatException("invalid length " + data.length + " for a secure channel response");

		final int start = offset + h.getStructLength();
		final ByteBuffer buffer = ByteBuffer.wrap(data, start, data.length - start);

		channelIdx = buffer.getShort() & 0xffff;
		if (channelIdx == 0)
			throw new KNXRemoteException("no more free secure channels / remote endpoint busy");

		serverPublicKey = new byte[keyLength];
		buffer.get(serverPublicKey);
		final byte[] aes128 = new byte[macSize];
		buffer.get(aes128);

		// NYI
		final byte[] clientPublicKey = ecdhPublicKey;
		final byte[] sessionKey = new byte[16];

		final BigInteger xor = new BigInteger(serverPublicKey).xor(new BigInteger(clientPublicKey));
		final byte[] mac = cbcMac(xor.toByteArray(), 0, serverPublicKey.length);

		// TODO compare encryped mac

		return new Object[] { channelIdx, serverPublicKey };
	}

	private int newChannelStatus(final KNXnetIPHeader h, final byte[] data, final int offset)
		throws KNXFormatException {

		if (h.getServiceType() != SecureChannelStatus)
			throw new KNXIllegalArgumentException("no secure channel status");
		if (h.getTotalLength() != 8)
			throw new KNXFormatException("invalid length " + h.getTotalLength() + " for a secure channel status");

		// 0: auth success
		// 1: auth failed
		// 2: error unauthorized
		// 3: timeout
		final int status = data[offset + h.getStructLength()] & 0xff;
		if (status > 3)
			; // log warning or throw with unknown status
		return status;
	}

	private byte[] newGroupSync(final boolean request, final long timestamp) {
		if (timestamp < 0 || timestamp > 0xffff_ffff_ffffL)
			throw new KNXIllegalArgumentException("timestamp " + timestamp + " out of range [0..0xffffffffffff]");

		final KNXnetIPHeader header = new KNXnetIPHeader(request ? SecureGroupSyncRequest : SecureGroupSyncResponse, 22);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.putShort((short) (timestamp >> 32));
		buffer.putInt((int) timestamp);

		final byte[] mac = cbcMac(buffer.array(), 0, header.getStructLength() + 6);
		buffer.put(mac);
		return buffer.array();
	}

	private long newGroupSync(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		if (h.getTotalLength() != 0x1c)
			throw new KNXFormatException("invalid length " + data.length + " for a secure group sync");

		long timestamp = 0;
		for (int i = 0; i < 6; i++)
			timestamp = (timestamp << 8) | (data[offset + i] & 0xff);
		final byte[] mac = Arrays.copyOfRange(data, offset + 6, data.length);
		// NYI check mac
		final byte[] cmp = cbcMac(data, offset - h.getStructLength(), h.getStructLength() + 6);
		return timestamp;
	}

	private void encrypt(final byte[] data, final int offset) {
		// NYI
	}

	private ByteBuffer decrypt(final ByteBuffer buffer) {
		// NYI
		return null;
	}

	// calculated using session (for unicast) or group key (for multicast)
	private byte[] cbcMac(final byte[] data, final int offset, final int length) {
		// NYI
		return new byte[macSize];
	}
}

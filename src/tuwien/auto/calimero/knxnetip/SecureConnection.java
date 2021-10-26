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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.time.Duration;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.SerialNumber;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.knxnetip.TcpConnection.SecureSession;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.secure.KnxSecureException;

/**
 * Provides KNX IP Secure routing and tunneling connections. All methods involving cryptographic
 * procedures might throw {@link KnxSecureException} in case of cryptographic setup/algorithm errors.
 */
public final class SecureConnection {

	static final int SecureSvc = 0x0950;
	static final int SecureSessionResponse = 0x0952;
	static final int SecureSessionAuth = 0x0953;
	static final int SecureSessionStatus = 0x0954;

	static final int macSize = 16; // [bytes]
	static final int keyLength = 32; // [bytes]

	static final String secureSymbol = new String(Character.toChars(0x1F512));


	private SecureConnection() {}

	public static KNXnetIPTunnel newTunneling(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
			final InetSocketAddress serverCtrlEP, final boolean useNat, final byte[] deviceAuthCode, final int userId,
			final byte[] userKey) throws KNXException, InterruptedException {

		final byte[] devAuth = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
		final var tunnelingAddress = KNXMediumSettings.BackboneRouter;
		final var udp = new SecureSessionUdp(userId, userKey, devAuth, serverCtrlEP);
		return new SecureTunnelUdp(knxLayer, localEP, serverCtrlEP, useNat, tunnelingAddress, udp);
	}

	/**
	 * Creates a new KNX IP secure tunneling connection within {@code session}.
	 *
	 * @param knxLayer tunneling layer
	 * @param session secure session with remote endpoint
	 * @param tunnelingAddress KNX device address of local endpoint, the requested address needs to be an element of the
	 *        set of additional addresses offered by the session's remote endpoint (server); use {@code 0.0.0} for any
	 *        suitable address
	 * @return new KNXnet/IP tunnel
	 * @throws KNXException on errors setting up the session (if necessary) or establishing the tunnel
	 * @throws InterruptedException on thread interrupt
	 */
	public static KNXnetIPTunnel newTunneling(final TunnelingLayer knxLayer, final SecureSession session,
			final IndividualAddress tunnelingAddress) throws KNXException, InterruptedException {
		session.ensureOpen();
		return new SecureTunnel(session, knxLayer, tunnelingAddress);
	}

	/**
	 * Creates a new KNX IP secure routing connection.
	 *
	 * Implementation note: the connection acquires an authentic timer value after joining the requested multicast
	 * group. For example, given a latency tolerance of 2 seconds, this adds a worst-case upper bound of 6.5 seconds,
	 * before the connection can be used.
	 *
	 * @param netIf network interface to join the multicast group
	 * @param mcGroup a valid KNX multicast address (see {@link KNXnetIPRouting#isValidRoutingMulticast(InetAddress)}
	 * @param groupKey secure group key (backbone key), {@code groupKey.length == 16}
	 * @param latencyTolerance the acceptance window for incoming secure multicasts having a past multicast timer value;
	 *        <code>0 &lt; latencyTolerance.toMillis() &le; 8000</code>, depending on max. end-to-end network latency
	 * @return new secure routing connection
	 * @throws KNXException if creation or initialization of the multicast socket failed
	 */
	public static KNXnetIPRouting newRouting(final NetworkInterface netIf, final InetAddress mcGroup,
			final byte[] groupKey, final Duration latencyTolerance) throws KNXException {
		return new SecureRouting(netIf, mcGroup, groupKey, latencyTolerance);
	}

	public static KNXnetIPDevMgmt newDeviceManagement(final InetSocketAddress localEP,
			final InetSocketAddress serverCtrlEP, final boolean useNat, final byte[] deviceAuthCode,
			final byte[] userKey) throws KNXException, InterruptedException {

		final byte[] devAuth = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
		final var udp = new SecureSessionUdp(1, userKey, devAuth, serverCtrlEP);
		return new SecureDeviceManagementUdp(localEP, serverCtrlEP, useNat, udp);
	}

	/**
	 * Creates a new KNX IP secure device management connection within {@code session}.
	 *
	 * @param session secure session with remote endpoint
	 * @return new KNXnet/IP device management connection
	 *
	 * @throws KNXException on errors setting up the session (if necessary) or establishing the connection
	 * @throws InterruptedException on thread interrupt
	 */
	public static KNXnetIPDevMgmt newDeviceManagement(final SecureSession session)
			throws KNXException, InterruptedException {
		session.ensureOpen();
		return new SecureDeviceManagement(session);
	}

	/**
	 * Creates the hash value for the given password using PBKDF2 with the HMAC-SHA256 hash function. Before returning,
	 * this method fills the {@code password} array with zeros.
	 *
	 * @param password input user password interpreted using the US-ASCII character encoding, the replacement for
	 *        unknown or non-printable characters is '?'
	 * @return the derived 16 byte hash value
	 */
	public static byte[] hashUserPassword(final char[] password) {
		final byte[] salt = "user-password.1.secure.ip.knx.org".getBytes(StandardCharsets.US_ASCII);
		return pbkdf2WithHmacSha256(password, salt);
	}

	/**
	 * Creates the hash value for the given device authentication password, for use as device authentication code.
	 * This method uses PBKDF2 with the HMAC-SHA256 hash function; before this method returns, {@code password}
	 * is filled with zeros.
	 *
	 * @param password input device authentication password interpreted using the US-ASCII character encoding, the
	 *        replacement for unknown or non-printable characters is '?'
	 * @return the derived device authentication code as 16 byte hash value
	 */
	public static byte[] hashDeviceAuthenticationPassword(final char[] password) {
		final byte[] salt = "device-authentication-code.1.secure.ip.knx.org".getBytes(StandardCharsets.US_ASCII);
		return pbkdf2WithHmacSha256(password, salt);
	}

	private static byte[] pbkdf2WithHmacSha256(final char[] password, final byte[] salt) {
		for (int i = 0; i < password.length; i++) {
			final char c = password[i];
			if (c < 0x20 || c > 0x7E)
				password[i] = '?';
		}

		final int iterations = 65_536;
		final int keyLength = 16 * 8;
		try {
			final SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			final PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
			final SecretKey key = skf.generateSecret(spec);
			return key.getEncoded();
		}
		catch (final GeneralSecurityException e) {
			// NoSuchAlgorithmException or InvalidKeySpecException, both imply a setup/programming error
			throw new KnxSecureException("PBKDF2WithHmacSHA256", e);
		}
		finally {
			Arrays.fill(password, (char) 0);
		}
	}

	// for multicast, the session = 0
	// seq: for unicast connections: monotonically increasing counter of sender.
	// seq: for multicasts, timestamp [ms]
	// msg tag: for unicasts, tag is 0
	public static byte[] newSecurePacket(final long sessionId, final long seq, final SerialNumber sno, final int msgTag,
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
		buffer.put(sno.array());
		buffer.putShort((short) msgTag);
		buffer.put(knxipPacket);

		final byte[] secInfo = securityInfo(buffer.array(), header.getStructLength() + 2, knxipPacket.length);
		final byte[] mac = cbcMac(buffer.array(), 0, buffer.position(), secretKey, secInfo);
		buffer.put(mac);
		encrypt(buffer.array(), header.getStructLength() + 2 + 6 + 6 + 2, secretKey,
				securityInfo(buffer.array(), 8, 0xff00));
		return buffer.array();
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
		final var sno = SerialNumber.of(uint48(buffer));
		final int tag = buffer.getShort() & 0xffff;

		final ByteBuffer dec = decrypt(buffer, secretKey, securityInfo(data, offset + 2, 0xff00));

		final byte[] knxipPacket = new byte[total - minLength + hdrLength];
		dec.get(knxipPacket);
		final byte[] mac = new byte[macSize];
		dec.get(mac);

		final byte[] frame = Arrays.copyOfRange(data, offset - hdrLength, offset - hdrLength + total);
		System.arraycopy(knxipPacket, 0, frame, hdrLength + 2 + 6 + 6 + 2, knxipPacket.length);
		cbcMacVerify(frame, 0, total - macSize, secretKey, securityInfo(data, offset + 2, knxipPacket.length), mac);

		return new Object[] { sid, seq, sno, tag, knxipPacket };
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

	static ByteBuffer decrypt(final ByteBuffer buffer, final Key secretKey, final byte[] secInfo) {
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

	static void cbcMacVerify(final byte[] data, final int offset, final int length, final Key secretKey,
		final byte[] secInfo, final byte[] verifyAgainst) {
		final byte[] mac = cbcMac(data, offset, length, secretKey, secInfo);
		final boolean authenticated = Arrays.equals(mac, verifyAgainst);
		if (!authenticated) {
			final String packet = toHex(Arrays.copyOfRange(data, offset, offset + length), " ");
			throw new KnxSecureException("authentication failed for " + packet);
		}
	}

	static byte[] cbcMac(final byte[] data, final int offset, final int length, final Key secretKey,
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
			final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			final IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			cipher.update(secInfo);

			final byte[] lenBuf = { 0, (byte) (hdr.length + session.length) };
			cipher.update(lenBuf);
			cipher.update(hdr);
			cipher.update(session);

			final int checkLength = 16 + 2 + 6 + session.length + frame.length;
			final byte[] padded = Arrays.copyOfRange(frame, 0, frame.length + 15 - (checkLength + 15) % 16);
			final byte[] result = cipher.doFinal(padded);

			final byte[] mac = Arrays.copyOfRange(result, result.length - macSize, result.length);
			return mac;
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("calculating CBC-MAC of " + toHex(log, " "), e);
		}
	}

	static byte[] securityInfo(final byte[] data, final int offset, final int lengthInfo) {
		final byte[] secInfo = Arrays.copyOfRange(data, offset, offset + 16);
		secInfo[14] = (byte) (lengthInfo >> 8);
		secInfo[15] = (byte) lengthInfo;
		return secInfo;
	}

	static String statusMsg(final int status) {
		final String[] msg = { "authorization success", "authorization failed", "unauthorized", "timeout", "keep-alive", "close" };
		if (status >= msg.length)
			return "unknown status " + status;
		return msg[status];
	}

	static SecretKey createSecretKey(final byte[] key) {
		if (key.length != 16)
			throw new KNXIllegalArgumentException("KNX key has to be 16 bytes in length");
		final SecretKeySpec spec = new SecretKeySpec(key, "AES");
		Arrays.fill(key, (byte) 0);
		return spec;
	}

	static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		final KeyPairGenerator gen = KeyPairGenerator.getInstance("X25519");
		return gen.generateKeyPair();
	}

	static byte[] keyAgreement(final PrivateKey privateKey, final byte[] spk) {
		try {
			final byte[] reversed = spk.clone();
			reverse(reversed);
			final KeySpec spec = new XECPublicKeySpec(NamedParameterSpec.X25519, new BigInteger(1, reversed));
			final PublicKey pubKey = KeyFactory.getInstance("X25519").generatePublic(spec);
			final KeyAgreement ka = KeyAgreement.getInstance("X25519");
			ka.init(privateKey);
			ka.doPhase(pubKey, true);
			return ka.generateSecret();
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("key agreement failed", e);
		}
	}

	static byte[] sessionKey(final byte[] sharedSecret) {
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

	static byte[] xor(final byte[] a, final int offsetA, final byte[] b, final int offsetB, final int len) {
		if (a.length - len < offsetA || b.length - len < offsetB)
			throw new KNXIllegalArgumentException("illegal offset or length");
		final byte[] res = new byte[len];
		for (int i = 0; i < len; i++)
			res[i] = (byte) (a[i + offsetA] ^ b[i + offsetB]);
		return res;
	}

	static void reverse(final byte[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			final byte b = array[i];
			array[i] = array[array.length - 1 - i];
			array[array.length - 1 - i] = b;
		}
	}

	private static long uint48(final ByteBuffer buffer) {
		long l = (buffer.getShort() & 0xffffL) << 32;
		l |= buffer.getInt() & 0xffffffffL;
		return l;
	}
}

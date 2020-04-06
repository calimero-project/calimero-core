/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2020 B. Malinowsky

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
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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
import java.security.interfaces.XECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.KnxSecureException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.knxnetip.Connection.SecureSession;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.util.CRI;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
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

	static final String secureSymbol = new String(Character.toChars(0x1F512));

	private final byte[] sno;
	private Key secretKey;
	private int sessionId;

	// tunneling connection setup

	private SecureSession session;

	private PrivateKey privateKey;
	private final byte[] publicKey = new byte[keyLength];

	// timeout session.req -> session.res, and session.auth -> session.status
	private static final int sessionSetupTimeout = 10_000; // [ms]

	private static final int SessionSetup = -1;
	private volatile int sessionStatus = SessionSetup;

	private DatagramSocket localSocket;
	private ReceiverLoop setupLoop;
	private final KNXnetIPConnection tunnel;


	// routing connection setup

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
	private byte[] timerNotifySN;
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


	public static KNXnetIPConnection newTunneling(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
			final InetSocketAddress serverCtrlEP, final boolean useNat, final byte[] deviceAuthCode, final int userId,
			final byte[] userKey) throws KNXException, InterruptedException {

		final byte[] devAuth = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
		final var tunnelingAddress = KNXMediumSettings.BackboneRouter;
		return new SecureConnection(knxLayer, localEP, serverCtrlEP, useNat, devAuth, userId, userKey,
				tunnelingAddress);
	}

	public static KNXnetIPConnection newTunneling(final TunnelingLayer knxLayer, final SecureSession session,
			final IndividualAddress tunnelingAddress) throws KNXException, InterruptedException {

		session.ensureOpen();
		final var tunnel = new KNXnetIPTunnel(knxLayer, session.connection(), tunnelingAddress) {
			@Override
			public String getName() {
				return "KNX IP " + secureSymbol + " Tunneling " + Connection.addressPort(ctrlEndpt);
			}

			@Override
			protected void connect(final Connection c, final CRI cri) throws KNXException, InterruptedException {
				session.registerConnectRequest(this);
				try {
					super.connect(c.localEndpoint(), c.server(), cri, false);
				}
				finally {
					session.unregisterConnectRequest(this);
				}
			}

			@Override
			protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
				final byte[] wrapped = newSecurePacket(session.id(), session.nextSendSeq(), session.serialNumber(), 0,
						packet, session.secretKey);
				super.send(wrapped, dst);
			}
		};
		return tunnel;
	}

	/**
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
	public static KNXnetIPConnection newRouting(final NetworkInterface netIf, final InetAddress mcGroup, final byte[] groupKey,
		final Duration latencyTolerance) throws KNXException {
		return new SecureConnection(netIf, mcGroup, groupKey, latencyTolerance);
	}

	public static KNXnetIPConnection newDeviceManagement(final InetSocketAddress localEP, final InetSocketAddress serverCtrlEP,
		final boolean useNat, final byte[] deviceAuthCode, final byte[] userKey) throws KNXException, InterruptedException {
		final byte[] devAuth = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
		return new SecureConnection(localEP, serverCtrlEP, useNat, devAuth, userKey);
	}

	public static KNXnetIPConnection newDeviceManagement(final SecureSession session)
			throws KNXException, InterruptedException {

		session.ensureOpen();
		final var tunnel = new KNXnetIPDevMgmt(session.connection()) {
			@Override
			public String getName() {
				return "KNX IP " + secureSymbol + " Management " + Connection.addressPort(ctrlEndpt);
			}

			@Override
			protected void connect(final Connection c, final CRI cri) throws KNXException, InterruptedException {
				session.registerConnectRequest(this);
				try {
					super.connect(c.localEndpoint(), c.server(), cri, false);
				}
				finally {
					session.unregisterConnectRequest(this);
				}
			}

			@Override
			protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
				final byte[] wrapped = newSecurePacket(session.id(), session.nextSendSeq(), session.serialNumber(), 0,
						packet, session.secretKey);
				super.send(wrapped, dst);
			}
		};

		return tunnel;
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

	@Deprecated
	public static byte[] hashDeviceAuthenticationCode(final char[] authCode) {
		final byte[] salt = "device-authentication-code.1.secure.ip.knx.org".getBytes(StandardCharsets.US_ASCII);
		return pbkdf2WithHmacSha256(authCode, salt);
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

	private SecureConnection(final NetworkInterface netif, final InetAddress mcGroup, final byte[] groupKey,
		final Duration latencyTolerance) throws KNXException {
		super(mcGroup);

		sno = deriveSerialNumber(netif);
		secretKey = createSecretKey(groupKey);
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

		// unused
		tunnel = null;
	}

	private SecureConnection(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat, final byte[] deviceAuthCode, final int userId,
		final byte[] userKey, final IndividualAddress tunnelingAddress) throws KNXException, InterruptedException {
		super(null);

		InetSocketAddress local = localEP;
		if (local.isUnresolved())
			throw new KNXIllegalArgumentException("unresolved address " + local);
		if (local.getAddress().isAnyLocalAddress()) {
			try {
				final InetAddress addr = useNat ? null : Optional.ofNullable(serverCtrlEP.getAddress())
						.flatMap(SecureConnection::onSameSubnet).orElse(InetAddress.getLocalHost());
				local = new InetSocketAddress(addr, localEP.getPort());
			}
			catch (final UnknownHostException e) {
				throw new KNXException("no local host address available", e);
			}
		}

		session = Connection.Udp.newSecureSession(userId, userKey, deviceAuthCode);

		sno = session.serialNumber();
		setupSecureSession(local, serverCtrlEP);

		tunnel = new KNXnetIPTunnel(knxLayer, localEP, serverCtrlEP, useNat, tunnelingAddress) {
			@Override
			public String getName() {
				return "KNX/IP " + secureSymbol + " Tunneling " + ctrlEndpt.getAddress().getHostAddress() + ":" + ctrlEndpt.getPort();
			}

			@Override
			protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset, final InetAddress src,
				final int port) throws KNXFormatException, IOException {

				final int svc = h.getServiceType();
				if (!h.isSecure()) {
					logger.trace("received insecure service type 0x{} - ignore", Integer.toHexString(svc));
					return true;
				}
				if (svc == SecureSvc) {
					final Object[] fields = unwrap(h, data, offset);
					final byte[] packet = (byte[]) fields[4];
					final KNXnetIPHeader containedHeader = new KNXnetIPHeader(packet, 0);

					if (containedHeader.getServiceType() == SecureSessionStatus) {
						final int status = newChannelStatus(containedHeader, packet, containedHeader.getStructLength());
						LogService.log(logger, status == 0 ? LogLevel.TRACE : LogLevel.ERROR, "{}", session);
						setupLoop.quit();
						sessionStatus = status;
						if (status != 0) // XXX do we need this throw, its swallowed by the loop anyway?
							throw new KnxSecureException("secure session " + statusMsg(status));
					}
					else {
						// let base class handle decrypted knxip packet
						return super.handleServiceType(containedHeader, packet, containedHeader.getStructLength(), src, port);
					}
				}
				else
					logger.warn("received unsupported secure service type 0x{} - ignore", Integer.toHexString(svc));
				return true;
			}

			@Override
			protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
				final byte[] wrapped = newSecurePacket(session.nextSendSeq(), 0, packet);
				super.send(wrapped, dst);
			}
		};

		// unused
		mcastLatencyTolerance = 0;
		syncLatencyTolerance = 0;
	}

	private SecureConnection(final InetSocketAddress localEP, final InetSocketAddress serverCtrlEP, final boolean useNat,
		final byte[] deviceAuthCode, final byte[] userKey) throws KNXException, InterruptedException {
		super(null);

		InetSocketAddress local = localEP;
		if (local.isUnresolved())
			throw new KNXIllegalArgumentException("unresolved address " + local);
		if (local.getAddress().isAnyLocalAddress()) {
			try {
				final InetAddress addr = useNat ? null : Optional.ofNullable(serverCtrlEP.getAddress())
						.flatMap(SecureConnection::onSameSubnet).orElse(InetAddress.getLocalHost());
				local = new InetSocketAddress(addr, localEP.getPort());
			}
			catch (final UnknownHostException e) {
				throw new KNXException("no local host address available", e);
			}
		}

		session = Connection.Udp.newSecureSession(1, userKey, deviceAuthCode);
		sno = session.serialNumber();
		setupSecureSession(local, serverCtrlEP);

		tunnel = new KNXnetIPDevMgmt(localEP, serverCtrlEP, useNat) {
			@Override
			public String getName() {
				return "KNX/IP " + secureSymbol + " Management " + ctrlEndpt.getAddress().getHostAddress() + ":" + ctrlEndpt.getPort();
			}

			@Override
			protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
				final byte[] wrapped = newSecurePacket(session.nextSendSeq(), 0, packet);
				super.send(wrapped, dst);
			}

			@Override
			protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset, final InetAddress src,
				final int port) throws KNXFormatException, IOException {

				final int svc = h.getServiceType();
				if (!h.isSecure()) {
					logger.trace("received insecure service type 0x{} - ignore", Integer.toHexString(svc));
					return true;
				}
				if (svc == SecureSvc) {
					final Object[] fields = unwrap(h, data, offset);
					final byte[] packet = (byte[]) fields[4];
					final KNXnetIPHeader containedHeader = new KNXnetIPHeader(packet, 0);

					if (containedHeader.getServiceType() == SecureSessionStatus) {
						final int status = newChannelStatus(containedHeader, packet, containedHeader.getStructLength());
						LogService.log(logger, status == 0 ? LogLevel.TRACE : LogLevel.ERROR, "{}", session);
						setupLoop.quit();
						sessionStatus = status;
						if (status != 0) // XXX do we need this throw, its swallowed by the loop anyway?
							throw new KnxSecureException("secure session " + statusMsg(status));
					}
					else {
						// let base class handle decrypted knxip packet
						return super.handleServiceType(containedHeader, packet, containedHeader.getStructLength(), src, port);
					}
				}
				else
					logger.warn("received unsupported secure service type 0x{} - ignore", Integer.toHexString(svc));
				return true;
			}
		};

		// unused
		mcastLatencyTolerance = 0;
		syncLatencyTolerance = 0;
	}

	// finds a local IPv4 address with its network prefix "matching" the remote address
	static Optional<InetAddress> onSameSubnet(final InetAddress remote) {
		try {
			return NetworkInterface.networkInterfaces().flatMap(ni -> ni.getInterfaceAddresses().stream())
					.filter(ia -> ia.getAddress() instanceof Inet4Address)
//					.peek(ia -> logger.trace("match local address {}/{} to {}", ia.getAddress(), ia.getNetworkPrefixLength(), remote))
					.filter(ia -> ClientConnection.matchesPrefix(ia.getAddress(), ia.getNetworkPrefixLength(), remote))
					.map(ia -> ia.getAddress()).findFirst();
		}
		catch (final SocketException ignore) {}
		return Optional.empty();
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
	public void addConnectionListener(final KNXListener l) {
		if (tunnel != null)
			tunnel.addConnectionListener(l);
		else
			super.addConnectionListener(l);
	}

	@Override
	public void removeConnectionListener(final KNXListener l) {
		if (tunnel != null)
			tunnel.addConnectionListener(l);
		else
			super.removeConnectionListener(l);
	}

	@Override
	public void send(final CEMI frame, final BlockingMode mode) throws KNXConnectionClosedException {
		if (tunnel != null) {
			try {
				tunnel.send(frame, mode);
			}
			catch (KNXTimeoutException | InterruptedException e) {
				if (tunnel.getState() == KNXnetIPConnection.CLOSED)
					throw new KNXConnectionClosedException(e.getMessage());
				// TODO
				logger.error("tunneling error", e);
			}
		}
		else
			super.send(frame, mode);
	}

	@Override
	public String getName() {
		if (tunnel != null)
			return tunnel.getName();
		return "KNX/IP " + secureSymbol + " Routing " + ctrlEndpt.getAddress().getHostAddress();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
		final int tag = routingCount.getAndIncrement() % 0x10000;
		final byte[] wrapped = newSecurePacket(timestamp(), tag, packet);
		final DatagramPacket p = new DatagramPacket(wrapped, wrapped.length, dst);
		socket.send(p);
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

		if (svc == SecureSessionResponse) {
			try {
				final Object[] res = newSessionResponse(h, data, offset, src, port);

				final byte[] serverPublicKey = (byte[]) res[1];
				final byte[] auth = newSessionAuth(serverPublicKey);
				final byte[] packet = newSecurePacket(session.nextSendSeq(), 0, auth);
				logger.debug("secure session {}, request access for user {}", sessionId, session.user());
				if (localSocket != null)
					localSocket.send(new DatagramPacket(packet, packet.length, src, port));
				else
					session.connection().send(packet);
			}
			catch (final RuntimeException e) {
				sessionStatus = 1;
				setupLoop.quit();
				logger.error("negotiating session key failed", e);
			}
		}
		else if (svc == SecureGroupSync) {
			try {
				final Object[] fields = newGroupSync(h, data, offset);
				onGroupSync(src, (long) fields[0], true, (byte[]) fields[1], (int) fields[2]);
			}
			catch (final KnxSecureException e) {
				logger.debug("group sync {}", e.getMessage());
				return true;
			}
		}
		else if (svc == SecureSvc) {
			final Object[] fields = unwrap(h, data, offset);
			final long timestamp = (long) fields[1];
			if (sessionId == 0 && !withinTolerance(src, timestamp, (byte[]) fields[2], (int) fields[3])) {
				logger.warn("{}:{} timestamp {} outside latency tolerance of {} ms (local {}) - ignore", src,
						port, timestamp, mcastLatencyTolerance, timestamp());
				return true;
			}

			final byte[] packet = (byte[]) fields[4];
			final KNXnetIPHeader containedHeader = new KNXnetIPHeader(packet, 0);

			if (containedHeader.getServiceType() == SecureSessionStatus) {
				final int status = newChannelStatus(containedHeader, packet, containedHeader.getStructLength());
				LogService.log(logger, status == 0 ? LogLevel.DEBUG : LogLevel.ERROR, "{}", session);
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
		if (tunnel != null)
			tunnel.close();
		else {
			groupSync.cancel(true);
			super.close(initiator, reason, level, t);
		}
	}

	// unicast session
	// session.req -> session.res -> auth.req -> session-status
	private void setupSecureSession(final InetSocketAddress localEP, final InetSocketAddress serverCtrlEP) throws KNXException {

		logger = LoggerFactory.getLogger("calimero.knxnetip.KNX/IP " + secureSymbol + " Session "
				+ serverCtrlEP.getAddress().getHostAddress() + ":" + serverCtrlEP.getPort());

		logger.debug("setup secure session with {}", serverCtrlEP);
		try {
			final KeyPair keyPair = generateKeyPair();
			privateKey = keyPair.getPrivate();
			final BigInteger u = ((XECPublicKey) keyPair.getPublic()).getU();
			final byte[] tmp = u.toByteArray();
			reverse(tmp);
			System.arraycopy(tmp, 0, publicKey, 0, tmp.length);
		}
		catch (final Throwable e) {
			throw new KnxSecureException("error creating secure key pair for " + serverCtrlEP, e);
		}
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
			Arrays.fill(publicKey, (byte) 0);
			// key.destroy() is not implemented
//			try {
//				privateKey.destroy();
//			}
//			catch (final DestroyFailedException e) {}
		}
	}

	private boolean withinTolerance(final InetAddress src, final long timestamp, final byte[] sn, final int tag) {
		onGroupSync(src, timestamp, false, sn, tag);
		final long diff = timestamp() - timestamp;
		return diff <= mcastLatencyTolerance;
	}

	private void onGroupSync(final InetAddress src, final long timestamp, final boolean byTimerNotify, final byte[] sn,
		final int tag) {
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
		else if (timestamp > (local - mcastLatencyTolerance)) {
			// received old timestamp within tolerance, do nothing
		}
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

	private void syncedWithGroup(final boolean byTimerNotify, final byte[] sn, final int tag) {
		if (byTimerNotify)
			becomeTimeFollower();

		scheduleGroupSync(periodicNotifyDelay());
		if (!syncedWithGroup && tag == sentGroupSyncTag && Arrays.equals(sno, sn)) {
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
			final NetworkInterface ni = ((MulticastSocket) socket).getNetworkInterface();
			final boolean noneSet = ni.getInetAddresses().nextElement().isAnyLocalAddress();
			netifs = noneSet ? NetworkInterface.networkInterfaces() : Stream.of(ni);
		}
		catch (final SocketException e) {}
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
					toHex(periodicSchedule ? sno : timerNotifySN, ""),
					periodicSchedule ? sentGroupSyncTag : timerNotifyTag);

			// schedule next sync before send to maintain happens-before with sync rcv
			becomeTimeKeeper();
			scheduleGroupSync(periodicNotifyDelay());
			socket.send(new DatagramPacket(sync, sync.length, dataEndpt.getAddress(), dataEndpt.getPort()));
		}
		catch (IOException | RuntimeException e) {
			if (socket.isClosed()) {
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
		if (sessionId != 0) {
			final var rcvSeq = session.nextReceiveSeq();
			if (seq < rcvSeq)
				throw new KnxSecureException("received secure packet with sequence " + seq + " < expected " + rcvSeq);
		}

		final long snLong = (long) fields[2];
		final byte[] sn = ByteBuffer.allocate(6).putShort((short) (snLong >> 32)).putInt((int) snLong).array();
		final int tag = (int) fields[3];
		final byte[] knxipPacket = (byte[]) fields[4];
		logger.trace("received {} (session {} seq {} S/N {} tag {})", toHex(knxipPacket, " "), sid, seq, toHex(sn, ""),
				tag);
		return new Object[] { fields[0], fields[1], sn, fields[3], fields[4] };
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

		final byte[] frame = Arrays.copyOfRange(data, offset - hdrLength, offset - hdrLength + total);
		System.arraycopy(knxipPacket, 0, frame, hdrLength + 2 + 6 + 6 + 2, knxipPacket.length);
		cbcMacVerify(frame, 0, total - macSize, secretKey, securityInfo(data, offset + 2, knxipPacket.length), mac);

		return new Object[] { sid, seq, sno, tag, knxipPacket };
	}

	private Object[] newSessionResponse(final KNXnetIPHeader h, final byte[] data, final int offset,
			final InetAddress src, final int port)
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

		final byte[] sharedSecret = keyAgreement(privateKey, serverPublicKey);
		final byte[] sessionKey = sessionKey(sharedSecret);
		secretKey = createSecretKey(sessionKey);

		final boolean skipDeviceAuth = Arrays.equals(session.deviceAuthKey().getEncoded(), new byte[16]);
		if (skipDeviceAuth) {
			logger.warn("skipping device authentication of {}:{} (no device key)", src.getHostAddress(), port);
		}
		else {
			final ByteBuffer mac = decrypt(buffer, session.deviceAuthKey(), securityInfo(new byte[16], 0, 0xff00));

			final int msgLen = h.getStructLength() + 2 + keyLength;
			final ByteBuffer macInput = ByteBuffer.allocate(16 + 2 + msgLen);
			macInput.put(new byte[16]);
			macInput.put((byte) 0);
			macInput.put((byte) msgLen);
			macInput.put(h.toByteArray());
			macInput.putShort((short) sessionId);
			macInput.put(xor(serverPublicKey, 0, publicKey, 0, keyLength));

			final byte[] verifyAgainst = cbcMacSimple(session.deviceAuthKey(), macInput.array(), 0, macInput.capacity());
			final boolean authenticated = Arrays.equals(mac.array(), verifyAgainst);
			if (!authenticated) {
				final String packet = toHex(Arrays.copyOfRange(data, offset - 6, offset - 6 + 0x38), " ");
				throw new KnxSecureException("authentication failed for session response " + packet);
			}
		}

		return new Object[] { sessionId, serverPublicKey };
	}

	private byte[] newSessionAuth(final byte[] serverPublicKey) {
		final KNXnetIPHeader header = new KNXnetIPHeader(SecureSessionAuth, 2 + macSize);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.putShort((short) session.user());

		final int msgLen = 6 + 2 + keyLength;
		final ByteBuffer macInput = ByteBuffer.allocate(16 + 2 + msgLen);
		macInput.put(new byte[16]);
		macInput.put((byte) 0);
		macInput.put((byte) msgLen);
		macInput.put(buffer.array(), 0, buffer.position());
		macInput.put(xor(serverPublicKey, 0, publicKey, 0, keyLength));
		final byte[] mac = cbcMacSimple(session.userKey(), macInput.array(), 0, macInput.capacity());
		encrypt(mac, 0, session.userKey(), securityInfo(new byte[16], 8, 0xff00));

		buffer.put(mac);
		return buffer.array();
	}

	static int newChannelStatus(final KNXnetIPHeader h, final byte[] data, final int offset)
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

		buffer.putShort((short) (timestamp >> 32)).putInt((int) timestamp);
		if (periodicSchedule) {
			sentGroupSyncTag = randomClosedRange(1, 0xffff);
			buffer.put(sno).putShort((short) sentGroupSyncTag);
		}
		else
			buffer.put(timerNotifySN).putShort((short) timerNotifyTag);

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
		cbcMacVerify(data, offset - h.getStructLength(), h.getTotalLength() - macSize, secretKey, secInfo, mac.array());
		logger.trace("received group sync timestamp {} ms (S/N {}, tag {})", timestamp, toHex(sn, ""), msgTag);
		return new Object[] { timestamp, sn, msgTag };
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
		return cbcMac(data, offset, length, secretKey, secInfo);
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

	private byte[] cbcMacSimple(final Key secretKey, final byte[] data, final int offset, final int length) {
		final byte[] exact = Arrays.copyOfRange(data, offset, offset + length);
		logger.trace("authenticating (length {}): {}", length, toHex(exact, " "));

		try {
			final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			final IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			final byte[] padded = Arrays.copyOfRange(exact, 0, (length + 15) / 16 * 16);
			final byte[] result = cipher.doFinal(padded);
			final byte[] mac = Arrays.copyOfRange(result, result.length - macSize, result.length);
			return mac;
		}
		catch (final GeneralSecurityException e) {
			throw new KnxSecureException("calculating CBC-MAC of " + toHex(exact, " "), e);
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

	private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
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

	private static long uint48(final ByteBuffer buffer) {
		long l = (buffer.getShort() & 0xffffL) << 32;
		l |= buffer.getInt() & 0xffffffffL;
		return l;
	}

	private static void reverse(final byte[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			final byte b = array[i];
			array[i] = array[array.length - 1 - i];
			array[array.length - 1 - i] = b;
		}
	}
}

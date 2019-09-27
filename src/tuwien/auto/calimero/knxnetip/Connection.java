/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019 B. Malinowsky

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
import static tuwien.auto.calimero.knxnetip.SecureConnection.secureSymbol;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.XECPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.KnxRuntimeException;
import tuwien.auto.calimero.KnxSecureException;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogService.LogLevel;

/**
 * Connection management for TCP connections to KNXnet/IP servers, and for KNX IP secure sessions.
 */
public final class Connection implements Closeable {

	// pseudo connection, so we can still run with udp
	static Connection Udp = new Connection(new InetSocketAddress(0));

	private static final Duration connectionTimeout = Duration.ofMillis(5000);

	private volatile InetSocketAddress localEndpoint;
	// ??? we currently cannot reuse a connection once it got closed
	private final InetSocketAddress server;
	private final Socket socket;

	private final Logger logger;

	// session ID -> secure session
	final Map<Integer, SecureSession> sessions = new ConcurrentHashMap<>();
	// communication channel ID -> plain connection
	private final Map<Integer, ClientConnection> unsecuredConnections = new ConcurrentHashMap<>();
	// we expect fifo processing by the server with multiple ongoing connect requests
	private final List<ClientConnection> ongoingConnectRequests = Collections.synchronizedList(new ArrayList<>());

	private final Lock sessionRequestLock = new ReentrantLock();
	private volatile SecureSession inSessionRequestStage;


	// we have to keep the following session stuff here, because session is a non-static class

	// timeout session.req -> session.res, and session.auth -> session.status
	private static final int sessionSetupTimeout = 10_000; // [ms]

	private static final Duration keepAliveInvterval = Duration.ofSeconds(30);
	private static final ScheduledThreadPoolExecutor keepAliveSender = new ScheduledThreadPoolExecutor(1, r -> {
		final Thread t = new Thread(r);
		t.setName("KNX IP Secure session keep-alive");
		t.setDaemon(true);
		return t;
	});
	static {
		// remove idle threads after a while
		keepAliveSender.setKeepAliveTime(90, TimeUnit.SECONDS);
		keepAliveSender.allowCoreThreadTimeOut(true);
	}

	private static final byte[] emptyUserPwdHash = { (byte) 0xe9, (byte) 0xc3, 0x04, (byte) 0xb9, 0x14, (byte) 0xa3,
		0x51, 0x75, (byte) 0xfd, 0x7d, 0x1c, 0x67, 0x3a, (byte) 0xb5, 0x2f, (byte) 0xe1 };

	private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		final KeyPairGenerator gen = KeyPairGenerator.getInstance("X25519");
		return gen.generateKeyPair();
	}

	private static byte[] deriveSerialNumber(final InetSocketAddress localEP) {
		try {
			if (localEP != null)
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

	private enum SessionState { Idle, Unauthenticated, Authenticated }

	public final class SecureSession implements AutoCloseable {

		// service codes
		private static final int SecureWrapper = 0x0950;
		private static final int SecureSessionResponse = 0x0952;
		private static final int SecureSessionAuth = 0x0953;
		private static final int SecureSessionStatus = 0x0954;

		// session status codes
		private static final int AuthSuccess = 0;
		private static final int AuthFailed = 1;
		private static final int Unauthenticated = 2;
		private static final int Timeout = 3;
		private static final int KeepAlive = 4;
		private static final int Close = 5;
		// internal session status we use for initial setup
		private static final int Setup = 6;

		private static final int keyLength = 32; // [bytes]
		private static final int macSize = 16; // [bytes]



		private final int user;
		private final SecretKey userKey;
		private final SecretKey deviceAuthKey;

		private PrivateKey privateKey;
		private final byte[] publicKey = new byte[keyLength];

		private final byte[] sno;

		private int sessionId;
		private volatile SessionState sessionState = SessionState.Idle;
		private volatile int sessionStatus = Setup;
		Key secretKey;

		private final AtomicLong sendSeq = new AtomicLong();
		private final AtomicLong rcvSeq = new AtomicLong();

		// assign dummy to have it initialized
		private Future<?> keepAliveFuture = CompletableFuture.completedFuture(Void.TYPE);

		// communication channel ID -> secured connection
		final Map<Integer, ClientConnection> securedConnections = new ConcurrentHashMap<>();
		// we expect fifo processing by the server with multiple ongoing connect requests
		private final List<ClientConnection> ongoingConnectRequests = Collections.synchronizedList(new ArrayList<>());

		private final Logger logger;


		private SecureSession(final int user, final byte[] userKey, final byte[] deviceAuthCode) {
			this.user = user;

			final byte[] key = userKey.length == 0 ? emptyUserPwdHash.clone() : userKey;
			this.userKey = SecureConnection.createSecretKey(key);

			final var authCode = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
			this.deviceAuthKey = SecureConnection.createSecretKey(authCode);

			sno = deriveSerialNumber((InetSocketAddress) socket.getLocalSocketAddress());

			logger = LoggerFactory.getLogger("calimero.knxnetip." + secureSymbol + " Session " + addressPort(server));
		}

		public int id() { return sessionId; }

		public int user() { return user; }

		public SecretKey userKey() { return userKey; }

		public byte[] serialNumber() { return sno.clone(); }

		public Connection connection() { return Connection.this; }

		@Override
		public void close() {
			if (sessionState == SessionState.Idle)
				return;

			sessionState = SessionState.Idle;
			keepAliveFuture.cancel(false);
			securedConnections.values().forEach(ClientConnection::close);
			securedConnections.clear();
			sessions.remove(sessionId);

			if (socket.isClosed())
				return;
			try {
				send(newStatusInfo(sessionId, nextSendSeq(), Close));
			}
			catch (final IOException e) {
				logger.info("I/O error closing secure session {}", sessionId, e);
			}
		}

		@Override
		public String toString() {
			return secureSymbol + " session " + sessionId + " (user " + user + "): " + sessionState;
		}

		SecretKey deviceAuthKey() { return deviceAuthKey; }

		void ensureOpen() throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException {
			if (sessionState == SessionState.Authenticated)
				return;
			setupSecureSession();
		}

		void registerConnectRequest(final ClientConnection c) { ongoingConnectRequests.add(c); }

		void unregisterConnectRequest(final ClientConnection c) {
			ongoingConnectRequests.remove(c);
			if (c.getState() == KNXnetIPConnection.OK)
				securedConnections.put(c.channelId, c);
		}

		long nextSendSeq() { return sendSeq.getAndIncrement(); }

		long nextReceiveSeq() { return rcvSeq.getAndIncrement(); }

		private void setupSecureSession()
				throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException {
			sessionRequestLock.lock();
			try {
				if (sessionState == SessionState.Authenticated)
					return;
				sessionState = SessionState.Idle;
				sessionStatus = Setup;
				inSessionRequestStage = this;

				logger.debug("setup secure session with {}", server);

				initKeys();
				connect();
				final byte[] sessionReq = PacketHelper.newChannelRequest(HPAI.Tcp, publicKey);
				send(sessionReq);
				awaitAuthenticationStatus();

				if (sessionState == SessionState.Unauthenticated || sessionStatus != AuthSuccess) {
					sessionState = SessionState.Idle;
					throw new KnxSecureException("secure session " + SecureConnection.statusMsg(sessionStatus));
				}
				if (sessionState == SessionState.Idle)
					throw new KNXTimeoutException("timeout establishing secure session with " + server);

				final var delay = keepAliveInvterval.toMillis();
				keepAliveFuture = keepAliveSender.scheduleWithFixedDelay(this::sendKeepAlive, delay, delay,
						TimeUnit.MILLISECONDS);
			}
			catch (final GeneralSecurityException e) {
				throw new KnxSecureException("error creating key pair for " + server, e);
			}
			catch (final SocketTimeoutException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedException(
						"interrupted I/O establishing secure session with " + server + ": " + e.getMessage());
			}
			catch (final IOException e) {
				close();
				Connection.this.close();
				throw new KNXConnectionClosedException("I/O error establishing secure session with " + server, e);
			}
			finally {
				sessionRequestLock.unlock();
				Arrays.fill(publicKey, (byte) 0);
			}
		}

		private void initKeys() throws NoSuchAlgorithmException {
			final var keyPair = generateKeyPair();
			privateKey = keyPair.getPrivate();

			final BigInteger u = ((XECPublicKey) keyPair.getPublic()).getU();
			// we need public key in little endian
			final byte[] tmp = u.toByteArray();
			reverse(tmp);
			System.arraycopy(tmp, 0, publicKey, 0, tmp.length);
			Arrays.fill(tmp, (byte) 0);
		}

		private void awaitAuthenticationStatus() throws InterruptedException, KNXTimeoutException {
			long end = System.nanoTime() / 1_000_000 + sessionSetupTimeout;
			long remaining = sessionSetupTimeout;
			boolean inAuth = false;
			while (remaining > 0 && sessionState != SessionState.Authenticated && sessionStatus == Setup) {
				synchronized (this) {
					wait(remaining);
				}
				remaining = end - System.nanoTime() / 1_000_000;
				if (sessionState == SessionState.Unauthenticated && !inAuth) {
					inAuth = true;
					end = end - remaining + sessionSetupTimeout;
				}
			}
			if (remaining <= 0)
				throw new KNXTimeoutException("timeout establishing secure session with " + addressPort(server));
		}

		private boolean acceptServiceType(final KNXnetIPHeader h, final byte[] data, final int offset, final int length)
				throws KNXFormatException {
			final int svc = h.getServiceType();
			if (!h.isSecure())
				throw new KnxSecureException(String.format("dispatched insecure service type 0x%h to %s", svc, this));

			// ensure minimum secure wrapper frame size (6 header, 16 security info, 6 encapsulated header, 16 MAC)
			if (h.getTotalLength() < 44)
				return false;

			if (svc == SecureSessionResponse) {
				if (sessionState != SessionState.Idle) {
					logger.warn("received session response in state {} - ignore", sessionState);
					return true;
				}
				try {
					final byte[] serverPublicKey = parseSessionResponse(h, data, offset, server);
					final byte[] auth = newSessionAuth(serverPublicKey);
					sessionState = SessionState.Unauthenticated;
					final byte[] packet = wrap(auth);
					logger.debug("secure session {}, request access for user {}", sessionId, user);
					send(packet);
				}
				catch (IOException | RuntimeException e) {
					sessionStatus = AuthFailed;
					logger.error("negotiating session key failed", e);
				}
				synchronized (this) {
					notifyAll();
				}
			}
			else if (svc == SecureWrapper) {
				final Object[] fields = unwrap(h, data, offset);

				final byte[] packet = (byte[]) fields[1];
				final var plainHeader = new KNXnetIPHeader(packet, 0);
				final var hdrLen = plainHeader.getStructLength();

				if (plainHeader.getServiceType() == SecureSessionStatus) {
					sessionStatus = SecureConnection.newChannelStatus(plainHeader, packet, hdrLen);

					if (sessionState == SessionState.Unauthenticated) {
						if (sessionStatus == AuthSuccess)
							sessionState = SessionState.Authenticated;

						LogService.log(logger, sessionStatus == AuthSuccess ? LogLevel.DEBUG : LogLevel.ERROR, "{} {}",
								SecureConnection.statusMsg(sessionStatus), this);
						synchronized (this) {
							notifyAll();
						}
					}
					else if (sessionStatus == Timeout || sessionStatus == Unauthenticated) {
						logger.error("{} {}", SecureConnection.statusMsg(sessionStatus), this);
						close();
					}
				}
				else
					dispatchToConnection(plainHeader, packet, hdrLen, plainHeader.getTotalLength() - hdrLen);
			}
			else
				logger.warn("received unsupported secure service type 0x{} - ignore", Integer.toHexString(svc));

			return true;
		}

		private void dispatchToConnection(final KNXnetIPHeader header, final byte[] data, final int offset,
				final int length) {
			final var channelId = channelId(header, data, offset);
			var connection = securedConnections.get(channelId);
			if (connection == null) {
				synchronized (ongoingConnectRequests) {
					if (!ongoingConnectRequests.isEmpty())
						connection = ongoingConnectRequests.remove(0);
				}
			}

			try {
				if (connection != null) {
					connection.handleServiceType(header, data, offset, server.getAddress(), server.getPort());
					if (header.getServiceType() == KNXnetIPHeader.DISCONNECT_RES) {
						logger.trace("remove connection {}", connection);
						securedConnections.remove(channelId);
					}
				}
				else
					logger.warn("communication channel {} does not exist", channelId);
			}
			catch (KNXFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void sendKeepAlive() {
			try {
				logger.trace("sending keep-alive");
				send(newStatusInfo(sessionId, nextSendSeq(), KeepAlive));
			}
			catch (final IOException e) {
				if (sessionState == SessionState.Authenticated && !socket.isClosed()) {
					logger.warn("error sending keep-alive: {}", e.getMessage());
					close();
					Connection.this.close();
				}
			}
		}

		private byte[] wrap(final byte[] plainPacket) {
			return SecureConnection.newSecurePacket(sessionId, nextSendSeq(), sno, 0, plainPacket, secretKey);
		}

		private Object[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
			final Object[] fields = SecureConnection.unwrap(h, data, offset, secretKey);

			final int sid = (int) fields[0];
			if (sid != sessionId)
				throw new KnxSecureException("secure session mismatch: received ID " + sid + ", expected " + sessionId);

			final long seq = (long) fields[1];
			if (seq < rcvSeq.get())
				throw new KnxSecureException("received secure packet with sequence " + seq + " < expected " + rcvSeq);
			rcvSeq.incrementAndGet();

			final long snLong = (long) fields[2];
			final byte[] sn = ByteBuffer.allocate(6).putShort((short) (snLong >> 32)).putInt((int) snLong).array();
			final int tag = (int) fields[3];
			if (tag != 0)
				throw new KnxSecureException("expected message tag 0, received " + tag);
			final byte[] knxipPacket = (byte[]) fields[4];
			logger.trace("received (seq {} S/N {}) {}", seq, toHex(sn, ""), toHex(knxipPacket, " "));
			return new Object[] { sn, fields[4] };
		}

		private byte[] parseSessionResponse(final KNXnetIPHeader h, final byte[] data, final int offset,
			final InetSocketAddress remote) throws KNXFormatException {

			if (h.getServiceType() != SecureSessionResponse)
				throw new IllegalArgumentException("no secure channel response");
			if (h.getTotalLength() != 0x38)
				throw new KNXFormatException("invalid length " + data.length + " for a secure session response");

			final ByteBuffer buffer = ByteBuffer.wrap(data, offset, h.getTotalLength() - h.getStructLength());

			sessionId = buffer.getShort() & 0xffff;
			if (sessionId == 0)
				throw new KnxSecureException("no more free secure sessions, or remote endpoint busy");

			final byte[] serverPublicKey = new byte[keyLength];
			buffer.get(serverPublicKey);

			final byte[] sharedSecret = SecureConnection.keyAgreement(privateKey, serverPublicKey);
			final byte[] sessionKey = SecureConnection.sessionKey(sharedSecret);
			synchronized (this) {
				secretKey = SecureConnection.createSecretKey(sessionKey);
			}

			sessions.put(sessionId, this);
			inSessionRequestStage = null;

			final boolean skipDeviceAuth = Arrays.equals(deviceAuthKey.getEncoded(), new byte[16]);
			if (skipDeviceAuth) {
				logger.warn("skipping device authentication of {} (no device key)", addressPort(remote));
			}
			else {
				final ByteBuffer mac = SecureConnection.decrypt(buffer, deviceAuthKey,
						SecureConnection.securityInfo(new byte[16], 0, 0xff00));

				final int msgLen = h.getStructLength() + 2 + keyLength;
				final ByteBuffer macInput = ByteBuffer.allocate(16 + 2 + msgLen);
				macInput.put(new byte[16]);
				macInput.put((byte) 0);
				macInput.put((byte) msgLen);
				macInput.put(h.toByteArray());
				macInput.putShort((short) sessionId);
				macInput.put(SecureConnection.xor(serverPublicKey, 0, publicKey, 0, keyLength));

				final byte[] verifyAgainst = cbcMacSimple(deviceAuthKey, macInput.array(), 0, macInput.capacity());
				final boolean authenticated = Arrays.equals(mac.array(), verifyAgainst);
				if (!authenticated) {
					final String packet = toHex(Arrays.copyOfRange(data, offset - 6, offset - 6 + 0x38), " ");
					throw new KnxSecureException("authentication failed for session response " + packet);
				}
			}

			return serverPublicKey;
		}

		private byte[] newSessionAuth(final byte[] serverPublicKey) {
			final var header = new KNXnetIPHeader(SecureSessionAuth, 2 + macSize);

			final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
			buffer.put(header.toByteArray());
			buffer.putShort((short) user);

			final int msgLen = 6 + 2 + keyLength;
			final ByteBuffer macInput = ByteBuffer.allocate(16 + 2 + msgLen);
			macInput.put(new byte[16]);
			macInput.put((byte) 0);
			macInput.put((byte) msgLen);
			macInput.put(buffer.array(), 0, buffer.position());
			macInput.put(SecureConnection.xor(serverPublicKey, 0, publicKey, 0, keyLength));
			final byte[] mac = cbcMacSimple(userKey, macInput.array(), 0, macInput.capacity());
			SecureConnection.encrypt(mac, 0, userKey, SecureConnection.securityInfo(new byte[16], 8, 0xff00));

			buffer.put(mac);
			return buffer.array();
		}

		private byte[] newStatusInfo(final int sessionId, final long seq, final int status) {
			final ByteBuffer packet = ByteBuffer.allocate(6 + 2);
			packet.put(new KNXnetIPHeader(SecureSessionStatus, 2).toByteArray());
			packet.put((byte) status);
			final int msgTag = 0;
			return SecureConnection.newSecurePacket(sessionId, seq, sno, msgTag, packet.array(), secretKey);
		}

		private byte[] cbcMacSimple(final Key secretKey, final byte[] data, final int offset, final int length) {
			final byte[] log = Arrays.copyOfRange(data, offset, offset + length);
			logger.trace("authenticating (length {}): {}", length, toHex(log, " "));

			try {
				final var cipher = Cipher.getInstance("AES/CBC/NoPadding");
				final var ivSpec = new IvParameterSpec(new byte[16]);
				cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

				final byte[] padded = Arrays.copyOfRange(data, offset, (length + 15) / 16 * 16);
				final byte[] result = cipher.doFinal(padded);
				final byte[] mac = Arrays.copyOfRange(result, result.length - macSize, result.length);
				return mac;
			}
			catch (final GeneralSecurityException e) {
				throw new KnxSecureException("calculating CBC-MAC of " + toHex(log, " "), e);
			}
		}
	}

	public static Connection newTcpConnection(final InetSocketAddress local, final InetSocketAddress server) {
		return new Connection(local, server);
	}

	private Connection(final InetSocketAddress server) {
		this.server = server;
		socket = new Socket();
		logger = LoggerFactory.getLogger("calimero.knxnetip.tcp " + addressPort(server));
	}

	protected Connection(final InetSocketAddress local, final InetSocketAddress server) {
		this(server);
		if (local.isUnresolved())
			throw new KNXIllegalArgumentException("unresolved address " + local);

		var bind = local;
		if (local.getAddress().isAnyLocalAddress()) {
			try {
				final InetAddress addr = Optional.ofNullable(server.getAddress())
						.flatMap(SecureConnection::onSameSubnet).orElse(InetAddress.getLocalHost());
				bind = new InetSocketAddress(addr, local.getPort());
			}
			catch (final UnknownHostException e) {
				throw new KnxRuntimeException("no local host address available", e);
			}
		}

		try {
			socket.bind(bind);
			// socket returns any-local after socket is closed, so keep actual address after bind
			localEndpoint = (InetSocketAddress) socket.getLocalSocketAddress();
		}
		catch (final IOException e) {
			throw new KnxRuntimeException("binding to local address " + bind, e);
		}
	}

	public SecureSession newSecureSession(final int user, final byte[] userKey, final byte[] deviceAuthCode) {
		return new SecureSession(user, userKey, deviceAuthCode);
	}

	public InetSocketAddress localEndpoint() { return localEndpoint; }

	public InetSocketAddress server() { return server; }

	public boolean isConnected() {
		final var connected = socket.isConnected();
		if (socket.isClosed())
			return false;
		return connected;
	}

	@Override
	public void close() {
		unsecuredConnections.values().forEach(ClientConnection::close);
		unsecuredConnections.clear();

		sessions.values().forEach(SecureSession::close);
		sessions.clear();

		try {
			socket.close();
		}
		catch (final IOException ignore) {}
	}

	@Override
	public String toString() {
		final var state = socket.isClosed() ? "closed" : socket.isConnected() ? "connected" : "bound";
		return addressPort(localEndpoint()) + " " + addressPort(server) + " (" + state +")";
	}

	Socket socket() { return socket; }

	void send(final byte[] data) throws IOException {
		final var os = socket.getOutputStream();
		os.write(data);
		os.flush();
	}

	void registerConnectRequest(final ClientConnection c) { ongoingConnectRequests.add(c); }

	void unregisterConnectRequest(final ClientConnection c) {
		ongoingConnectRequests.remove(c);
		if (c.getState() == KNXnetIPConnection.OK)
			unsecuredConnections.put(c.channelId, c);
	}

	// InetSocketAddress::toString always prepends a '/' even if there is no host name
	static String addressPort(final InetSocketAddress addr) {
		return addr.getAddress().getHostAddress() + ":" + addr.getPort();
	}

	synchronized void connect() throws IOException {
		if (!socket.isConnected()) {
			socket.connect(server, (int) connectionTimeout.toMillis());
			startTcpReceiver();
		}
	}

	private void startTcpReceiver() {
		final Thread t = new Thread(this::runReceiveLoop, "KNXnet/IP tcp receiver " + addressPort(server));
		t.setDaemon(true);
		t.start();
	}

	private void runReceiveLoop() {
		final int rcvBufferSize = 512;
		final byte[] data = new byte[rcvBufferSize];
		int offset = 0;

		try {
			final var in = socket.getInputStream();
			while (!socket.isClosed()) {
				if (offset >= 6) {
					try {
						final var header = new KNXnetIPHeader(data, 0);
						if (header.getTotalLength() <= offset) {
							final int length = header.getTotalLength() - header.getStructLength();
							final int leftover = offset - header.getTotalLength();
							offset = leftover;

							if (header.isSecure())
								dispatchToSession(header, data, header.getStructLength(), length);
							else
								dispatchToConnection(header, data, header.getStructLength());

							if (leftover > 0) {
								System.arraycopy(data, header.getTotalLength(), data, 0, leftover);
								continue;
							}
						}
						// skip bodies which do not fit into rcv buffer
						else if (header.getTotalLength() > rcvBufferSize) {
							int skip = header.getTotalLength() - offset;
							while (skip-- > 0 && in.read() != -1);
							offset = 0;
						}
					}
					catch (KNXFormatException | KnxSecureException e) {
						logger.warn("received invalid frame", e);
						offset = 0;
					}
				}

				final int read = in.read(data, offset, data.length - offset);
				if (read == -1)
					return;
				offset += read;
			}
		}
		catch (final InterruptedIOException e) {
			Thread.currentThread().interrupt();
		}
		catch (IOException | RuntimeException e) {
			logger.error("receiver communication failure", e);
		}
		finally {
			close();
		}
	}

	private void dispatchToSession(final KNXnetIPHeader header, final byte[] data, final int offset, final int length)
			throws KNXFormatException {
		final var sessionId = ByteBuffer.wrap(data, offset, length).getShort() & 0xffff;
		if (sessionId == 0)
			throw new KnxSecureException("no more free secure sessions, or remote endpoint busy");

		var session = sessions.get(sessionId);
		if (session == null && header.getServiceType() == SecureSession.SecureSessionResponse)
			session = inSessionRequestStage;

		if (session != null)
			session.acceptServiceType(header, data, offset, length);
		else
			logger.warn("session {} does not exist", sessionId);
	}

	private void dispatchToConnection(final KNXnetIPHeader header, final byte[] data, final int offset)
			throws IOException, KNXFormatException {
		final var channelId = channelId(header, data, offset);
		var connection = unsecuredConnections.get(channelId);
		if (connection == null) {
			synchronized (ongoingConnectRequests) {
				if (!ongoingConnectRequests.isEmpty())
					connection = ongoingConnectRequests.remove(0);
			}
		}

		if (connection != null) {
			connection.handleServiceType(header, data, offset, server.getAddress(), server.getPort());
			if (header.getServiceType() == KNXnetIPHeader.DISCONNECT_RES)
				unsecuredConnections.remove(channelId);
		}
		else
			logger.warn("communication channel {} does not exist", channelId);
	}

	private static int channelId(final KNXnetIPHeader header, final byte[] data, final int offset) {
		// communication channel ID in the connection header of a tunneling/config request has a different offset
		// than in connection management services
		int channelIdOffset = offset;
		switch (header.getServiceType()) {
		case KNXnetIPHeader.TUNNELING_REQ:
		case KNXnetIPHeader.DEVICE_CONFIGURATION_REQ:
		case KNXnetIPHeader.TunnelingFeatureResponse:
		case KNXnetIPHeader.TunnelingFeatureInfo:
			channelIdOffset = offset + 1;
		}
		final var channelId = data[channelIdOffset] & 0xff;
		return channelId;
	}

	private static void reverse(final byte[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			final byte b = array[i];
			array[i] = array[array.length - 1 - i];
			array[array.length - 1 - i] = b;
		}
	}
}

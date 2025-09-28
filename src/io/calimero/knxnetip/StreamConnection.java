/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2019, 2025 B. Malinowsky

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

package io.calimero.knxnetip;

import static io.calimero.knxnetip.SecureConnection.secureSymbol;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;
import static java.text.MessageFormat.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigInteger;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.calimero.CloseEvent;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.KNXTimeoutException;
import io.calimero.SerialNumber;
import io.calimero.internal.Executor;
import io.calimero.knxnetip.servicetype.KNXnetIPHeader;
import io.calimero.knxnetip.servicetype.PacketHelper;
import io.calimero.knxnetip.util.HPAI;
import io.calimero.log.LogService;
import io.calimero.secure.Keyring;
import io.calimero.secure.Keyring.Interface.Type;
import io.calimero.secure.KnxSecureException;

/**
 * Connection management for stream connections to KNXnet/IP servers, and for KNX IP secure sessions.
 */
public sealed abstract class StreamConnection implements Closeable
		permits TcpConnection, UnixDomainSocketConnection {

	private final EndpointAddress server;
	private final Logger logger;

	// session ID -> secure session
	final Map<Integer, SecureSession> sessions = new ConcurrentHashMap<>();
	// communication channel ID -> plain connection
	private final Map<Integer, ClientConnection> unsecuredConnections = new ConcurrentHashMap<>();
	// we expect fifo processing by the server with multiple ongoing connect requests
	private final List<ClientConnection> ongoingConnectRequests = Collections.synchronizedList(new ArrayList<>());

	private final Lock sessionRequestLock = new ReentrantLock();
	private volatile SecureSession inSessionRequestStage;



	/**
	 * A KNX IP secure session used over a TCP connection.
	 */
	public static final class SecureSession implements AutoCloseable {

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


		// timeout session.req -> session.res, and session.auth -> session.status
		private static final int sessionSetupTimeout = 10_000; // [ms]

		private static final Duration keepAliveInvterval = Duration.ofSeconds(30);

		private static final byte[] emptyUserPwdHash = { (byte) 0xe9, (byte) 0xc3, 0x04, (byte) 0xb9, 0x14, (byte) 0xa3,
			0x51, 0x75, (byte) 0xfd, 0x7d, 0x1c, 0x67, 0x3a, (byte) 0xb5, 0x2f, (byte) 0xe1 };


		private enum SessionState { Idle, Unauthenticated, Authenticated }


		private final StreamConnection conn;
		private final int user;
		private final SecretKey userKey;
		private final SecretKey deviceAuthKey;

		private PrivateKey privateKey;
		private final byte[] publicKey = new byte[keyLength];

		private final SerialNumber sno;

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


		SecureSession(final StreamConnection connection, final int user, final byte[] userKey,
				final byte[] deviceAuthCode, final SerialNumber serialNumber) {
			this.conn = connection;
			if (user < 1 || user > 127)
				throw new KNXIllegalArgumentException("user " + user + " out of range [1..127]");
			this.user = user;

			final byte[] key = userKey.length == 0 ? emptyUserPwdHash.clone() : userKey;
			this.userKey = SecureConnection.createSecretKey(key);

			final var authCode = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
			this.deviceAuthKey = SecureConnection.createSecretKey(authCode);

			sno = serialNumber;

			logger = LogService.getLogger("io.calimero.knxnetip." + secureSymbol + " Session " + conn.server);
		}

		/**
		 * @return the session identifier assigned by the server
		 */
		public int id() { return sessionId; }

		public int user() { return user; }

		public SecretKey userKey() { return userKey; }

		public SerialNumber serialNumber() { return sno; }

		public StreamConnection connection() { return conn; }

		@Override
		public void close() {
			close(CloseEvent.USER_REQUEST, "user request");
		}

		void close(final int initiator, final String reason) {
			if (sessionState == SessionState.Idle)
				return;

			sessionState = SessionState.Idle;
			keepAliveFuture.cancel(false);
			securedConnections.values().forEach(c -> c.close(initiator, reason, Level.DEBUG, null));
			securedConnections.clear();
			conn.sessions.remove(sessionId);

			if (conn.streamClosed())
				return;
			try {
				conn.send(newStatusInfo(sessionId, nextSendSeq(), Close));
			}
			catch (final IOException e) {
				logger.log(INFO, "I/O error closing secure session " + sessionId, e);
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

		private void setupSecureSession()
				throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException {
			conn.sessionRequestLock.lock();
			final var socketName = conn.server;
			try {
				if (sessionState == SessionState.Authenticated)
					return;
				sessionState = SessionState.Idle;
				sessionStatus = Setup;
				conn.inSessionRequestStage = this;

				logger.log(DEBUG, "setup secure session with {0}", socketName);

				initKeys();
				conn.connect();
				final byte[] sessionReq = PacketHelper.newChannelRequest(HPAI.Tcp, publicKey);
				conn.send(sessionReq);
				awaitAuthenticationStatus();

				if (sessionState == SessionState.Unauthenticated || sessionStatus != AuthSuccess) {
					sessionState = SessionState.Idle;
					throw new KnxSecureException("secure session " + SecureConnection.statusMsg(sessionStatus));
				}
				if (sessionState == SessionState.Idle)
					throw new KNXTimeoutException("timeout establishing secure session with " + socketName);

				final var delay = keepAliveInvterval.toMillis();
				keepAliveFuture = Executor.scheduledExecutor().scheduleWithFixedDelay(this::sendKeepAlive, delay, delay,
						TimeUnit.MILLISECONDS);
			}
			catch (final GeneralSecurityException e) {
				throw new KnxSecureException("error creating key pair for " + socketName, e);
			}
			catch (final IOException e) {
				close();
				final String reason = "I/O error establishing secure session with " + socketName;
				conn.close(CloseEvent.INTERNAL, reason);
				throw new KNXConnectionClosedException(reason, e);
			}
			finally {
				conn.sessionRequestLock.unlock();
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
				throw new KNXTimeoutException("timeout establishing secure session with " + conn.server);
		}

		boolean acceptServiceType(final KNXnetIPHeader h, final byte[] data, final int offset, final int length)
				throws KNXFormatException {
			final int svc = h.getServiceType();
			if (!h.isSecure())
				throw new KnxSecureException(String.format("dispatched insecure service type 0x%h to %s", svc, this));

			// ensure minimum secure wrapper frame size (6 header, 16 security info, 6 encapsulated header, 16 MAC)
			if (h.getTotalLength() < 44)
				return false;

			if (svc == SecureSessionResponse) {
				if (sessionState != SessionState.Idle) {
					logger.log(WARNING, "received session response in state {0} - ignore", sessionState);
					return true;
				}
				try {
					final byte[] serverPublicKey = parseSessionResponse(h, data, offset, conn.server);
					final byte[] auth = newSessionAuth(serverPublicKey);
					sessionState = SessionState.Unauthenticated;
					final byte[] packet = wrap(auth);
					logger.log(DEBUG, "secure session {0}, request access for user {1}", sessionId, user);
					conn.send(packet);
				}
				catch (IOException | RuntimeException e) {
					sessionStatus = AuthFailed;
					logger.log(ERROR, "negotiating session key failed", e);
				}
				synchronized (this) {
					notifyAll();
				}
			}
			else if (svc == SecureWrapper) {
				final byte[] packet = unwrap(h, data, offset);
				final var plainHeader = new KNXnetIPHeader(packet, 0);
				final var hdrLen = plainHeader.getStructLength();

				if (plainHeader.getServiceType() == SecureSessionStatus) {
					sessionStatus = newChannelStatus(plainHeader, packet, hdrLen);

					if (sessionState == SessionState.Unauthenticated) {
						if (sessionStatus == AuthSuccess)
							sessionState = SessionState.Authenticated;

						logger.log(sessionStatus == AuthSuccess ? DEBUG : ERROR, "{0} {1}",
								SecureConnection.statusMsg(sessionStatus), this);
						synchronized (this) {
							notifyAll();
						}
					}
					else if (sessionStatus == Timeout || sessionStatus == Unauthenticated) {
						logger.log(ERROR, "{0} {1}", SecureConnection.statusMsg(sessionStatus), this);
						close();
					}
				}
				else
					dispatchToConnection(plainHeader, packet, hdrLen, plainHeader.getTotalLength() - hdrLen);
			}
			else
				logger.log(WARNING, "received unsupported secure service type 0x{0} - ignore", Integer.toHexString(svc));

			return true;
		}

		private void dispatchToConnection(final KNXnetIPHeader header, final byte[] data, final int offset,
				final int length) {

			final int svcType = header.getServiceType();
			if (svcType == KNXnetIPHeader.SearchResponse || svcType == KNXnetIPHeader.DESCRIPTION_RES) {
				for (final var client : securedConnections.values())
					try {
						client.receivedServiceType(conn.server, header, data, offset);
					}
					catch (KNXFormatException | IOException e) {
						logger.log(WARNING, format("{0} error processing {1}", client, header), e);
					}
				return;
			}


			final var channelId = channelId(header, data, offset);
			var connection = securedConnections.get(channelId);
			if (connection == null) {
				synchronized (ongoingConnectRequests) {
					if (!ongoingConnectRequests.isEmpty())
						connection = ongoingConnectRequests.removeFirst();
				}
			}

			try {
				if (connection != null) {
					connection.receivedServiceType(conn.server, header, data, offset);
					if (header.getServiceType() == KNXnetIPHeader.DISCONNECT_RES) {
						logger.log(TRACE, "remove connection {0}", connection);
						securedConnections.remove(channelId);
					}
				}
				else
					logger.log(WARNING, "communication channel {0} does not exist", channelId);
			}
			catch (KNXFormatException | IOException e) {
				logger.log(WARNING, format("{0} error processing {1}", connection, header), e);
			}
		}

		private void sendKeepAlive() {
			try {
				logger.log(TRACE, "sending keep-alive");
				conn.send(newStatusInfo(sessionId, nextSendSeq(), KeepAlive));
			}
			catch (final IOException e) {
				if (sessionState == SessionState.Authenticated && !conn.streamClosed()) {
					logger.log(WARNING, "error sending keep-alive: {0}", e.getMessage());
					close();
					conn.close(CloseEvent.INTERNAL, "error sending keep-alive");
				}
			}
		}

		private byte[] wrap(final byte[] plainPacket) {
			return SecureConnection.newSecurePacket(sessionId, nextSendSeq(), sno, 0, plainPacket, secretKey);
		}

		private byte[] unwrap(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
			final Object[] fields = SecureConnection.unwrap(h, data, offset, secretKey);

			final int sid = (int) fields[0];
			if (sid != sessionId)
				throw new KnxSecureException("secure session mismatch: received ID " + sid + ", expected " + sessionId);

			final long seq = (long) fields[1];
			if (seq < rcvSeq.get())
				throw new KnxSecureException("received secure packet with sequence " + seq + " < expected " + rcvSeq);
			rcvSeq.incrementAndGet();

			final var sn = (SerialNumber) fields[2];
			final int tag = (int) fields[3];
			if (tag != 0)
				throw new KnxSecureException("expected message tag 0, received " + tag);
			final byte[] knxipPacket = (byte[]) fields[4];
			logger.log(TRACE, "received (seq {0} S/N {1}) {2}", seq, sn, HexFormat.ofDelimiter(" ").formatHex(knxipPacket));
			return knxipPacket;
		}

		private byte[] parseSessionResponse(final KNXnetIPHeader h, final byte[] data, final int offset,
				final EndpointAddress remote) throws KNXFormatException {

			if (h.getServiceType() != SecureSessionResponse)
				throw new KNXIllegalArgumentException("no secure channel response");
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

			conn.sessions.put(sessionId, this);
			conn.inSessionRequestStage = null;

			final boolean skipDeviceAuth = Arrays.equals(deviceAuthKey.getEncoded(), new byte[16]);
			if (skipDeviceAuth) {
				logger.log(WARNING, "skipping device authentication of {0} (no device key)", remote);
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
					final String packet = HexFormat.ofDelimiter(" ").formatHex(data, offset - 6, offset - 6 + 0x38);
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
			logger.log(TRACE, "authenticating (length {0}): {1}", length, HexFormat.ofDelimiter(" ").formatHex(log));

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
				throw new KnxSecureException("calculating CBC-MAC of " + HexFormat.ofDelimiter(" ").formatHex(log), e);
			}
		}

		private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
			final KeyPairGenerator gen = KeyPairGenerator.getInstance("X25519");
			return gen.generateKeyPair();
		}
	}

	StreamConnection(final EndpointAddress server) {
		this.server = server;
		logger = LogService.getLogger("io.calimero.knxnetip." + server);
	}

	/**
	 * Creates a new secure session for this TCP connection.
	 *
	 * @param user user to authenticate for the session, {@code 0 < user < 128}
	 * @param userKey user key with {@code userKey.length == 16}
	 * @param deviceAuthCode device authentication code with {@code deviceAuthCode.length == 16}, a
	 *        {@code deviceAuthCode.length == 0} will skip device authentication
	 * @return new secure session
	 */
	// TODO check if for domain sockets we can pass a nicer serial number
	public SecureSession newSecureSession(final int user, final byte[] userKey, final byte[] deviceAuthCode) {
		return new SecureSession(this, user, userKey, deviceAuthCode, SerialNumber.Zero);
	}

	public SecureSession newSecureSession(final Keyring.DecryptedInterface tunnelInterface) {
		if (tunnelInterface.type() != Type.Tunneling)
			throw new IllegalArgumentException("'" + tunnelInterface + "' is not a tunneling interface");
		return newSecureSession(tunnelInterface.user(), tunnelInterface.userKey(), tunnelInterface.deviceAuthCode());
	}

	abstract EndpointAddress localEndpoint();

	public EndpointAddress server() { return server; }

	public abstract boolean isConnected();

	abstract boolean streamClosed();

	/**
	 * Closes this connection and all its contained KNXnet/IP connections and secure sessions.
	 */
	@Override
	public void close() {
		close(CloseEvent.USER_REQUEST, "user request");
	}

	void close(final int initiator, final String reason) {
		unsecuredConnections.values().forEach(t -> t.close(initiator, reason, Level.DEBUG, null));
		unsecuredConnections.clear();

		sessions.values().forEach(s -> s.close(initiator, reason));
		sessions.clear();
	}

	abstract void send(byte[] data) throws IOException;

	void registerConnectRequest(final ClientConnection c) { ongoingConnectRequests.add(c); }

	void unregisterConnectRequest(final ClientConnection c) {
		ongoingConnectRequests.remove(c);
		registerConnection(c);
	}

	public void registerConnection(final ClientConnection c) {
		if (c.getState() == KNXnetIPConnection.OK)
			unsecuredConnections.put(c.channelId, c);
	}

	public abstract void connect() throws IOException;

	void startReceiver() {
		Executor.execute(this::runReceiveLoop, "KNXnet/IP receiver " + server());
	}

	void runReceiveLoop() {
		final int rcvBufferSize = 512;
		final byte[] data = new byte[rcvBufferSize];
		int offset = 0;

		int initiator = CloseEvent.USER_REQUEST;
		String reason = "user request";
		try {
			while (!streamClosed()) {
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
							while (skip-- > 0 && read(new byte[1], 0) != -1);
							offset = 0;
						}
					}
					catch (KNXFormatException | KnxSecureException e) {
						logger.log(WARNING, "received invalid frame", e);
						offset = 0;
					}
				}

				final int read = read(data, offset);
				if (read == -1) {
					initiator = CloseEvent.SERVER_REQUEST;
					reason = "server request";
					return;
				}
				offset += read;
			}
		}
		catch (final InterruptedIOException e) {
			Thread.currentThread().interrupt();
		}
		catch (IOException | RuntimeException e) {
			if (!streamClosed()) {
				initiator = CloseEvent.INTERNAL;
				reason = e.getMessage();
				logger.log(ERROR, "receiver communication failure", e);
			}
		}
		finally {
			close(initiator, reason);
		}
	}

	abstract int read(byte[] data, int offset) throws IOException;

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
			logger.log(WARNING, "session {0} does not exist", sessionId);
	}

	private void dispatchToConnection(final KNXnetIPHeader header, final byte[] data, final int offset)
			throws IOException, KNXFormatException {
		final int svcType = header.getServiceType();
		if (svcType == KNXnetIPHeader.SearchResponse || svcType == KNXnetIPHeader.DESCRIPTION_RES) {
			for (final var client : unsecuredConnections.values())
				client.receivedServiceType(server, header, data, offset);
			return;
		}

		final var channelId = channelId(header, data, offset);
		var connection = unsecuredConnections.get(channelId);
		if (connection == null) {
			synchronized (ongoingConnectRequests) {
				if (!ongoingConnectRequests.isEmpty())
					connection = ongoingConnectRequests.removeFirst();
			}
		}

		if (connection != null) {
			connection.receivedServiceType(server, header, data, offset);
			if (svcType == KNXnetIPHeader.DISCONNECT_RES)
				unsecuredConnections.remove(channelId);
		}
		else
			logger.log(WARNING, "communication channel {0} does not exist", channelId);
	}

	private static int channelId(final KNXnetIPHeader header, final byte[] data, final int offset) {
		// communication channel ID in the connection header of a tunneling/config request has a different offset
		// than in connection management services
		final int channelIdOffset = switch (header.getServiceType()) {
			case KNXnetIPHeader.TUNNELING_REQ, KNXnetIPHeader.DEVICE_CONFIGURATION_REQ,
					KNXnetIPHeader.TunnelingFeatureResponse, KNXnetIPHeader.TunnelingFeatureInfo,
					KNXnetIPHeader.ObjectServerRequest, KNXnetIPHeader.ObjectServerAck -> offset + 1;
			default -> offset;
		};
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

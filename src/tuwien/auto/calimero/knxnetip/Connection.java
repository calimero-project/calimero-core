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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;

/**
 * Connection management for TCP connections to KNXnet/IP servers, and for KNX IP secure sessions.
 */
public final class Connection implements Closeable {

	// pseudo connection, so we can still run with udp
	static Connection Udp = new Connection(new InetSocketAddress(0));

	// TODO we currently cannot reuse a connection once it got closed
	private final InetSocketAddress server;
	private final Socket socket;

	private final Logger logger;

	// session ID -> secure session
	private final Map<Integer, SecureSession> sessions = Collections.synchronizedMap(new HashMap<>());
	// communication channel ID -> plain connection
	private final Map<Integer, ClientConnection> unsecuredConnections = Collections.synchronizedMap(new HashMap<>());



	private static final byte[] emptyUserPwdHash = { (byte) 0xe9, (byte) 0xc3, 0x04, (byte) 0xb9, 0x14, (byte) 0xa3,
		0x51, 0x75, (byte) 0xfd, 0x7d, 0x1c, 0x67, 0x3a, (byte) 0xb5, 0x2f, (byte) 0xe1 };

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

	private static final String lock = new String(Character.toChars(0x1F512));

	private enum SessionState { Idle, Unauthenticated, Authenticated }

	public final class SecureSession implements AutoCloseable {

		private static final int SecureSessionStatus = 0x0954;

		// session status codes
		private static final int AuthSuccess = 0;
		private static final int AuthFailed = 1;
		private static final int Timeout = 3;
		private static final int KeepAlive = 4;
		private static final int Close = 5;


		private final int user;
		private final SecretKey userKey;
		private final SecretKey deviceAuthKey;

		private final byte[] sno;

		private int sessionId;
		private volatile SessionState sessionState = SessionState.Idle;
		Key secretKey;

		private final AtomicLong sendSeq = new AtomicLong();
		private final AtomicLong rcvSeq = new AtomicLong();

		// assign dummy to have it initialized
		private final Future<?> keepAliveFuture = CompletableFuture.completedFuture(Void.TYPE);

		private final Logger logger;


		public SecureSession(final int user, final byte[] userKey, final byte[] deviceAuthCode) {
			this.user = user;

			final byte[] key = userKey.length == 0 ? emptyUserPwdHash.clone() : userKey;
			this.userKey = SecureConnection.createSecretKey(key);

			final var authCode = deviceAuthCode.length == 0 ? new byte[16] : deviceAuthCode;
			this.deviceAuthKey = SecureConnection.createSecretKey(authCode);

			sno = deriveSerialNumber((InetSocketAddress) socket.getLocalSocketAddress());

			logger = LoggerFactory.getLogger("calimero.knxnetip." + lock + " Session "
					+ server.getAddress().getHostAddress() + ":" + server.getPort());
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
			unsecuredConnections.values().forEach(ClientConnection::close);
			sessions.remove(sessionId);

			if (socket.isClosed())
				return;
			final byte[] close = newStatusInfo(sessionId, nextSendSeq(), Close);
			try {
				send(close);
			}
			catch (final IOException e) {
				if (!socket.isClosed())
					logger.warn("error sending close secure session {}", sessionId, e);
			}
		}

		@Override
		public String toString() {
			return lock + " session " + sessionId + " (user " + user + "): " + sessionState;
		}

		SecretKey deviceAuthKey() { return deviceAuthKey; }

		void ensureOpen() {
			if (sessionState == SessionState.Authenticated)
				return;
		}

		long nextSendSeq() {
			return sendSeq.getAndIncrement();
		}

		long nextReceiveSeq() {
			return rcvSeq.getAndIncrement();
		}

		private byte[] newStatusInfo(final int sessionId, final long seq, final int status) {
			final ByteBuffer packet = ByteBuffer.allocate(6 + 2);
			packet.put(new KNXnetIPHeader(SecureSessionStatus, 2).toByteArray());
			packet.put((byte) status);
			final int msgTag = 0;
			return SecureConnection.newSecurePacket(sessionId, seq, sno, msgTag, packet.array(), secretKey);
		}
	}

	private Connection(final InetSocketAddress server) {
		this.server = server;
		socket = new Socket();
		logger = LoggerFactory.getLogger("calimero.knxnetip.tcp " + addressPort(server));
	}

	public Connection(final InetSocketAddress local, final InetSocketAddress server) throws KNXException {
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
				throw new KNXException("no local host address available", e);
			}
		}

		try {
			socket.bind(bind);
		}
		catch (final IOException e) {
			throw new KNXException("binding to local address " + bind, e);
		}
	}

	public InetSocketAddress localEndpoint() { return (InetSocketAddress) socket.getLocalSocketAddress(); }

	public InetSocketAddress server() { return server; }

	public boolean isConnected() { return socket.isConnected(); }

	@Override
	public void close() {
		try {
			socket.close();
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	Socket socket() { return socket; }

	void send(final byte[] data) throws IOException {
		final var os = socket.getOutputStream();
		os.write(data);
		os.flush();
	}

	// InetSocketAddress::toString always prepends a '/' even if there is no host name
	static String addressPort(final InetSocketAddress addr) {
		return addr.getAddress().getHostAddress() + ":" + addr.getPort();
	}
}

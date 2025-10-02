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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;

import io.calimero.KNXException;
import io.calimero.KnxRuntimeException;
import io.calimero.SerialNumber;

/**
 * Connection management for TCP connections to KNXnet/IP servers, and for KNX IP secure sessions.
 */
public final class TcpConnection extends StreamConnection {

	// pseudo connection, so we can still run with udp
	static final TcpConnection Udp = new TcpConnection(new InetSocketAddress(0));

	private static final Duration connectionTimeout = Duration.ofMillis(5000);

	private volatile TcpEndpointAddress localEndpoint;
	private final Socket socket;


	/**
	 * Creates a new TCP connection to a KNXnet/IP server.
	 *
	 * @param server remote endpoint address
	 * @return a new TCP connection
	 */
	public static TcpConnection newTcpConnection(final InetSocketAddress server) {
		return new TcpConnection(server);
	}

	/**
	 * Creates a new TCP connection to a KNXnet/IP server.
	 *
	 * @param local local endpoint address
	 * @param server remote endpoint address
	 * @return a new TCP connection
	 */
	public static TcpConnection newTcpConnection(final InetSocketAddress local, final InetSocketAddress server) {
		return new TcpConnection(local, server);
	}

	private TcpConnection(final InetSocketAddress server) {
		super(new TcpEndpointAddress(server));
		socket = new Socket();
		localEndpoint = new TcpEndpointAddress(new InetSocketAddress(0));
	}

	private TcpConnection(final InetSocketAddress local, final InetSocketAddress server) {
		this(server);
		InetSocketAddress bind = null;
		try {
			bind = Net.matchRemoteEndpoint(local, server, false);
			socket.bind(bind);
			// socket returns any-local after socket is closed, so keep actual address after bind
			localEndpoint = new TcpEndpointAddress((InetSocketAddress) socket.getLocalSocketAddress());
		}
		catch (final KNXException e) {
			throw new KnxRuntimeException("no local host address available", e.getCause());
		}
		catch (final IOException e) {
			throw new KnxRuntimeException("binding to local address " + bind, e);
		}
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
	@Override
	public SecureSession newSecureSession(final int user, final byte[] userKey, final byte[] deviceAuthCode) {
		final SerialNumber sno = deriveSerialNumber(localEndpoint().address());
		return new SecureSession(this, user, userKey, deviceAuthCode, sno);
	}

	private static SerialNumber deriveSerialNumber(final InetSocketAddress localEP) {
		try {
			if (localEP != null)
				return deriveSerialNumber(NetworkInterface.getByInetAddress(localEP.getAddress()));
		}
		catch (final SocketException ignore) {}
		return SerialNumber.Zero;
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

	TcpEndpointAddress localEndpoint() { return localEndpoint; }

	public TcpEndpointAddress server() { return (TcpEndpointAddress) super.server(); }

	@Override
	public String toString() {
		final var state = socket.isClosed() ? "closed"
				: socket.isConnected() ? "connected" : socket.isBound() ? "bound" : "unbound";
		return localEndpoint + "<=>" + server() + " (" + state +")";
	}

	void send(final byte[] data) throws IOException {
		final var os = socket.getOutputStream();
		os.write(data);
		os.flush();
	}

	@Override
	public synchronized void connect() throws IOException {
		if (!socket.isConnected()) {
			socket.connect(server().address(), (int) connectionTimeout.toMillis());
			localEndpoint = new TcpEndpointAddress((InetSocketAddress) socket.getLocalSocketAddress());
			startReceiver();
		}
	}

	@Override
	public boolean isConnected() {
		final var connected = socket.isConnected();
		if (socket.isClosed())
			return false;
		return connected;
	}

	@Override
	void close(final int initiator, final String reason) {
		super.close(initiator, reason);
		try {
			socket.close();
		}
		catch (final IOException ignore) {}
	}

	@Override
	boolean streamClosed() {
		return socket.isClosed();
	}

	@Override
	int read(final byte[] data, final int offset) throws IOException {
		return socket.getInputStream().read(data, offset, data.length - offset);
	}
}

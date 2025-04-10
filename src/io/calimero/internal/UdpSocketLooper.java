/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2010, 2024 B. Malinowsky

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

package io.calimero.internal;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;

/**
 * @author B. Malinowsky
 */
public abstract class UdpSocketLooper
{
	/**
	 * The socket on which to loop. It must be initialized during construction, either by
	 * supplying an already initialized socket using a constructor of this class, or
	 * directly assigning it in a constructor of a derived class
	 */
	protected DatagramSocket s;

	protected volatile boolean reboundSocket;

	/**
	 * The socket timeout, set 0 for no timeout.
	 */
	private final int timeout;

	private final int maxRcvBuf;
	private final int total;
	private final boolean closeSocket;
	private volatile boolean quit;


	/**
	 * Creates a socket looper for the supplied UDP socket and timeout parameters.
	 *
	 * @param socket the UDP socket to loop on
	 * @param closeSocket {@code true} to close the socket on {@link #quit()},
	 *        {@code false} otherwise
	 * @param receiveBufferSize sets the maximum size of the socket receive buffer
	 * @param socketTimeout the socket's receive method shall time out after a maximum of
	 *        {@code socketTimeout} milliseconds
	 * @param loopTimeout this looper shall quit after {@code loopTimeout} milliseconds
	 */
	public UdpSocketLooper(final DatagramSocket socket, final boolean closeSocket,
		final int receiveBufferSize, final int socketTimeout, final int loopTimeout)
	{
		s = socket;
		maxRcvBuf = receiveBufferSize;
		timeout = socketTimeout;
		total = loopTimeout;
		this.closeSocket = closeSocket;
	}

	/**
	 * Runs the looper.
	 *
	 * @throws IOException on socket I/O error
	 */
	public void loop() throws IOException
	{
		final long start = System.currentTimeMillis();

		final byte[] buf = new byte[maxRcvBuf];
		try {
			if (timeout > 0)
				setTimeout(timeout);
			while (!quit) {
				if (total > 0) {
					final long now = System.currentTimeMillis();
					int to = (int) (start + total - now);
					if (to <= 0)
						break;
					// query socket timeout directly from socket since subtypes might
					// have modified the timeout during looping
					final int sto = timeout();
					if (sto > 0)
						to = Math.min(to, sto);
					setTimeout(to);
				}
				try {
					receive(buf);
				}
				catch (final SocketTimeoutException e) {
					if (total == 0 || start + total > System.currentTimeMillis())
						onTimeout();
				}
				catch (final IOException e) {
					if (!reboundSocket)
						throw e;
					reboundSocket = false;
				}
			}
		}
		catch (final InterruptedIOException e) {
			Thread.currentThread().interrupt();
		}
		catch (final ClosedChannelException ignore) {}
		catch (IOException | UncheckedIOException e) {
			if (!quit)
				throw e;
		}
		finally {
			quit();
		}
	}

	protected int timeout() throws SocketException {
		if (s == null) return 0;
		return s.getSoTimeout();
	}

	protected void setTimeout(final int timeout) throws SocketException {
		if (s == null) return;
		s.setSoTimeout(timeout);
	}

	protected void receive(final byte[] buf) throws IOException {
		Arrays.fill(buf, (byte) 0);
		final DatagramPacket p = new DatagramPacket(buf, buf.length);
		s.receive(p);
		// weird jdk 17/18 behavior if socket got closed
		if (p.getLength() == 0)
			return;
		onReceive((InetSocketAddress) p.getSocketAddress(), buf, p.getOffset(), p.getLength());
	}

	/**
	 * Invoked on socket timeout.
	 */
	protected void onTimeout() {}

	/**
	 * Invoked on receiving a datagram over the socket.
	 *
	 * @param source the sender's address, where the data is coming from
	 * @param data the received data
	 * @param offset offset of the data (see {@link DatagramPacket#getOffset()})
	 * @param length length of the data (see {@link DatagramPacket#getLength()})
	 * @throws IOException on communication errors while processing the received data
	 */
	protected abstract void onReceive(InetSocketAddress source, byte[] data, int offset, int length)
		throws IOException;

	/**
	 * Quits the looper, closing the socket if requested during construction of this looper.
	 */
	public void quit()
	{
		quit = true;
		// On platforms with non-interruptible network sockets, the receiver
		// might not handle the interrupt flag for a longer period of time.
		// That's why the closeSocket option can be set during construction.
		if (closeSocket && s != null)
			s.close();
	}
}

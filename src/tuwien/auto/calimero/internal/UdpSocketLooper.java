/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2015 B. Malinowsky

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

package tuwien.auto.calimero.internal;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Datagram;
import javax.microedition.io.UDPDatagramConnection;

/**
 * @author B. Malinowsky
 */
public abstract class UdpSocketLooper
{
	/**
	 * The socket on which to loop. It must be initialized during construction, either by supplying
	 * an already initialized socket using a constructor of this class, or directly assigning it in
	 * a constructor of a derived class
	 */
	protected UDPDatagramConnection s;

	private final int maxRcvBuf;
	private final int total;
	private final boolean closeSocket;
	private volatile boolean quit;

	/**
	 * Creates a socket looper for the supplied UDP socket.
	 * <p>
	 *
	 * @param socket the UDP socket to loop on
	 * @param closeSocket <code>true</code> to close the socket on {@link #quit()},
	 *        <code>false</code> otherwise
	 */
	public UdpSocketLooper(final UDPDatagramConnection socket, final boolean closeSocket)
	{
		this(socket, closeSocket, 512, 0);
	}

	/**
	 * Creates a socket looper for the supplied UDP socket and timeout parameters.
	 * <p>
	 *
	 * @param socket the UDP socket to loop on
	 * @param closeSocket <code>true</code> to close the socket on {@link #quit()},
	 *        <code>false</code> otherwise
	 * @param receiveBufferSize sets the maximum size of the socket receive buffer
	 * @param loopTimeout this looper shall quit after <code>loopTimeout</code> milliseconds
	 */
	public UdpSocketLooper(final UDPDatagramConnection socket, final boolean closeSocket,
		final int receiveBufferSize, final int loopTimeout)
	{
		s = socket;
		maxRcvBuf = receiveBufferSize;
		total = loopTimeout;
		this.closeSocket = closeSocket;
	}

	/**
	 * Runs the looper.
	 * <p>
	 *
	 * @throws IOException
	 */
	public void loop() throws IOException
	{
		final long start = System.currentTimeMillis();
		final byte[] buf = new byte[maxRcvBuf];
		try {
			while (!quit) {
				if (total > 0) {
					final long now = System.currentTimeMillis();
					final int to = (int) (start + total - now);
					if (to <= 0)
						break;
					// query socket timeout directly from socket since subtypes might
					// have modified the timeout during looping
					// XXX Java8ME socket timeout is set once during socket creation
//					final int sto = s.getSoTimeout();
//					if (sto > 0)
//						to = Math.min(to, sto);
//					s.setSoTimeout(to);
				}
				try {
					final Datagram p = s.newDatagram(buf, buf.length);
					s.receive(p);
					final byte[] data = p.getData();
					onReceive(EndpointAddress.of(p.getAddress()), data, p.getOffset(), 
							p.getLength());
				}
				catch (final InterruptedIOException e) {
					if (total == 0 || start + total > System.currentTimeMillis())
						onTimeout();
				}
			}
		}
		catch (final InterruptedIOException e) {
			Thread.currentThread().interrupt();
		}
		catch (final IOException e) {
			if (!quit)
				throw e;
		}
		finally {
			quit();
		}
	}

	/**
	 * Invoked on socket timeout.
	 */
	protected void onTimeout()
	{}

	/**
	 * Invoked on receiving a datagram over the socket.
	 * <p>
	 *
	 * @param sender the sender's address, where the data is coming from
	 * @param data the received data
	 * @param offset offset of the data (see {@link Datagram#getOffset()})
	 * @param length length of the data (see {@link Datagram#getLength()})
	 * @throws IOException on communication errors while processing the received data
	 */
	protected abstract void onReceive(EndpointAddress sender, byte[] data, int offset, int length)
		throws IOException;

	/**
	 * Quits the looper.
	 * <p>
	 * If closing the socket was requested during construction of this looper, the socket is closed.
	 */
	public void quit()
	{
		// On platforms with non-interruptible network sockets, the receiver
		// might not handle the interrupt flag for a longer period of time.
		// That's why the closeSocket option can be set during construction.
		quit = true;
		if (closeSocket) {
			try {
				s.close();
			}
			catch (final IOException ignore) {}
		}
	}
}

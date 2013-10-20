/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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
*/

package tuwien.auto.calimero.log;

import java.io.IOException;
import java.net.Socket;

/**
 * A LogNetWriter is used to log information over a socket network connection.
 * <p>
 * A destination host is supplied on creation of this log writer, a {@link Socket} TCP
 * connection is opened and used for further logging. After {@link #close()}ing the log
 * writer, it cannot be connected again.<br>
 * For output on the socket the platform's default character set is used.
 * 
 * @author B. Malinowsky
 * @see Socket
 */
public class LogNetWriter extends LogStreamWriter
{
	/**
	 * Socket connection used as logging destination.
	 * <p>
	 */
	protected Socket s;

	/**
	 * Creates a log writer and opens a socket connection to destination <code>host</code>
	 * and <code>port</code>.
	 * <p>
	 * 
	 * @param host destination host name or IP address in textual presentation; if
	 *        <code>null</code> or an empty string is specified, an address of the
	 *        loopback interface is used
	 * @param port destination port, 0 &lt;= port &lt;= 65535
	 * @throws KNXLogException if IP address of host could not be determined or socket
	 *         binding / connecting failed
	 * @see Socket
	 */
	public LogNetWriter(final String host, final int port) throws KNXLogException
	{
		try {
			s = new Socket(host, port);
			createWriter(s.getOutputStream());
		}
		catch (final IOException e) {
			throw new KNXLogException("log to " + host + ":" + port + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Like {@link #LogNetWriter(String, int)} with the ability to set the filter log
	 * level for information logged by LogNetWriter.
	 * <p>
	 * 
	 * @param level log level used by this LogWriter to filter log information
	 * @param host destination host name or IP address in textual presentation; if
	 *        <code>null</code> or an empty string is specified, an address of the
	 *        loopback interface is used
	 * @param port destination port, 0 &lt;= port &lt;= 65535
	 * @throws KNXLogException if IP address of host could not be determined or socket
	 *         binding / connecting failed
	 */
	public LogNetWriter(final LogLevel level, final String host, final int port)
		throws KNXLogException
	{
		this(host, port);
		setLogLevel(level);
	}

	/**
	 * Returns the remote host IP address of this log writer, or "" if the connection was
	 * closed.
	 * <p>
	 * 
	 * @return IP address as String
	 * @see java.net.InetAddress#getHostAddress()
	 */
	public final String getHostAddress()
	{
		final Socket socket = s;
		return socket == null ? "" : socket.getInetAddress().getHostAddress();
	}

	/**
	 * Returns the remote host name of this log writer, or "" if the connection was
	 * closed.
	 * <p>
	 * Note that this might involve a reverse name lookup. It is possible that only the IP
	 * address string is returned.
	 * 
	 * @return host name (or IP address) as String
	 * @see java.net.InetAddress#getHostName()
	 */
	public final String getHostName()
	{
		final Socket socket = s;
		return socket == null ? "" : socket.getInetAddress().getHostName();
	}

	/**
	 * Returns the destination port of this log writer, or 0 if the connection was closed.
	 * <p>
	 * 
	 * @return port number as unsigned
	 */
	public final int getPort()
	{
		final Socket socket = s;
		return socket == null ? 0 : socket.getPort();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogStreamWriter#close()
	 */
	public synchronized void close()
	{
		if (s != null) {
			super.close();
			try {
				s.close();
			}
			catch (final IOException e) {}
			s = null;
		}
	}
}

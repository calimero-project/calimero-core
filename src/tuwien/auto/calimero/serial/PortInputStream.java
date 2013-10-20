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

package tuwien.auto.calimero.serial;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream for a serial port.
 * <p>
 * 
 * @author B. Malinowsky
 */
class PortInputStream extends InputStream
{
	private final SerialComAdapter p;

	/**
	 * Creates a new input stream for <code>port</code>.
	 * <p>
	 * 
	 * @param port open port for input
	 */
	public PortInputStream(final SerialComAdapter port)
	{
		p = port;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException
	{
		return p.read();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(final byte[] b) throws IOException
	{
		if (b == null)
			throw new NullPointerException();
		return p.readBytes(b, 0, b.length);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(final byte[] b, final int off, final int len) throws IOException
	{
		if (b == null)
			throw new NullPointerException();
		if (off < 0 || len < 0 || len > b.length - off)
			throw new IndexOutOfBoundsException();
		return p.readBytes(b, off, len);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	public int available()
	{
		return p.getStatus(SerialComAdapter.AVAILABLE_INPUT_STATUS);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException
	{
		super.close();
	}
}

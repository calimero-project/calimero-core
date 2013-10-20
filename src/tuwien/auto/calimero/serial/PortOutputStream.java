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
import java.io.OutputStream;

/**
 * Output stream for a serial port.
 * <p>
 * 
 * @author B. Malinowsky
 */
class PortOutputStream extends OutputStream
{
	private final SerialComAdapter p;

	/**
	 * Creates a new output stream for <code>port</code>.
	 * <p>
	 * @param port open port for output
	 */
	public PortOutputStream(final SerialComAdapter port)
	{
		p = port;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(final int b) throws IOException
	{
		p.write(b);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(final byte[] b) throws IOException
	{
		if (b == null)
			throw new NullPointerException();
		p.writeBytes(b, 0, b.length);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(final byte[] b, final int off, final int len) throws IOException
	{
		if (b == null)
			throw new NullPointerException();
		if (off < 0 || off > b.length || len < 0 || off + len > b.length
			|| off + len < 0)
			throw new IndexOutOfBoundsException();
		p.writeBytes(b, off, len);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException
	{
		super.flush();
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException
	{
		super.flush();
		super.close();
	}
}

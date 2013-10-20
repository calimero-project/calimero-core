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

import tuwien.auto.calimero.exception.KNXException;

/**
 * Thrown to indicate illegal access to a closed serial port.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class KNXPortClosedException extends KNXException
{
	private static final long serialVersionUID = 1L;

	private final String id;

	/**
	 * Constructs a new <code>KNXPortClosedException</code> without a detail
	 * message.
	 * <p>
	 */
	public KNXPortClosedException()
	{
		id = null;
	}

	/**
	 * Constructs a new <code>KNXPortClosedException</code> with the specified
	 * detail message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXPortClosedException(final String s)
	{
		super(s);
		id = null;
	}

	/**
	 * Constructs a new <code>KNXPortClosedException</code> with the specified
	 * detail message and the serial port identifier.
	 * <p>
	 * 
	 * @param s the detail message
	 * @param portID serial port identifier of the closed port
	 */
	public KNXPortClosedException(final String s, final String portID)
	{
		super(s);
		id = portID;
	}

	/**
	 * Returns the serial port identifier supplied with this exception.
	 * <p>
	 * 
	 * @return port ID as string, or <code>null</code> if no port ID specified
	 */
	public final String getPortID()
	{
		return id;
	}
}

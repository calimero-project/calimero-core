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

package tuwien.auto.calimero.mgmt;

import tuwien.auto.calimero.exception.KNXException;

/**
 * Thrown to indicate that a layer 4 disconnect event happened.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class KNXDisconnectException extends KNXException
{
	private static final long serialVersionUID = 1L;

	private final Destination dst;

	/**
	 * Constructs a <code>KNXDisconnectException</code> without a detail message.
	 * <p>
	 */
	public KNXDisconnectException()
	{
		dst = null;
	}

	/**
	 * Constructs a <code>KNXDisconnectException</code> with the specified detail
	 * message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXDisconnectException(final String s)
	{
		super(s);
		dst = null;
	}

	/**
	 * Constructs a <code>KNXDisconnectException</code> with the specified detail
	 * message and the affected destination object.
	 * <p>
	 * 
	 * @param s the detail message
	 * @param d destination causing this exception by its disconnected state
	 */
	public KNXDisconnectException(final String s, final Destination d)
	{
		super(s);
		dst = d;
	}

	/**
	 * Returns the destination which caused this exception.
	 * <p>
	 * 
	 * @return the Destination, or <code>null</code> if not supplied
	 */
	public final Destination getDestination()
	{
		return dst;
	}
}

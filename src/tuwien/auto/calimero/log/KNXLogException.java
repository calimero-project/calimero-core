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

import tuwien.auto.calimero.exception.KNXException;

/**
 * Thrown to indicate a problem while logging.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class KNXLogException extends KNXException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a <code>KNXLogException</code> without a detail message.
	 * <p>
	 */
	public KNXLogException()
	{}

	/**
	 * Constructs a <code>KNXLogException</code> with the specified detail message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXLogException(final String s)
	{
		super(s);
	}
	
	/**
	 * Constructs a <code>KNXLogException</code> with the specified detail message and
	 * cause.
	 * <p>
	 * 
	 * @param s the detail message
	 * @param cause the cause in form of a throwable object, can be <code>null</code>
	 */
	public KNXLogException(final String s, final Throwable cause)
	{
		super(s, cause);
	}
}

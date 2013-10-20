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

package tuwien.auto.calimero.exception;

/**
 * Thrown to indicate that a timeout occurred.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class KNXTimeoutException extends KNXException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>KNXTimeoutException</code> without a detail message.
	 * <p>
	 */
	public KNXTimeoutException()
	{}

	/**
	 * Constructs a new <code>KNXTimeoutException</code> with the specified detail
	 * message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXTimeoutException(final String s)
	{
		super(s);
	}

	/**
	 * Constructs a new <code>KNXTimeoutException</code> with the specified detail message
	 * and cause.
	 * <p>
	 * 
	 * @param s the detail message
	 * @param cause the cause
	 */
	public KNXTimeoutException(final String s, final Throwable cause)
	{
		super(s, cause);
	}
}

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
 * Thrown to indicate that a method has been passed an illegal or inappropriate argument.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class KNXIllegalArgumentException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	private final String arg;

	/**
	 * Constructs a new <code>KNXIllegalArgumentException</code> without a detail message.
	 * <p>
	 */
	public KNXIllegalArgumentException()
	{
		arg = null;
	}

	/**
	 * Constructs a new <code>KNXIllegalArgumentException</code> with the specified detail
	 * message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXIllegalArgumentException(final String s)
	{
		super(s);
		arg = null;
	}

	/**
	 * Constructs a new <code>KNXIllegalArgumentException</code> with the specified detail
	 * message and cause.
	 * <p>
	 * 
	 * @param s the detail message
	 * @param cause the cause (which is saved for later retrieval by the
	 *        {@link #getCause()} method).
	 */
	public KNXIllegalArgumentException(final String s, final Throwable cause)
	{
		super(s, cause);
		arg = null;
	}

	/**
	 * Returns the argument which caused the exception.
	 * <p>
	 * 
	 * @return argument as string, or <code>null</code> if no argument was set
	 */
	public final String getArgument()
	{
		return arg;
	}
}

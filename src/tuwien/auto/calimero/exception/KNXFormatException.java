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
 * Thrown when some value or content is not in accordance with the expected or requested
 * format or result.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class KNXFormatException extends KNXException
{
	private static final long serialVersionUID = 1L;

	private final String item;

	/**
	 * Constructs a new <code>KNXFormatException</code> without a detail message.
	 * <p>
	 */
	public KNXFormatException()
	{
		item = null;
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail
	 * message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXFormatException(final String s)
	{
		super(s);
		item = null;
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail
	 * message and the invalid item.
	 * <p>
	 * 
	 * @param s the detail message
	 * @param item value, content or piece of information causing this exception (allowed
	 *        to be <code>null</code>)
	 */
	public KNXFormatException(final String s, final String item)
	{
		super(s);
		this.item = item;
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail
	 * message and the invalid item value of type int.
	 * <p>
	 * The <code>item</code> value is formatted into a hexadecimal string representation
	 * using the format "0x" prefix + value (e.g. "0x23" for an item value of 23).
	 * 
	 * @param s the detail message
	 * @param item the value causing this exception
	 */
	public KNXFormatException(final String s, final int item)
	{
		super(s);
		this.item = "0x" + Integer.toHexString(item);
	}

	/**
	 * Returns the value, content, or piece of information which caused the exception.
	 * <p>
	 * 
	 * @return item representation as string, or <code>null</code> if no item was set
	 */
	public final String getItem()
	{
		return item;
	}
}

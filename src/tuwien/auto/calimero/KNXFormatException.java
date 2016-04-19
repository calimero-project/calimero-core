/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

package tuwien.auto.calimero;

/**
 * Thrown when some value or content is not in accordance with the expected or requested format or result.
 *
 * @author B. Malinowsky
 */
public class KNXFormatException extends KNXException
{
	private static final long serialVersionUID = 1L;

	private final String item;

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail message.
	 *
	 * @param s the detail message
	 */
	public KNXFormatException(final String s)
	{
		super(s);
		item = null;
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail message and cause.
	 *
	 * @param s the detail message
	 * @param cause the cause in form of a throwable object, can be <code>null</code>
	 */
	public KNXFormatException(final String s, final Throwable cause)
	{
		super(s, cause);
		this.item = null;
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail message and the invalid item.
	 *
	 * @param s the detail message
	 * @param item value, content or piece of information causing this exception (allowed to be <code>null</code>)
	 */
	public KNXFormatException(final String s, final String item)
	{
		this(s, item, null);
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail message, invalid item, and cause.
	 *
	 * @param s the detail message
	 * @param item value, content or piece of information causing this exception (allowed to be <code>null</code>)
	 * @param cause the cause in form of a throwable object, can be <code>null</code>
	 */
	public KNXFormatException(final String s, final String item, final Throwable cause)
	{
		super(s + ": " + item);
		this.item = item;
	}

	/**
	 * Constructs a new <code>KNXFormatException</code> with the specified detail message and the invalid item value of
	 * type int. The <code>item</code> value is formatted into a hexadecimal string representation using the format "0x"
	 * prefix + value (e.g. "0x23" for an item value of hexadecimal 23).
	 *
	 * @param s the detail message
	 * @param item the value causing this exception
	 */
	public KNXFormatException(final String s, final int item)
	{
		super(s + ": 0x" + Integer.toHexString(item));
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

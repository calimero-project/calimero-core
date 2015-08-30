/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

package tuwien.auto.calimero.serial;

import tuwien.auto.calimero.KNXException;

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
	 * Constructs a new <code>KNXPortClosedException</code> with the specified detail message,
	 * cause, and port identifier.
	 *
	 * @param s the detail message
	 * @param portId port identifier, can be <code>null</code>
	 * @param cause the cause in form of a throwable object, can be <code>null</code>
	 */
	public KNXPortClosedException(final String s, final String portId, final Throwable cause)
	{
		super(s, cause);
		id = portId;
	}

	/**
	 * Constructs a new <code>KNXPortClosedException</code> with the specified detail message and
	 * the serial port identifier.
	 *
	 * @param s the detail message
	 * @param portId serial port identifier of the closed port, can be <code>null</code>
	 */
	public KNXPortClosedException(final String s, final String portId)
	{
		super(s);
		id = portId;
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

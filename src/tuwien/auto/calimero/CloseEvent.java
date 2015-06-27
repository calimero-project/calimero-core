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

package tuwien.auto.calimero;

import java.util.EventObject;

import tuwien.auto.calimero.link.LinkListener;

/**
 * Informs about the closing of a previously established communication or communication
 * provider with a KNX network.
 * <p>
 * In general, the source of the event is the connection object or network link to be
 * closed.
 *
 * @author B. Malinowsky
 * @see LinkListener
 * @see KNXListener
 */
public class CloseEvent extends EventObject
{
	/**
	 * Identifies the close event to originate from the user (or owner) of the network
	 * endpoint or connection.
	 */
	public static final int USER_REQUEST = 0;

	/**
	 * Identifies the close event to originate from the KNXnet/IP server side.
	 */
	public static final int SERVER_REQUEST = 1;

	/**
	 * Identifies the close event to originate from the KNXnet/IP client side.
	 */
	public static final int CLIENT_REQUEST = 2;

	/**
	 * Identifies the close event to originate from an internal communication event, for
	 * example, initiated by an unsupported protocol version.
	 */
	public static final int INTERNAL = 3;

	private static final long serialVersionUID = 1L;

	private final int initiator;

	private final String msg;

	/**
	 * Creates a new close event object.
	 * <p>
	 *
	 * @param source the communication object to be closed
	 * @param initiator the initiator of this close event, one of {@link #USER_REQUEST},
	 *        {@link #SERVER_REQUEST}, and {@link #INTERNAL}
	 * @param reason brief description of the reason leading to the close event
	 */
	public CloseEvent(final Object source, final int initiator, final String reason)
	{
		super(source);
		this.initiator = initiator;
		msg = reason;
	}

	/**
	 * Returns the initiator of the close event, see the declared initiator constants of
	 * this class or its sub-types.
	 * <p>
	 * For the base class {@link CloseEvent}, defined are {@link #USER_REQUEST},
	 * {@link #SERVER_REQUEST}, and {@link #INTERNAL}.
	 *
	 * @return identifier stating the initiator of this close event
	 */
	public final int getInitiator()
	{
		return initiator;
	}

	/**
	 * Returns a brief textual description why the close event was initiated.
	 * <p>
	 *
	 * @return close reason as string
	 */
	public final String getReason()
	{
		return msg;
	}
}

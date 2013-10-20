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
	 * <p>
	 */
	public static final int USER_REQUEST = 0;

	/**
	 * Identifies the close event to originate from the KNXnet/IP server side.
	 * <p>
	 */
	public static final int SERVER_REQUEST = 1;

	/**
	 * Identifies the close event to originate from the KNXnet/IP client side.
	 * <p>
	 */
	public static final int CLIENT_REQUEST = 2;
	
	/**
	 * Identifies the close event to originate from an internal communication event, for
	 * example, initiated by an unsupported protocol version.
	 * <p>
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
	 * @param userRequest <code>true</code> if the closing was requested by the user of
	 *        the object, <code>false</code> otherwise (for example, a close initiated by
	 *        a remote server)
	 * @param reason brief description of the reason leading to the close event
	 */
	//public CloseEvent(final Object source, final boolean userRequest, final String reason)
	//	{
	//		super(source);
	//		initiator = USER_REQUEST;
	//		msg = reason;
	//	}

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
	 * Returns whether the close event was initiated by the user of the communication
	 * object.
	 * <p>
	 * 
	 * @return <code>true</code> if close is user requested, <code>false</code> otherwise
	 */
	//public final boolean isUserRequest()
	//{
	//	return initiator == USER_REQUEST;
	//}

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

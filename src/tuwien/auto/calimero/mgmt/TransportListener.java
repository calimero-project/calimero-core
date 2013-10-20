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

import java.util.EventListener;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;

/**
 * Listener interface for getting transport layer events.
 * <p>
 * 
 * @author B. Malinowsky
 * @see TransportLayer
 */
public interface TransportListener extends EventListener
{
	/**
	 * Indicates that a KNX group message was received from the KNX network.
	 * <p>
	 * 
	 * @param e frame event object
	 */
	void group(FrameEvent e);

	/**
	 * Indicates that a KNX broadcast message was received from the KNX network.
	 * <p>
	 * 
	 * @param e frame event object
	 */
	void broadcast(FrameEvent e);

	/**
	 * Indicates that a KNX message with data individual service was received from the KNX
	 * network.
	 * <p>
	 * Such data is sent from destinations communicating in connectionless mode.
	 * 
	 * @param e frame event object
	 */
	void dataIndividual(FrameEvent e);

	/**
	 * Indicates that a KNX message with data connected service was received from the KNX
	 * network.
	 * <p>
	 * Such data is sent from destinations communicating in connection oriented mode.
	 * 
	 * @param e frame event object
	 */
	void dataConnected(FrameEvent e);

	/**
	 * The connection state of a destination changed from connected to disconnected.
	 * <p>
	 * 
	 * @param d the disconnected destination
	 */
	void disconnected(Destination d);

	/**
	 * The KNX network link was detached from the transport layer.
	 * 
	 * @param e detach event object
	 */
	void detached(DetachEvent e);

	/**
	 * The attached KNX network link was closed.
	 * <p>
	 * 
	 * @param e close event object
	 */
	void linkClosed(CloseEvent e);
}

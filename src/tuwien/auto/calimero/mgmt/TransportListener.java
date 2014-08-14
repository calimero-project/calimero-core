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

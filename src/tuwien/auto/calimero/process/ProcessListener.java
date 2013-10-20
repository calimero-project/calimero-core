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

package tuwien.auto.calimero.process;

import java.util.EventListener;

import tuwien.auto.calimero.DetachEvent;

/**
 * Listener interface for getting process communicator events.
 * <p>
 * 
 * @author B. Malinowsky
 * @see ProcessCommunicator
 */
public interface ProcessListener extends EventListener
{
	/**
	 * Indicates that a KNX group write message indication was received from the KNX
	 * network.
	 * <p>
	 * 
	 * @param e process event object
	 */
	void groupWrite(ProcessEvent e);

	/**
	 * The KNX network link was detached from the process communicator.
	 * 
	 * @param e detach event object
	 */
	void detached(DetachEvent e);
}

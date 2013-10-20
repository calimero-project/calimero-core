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

package tuwien.auto.calimero.link;

import tuwien.auto.calimero.FrameEvent;

/**
 * Listener interface for getting events from a KNX network link.
 * <p>
 * 
 * @author B. Malinowsky
 */
public interface NetworkLinkListener extends LinkListener
{
	/**
	 * Invoked to indicate the confirmation to a preceding request to the KNX network.
	 * <p>
	 * Both positive and negative confirmations are provided here.
	 * 
	 * @param e frame event object
	 */
	void confirmation(FrameEvent e);
}

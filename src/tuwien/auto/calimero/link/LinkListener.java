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

import java.util.EventListener;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;

/**
 * Base listener interface for getting events from a KNX link (network or monitor link).
 * <p>
 * 
 * @author B. Malinowsky
 */
public interface LinkListener extends EventListener
{
	/**
	 * Invoked on arrival of a new KNX indication message from the KNX network.
	 * <p>
	 * 
	 * @param e frame event object
	 */
	void indication(FrameEvent e);

	/**
	 * Invoked after close of the link.
	 * <p>
	 * 
	 * @param e close event object
	 */
	void linkClosed(CloseEvent e);
}

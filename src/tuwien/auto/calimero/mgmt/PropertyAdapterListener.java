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

import tuwien.auto.calimero.CloseEvent;

/**
 * Listener interface to receive events of a property adapter.
 * <p>
 * 
 * @author B. Malinowsky
 * @see PropertyAdapter
 */
public interface PropertyAdapterListener
{
	/**
	 * The adapter is closed.
	 * <p>
	 * This notification is primary for detecting a disconnection initiated by the
	 * remote endpoint.
	 * 
	 * @param e close event object
	 */
	void adapterClosed(CloseEvent e);
}

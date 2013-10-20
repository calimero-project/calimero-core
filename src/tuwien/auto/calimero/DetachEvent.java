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

/**
 * Informs about the detaching of an object from a KNX link.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class DetachEvent extends EventObject
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new detach event with the object being detached.
	 * <p>
	 * 
	 * @param source the object which is detached from the link
	 */
	public DetachEvent(final Object source)
	{
		super(source);
	}
}

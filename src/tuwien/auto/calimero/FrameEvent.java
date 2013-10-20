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

import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIFactory;

/**
 * Event to communicate the arrival of a new cEMI or EMI2 frame.
 * <p>
 * Depending on the type of frame supplied on creation of a new frame event, either
 * {@link #getFrame()} or {@link #getFrameBytes()} has to be used to retrieve the
 * associated frame.
 * 
 * @see KNXListener
 */
public class FrameEvent extends EventObject
{
	private static final long serialVersionUID = 1L;

	private final CEMI c;
	private final byte[] b;

	/**
	 * Creates a new frame event for <code>frame</code>.
	 * <p>
	 * 
	 * @param source the creator of this event
	 * @param frame cEMI frame
	 */
	public FrameEvent(final Object source, final CEMI frame)
	{
		super(source);
		c = frame;
		b = null;
	}

	/**
	 * Creates a new frame event for <code>frame</code>.
	 * <p>
	 * 
	 * @param source the creator of this event
	 * @param frame EMI2 L-data frame
	 */
	public FrameEvent(final Object source, final byte[] frame)
	{
		super(source);
		b = frame;
		c = null;
	}
	
	/**
	 * Returns the cEMI frame, if supplied at event creation.
	 * <p>
	 * 
	 * @return cEMI frame object, or <code>null</code>
	 */
	public final CEMI getFrame()
	{
		return CEMIFactory.copy(c);
	}
	
	/**
	 * Returns the frame as byte array, if supplied at event creation.
	 * <p>
	 * 
	 * @return copy of frame as byte array, or <code>null</code>
	 */
	public final byte[] getFrameBytes()
	{
		return b != null ? (byte[]) b.clone() : null;
	}
}

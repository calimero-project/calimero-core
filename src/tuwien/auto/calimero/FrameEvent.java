/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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
import java.util.concurrent.atomic.AtomicLong;

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

	private static final AtomicLong idCounter = new AtomicLong();

	private final long id;
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
		id = idCounter.incrementAndGet();
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
		id = idCounter.incrementAndGet();
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
		return b != null ? b.clone() : null;
	}

	public final long id() { return id; }
}

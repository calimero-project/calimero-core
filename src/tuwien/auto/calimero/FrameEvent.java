/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2020 B. Malinowsky

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
import java.util.Optional;

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

	private final long id;
	private final CEMI c;
	private final byte[] b;

	private final boolean systemBroadcast;
	private final SecurityControl securityCtrl;

	/**
	 * Creates a new frame event for <code>frame</code>.
	 *
	 * @param source the creator of this event
	 * @param frame cEMI frame
	 */
	public FrameEvent(final Object source, final CEMI frame) {
		this(source, frame, false);
	}

	/**
	 * Creates a new frame event for <code>frame</code>.
	 *
	 * @param source the creator of this event
	 * @param frame cEMI frame
	 * @param systemBroadcast <code>true</code> if the cEMI frame was received as IP system broadcast,
	 *        <code>false</code> otherwise
	 */
	public FrameEvent(final Object source, final CEMI frame, final boolean systemBroadcast)
	{
		this(source, frame, systemBroadcast, null);
	}

	/**
	 * Creates a new frame event for <code>frame</code>.
	 *
	 * @param source the creator of this event
	 * @param frame cEMI frame
	 * @param systemBroadcast <code>true</code> if the cEMI frame was received as IP system broadcast,
	 *        <code>false</code> otherwise
	 * @param securityCtrl data security control
	 */
	public FrameEvent(final Object source, final CEMI frame, final boolean systemBroadcast,
			final SecurityControl securityCtrl) {
		super(source);
		id = System.nanoTime();
		c = frame;
		b = null;
		this.systemBroadcast = systemBroadcast;
		this.securityCtrl = securityCtrl;
	}

	/**
	 * Creates a new frame event for <code>frame</code>.
	 *
	 * @param source the creator of this event
	 * @param frame EMI2 L-data frame
	 */
	public FrameEvent(final Object source, final byte[] frame)
	{
		super(source);
		id = System.nanoTime();
		b = frame;
		c = null;
		systemBroadcast = false;
		securityCtrl = null;
	}

	/**
	 * Returns the cEMI frame, if supplied at event creation.
	 *
	 * @return cEMI frame object, or <code>null</code>
	 */
	public final CEMI getFrame()
	{
		return CEMIFactory.copy(c);
	}

	/**
	 * Returns the frame as byte array, if supplied at event creation.
	 *
	 * @return copy of frame as byte array, or <code>null</code>
	 */
	public final byte[] getFrameBytes()
	{
		return b != null ? b.clone() : null;
	}

	public final long id() { return id; }

	public final boolean systemBroadcast() { return systemBroadcast; }

	public final Optional<SecurityControl> security() { return Optional.ofNullable(securityCtrl); }
}

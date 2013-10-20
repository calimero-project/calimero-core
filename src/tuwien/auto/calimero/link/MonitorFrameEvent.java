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
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.link.medium.RawFrame;

/**
 * Informs about a new monitor indication received from the KNX network and contains the
 * received indication frame.
 * <p>
 * The source of the event is a {@link KNXNetworkMonitor}.
 * 
 * @author B. Malinowsky
 */
public class MonitorFrameEvent extends FrameEvent
{
	private static final long serialVersionUID = 1L;

	private final RawFrame raw;
	private final KNXFormatException e;

	/**
	 * Creates a new monitor frame event with the indication frame.
	 * 
	 * @param source the network monitor which received the frame
	 * @param frame monitor indication frame encapsulated in cEMI type
	 */
	public MonitorFrameEvent(final KNXNetworkMonitor source, final CEMI frame)
	{
		super(source, frame);
		raw = null;
		e = null;
	}

	/**
	 * Creates a new monitor frame event with the indication frame and the decoded raw
	 * frame.
	 * 
	 * @param source the network monitor which received the frame
	 * @param frame monitor indication frame encapsulated in cEMI type
	 * @param rawFrame the decoded raw frame on medium encapsulated in type RawFrame, use
	 *        <code>null</code> if no decoded raw frame is available
	 */
	public MonitorFrameEvent(final KNXNetworkMonitor source, final CEMI frame,
		final RawFrame rawFrame)
	{
		super(source, frame);
		raw = rawFrame;
		e = null;
	}

	/**
	 * Creates a new monitor frame event with the indication frame and error information about a
	 * failed decoding of the raw frame.
	 * 
	 * @param source the network monitor which received the frame
	 * @param frame monitor indication frame encapsulated in cEMI type
	 * @param decodeError the exception object obtained during decoding the received raw
	 *        frame on medium
	 */
	public MonitorFrameEvent(final KNXNetworkMonitor source, final CEMI frame,
		final KNXFormatException decodeError)
	{
		super(source, frame);
		raw = null;
		e = decodeError;
	}
	
	/**
	 * Returns the decoded raw frame on medium.
	 * <p>
	 * If decoding of raw frames is enabled and this method returns <code>null</code>, try
	 * {@link #getDecodeError()} for decode error information.
	 * 
	 * @return the frame of type RawFrame or <code>null</code> on no decoded raw frame
	 */
	public final RawFrame getRawFrame()
	{
		return raw;
	}
	
	/**
	 * Returns the exception object obtained during creation of a decoded raw frame, providing error
	 * information while decoding the raw frame on medium.<p>
	 * 
	 * If decoding of raw frames is disabled, this method returns always <code>null</code>.
	 * 
	 * @return the exception object, or <code>null</code> if {@link #getRawFrame()} returns the
	 *         decoded {@link RawFrame}, i.e., not <code>null</code>
	 */
	public final KNXFormatException getDecodeError()
	{
		return e;
	}
}

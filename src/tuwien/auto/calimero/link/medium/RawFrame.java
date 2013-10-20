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

package tuwien.auto.calimero.link.medium;

/**
 * Base interface used for raw network frames.
 * <p>
 * A raw frame is a KNX message in a KNX network sent on a particular communication
 * medium, built up and consisting of medium specific parts within the frame.
 * <p>
 * Subtypes should offer decoding of message parts up to OSI Layer 4 (TPDU) if possible.
 * 
 * @author B. Malinowsky
 */
public interface RawFrame
{
	// NYI TP0 communication medium frame format
	// NYI RF communication medium frame format

	/**
	 * Indicates a L-data frame type.
	 * <p>
	 * 
	 * @see #getFrameType()
	 */
	int LDATA_FRAME = 0;

	/**
	 * Indicates a L-poll-data frame type.
	 * <p>
	 * 
	 * @see #getFrameType()
	 */
	int LPOLLDATA_FRAME = 1;

	/**
	 * Indicates an L-ack frame type.
	 * <p>
	 * 
	 * @see #getFrameType()
	 */
	int ACK_FRAME = 2;

	/**
	 * Returns the type of this frame.
	 * <p>
	 * 
	 * @return unsigned frame type identifier
	 */
	int getFrameType();
}

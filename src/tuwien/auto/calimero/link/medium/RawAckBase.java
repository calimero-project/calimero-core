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
 * Implementation of common functionality for raw acknowledgment frames.
 * <p>
 * Objects of this type are considered immutable.
 * 
 * @author B. Malinowsky
 */
public abstract class RawAckBase implements RawFrame
{
	/**
	 * Positive acknowledge type.
	 * <p>
	 * 
	 * @see #getAckType()
	 */
	public static final int ACK = 0xCC;

	/**
	 * Negative acknowledge type.
	 * <p>
	 * 
	 * @see #getAckType()
	 */
	public static final int NAK = 0x0C;

	/**
	 * Acknowledge type transmitted with this frame.
	 * <p>
	 */
	protected int ack;

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.RawFrame#getFrameType()
	 */
	public final int getFrameType()
	{
		return ACK_FRAME;
	}

	/**
	 * Returns the acknowledge type transmitted with this frame.
	 * <p>
	 * 
	 * @return type of acknowledge as unsigned byte
	 */
	public final int getAckType()
	{
		return ack;
	}

	/**
	 * Returns a textual representation of this acknowledgment frame information.
	 * <p>
	 * 
	 * @return string representation of the object
	 */
	public String toString()
	{
		return ack == ACK ? "ACK" : "NAK";
	}
}

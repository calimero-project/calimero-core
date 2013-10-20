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

import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * Raw acknowledgment frame on TP1 communication medium.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class TP1Ack extends RawAckBase
{
	/**
	 * Acknowledge type busy.
	 * <p>
	 * The remote data link layer does not have resources to process the request.
	 * 
	 * @see #getAckType()
	 */
	public static final int BUSY = 0xC0;

	/**
	 * Acknowledge type {@link RawAckBase#NAK} and {@link TP1Ack#BUSY}.
	 * <p>
	 * An acknowledgment frame of this type shall be handled as acknowledge type 'busy'.
	 * 
	 * @see #getAckType()
	 */
	public static final int NAK_BUSY = 0x00;

	/**
	 * Creates a new TP1 acknowledgment frame out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing the acknowledgment frame structure
	 * @param offset start offset of frame structure in <code>data</code>, offset &gt;=
	 *        0
	 * @throws KNXFormatException if no valid acknowledgment frame was found
	 */
	public TP1Ack(final byte[] data, final int offset) throws KNXFormatException
	{
		final int ctrl = data[offset] & 0xff;
		if ((ctrl & 0x33) != 0x00)
			throw new KNXFormatException("no TP1 ACK frame, invalid control field", ctrl);
		if (ctrl == ACK)
			ack = ACK;
		else if (ctrl == NAK)
			ack = NAK;
		else if (ctrl == BUSY)
			ack = BUSY;
		else if (ctrl == NAK_BUSY)
			ack = NAK_BUSY;
		else
			throw new KNXFormatException("no valid acknowledge type", ctrl);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.RawAckBase#toString()
	 */
	public String toString()
	{
		return ack == BUSY ? "BUSY" : ack == NAK_BUSY ? "NAK-BUSY" : super.toString();
	}
}

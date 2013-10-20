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
 * Raw acknowledgment frame on PL110 communication medium.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class PL110Ack extends RawAckBase
{
	/**
	 * Creates a new PL110 acknowledgment frame out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing the acknowledgment frame structure
	 * @param offset start offset of frame structure in <code>data</code>, offset &gt;=
	 *        0
	 * @throws KNXFormatException if no valid PL110 acknowledgment frame was found
	 */
	public PL110Ack(final byte[] data, final int offset) throws KNXFormatException
	{
		final int ctrl = data[offset] & 0xff;
		if (ctrl == ACK)
			ack = ACK;
		else if (ctrl == NAK)
			ack = NAK;
		else if ((ctrl & 0xD3) == 0x90)
			// filter L-Data.req ID
			throw new KNXFormatException("no PL110 ACK frame, L-Data.req control field");
		else
			// everything else is interpreted as NAK
			ack = NAK;
	}
}

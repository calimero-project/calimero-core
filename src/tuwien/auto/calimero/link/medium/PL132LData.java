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

import java.io.ByteArrayInputStream;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * L-data frame format on PL132 communication medium.
 * <p>
 * Supports standard and extended frame format.
 * 
 * @author B. Malinowsky
 */
public class PL132LData extends RawFrameBase
{
	private static final int MIN_LENGTH = 9;

	private final byte[] doa;
	private final boolean ack;
	
	/**
	 * Creates a new L-data frame out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing the L-data frame
	 * @param offset start offset of frame structure in <code>data</code>, offset &gt;=
	 *        0
	 * @throws KNXFormatException if length of data too short for frame, on no valid frame
	 *         structure
	 */
	public PL132LData(final byte[] data, final int offset) throws KNXFormatException
	{
		final ByteArrayInputStream is = asStream(data, offset, MIN_LENGTH, "L-data");
		doa = new byte[2];
		is.read(doa, 0, 2);
		final int ctrl = is.read();
		// parse control field and check if valid
		if ((ctrl & 0xC) != 0xC)
			throw new KNXFormatException("invalid control field", ctrl);
		type = LDATA_FRAME;
		ext = (ctrl & 0x80) == 0;
		repetition = (ctrl & 0x40) == 0;
		p = Priority.get(ctrl & 0x3);
		final boolean group = (ctrl & 0x20) == 0x20;
		ack = (ctrl & 0x10) == 0x10;
		// check fourth byte for extended control field
		final int ctrle = ext ? readCtrlEx(is) : 0;
		src = new IndividualAddress((is.read() << 8) | is.read());
		setDestination((is.read() << 8) | is.read(), group);

		final int npci = is.read();
		final int len;
		if (ext) {
			hopcount = (ctrle & 0x70) >> 4;
			len = npci;
			if (len > 64)
				throw new KNXFormatException("APDU length exceeds maximum of 64 bytes", len);
		}
		else {
			hopcount = (npci & 0x70) >> 4;
			len = npci & 0x0f;
		}
		tpdu = new byte[len + 1];
		if (is.read(tpdu, 0, tpdu.length) != tpdu.length)
			throw new KNXFormatException("data too short for L-data TPDU");
		fcs = is.read() << 8 | is.read();
	}

	/**
	 * Returns the domain address of this frame.
	 * <p>
	 * The address is returned in network byte order.
	 * 
	 * @return domain address as byte array of length 2
	 */
	public final byte[] getDomainAddress()
	{
		return (byte[]) doa.clone();
	}

	/**
	 * Returns whether a Layer 2 acknowledgment is requested or not.
	 * <p>
	 * 
	 * @return <code>true</code> if an L2-ACK requested, <code>false</code> otherwise
	 */
	public final boolean isAckRequested()
	{
		return ack;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.RawFrameBase#toString()
	 */
	public String toString()
	{
		final int domain = (doa[0] & 0xff) << 8 | (doa[1] & 0xff);
		return super.toString() + " hop count " + hopcount + " domain " + domain
			+ ", tpdu " + DataUnitBuilder.toHex(tpdu, " ");
	}
}

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
 * Factory for raw frames on medium.
 * <p>
 * Supports creation of raw frames out of byte arrays for now.
 * 
 * @author B. Malinowsky
 */
public final class RawFrameFactory
{
	private RawFrameFactory()
	{}

	/**
	 * Creates a raw frame out of a byte array for the specified communication medium.
	 * <p>
	 * This method just invokes one of the other medium type specific creation methods
	 * according the given medium type.
	 * 
	 * @param mediumType KNX communication medium, one of the media types declared in
	 *        {@link KNXMediumSettings}
	 * @param data byte array containing the raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;=
	 *        offset &lt; <code>data.length</code>
	 * @return the created raw frame for the specified medium
	 * @throws KNXFormatException on unknown/not supported KNX medium or no valid frame
	 *         structure
	 */
	public static RawFrame create(final int mediumType, final byte[] data,
		final int offset) throws KNXFormatException
	{
		switch (mediumType) {
		case KNXMediumSettings.MEDIUM_TP0:
			throw new KNXFormatException("TP0 raw frame not supported yet");
		case KNXMediumSettings.MEDIUM_TP1:
			return createTP1(data, offset);
		case KNXMediumSettings.MEDIUM_PL110:
			return createPL110(data, offset);
		case KNXMediumSettings.MEDIUM_PL132:
			return createPL132(data, offset);
		case KNXMediumSettings.MEDIUM_RF:
			throw new KNXFormatException("RF raw frame not supported yet");
		default:
			throw new KNXFormatException("unknown KNX medium for raw frame", mediumType);
		}
	}

	/**
	 * Creates a raw frame out of a byte array for the TP1 communication medium.
	 * <p>
	 * 
	 * @param data byte array containing the TP1 raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;=
	 *        offset &lt; <code>data.length</code>
	 * @return the created TP1 raw frame
	 * @throws KNXFormatException on no valid frame structure
	 */
	public static RawFrame createTP1(final byte[] data, final int offset)
		throws KNXFormatException
	{
		final int ctrl = data[offset] & 0xff;
		// parse control field and check if valid
		if ((ctrl & 0x10) == 0x10) {
			if ((ctrl & 0x40) == 0x00)
				return new TP1LData(data, offset);
			else if (ctrl == 0xF0)
				return new TP1LPollData(data, offset);
			throw new KNXFormatException("invalid raw frame control field", ctrl);
		}
		return new TP1Ack(data, offset);
	}

	/**
	 * Creates a raw frame out of a byte array for the PL110 communication medium.
	 * <p>
	 * 
	 * @param data byte array containing the PL110 raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;=
	 *        offset &lt; <code>data.length</code>
	 * @return the created PL110 raw frame
	 * @throws KNXFormatException on no valid frame structure
	 */
	public static RawFrame createPL110(final byte[] data, final int offset)
		throws KNXFormatException
	{
		if ((data[0] & 0x10) == 0x10)
			return new PL110LData(data, offset);
		return new PL110Ack(data, offset);
	}

	/**
	 * Creates a raw frame out of a byte array for the PL132 communication medium.
	 * <p>
	 * 
	 * @param data byte array containing the PL132 raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;=
	 *        offset &lt; <code>data.length</code>
	 * @return the created PL132 raw frame
	 * @throws KNXFormatException on no valid frame structure
	 */
	public static RawFrame createPL132(final byte[] data, final int offset)
		throws KNXFormatException
	{
		if (data.length - offset == 2)
			return new PL132Ack(data, offset);
		return new PL132LData(data, offset);
	}
}

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

package tuwien.auto.calimero.knxnetip.util;

import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * Description Information Block (DIB).
 * <p>
 * A DIB is used to return device specific information.<br>
 * This DIB is a common base for more detailed description formats contained in DIBs. For
 * usage of the different description information available, refer to the DIB subtypes.
 * <p>
 * The currently known valid descriptor type codes (KNXnet/IP core specification v1.2) are
 * defined as available DIB constants.
 * 
 * @author B. Malinowsky
 */
public abstract class DIB
{
	/**
	 * Description type code for device information e.g. KNX medium.
	 * <p>
	 */
	public static final int DEVICE_INFO = 0x01;

	/**
	 * Description type code for further data defined by device manufacturer.
	 * <p>
	 */
	public static final int MFR_DATA = 0xFE;

	/**
	 * Description type code for service families supported by the device.
	 * <p>
	 */
	public static final int SUPP_SVC_FAMILIES = 0x02;

	final int size;
	final int type;

	/**
	 * Creates a new DIB out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing DIB structure
	 * @param offset start offset of DIB in <code>data</code>
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	protected DIB(final byte[] data, final int offset) throws KNXFormatException
	{
		if (data.length - offset < 2)
			throw new KNXFormatException("buffer too short for DIB header");
		size = data[offset] & 0xFF;
		type = data[offset + 1] & 0xFF;
		if (size > data.length - offset)
			throw new KNXFormatException("DIB size bigger than actual data length", size);
	}

	/**
	 * Creates a new DIB and initializes basic fields.
	 * <p>
	 * 
	 * @param dibSize total size of DIB in bytes, <code>dibSize > 0</code>
	 * @param descriptionType one of the description type code constants of this class
	 */
	protected DIB(final int dibSize, final int descriptionType)
	{
		size = dibSize;
		type = descriptionType;
	}
	
	/**
	 * Returns the description type code of this DIB.
	 * <p>
	 * The type code specifies which kind of description information is contained in the
	 * DIB.
	 * 
	 * @return description type code as unsigned byte
	 */
	public final int getDescTypeCode()
	{
		return type;
	}

	/**
	 * Returns the structure length of this DIB in bytes.
	 * <p>
	 * 
	 * @return structure length as unsigned byte
	 */
	public final int getStructLength()
	{
		return size;
	}

	/**
	 * Returns the byte representation of the whole DIB structure.
	 * <p>
	 * 
	 * @return byte array containing structure
	 */
	public byte[] toByteArray()
	{
		final byte[] buf = new byte[size];
		buf[0] = (byte) size;
		buf[1] = (byte) type;
		return buf;
	}
}

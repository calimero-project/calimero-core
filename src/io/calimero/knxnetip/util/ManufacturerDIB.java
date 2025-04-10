/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.knxnetip.util;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * Represents a manufacturer data description information block.
 * <p>
 * Since the data in this DIB is dependent on the manufacturer and might contain any
 * information, no specific content parsing is done.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 */
public class ManufacturerDIB extends DIB
{
	private final int id;
	private final byte[] mfrData;

	/**
	 * Creates a manufacturer data DIB out of a byte array.
	 *
	 * @param data byte array containing manufacturer data DIB structure
	 * @param offset start offset of DIB in {@code data}
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public ManufacturerDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != MFR_DATA)
			throw new KNXFormatException("DIB is not of type manufacturer data", type);
		if (size < 4)
			throw new KNXFormatException("manufacturer DIB too short");
		id = (data[offset + 2] & 0xFF) << 8 | (data[offset + 3] & 0xFF);
		mfrData = new byte[size - 4];
		for (int i = 0; i < mfrData.length; ++i)
			mfrData[i] = data[4 + offset + i];
	}

	/**
	 * Creates a manufacturer data DIB using a manufacturer identifier and a byte array
	 * with manufacturer specific data.
	 *
	 * @param mfrID KNX manufacturer identifier, assigned by Konnex
	 * @param mfrSpecificData manufacturer data, {@code mfrSpecificData.length > 0}
	 */
	public ManufacturerDIB(final int mfrID, final byte[] mfrSpecificData)
	{
		super(4 + mfrSpecificData.length, DIB.MFR_DATA);
		if (mfrID < 0 || mfrID > 0xffff)
			throw new KNXIllegalArgumentException("manufacturer ID out of range [0..0xffff]");
		id = mfrID;
		mfrData = mfrSpecificData.clone();
	}

	/**
	 * Returns the KNX manufacturer ID.
	 * <p>
	 * The ID clearly identifies the manufacturer who created this DIB structure.
	 *
	 * @return ID as unsigned 16 bit value
	 */
	public final int getID()
	{
		return id;
	}

	/**
	 * Returns the manufacturer specific description data.
	 * <p>
	 * This data block starts at byte offset 4 in the DIB structure.
	 *
	 * @return byte array with manufacturer data
	 */
	public final byte[] getData()
	{
		return mfrData.clone();
	}

	@Override
	public byte[] toByteArray()
	{
		final byte[] buf = super.toByteArray();
		buf[2] = (byte) (id >> 8);
		buf[3] = (byte) id;
		System.arraycopy(mfrData, 0, buf, 4, mfrData.length);
		return buf;
	}

	/**
	 * Returns a textual representation of this manufacturer DIB.
	 *
	 * @return a string representation of the DIB object
	 */
	@Override
	public String toString()
	{
		return "KNX manufacturer ID 0x" + Integer.toHexString(id) + ", data 0x" + HexFormat.of().formatHex(mfrData);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hash(id);
		result = prime * result + Arrays.hashCode(mfrData);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final ManufacturerDIB other))
			return false;
		return id == other.id && Arrays.equals(mfrData, other.mfrData);
	}
}

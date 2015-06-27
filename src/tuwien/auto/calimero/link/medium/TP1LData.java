/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

package tuwien.auto.calimero.link.medium;

import java.io.ByteArrayInputStream;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXFormatException;

/**
 * L-data frame format on TP1 communication medium.
 * <p>
 * Supports standard and extended frame format.
 *
 * @author B. Malinowsky
 */
public class TP1LData extends RawFrameBase
{
	private static final int MIN_LENGTH = 7;

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
	public TP1LData(final byte[] data, final int offset) throws KNXFormatException
	{
		final ByteArrayInputStream is = asStream(data, offset, MIN_LENGTH, "L-data");
		final int len = init(is, false);

		if (ext && len == 255)
			throw new KNXFormatException("escape-code in length field not supported");

		tpdu = new byte[len + 1];
		if (is.read(tpdu, 0, tpdu.length) != tpdu.length)
			throw new KNXFormatException("data too short for L-data TPDU");
		fcs = is.read();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.RawFrameBase#toString()
	 */
	@Override
	public String toString()
	{
		return super.toString() + ", tpdu " + DataUnitBuilder.toHex(tpdu, " ");
	}
}

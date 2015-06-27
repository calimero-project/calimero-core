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
 * L-data frame format on PL110 communication medium. Supports standard and extended frame format.
 *
 * @author B. Malinowsky
 */
public class PL110LData extends RawFrameBase
{
	private static final int MIN_LENGTH = 7;

	private final boolean skipDoA;

	/**
	 * Creates a new L-data frame out of a byte array.
	 *
	 * @param data byte array containing the L-data frame
	 * @param offset start offset of frame structure in <code>data</code>, offset &gt;= 0
	 * @param extBusmon is <code>data</code> a PL110 extended busmonitor frame, i.e., contains the
	 *        domain address
	 * @throws KNXFormatException if length of data too short for frame, on no valid frame structure
	 */
	public PL110LData(final byte[] data, final int offset, final boolean extBusmon)
		throws KNXFormatException
	{
		final ByteArrayInputStream is = asStream(data, offset, MIN_LENGTH, "L-data");
		final int len = init(is, extBusmon);

		if (ext && len > 64)
			throw new KNXFormatException("APDU length exceeds maximum of 64 bytes", len);

		tpdu = new byte[len + 1];
		if (is.read(tpdu, 0, tpdu.length) != tpdu.length)
			throw new KNXFormatException("data too short for L-data TPDU");
		fcs = is.read();
		if (extBusmon)
			skipDoA = false;
		else {
			doa = new byte[2];
			skipDoA = is.read(doa, 1, 1) == -1;
		}
	}

	/**
	 * Returns the domain address of this frame. The address is returned in network byte order.
	 *
	 * @return domain address as byte array of length 2
	 */
	public final byte[] getDomainAddress()
	{
		return doa.clone();
	}

	@Override
	public String toString()
	{
		return super.toString()
				+ (skipDoA ? "" : " domain 0x" + Integer.toHexString(doa[1] & 0xff)) + ", tpdu "
				+ DataUnitBuilder.toHex(tpdu, " ");
	}
}

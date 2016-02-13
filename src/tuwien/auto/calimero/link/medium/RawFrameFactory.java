/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.cemi.CEMIFactory;

/**
 * Factory for raw frames on medium. Supports creation of raw frames out of byte arrays. The main
 * purpose is to create frames for the communication media TP1, PL110, and RF. For the
 * communication medium KNX IP, which uses cEMI frames, the {@link CEMIFactory} can be used
 * directly.
 *
 * @author B. Malinowsky
 */
public final class RawFrameFactory
{
	private RawFrameFactory()
	{}

	/**
	 * Creates a raw frame out of a byte array for the specified communication medium. This method just invokes one of
	 * the other medium type specific creation methods according the given medium type.
	 *
	 * @param mediumType KNX communication medium, one of the media types declared in {@link KNXMediumSettings}
	 * @param data byte array containing the raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;= offset &lt; <code>data.length</code>
	 * @param extBusmon parse <code>data</code> as PL110 extended busmonitor frame, i.e., <code>data</code> contains the
	 *        domain address; ignored if medium type is not PL110
	 * @return the created raw frame for the specified medium
	 * @throws KNXFormatException on unknown/not supported KNX medium or no valid frame structure
	 */
	public static RawFrame create(final int mediumType, final byte[] data,
		final int offset, final boolean extBusmon) throws KNXFormatException
	{
		switch (mediumType) {
		case KNXMediumSettings.MEDIUM_TP1:
			return createTP1(data, offset);
		case KNXMediumSettings.MEDIUM_PL110:
			return createPL110(data, offset, extBusmon);
		case KNXMediumSettings.MEDIUM_RF:
			return new RFLData(data, offset);
		default:
			throw new KNXFormatException("unknown KNX medium for raw frame", mediumType);
		}
	}

	/**
	 * Creates a raw frame out of a byte array for the TP1 communication medium.
	 *
	 * @param data byte array containing the TP1 raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;=
	 *        offset &lt; <code>data.length</code>
	 * @return the created TP1 raw frame
	 * @throws KNXFormatException on no valid frame structure
	 */
	public static RawFrame createTP1(final byte[] data, final int offset) throws KNXFormatException
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
	 *
	 * @param data byte array containing the PL110 raw frame structure
	 * @param offset start offset of frame structure in <code>data</code>, 0 &lt;= offset &lt; <code>data.length</code>
	 * @param extBusmon parse <code>data</code> as PL110 extended busmonitor frame, i.e., <code>data</code> contains the
	 *        domain address
	 * @return the created PL110 raw frame
	 * @throws KNXFormatException on no valid frame structure
	 */
	public static RawFrame createPL110(final byte[] data, final int offset, final boolean extBusmon)
		throws KNXFormatException
	{
		if ((data[0] & 0x10) == 0x10)
			return new PL110LData(data, offset, extBusmon);
		return new PL110Ack(data, offset);
	}
}

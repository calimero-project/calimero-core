/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019, 2020 B. Malinowsky

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

package tuwien.auto.calimero.cemi;

import java.io.ByteArrayInputStream;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Holds a cEMI additional info type with corresponding type-specific information. Objects of this type are immutable.
 */
public class AdditionalInfo {

	/**
	 * Additional information type for PL medium information.
	 */
	public static final int PlMedium = 0x01;

	/**
	 * Additional information type for RF medium information.
	 */
	public static final int RfMedium = 0x02;

	/**
	 * Additional information type for relative timestamp information.
	 */
	public static final int Timestamp = 0x04;

	/**
	 * Additional information type for time delay until sending information.
	 */
	public static final int TimeDelay = 0x05;

	/**
	 * Additional information type for extended relative timestamp information.
	 */
	public static final int ExtendedTimestamp = 0x06;

	/**
	 * Additional information type for BiBat information.
	 */
	public static final int BiBat = 0x07;

	/**
	 * RF Multi frequency, call channel and Fast Ack number.
	 */
	public static final int RfMulti = 0x08;

	/**
	 * Preamble and postamble length.
	 */
	public static final int PreamblePostamble = 0x09;

	/**
	 * Status and information about each expected number of Fast Ack.
	 */
	public static final int RfFastAck = 0x0a;

	/**
	 * Manufacturer-specific data, including manufacturer ID (2 bytes) and subfunction ID (1 byte).
	 */
	public static final int ManufacturerSpecific = 0xfe;

	static final int Escape = 0xFF;


	private static final int[] typeLengths = { 0, 2, 8, 1, 2, 4, 4, 2, 4, 3 };

	private final int type;
	private final byte[] data;

	/**
	 * Creates new cEMI additional information from the input stream.
	 *
	 * @param is input stream containing cEMI additional information to parse
	 * @return cEMI additional information
	 * @throws KNXFormatException on invalid length or information structure
	 */
	public static AdditionalInfo from(final ByteArrayInputStream is) throws KNXFormatException {
		final int remaining = is.available();
		if (remaining < 3)
			throw new KNXFormatException("invalid additional info, remaining length " + remaining + " < 3 bytes");
		final int type = is.read();
		final int len = is.read();
		if (len > remaining)
			throw new KNXFormatException("additional info length of type " + type + " exceeds info block", len);
		final byte[] info = new byte[len];
		is.read(info, 0, len);
		try {
			if (type == RfMedium)
				return new RFMediumInfo(info);
			return of(type, info);
		}
		catch (final KNXIllegalArgumentException e) {
			throw new KNXFormatException(e.getMessage());
		}
	}

	/**
	 * Creates new cEMI additional information using the type ID and a copy of the supplied info data.
	 *
	 * @param type additional information type ID
	 * @param info information data
	 * @return cEMI additional information
	 */
	public static AdditionalInfo of(final int type, final byte[] info) {
		if (type < 0 || type >= Escape)
			throw new KNXIllegalArgumentException("cEMI additional info type " + type + " out of range [0..254]");
		if (info.length > 255)
			throw new KNXIllegalArgumentException(
					"cEMI additional info of type " + type + " exceeds maximum length of 255 bytes");
		if (type < typeLengths.length && info.length != typeLengths[type])
			throw new KNXIllegalArgumentException(
					"invalid length " + info.length + " for cEMI additional info type " + type);
		if (type == RfMedium)
			return new RFMediumInfo(info);
		return new AdditionalInfo(type, info);
	}

	AdditionalInfo(final int type, final byte[] info) {
		this.type = type;
		data = info.clone();
	}

	/**
	 * Returns the type of additional information.
	 *
	 * @return type ID
	 */
	public final int type() { return type; }

	/**
	 * Returns the additional information associated with this type.
	 *
	 * @return copy of the info as byte array
	 */
	public final byte[] info() { return data.clone(); }

	@Override
	public String toString() {
		switch (type) {
		case PlMedium:
			return "PL DoA " + (Integer.toHexString((data[0] & 0xff) << 8 | (data[1] & 0xff)));
		case RfMedium:
			return new RFMediumInfo(data, false).toString(); // we default to domain broadcast
		case Timestamp:
			return "timestamp " + ((data[0] & 0xff) << 8 | (data[1] & 0xff));
		case TimeDelay:
			return "timedelay " + unsigned(data);
		case ExtendedTimestamp:
			return "ext.timestamp " + unsigned(data);
		case BiBat:
			return "BiBat 0x" + DataUnitBuilder.toHex(data, " ");
		default:
			return "type " + type + " = 0x" + DataUnitBuilder.toHex(data, "");
		}
	}

	private static long unsigned(final byte[] data) {
		long l = 0;
		for (final byte b : data)
			l = l << 8 | (b & 0xff);
		return l;
	}
}

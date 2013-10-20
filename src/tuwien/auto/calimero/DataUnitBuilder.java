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

package tuwien.auto.calimero;

import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Creates, extracts and decodes information of protocol data units.
 * <p>
 * The methods focus on transport layer and application layer data units.
 * 
 * @author B. Malinowsky
 */
public final class DataUnitBuilder
{
	/** Name of the log service used by data-unit builder methods. */
	public static final String LOG_SERVICE = "Data-unit builder";

	private static final LogService logger =
		LogManager.getManager().getLogService(LOG_SERVICE);

	private static final int T_DATA_CONNECTED = 0x40;
	private static final int T_CONNECT = 0x80;
	private static final int T_DISCONNECT = 0x81;
	private static final int T_ACK = 0xC2;
	private static final int T_NAK = 0xC3;

	private DataUnitBuilder()
	{}

	/**
	 * Returns the application layer service of a given protocol data unit.
	 * <p>
	 * 
	 * @param apdu application layer protocol data unit, requires <code>apdu.length</code>
	 *        > 1
	 * @return APDU service code
	 */
	public static int getAPDUService(final byte[] apdu)
	{
		if (apdu.length < 2)
			throw new KNXIllegalArgumentException("getting APDU service from [0x"
				+ toHex(apdu, "") + "], APCI length < 2");
		// high 4 bits of APCI
		final int apci4 = (apdu[0] & 0x03) << 2 | (apdu[1] & 0xC0) >> 6;
		// lowest 6 bits of APCI
		final int apci6 = apdu[1] & 0x3f;
		// group value codes
		// group read
		if (apci4 == 0) {
			if (apci6 == 0)
				return 0;
		}
		// group response
		else if (apci4 == 1)
			return 0x40;
		// group write
		else if (apci4 == 2)
			return 0x80;
		// individual address codes
		else if (apci4 == 3 || apci4 == 4 || apci4 == 5) {
			if (apci6 == 0)
				return apci4 << 6;
		}
		// ADC codes
		else if (apci4 == 6 || apci4 == 7)
			return apci4 << 6;
		// memory codes
		else if (apci4 == 8 || apci4 == 9 || apci4 == 10) {
			if ((apci6 & 0x30) == 0)
				return apci4 << 6;
		}
		// the rest
		else
			return apci4 << 6 | apci6;
		// unknown codes
		final int code = apci4 << 6 | apci6;
		logger.warn("unknown APCI service code 0x" + Integer.toHexString(code));
		return code;
	}

	/**
	 * Returns the transport layer service of a given protocol data unit.
	 * <p>
	 * 
	 * @param tpdu transport layer protocol data unit
	 * @return TPDU service code
	 */
	public static int getTPDUService(final byte[] tpdu)
	{
		final int ctrl = tpdu[0] & 0xff;
		if ((ctrl & 0xFC) == 0)
			return 0;
		// 0x04 is tag_group service code, not used by us
		if ((ctrl & 0xFC) == 0x04)
			return 0x04;
		if ((ctrl & 0xC0) == 0x40)
			return T_DATA_CONNECTED;
		if (ctrl == T_CONNECT)
			return T_CONNECT;
		if (ctrl == T_DISCONNECT)
			return T_DISCONNECT;
		if ((ctrl & 0xC3) == T_ACK)
			return T_ACK;
		if ((ctrl & 0xC3) == T_NAK)
			return T_NAK;
		logger.warn("unknown TPCI service code 0x" + Integer.toHexString(ctrl));
		return ctrl;
	}

	/**
	 * Creates an application layer protocol data unit out of a service code and a service
	 * data unit.
	 * <p>
	 * The transport layer bits in the first byte (TL / AL control field) are set 0. For
	 * creating a compact APDU, refer to {@link #createCompactAPDU(int, byte[])}.
	 * 
	 * @param service application layer service code
	 * @param asdu application layer service data unit, <code>asdu.length</code> &lt;
	 *        255
	 * @return APDU as byte array
	 */
	public static byte[] createAPDU(final int service, final byte[] asdu)
	{
		if (asdu.length > 254)
			throw new KNXIllegalArgumentException(
				"ASDU length exceeds maximum of 254 bytes");
		final byte[] apdu = new byte[2 + asdu.length];
		apdu[0] = (byte) ((service >> 8) & 0x03);
		apdu[1] |= (byte) service;
		for (int i = 0; i < asdu.length; ++i)
			apdu[i + 2] = asdu[i];
		return apdu;
	}

	/**
	 * Creates a compact application layer protocol data unit out of a service code and a
	 * service data unit.
	 * <p>
	 * The transport layer bits in the first byte (TL / AL control field) are set 0. If
	 * the compact APDU shall not contain any ASDU information, <code>asdu</code> can be
	 * left <code>null</code>.
	 * 
	 * @param service application layer service code
	 * @param asdu application layer service data unit, <code>asdu.length</code> &lt;
	 *        255; or <code>null</code> for no ASDU
	 * @return APDU as byte array
	 */
	public static byte[] createCompactAPDU(final int service, final byte[] asdu)
	{
		final byte[] apdu =
			new byte[(asdu != null && asdu.length > 0) ? 1 + asdu.length : 2];
		if (apdu.length > 255)
			throw new KNXIllegalArgumentException(
				"APDU length exceeds maximum of 255 bytes");
		apdu[0] = (byte) ((service >> 8) & 0x03);
		apdu[1] = (byte) service;
		if (asdu != null && asdu.length > 0) {
			// maximum of 6 bits in asdu[0] are valid
			apdu[1] |= asdu[0] & 0x3F;
			for (int i = 1; i < asdu.length; ++i)
				apdu[i + 1] = asdu[i];
		}
		return apdu;
	}

	/**
	 * Returns a copy of the ASDU contained in the supplied APDU.
	 * <p>
	 * The application layer service data unit (ASDU) is the APDU with the application
	 * layer service code removed.
	 * 
	 * @param apdu application layer protocol data unit for which to get the ASDU
	 * @return the ASDU as byte array
	 */
	public static byte[] extractASDU(final byte[] apdu)
	{
		final int svc = getAPDUService(apdu);
		int offset = 2;
		int mask = 0xff;
		// 0x40 A_GroupValue_Response-PDU
		// 0x80 A_GroupValue_Write-PDU
		if (svc == 0x40 || svc == 0x80) {
			// only adjust for optimized read.res and write
			if (apdu.length <= 2) {
				offset = 1;
				mask = 0x3f;
			}
		}
		// 0x0180 A_ADC_Read-PDU
		// 0x01C0 A_ADC_Response-PDU
		else if (svc == 0x0180 || svc == 0x01C0) {
			offset = 1;
			mask = 0x3f;
		}
		// 0x0200 A_Memory_Read-PDU
		// 0x0240 A_Memory_Response-PDU
		// 0x0280 A_Memory_Write-PDU
		else if (svc == 0x0200 || svc == 0x0240 || svc == 0x0280) {
			offset = 1;
			// masks the number of bytes field,
			// the field size was increased to 6 bits in 03/03/07 2007
			mask = 0x3f;
		}
		// 0x300 A_DeviceDescriptor_Read-PDU
		// 0x340 A_DeviceDescriptor_Response-PDU
		else if (svc == 0x300 || svc == 0x340) {
			offset = 1;
			// we mask the descriptor type
			mask = 0x3f;
		}
		final byte[] asdu = new byte[apdu.length - offset];
		for (int i = 0; i < asdu.length; ++i)
			asdu[i] = apdu[offset + i];
		asdu[0] &= mask;
		return asdu;
	}
	
	/**
	 * Decodes a protocol data unit into a textual representation.
	 * <p>
	 * Currently, the transport layer protocol control information (TPCI) and the
	 * application layer protocol control information (APCI) is decoded. Decoding might be
	 * extended in the future.<br>
	 * The optional KNX destination address helps to determine the exact transport layer
	 * service.
	 * 
	 * @param tpdu transport layer protocol data unit to decode
	 * @param dst KNX destination address belonging to the TPDU, might be
	 *        <code>null</code>
	 * @return textual representation of control information in the TPDU
	 */
	public static String decode(final byte[] tpdu, final KNXAddress dst)
	{
		if (tpdu.length < 1)
			throw new KNXIllegalArgumentException("TPDU length too short");
		final String s = decodeTPCI(tpdu[0] & 0xff, dst);
		if (tpdu.length > 1)
			return s + ", " + decodeAPCI(getAPDUService(tpdu));
		return s;
	}

	/**
	 * Decodes a transport layer protocol control information into a textual
	 * representation.
	 * <p>
	 * 
	 * @param tpci transport layer protocol control information
	 * @param dst KNX destination address belonging to the tpci, might be
	 *        <code>null</code>
	 * @return textual representation of TPCI
	 */
	public static String decodeTPCI(final int tpci, final KNXAddress dst)
	{
		final int ctrl = tpci & 0xff;
		if ((ctrl & 0xFC) == 0) {
			if (dst == null)
				return "T-broadcast/group/ind";
			if (dst.getRawAddress() == 0)
				return "T-broadcast";
			if (dst instanceof GroupAddress)
				return "T-group";
			return "T-individual";
		}
		if ((ctrl & 0xC0) == 0x40)
			return "T-connected seq " + (ctrl >> 2 & 0xF);
		if (ctrl == T_CONNECT)
			return "T-connect";
		if (ctrl == T_DISCONNECT)
			return "T-disconnect";
		if ((ctrl & 0xC3) == T_ACK)
			return "T-ack seq " + (ctrl >> 2 & 0xF);
		if ((ctrl & 0xC3) == T_NAK)
			return "T-nak seq " + (ctrl >> 2 & 0xF);
		return "unknown TPCI";
	}

	/**
	 * Decodes an application layer protocol control information into a textual
	 * representation.
	 * <p>
	 * 
	 * @param apci application layer protocol control information
	 * @return textual representation of APCI
	 */
	public static String decodeAPCI(final int apci)
	{
		switch (apci) {
		case 0x00:
			return "A-group.read";
		case 0x40:
			return "A-group.response";
		case 0x80:
			return "A-group.write";
		case 0x0180:
			return "A-ADC.read";
		case 0x01C0:
			return "A-ADC.response";
		case 0x03D1:
			return "A-authorize.read";
		case 0x03D2:
			return "A-authorize.response";
		case 0x3E0:
			return "A-domain.write";
		case 0x3E1:
			return "A-domain.read";
		case 0x3E2:
			return "A-domain.response";
		case 0x3E3:
			return "A-domain-selective.read";
		case 0x0100:
			return "A-ind.addr.read";
		case 0x0140:
			return "A-ind.addr.response";
		case 0xC0:
			return "A-ind.addr.write";
		case 0x03DC:
			return "A-ind.addr-sno.read";
		case 0x03DD:
			return "A-ind.addr_sno.response";
		case 0x03DE:
			return "A-ind.addr_sno.write";
		case 0x300:
			return "A-device-desc.read";
		case 0x340:
			return "A-device-desc.response";
		case 0x03D3:
			return "A-key.write";
		case 0x03D4:
			return "A-key.response";
		case 0x0200:
			return "A-memory.read";
		case 0x0240:
			return "A-memory.response";
		case 0x0280:
			return "A-memory.write";
		case 0x03D8:
			return "A-property-desc.read";
		case 0x03D9:
			return "A-property-desc.response";
		case 0x03D5:
			return "A-property.read";
		case 0x03D6:
			return "A-property.response";
		case 0x03D7:
			return "A-property.write";
		case 0x0380:
			return "A-restart";
		default:
			return "unknown APCI";
		}
	}
	
	/**
	 * Returns the content of <code>data</code> as unsigned bytes in hexadecimal string
	 * representation.
	 * <p>
	 * This method does not add hexadecimal prefixes (like 0x).
	 * 
	 * @param data data array to format
	 * @param sep separator to insert between 2 formatted data bytes, <code>null</code>
	 *        or "" for no gap between byte tokens
	 * @return an unsigned hexadecimal string of data
	 */
	public static String toHex(final byte[] data, final String sep)
	{
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < data.length; ++i) {
			final int no = data[i] & 0xff;
			if (no < 0x10)
				sb.append('0');
			sb.append(Integer.toHexString(no));
			if (sep != null && i < data.length - 1)
				sb.append(sep);
		}
		return sb.toString();
	}
	
	/**
	 * Returns a new byte array of { data[from], data[from + 1], .. , data[min(to,
	 * data.length) - 1] }.
	 * <p>
	 * 
	 * @param data source data array
	 * @param from start of range index
	 * @param to end of range index, exclusive
	 * @return the byte array filled with the corresponding data elements
	 */
	public static byte[] copyOfRange(final byte[] data, final int from, final int to)
	{
		final byte[] range = new byte[Math.min(data.length, to) - from];
		for (int i = 0; i < range.length; ++i)
			range[i] = data[from + i];
		return range;
	}
}

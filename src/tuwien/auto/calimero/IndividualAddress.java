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

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;

/**
 * Represents an immutable KNX individual address.
 * <p>
 * An individual address is built up from the 3 levels <i>area</i>, <i>line</i>, and
 * <i>device</i>. The address structure consists of a 16 bit address, consisting of
 * (starting from the most significant bit): area (4 bits), line (4 bits), and device (8
 * bits). The individual address KNX notation follows <i>area.line.device</i>, with the
 * recommended separator of type '.', or alternatively, '/' (area/line/device).<br>
 * <br>
 * The combined address levels <i>area</i> and <i>line</i> are referred to as subnetwork
 * address, i.e., and described by the higher 8 bits of the address value.<br>
 * The sometimes used term <i>zone</i> is synonymous with <i>area</i>.
 * 
 * @see GroupAddress
 */
public class IndividualAddress extends KNXAddress
{
	static final String ATTR_IND = "individual";

	/**
	 * Creates a KNX individual address from a 16 Bit address value.
	 * <p>
	 * 
	 * @param address the address value in the range 0 <= value <= 0xFFFF
	 */
	public IndividualAddress(final int address)
	{
		super(address);
	}

	/**
	 * Creates a KNX individual address from the 3-level notation area-, line- and
	 * device-address.
	 * <p>
	 * 
	 * @param area area address value, in the range 0 <= value <= 0xF
	 * @param line line address value, in the range 0 <= value <= 0xF
	 * @param device device address value, in the range 0 <= value <= 0xFF
	 */
	public IndividualAddress(final int area, final int line, final int device)
	{
		init(area, line, device);
	}

	/**
	 * Creates a KNX individual address from a byte array value.
	 * <p>
	 * The address is read out of the first 2 byte fields, while the address array itself
	 * might be longer. The content of <code>address</code> is not modified.
	 * 
	 * @param address the address byte array in big-endian format, with address.length > 1
	 */
	public IndividualAddress(final byte[] address)
	{
		super(address);
	}

	/**
	 * Creates a KNX individual address from a string <code>address</code> representation.
	 * <p>
	 * The address consists of 3 levels (see class header specification). Allowed
	 * separators are '.' or '/', mutually exclusive.
	 * 
	 * @param address string containing the KNX address
	 * @throws KNXFormatException on unknown address type, wrong address syntax, address
	 *         values out of range, or wrong separator used
	 */
	public IndividualAddress(final String address) throws KNXFormatException
	{
		final String[] tokens = parse(address);
		if (tokens.length != 3)
			throw new KNXFormatException("wrong individual address syntax with "
				+ tokens.length + " levels", address);
		try {
			init(Byte.parseByte(tokens[0]), Byte.parseByte(tokens[1]),
				Short.parseShort(tokens[2]));
		}
		catch (final KNXIllegalArgumentException e) {
			throw new KNXFormatException(e.getMessage());
		}
	}

	/**
	 * Creates a KNX individual address from xml input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The KNX address element is then expected to be the current element in the reader.
	 * 
	 * @param r a XML reader
	 * @throws KNXMLException if the XML element is no KNXAddress or the address couldn't
	 *         be read in correctly
	 */
	public IndividualAddress(final XMLReader r) throws KNXMLException
	{
		super(r);
	}

	/**
	 * Returns the area address.
	 * <p>
	 * The area address consists of the 4 most significant Bits in the address field.
	 * 
	 * @return the area value (high nibble of the address high byte)
	 */
	public final int getArea()
	{
		return address >>> 12;
	}

	/**
	 * Returns the line address.
	 * <p>
	 * The line address consists of 4 bits, starting with bit 8 to 11 in the address
	 * field.
	 * 
	 * @return the line value (low nibble of the address high byte)
	 */
	public final int getLine()
	{
		return address >>> 8 & 0x0F;
	}

	/**
	 * Returns the device address.
	 * <p>
	 * The device address consists of the low byte of the 16 Bit address field.
	 * 
	 * @return the device value (8 least significant bits)
	 */
	public final int getDevice()
	{
		return address & 0xFF;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.KNXAddress#getType()
	 */
	public String getType()
	{
		return ATTR_IND;
	}

	/**
	 * Returns the address as a string using the 3-level "area.line.device" notation.
	 * <p>
	 * 
	 * @return the address string
	 */
	public String toString()
	{
		return getArea() + "." + getLine() + "." + getDevice();
	}

	/**
	 * Returns whether <code>obj</code> is equal to this KNX address type.
	 * <p>
	 * 
	 * @param obj knx address object
	 * @return <code>true</code> iff <code>obj</code> is of this type and contains the
	 *         same address, <code>false</code> otherwise
	 */
	public boolean equals(final Object obj)
	{
		if (obj instanceof IndividualAddress)
			return address == ((IndividualAddress) obj).address;
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		// offset to distinguish between group address
		final int offset = 0x10000;
		return offset ^ address;
	}

	private void init(final int area, final int line, final int device)
	{
		if ((area & ~0xF) != 0 || (line & ~0xF) != 0 || (device & ~0xFF) != 0)
			throw new KNXIllegalArgumentException("address value out of range");
		address = area << 12 | line << 8 | device;
	}
}

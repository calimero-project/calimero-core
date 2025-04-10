/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

package io.calimero;

import io.calimero.xml.KNXMLException;
import io.calimero.xml.XmlReader;

/**
 * Represents an immutable KNX individual address.
 * <p>
 * An individual address is built up from the 3 levels <i>area</i>, <i>line</i>, and
 * <i>device</i>. The address structure consists of a 16 bit address, consisting of
 * (starting from the most significant bit): area (4 bits), line (4 bits), and device (8
 * bits). The individual address KNX notation follows <i>area.line.device</i>, with the
 * required separator of type '.'.
 * <br>
 * The combined address levels <i>area</i> and <i>line</i> are referred to as subnetwork
 * address, i.e., and described by the higher 8 bits of the address value.<br>
 * The sometimes used term <i>zone</i> is synonymous with <i>area</i>.
 *
 * @see GroupAddress
 */
public final class IndividualAddress extends KNXAddress
{
	static final String ATTR_IND = "individual";

	/**
	 * Creates a KNX individual address from a 16 Bit address value.
	 *
	 * @param address the address value in the range 0 &le; value &le; 0xFFFF
	 */
	public IndividualAddress(final int address)
	{
		super(address);
	}

	/**
	 * Creates a KNX individual address from the 3-level notation area-, line-, and device-address.
	 *
	 * @param area area address value, in the range 0 &le; value &le; 0xF
	 * @param line line address value, in the range 0 &le; value &le; 0xF
	 * @param device device address value, in the range 0 &le; value &le; 0xFF
	 */
	public IndividualAddress(final int area, final int line, final int device)
	{
		super(address(area, line, device));
	}

	/**
	 * Creates a KNX individual address from a byte array value.
	 * <p>
	 * The address is read out of the first 2 byte fields, while the address array itself
	 * might be longer. The content of {@code address} is not modified.
	 *
	 * @param address the address byte array in big-endian format, with address.length &gt; 1
	 */
	public IndividualAddress(final byte[] address)
	{
		super(address);
	}

	/**
	 * Creates a KNX individual address from a string {@code address} representation.
	 * <p>
	 * The address string can either be formatted, e.g., "1.1.2", or the raw address, e.g., "4354".
	 * A formatted address consists of 3 levels (see class header specification), the allowed
	 * separators are '.' or '/', mutually exclusive.
	 *
	 * @param address string containing the KNX address
	 * @throws KNXFormatException on unknown address type, wrong address syntax, address
	 *         values out of range, or wrong separator used
	 */
	public IndividualAddress(final String address) throws KNXFormatException
	{
		super(parse(address));
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
	public IndividualAddress(final XmlReader r) throws KNXMLException
	{
		super(parse(r));
	}

	/**
	 * Returns the area address, consisting of the 4 most significant Bits in the address field.
	 *
	 * @return the area value (high nibble of the address high byte)
	 */
	public int getArea()
	{
		return address >>> 12;
	}

	/**
	 * Returns the line address, consisting of 4 bits, starting with bit 8 to 11 in the address field.
	 *
	 * @return the line value (low nibble of the address high byte)
	 */
	public int getLine()
	{
		return address >>> 8 & 0x0F;
	}

	/**
	 * Returns the device address, consisting of the low byte of the 16 Bit address field.
	 *
	 * @return the device value (8 least significant bits)
	 */
	public int getDevice()
	{
		return address & 0xFF;
	}

	@Override
	public String getType()
	{
		return ATTR_IND;
	}

	/**
	 * Returns the address as a string using the 3-level "area.line.device" notation.
	 *
	 * @return the address string
	 */
	@Override
	public String toString()
	{
		return getArea() + "." + getLine() + "." + getDevice();
	}

	/**
	 * Returns whether {@code obj} is equal to this KNX address type.
	 *
	 * @param obj knx address object
	 * @return {@code true} iff {@code obj} is of this type and contains the
	 *         same address, {@code false} otherwise
	 */
	@Override
	public boolean equals(final Object obj)
	{
		if (obj instanceof IndividualAddress individualAddress)
			return address == individualAddress.address;
		return false;
	}

	@Override
	public int hashCode()
	{
		// offset to distinguish between group address
		final int offset = 0x10000;
		return offset ^ address;
	}

	private static int parse(final XmlReader r) {
		try {
			return parse(address(r, ATTR_IND));
		}
		catch (final KNXFormatException e) {
			throw new KNXMLException(e.getMessage(), r);
		}
	}

	private static int parse(final String address) throws KNXFormatException {
		final var tokens = parse(address, false);
		try {
			int value;
			if (tokens.length == 1)
				value = Integer.decode(tokens[0]);
			else if (tokens.length == 3)
				value = address(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
			else
				throw new KNXFormatException("wrong individual address syntax with " + tokens.length + " levels",
						address);

			if (value < 0 || value > 0xffff)
				throw new KNXFormatException("individual address out of range [0..0xffff]", value);
			return value;
		}
		catch (final NumberFormatException | KNXIllegalArgumentException e) {
			throw new KNXFormatException("invalid individual address", address, e);
		}
	}

	private static int address(final int area, final int line, final int device)
	{
		if ((area & ~0xF) != 0 || (line & ~0xF) != 0 || (device & ~0xFF) != 0)
			throw new KNXIllegalArgumentException("address value out of range");
		return area << 12 | line << 8 | device;
	}
}

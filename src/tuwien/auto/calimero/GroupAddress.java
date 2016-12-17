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

package tuwien.auto.calimero;

import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XmlReader;

/**
 * Represents an immutable KNX group address.
 * <p>
 * Both 2- and 3-level formats are supported. Available groups are<br>
 * <ul>
 * <li>2-level group address: main/sub (5/11 bits)</li>
 * <li>3-level group address: main/middle/sub (5/3/8 bits)</li>
 * </ul>
 * with separator of type '.' or '/'.<br>
 * By default, the 3-level group notation is used.
 * <p>
 * Note, that the most significant bit of the main group (i.e., bit 15 in the unstructured
 * address) is reserved, but not in use for now. Hence this bit is not checked for, but
 * nevertheless stored and returned by this implementation.
 */
public class GroupAddress extends KNXAddress
{
	static final String ATTR_GROUP = "group";

	private static volatile boolean fmt3Level = true;

	/**
	 * Creates a KNX group address from a 16 Bit address value.
	 * <p>
	 *
	 * @param address the address value in the range 0 &le; value &le; 0xFFFF
	 */
	public GroupAddress(final int address)
	{
		super(address);
	}

	/**
	 * Creates a KNX group address from the 3-level notation main-, middle- and sub-group.
	 * <p>
	 *
	 * @param mainGroup main group value, in the range 0 &le; value &le; 0x1F
	 * @param middleGroup middle group value, in the range 0 &le; value &le; 0x7
	 * @param subGroup sub group value, in the range 0 &le; value &le; 0xFF
	 */
	public GroupAddress(final int mainGroup, final int middleGroup, final int subGroup)
	{
		init(mainGroup, middleGroup, subGroup);
	}

	/**
	 * Creates a KNX group address from the 2-level notation main- and sub-group.
	 * <p>
	 *
	 * @param mainGroup main group value, in the range 0 &le; value &le; 0x1F
	 * @param subGroup sub group value, in the range 0 &le; value &le; 0x7FF
	 */
	public GroupAddress(final int mainGroup, final int subGroup)
	{
		init(mainGroup, subGroup);
	}

	/**
	 * Creates a KNX group address from a byte array value.
	 * <p>
	 * The address is read out of the first 2 byte fields, while the address array itself
	 * might be longer. The content of <code>address</code> is not modified.
	 *
	 * @param address the address byte array in big-endian format, with address.length &gt; 1
	 */
	public GroupAddress(final byte[] address)
	{
		super(address);
	}

	/**
	 * Creates a KNX group address from a string <code>address</code> representation.
	 * <p>
	 * The address string can either be formatted, e.g., "2/1/2", or the raw address, e.g., "4354".
	 * A formatted address is a 2- or 3-level group address, the allowed separators are '.'
	 * or '/', mutually exclusive.
	 *
	 * @param address string containing the KNX address
	 * @throws KNXFormatException on unknown address type, wrong address syntax,
	 *         group values out of range, or wrong separator used
	 */
	public GroupAddress(final String address) throws KNXFormatException
	{
		init(address);
	}

	/**
	 * Creates a KNX group address from XML input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The KNX address element is then expected to be the current element in the reader.
	 *
	 * @param r a XML reader
	 * @throws KNXMLException if the xml element is no KNX address or the
	 *         address couldn't be read in correctly
	 */
	public GroupAddress(final XmlReader r) throws KNXMLException
	{
		super(r);
	}

	/**
	 * Specifies the level presentation for KNX group addresses.
	 * <p>
	 * Note that, since this notation of levels only affects visual presentation and not
	 * internal operation, this is a class setting.
	 *
	 * @param format3Level <code>true</code> for 3-level group format,
	 *        <code>false</code> for 2-level group format
	 */
	public static void setLevelPresentation(final boolean format3Level)
	{
		fmt3Level = format3Level;
	}

	/**
	 * Returns the current presentation of group addresses.
	 * <p>
	 *
	 * @return <code>true</code> for 3-level formatting, <code>false</code> otherwise
	 */
	public static boolean is3LevelPresentation()
	{
		return fmt3Level;
	}

	/**
	 * Returns the main group value.
	 * <p>
	 * The main group is equal for both the 2- and 3-level group address notation (see
	 * class header specification).
	 *
	 * @return the main group value (5 most significant address bits)
	 */
	public final int getMainGroup()
	{
		return address >>> 11 & 0x1F;
	}

	/**
	 * Returns the middle group value for the 3-level group notation.
	 * <p>
	 * The middle group consists of 3 bits, starting with bit 8 to 10 in the address
	 * field.
	 *
	 * @return the middle group value (3 bits)
	 */
	public final int getMiddleGroup()
	{
		return address >>> 8 & 0x07;
	}

	/**
	 * Returns the sub group value for the 3-level group notation.
	 * <p>
	 * The sub group consists of the low byte of the address field.
	 *
	 * @return the sub group value (8 least significant address bits)
	 */
	public final int getSubGroup8()
	{
		return address & 0xFF;
	}

	/**
	 * Returns the sub group value for the 2-level group notation.
	 * <p>
	 * The sub group consists of the lower 11 bits in the address field.
	 *
	 * @return the sub group value (11 least significant address bits)
	 */
	public final int getSubGroup11()
	{
		return address & 0x07FF;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.KNXAddress#getType()
	 */
	@Override
	public final String getType()
	{
		return ATTR_GROUP;
	}

	/**
	 * Returns the address as string using the '/' separator.
	 * <p>
	 * Depending on {@link #is3LevelPresentation()}, the address is formatted in 2- or
	 * 3-level group notation.
	 *
	 * @return the address string
	 */
	@Override
	public String toString()
	{
		if (fmt3Level)
			return getMainGroup() + "/" + getMiddleGroup() + "/"
				+ getSubGroup8();
		return getMainGroup() + "/" + getSubGroup11();
	}

	/**
	 * Returns whether <code>obj</code> is equal to this KNX address (type).
	 * <p>
	 *
	 * @param obj KNX address object
	 * @return <code>true</code> iff <code>obj</code> is of this type and contains the
	 *         same address (raw), <code>false</code> otherwise
	 */
	@Override
	public boolean equals(final Object obj)
	{
		if (obj instanceof GroupAddress)
			return address == ((GroupAddress) obj).address;
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return address;
	}

	@Override
	void init(final String address) throws KNXFormatException
	{
		final String[] tokens = parse(address);
		try {
			if (tokens.length == 1)
				init(Integer.decode(tokens[0]));
			else if (tokens.length == 2)
				init(Byte.parseByte(tokens[0]), Short.parseShort(tokens[1]));
			else if (tokens.length == 3)
				init(Byte.parseByte(tokens[0]), Byte.parseByte(tokens[1]),
						Short.parseShort(tokens[2]));
		}
		catch (NumberFormatException | KNXIllegalArgumentException e) {
			throw new KNXFormatException("invalid group address", address, e);
		}
	}

	private void init(final int main, final int middle, final int sub)
	{
		if ((main & ~0x1F) != 0 || (middle & ~0x7) != 0 || (sub & ~0xFF) != 0)
			throw new KNXIllegalArgumentException("address group out of range");
		address = main << 11 | middle << 8 | sub;
	}

	private void init(final int main, final int sub)
	{
		if ((main & ~0x1F) != 0 || (sub & ~0x7FF) != 0)
			throw new KNXIllegalArgumentException("address group out of range");
		address = main << 11 | sub;
	}
}

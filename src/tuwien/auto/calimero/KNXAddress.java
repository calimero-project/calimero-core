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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.xml.Attribute;
import tuwien.auto.calimero.xml.Element;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

/**
 * Represents a KNX address.
 * <p>
 * An address consists of a 16 Bit unsigned value. Concrete implementations of address are
 * {@link GroupAddress} and {@link IndividualAddress}. Instances of
 * <code>KNXAddress</code> are immutable.<br>
 * Loading and saving KNX addresses in XML format is supported.
 */
public abstract class KNXAddress
{
	private static final String ATTR_TYPE = "type";
	private static final String TAG_ADDRESS = "knxAddress";

	int address;

	/**
	 * Creates a KNX address from a 16 Bit address value.
	 * <p>
	 * 
	 * @param address the address value in the range [0..0xffff]
	 */
	KNXAddress(final int address)
	{
		if (address < 0 || address > 0xffff)
			throw new KNXIllegalArgumentException("address out of range [0..0xFFFF]");
		this.address = address;
	}

	/**
	 * Creates a KNX address from a byte array.
	 * <p>
	 * The address is read out of the first 2 byte fields, while the address array itself
	 * might be longer. The content of <code>address</code> is not modified.
	 * 
	 * @param address the address byte array in big-endian format, with address.length >=
	 *        2
	 */
	KNXAddress(final byte[] address)
	{
		if (address.length < 2)
			throw new KNXIllegalArgumentException("address byte array too short");
		this.address = (address[0] & 0xFF) << 8 | address[1] & 0xFF;
	}

	/**
	 * Creates a KNX address from its XML representation.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The KNX address element is then expected to be the current element in the reader.
	 * 
	 * @param r a xml reader
	 * @throws KNXMLException if the XML element represents no KNX address or the address
	 *         couldn't be read correctly
	 */
	KNXAddress(final XMLReader r) throws KNXMLException
	{
		if (r.getPosition() != XMLReader.START_TAG)
			r.read();
		final Element e = r.getCurrent();
		if (r.getPosition() != XMLReader.START_TAG || !e.getName().equals(TAG_ADDRESS)
				|| !getType().equals(e.getAttribute(ATTR_TYPE)))
			throw new KNXMLException("XML element represents no KNX " + getType() + " address",
					e != null ? e.getName() : null, r.getLineNumber());
		r.complete(e);
		try {
			address = Integer.parseInt(e.getCharacterData());
			if (address >= 0 && address <= 0xffff)
				return;
		}
		catch (final NumberFormatException nfe) {}
		throw new KNXMLException("malformed KNX address value", e.getCharacterData(),
				r.getLineNumber());
	}

	/**
	 * Creates KNX address 0 (reserved address).
	 * <p>
	 */
	KNXAddress()
	{}

	/**
	 * Creates a KNX address from xml input.
	 * <p>
	 * The KNX address element is expected to be the current or next element from the
	 * parser.
	 * 
	 * @param r a XML reader
	 * @return the created KNXAddress, either of subtype {@link GroupAddress} or
	 *         {@link IndividualAddress}
	 * @throws KNXMLException if the XML element is no KNX address, on unknown address
	 *         type or wrong address syntax
	 */
	public static KNXAddress create(final XMLReader r) throws KNXMLException
	{
		if (r.getPosition() != XMLReader.START_TAG)
			r.read();
		if (r.getPosition() == XMLReader.START_TAG) {
			final String type = r.getCurrent().getAttribute(ATTR_TYPE);
			if (GroupAddress.ATTR_GROUP.equals(type))
				return new GroupAddress(r);
			else if (IndividualAddress.ATTR_IND.equals(type))
				return new IndividualAddress(r);
		}
		throw new KNXMLException("not a KNX address", null, r.getLineNumber());
	}

	/**
	 * Creates a KNX address from a string <code>address</code> representation.
	 * <p>
	 * An address level separator of type '.' found in <code>address</code> indicates an
	 * individual address, i.e., an {@link IndividualAddress} is created, otherwise a
	 * {@link GroupAddress} is created.<br>
	 * Allowed separators are '.' or '/', mutually exclusive.
	 * 
	 * @param address string containing the KNX address
	 * @return the created KNX address, either of subtype {@link GroupAddress} or
	 *         {@link IndividualAddress}
	 * @throws KNXFormatException thrown on unknown address type, wrong address syntax or
	 *         wrong separator used
	 */
	public static KNXAddress create(final String address) throws KNXFormatException
	{
		if (address.indexOf('.') != -1)
			return new IndividualAddress(address);
		return new GroupAddress(address);
	}

	/**
	 * Returns the KNX address type, identifying a group or individual address.
	 * <p>
	 * 
	 * @return address type as string
	 */
	public abstract String getType();

	/**
	 * Returns the KNX address in 16 Bit value representation.
	 * <p>
	 * 
	 * @return the 16 Bit address value
	 */
	public final int getRawAddress()
	{
		return address;
	}

	/**
	 * Writes the KNX address in XML format to the supplied writer.
	 * <p>
	 * 
	 * @param w a XML writer
	 * @throws KNXMLException on output error
	 */
	public void save(final XMLWriter w) throws KNXMLException
	{
		final List att = new ArrayList();
		att.add(new Attribute(ATTR_TYPE, getType()));
		w.writeComment(" " + toString() + " ");
		w.writeElement(TAG_ADDRESS, att, Integer.toString(address));
		w.endElement();
	}

	/**
	 * Returns the raw address value in a new byte array.
	 * 
	 * @return The address value. The high byte of the address is placed at index 0.
	 */
	public final byte[] toByteArray()
	{
		return new byte[] { (byte) (address >>> 8), (byte) address };
	}

	static String[] parse(final String address) throws KNXFormatException
	{
		StringTokenizer t = null;
		if (address.indexOf('/') > -1)
			t = new StringTokenizer(address, "/");
		else if (address.indexOf('.') > -1)
			t = new StringTokenizer(address, ".");
		else
			throw new KNXFormatException("wrong KNX address format, no valid separator", address);
		final int count = t.countTokens();
		if (count == 2)
			return new String[] { t.nextToken(), t.nextToken() };
		else if (count == 3)
			return new String[] { t.nextToken(), t.nextToken(), t.nextToken(), };
		else
			throw new KNXFormatException("wrong KNX address syntax with " + count + " levels",
					address);
	}
}

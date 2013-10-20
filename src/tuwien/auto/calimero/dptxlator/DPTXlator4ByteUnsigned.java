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

package tuwien.auto.calimero.dptxlator;

import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.log.LogLevel;

/**
 * Translator for KNX DPTs with main number 12, type <b>4 byte unsigned value</b>.
 * <p>
 * The KNX data type width is 4 bytes.<br>
 * The default return value after creation is 0.
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal,
 * hexadecimal, and octal numbers, distinguished by using these prefixes:
 * <dd>no prefix for decimal numeral
 * <dd><code>0x</code>, <code>0X</code> or <code>#</code> for hexadecimal
 * <dd><code>0</code> for octal numeral
 */
public class DPTXlator4ByteUnsigned extends DPTXlator
{
	/**
	 * DPT ID 12.001, Unsigned count; values from <b>0</b> to <b>4294967295</b> counter
	 * pulses.
	 * <p>
	 */
	public static final DPT DPT_VALUE_4_UCOUNT = new DPT("12.001", "Unsigned count", "0",
			"4294967295", "counter pulses");

	private static final Map types;

	static {
		types = new HashMap(2);
		types.put(DPT_VALUE_4_UCOUNT.getID(), DPT_VALUE_4_UCOUNT);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 * 
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator4ByteUnsigned(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 * <p>
	 * 
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available)
	 *         <code>dptID</code>
	 */
	public DPTXlator4ByteUnsigned(final String dptID) throws KNXFormatException
	{
		super(4);
		setTypeID(types, dptID);
		data = new short[4];
	}

	/**
	 * Sets the value of the first translation item.
	 * <p>
	 * 
	 * @param value unsigned value, 0 &lt;= value &lt;= 0xFFFFFFFF
	 * @throws KNXFormatException on input value out of range for DPT
	 * @see #getType()
	 */
	public final void setValue(final long value) throws KNXFormatException
	{
		data = toDPT(value, new short[4], 0);
	}

	/**
	 * Returns the first translation item as unsigned 32 Bit value.
	 * <p>
	 * 
	 * @return unsigned 32 Bit value using type long
	 * @see #getType()
	 */
	public final long getValueUnsigned()
	{
		return fromDPT(0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	public String getValue()
	{
		return makeString(0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	public String[] getAllValues()
	{
		final String[] s = new String[data.length / 4];
		for (int i = 0; i < s.length; ++i)
			s[i] = makeString(i);
		return s;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
	public final Map getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 4-byte unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map getSubTypesStatic()
	{
		return types;
	}

	private long fromDPT(final int index)
	{
		final int i = 4 * index;
		return (long) data[i] << 24 | data[i + 1] << 16 | data[i + 2] << 8 | data[i + 3];
	}

	private String makeString(final int index)
	{
		return appendUnit(Long.toString(fromDPT(index)));
	}

	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			toDPT(Long.decode(removeUnit(value)).longValue(), dst, index);
		}
		catch (final NumberFormatException e) {
			logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
		}
	}

	private short[] toDPT(final long value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (value < 0 || value > 0xFFFFFFFFL)
			logThrow(LogLevel.WARN, "translation error for " + value,
					"input value out of range", Long.toString(value));
		final int i = 4 * index;
		dst[i] = (short) ((value >> 24) & 0xFF);
		dst[i + 1] = (short) ((value >> 16) & 0xFF);
		dst[i + 2] = (short) ((value >> 8) & 0xFF);
		dst[i + 3] = (short) (value & 0xFF);
		return dst;
	}
}

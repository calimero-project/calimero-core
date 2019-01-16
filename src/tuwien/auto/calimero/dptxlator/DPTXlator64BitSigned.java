/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2019 B. Malinowsky

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

package tuwien.auto.calimero.dptxlator;

import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 29, type <b>8 Byte signed (V64)</b>, used for electrical
 * energy.
 * <p>
 * The KNX data type width is 8 bytes.<br>
 * The default return value after creation is 0.
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal, hexadecimal,
 * and octal numbers, distinguished by using these prefixes:
 * <dl>
 * <dt>no prefix</dt><dd>for decimal numeral</dd>
 * <dt><code>0x</code>, <code>0X</code>, <code>#</code><dd>for hexadecimal numeral</dd>
 * <dt><code>0</code><dd>for octal numeral</dd>
 * </dl>
 */
public class DPTXlator64BitSigned extends DPTXlator
{
	/**
	 * DPT ID 29.010, Active energy in watthours; values from <b>-9223372036854775808</b> to
	 * <b>9223372036854775807</b> Wh, resolution 1 Wh.
	 */
	public static final DPT DPT_ACTIVE_ENERGY = new DPT("29.010", "Active Energy",
			"-9223372036854775808", "9223372036854775807", "Wh");

	/**
	 * DPT ID 29.011, Apparent energy; values from <b>-9223372036854775808</b> to <b>2147483647</b>
	 * VAh, resolution 1 VAh.
	 */
	public static final DPT DPT_APPARENT_ENERGY = new DPT("29.011", "Apparent energy",
			"-9223372036854775808", "9223372036854775807", "VAh");

	/**
	 * DPT ID 29.012, Reactive energy; values from <b>-9223372036854775808</b> to <b>2147483647</b>
	 * VARh, resolution 1 VARh.
	 */
	public static final DPT DPT_REACTIVE_ENERGY = new DPT("29.012", "Reactive energy",
			"-9223372036854775808", "9223372036854775807", "VARh");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator64BitSigned.class);

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator64BitSigned(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 * <p>
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptID</code>
	 */
	public DPTXlator64BitSigned(final String dptId) throws KNXFormatException
	{
		super(8);
		setTypeID(types, dptId);
		data = new short[8];
	}

	/**
	 * Sets the value of the first translation item.
	 * <p>
	 *
	 * @param value signed value
	 * @see #getType()
	 */
	public final void setValue(final long value)
	{
		data = toDPT(value, new short[8], 0);
	}

	/**
	 * Returns the first translation item as signed 64 Bit value.
	 * <p>
	 *
	 * @return signed 64 Bit value using type long
	 * @see #getType()
	 */
	public final long getValueSigned()
	{
		return fromDPT(0);
	}

	/**
	 * Returns the first translation item as signed 64 Bit value.
	 *
	 * @return numeric value
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 * @see #getValueSigned()
	 */
	@Override
	public final double getNumericValue()
	{
		return getValueSigned();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	@Override
	public String getValue()
	{
		return makeString(0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length / 8];
		for (int i = 0; i < s.length; ++i)
			s[i] = makeString(i);
		return s;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 4-byte unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private long fromDPT(final int index)
	{
		final int i = 8 * index;
		return ((long) data[i]) << 56 | ((long) data[i + 1]) << 48 | ((long) data[i + 2]) << 40
				| ((long) data[i + 3]) << 32 | ((long) data[i + 4]) << 24 | data[i + 5] << 16
				| data[i + 6] << 8 | data[i + 7];
	}

	private String makeString(final int index)
	{
		return appendUnit(Long.toString(fromDPT(index)));
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#toDPT(java.lang.String, short[], int)
	 */
	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			toDPT(Long.decode(removeUnit(value)).longValue(), dst, index);
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}

	private static short[] toDPT(final long value, final short[] dst, final int index)
	{
		final int i = 8 * index;
		dst[i] = (short) ((value >> 56) & 0xFF);
		dst[i + 1] = (short) ((value >> 48) & 0xFF);
		dst[i + 2] = (short) ((value >> 40) & 0xFF);
		dst[i + 3] = (short) (value >> 32 & 0xFF);
		dst[i + 4] = (short) ((value >> 24) & 0xFF);
		dst[i + 5] = (short) ((value >> 16) & 0xFF);
		dst[i + 6] = (short) ((value >> 8) & 0xFF);
		dst[i + 7] = (short) (value & 0xFF);
		return dst;
	}
}

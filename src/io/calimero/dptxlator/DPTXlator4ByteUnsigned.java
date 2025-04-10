/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.dptxlator;

import java.util.Map;

import io.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 12, type <b>4 byte unsigned value</b>.
 * <p>
 * The KNX data type width is 4 bytes.<br>
 * The default return value after creation is 0.
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal,
 * hexadecimal, and octal numbers, distinguished by using these prefixes:
 * <dl>
 * <dt>no prefix</dt><dd>for decimal numeral</dd>
 * <dt>{@code 0x}, {@code 0X}, {@code #}<dd>for hexadecimal numeral</dd>
 * <dt>{@code 0}<dd>for octal numeral</dd>
 * </dl>
 */
public class DPTXlator4ByteUnsigned extends DPTXlator
{
	/**
	 * DPT ID 12.001, Unsigned count; values from <b>0</b> to <b>4294967295</b> counter
	 * pulses.
	 */
	public static final DPT DPT_VALUE_4_UCOUNT = new DPT("12.001", "Unsigned count", "0",
			"4294967295", "counter pulses");

	/** DPT ID 12.100, counter time [s]; values from <b>0</b> to <b>4294967295</b> s. */
	public static final DPT DptTimePeriodSec = new DPT("12.100", "counter time [s]", "0", "4294967295", "s");

	/** DPT ID 12.101, counter time [min]; values from <b>0</b> to <b>4294967295</b> min. */
	public static final DPT DptTimePeriodMin = new DPT("12.101", "counter time [min]", "0", "4294967295", "min");

	/** DPT ID 12.102, counter time [h]; values from <b>0</b> to <b>4294967295</b> h. */
	public static final DPT DptTimePeriodHours = new DPT("12.102", "counter time [h]", "0", "4294967295", "h");

	// metering

	/**
	 * DPT ID 12.1200, volume liquid [liter] for water/heat meter total consumption; values from <b>0</b> to
	 * <b>4294967295</b> liter.
	 */
	public static final DPT DptVolumeLiquid = new DPT("12.1200", "volume liquid [liter]", "0", "4294967295", "l");

	/**
	 * DPT ID 12.1201, volume [m³] for gas/water/heat meter total consumption; values from <b>0</b> to
	 * <b>4294967295</b> m³.
	 */
	public static final DPT DptVolume = new DPT("12.1201", "volume [m³]", "0", "4294967295", "m³");


	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator4ByteUnsigned.class);


	/**
	 * Creates a translator for the given datapoint type.
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
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available)
	 *         {@code dptID}
	 */
	public DPTXlator4ByteUnsigned(final String dptID) throws KNXFormatException
	{
		super(4);
		setTypeID(types, dptID);
		data = new short[4];
	}

	@Override
	public void setValue(final double value) throws KNXFormatException {
		setValue((long) value);
	}

	/**
	 * Sets the value of the first translation item.
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
	 *
	 * @return unsigned 32 Bit value using type long
	 * @see #getType()
	 */
	public final long getValueUnsigned()
	{
		return fromDPT(0);
	}

	/**
	 * Returns the first translation item as unsigned 32 Bit value.
	 *
	 * @return numeric value
	 * @see io.calimero.dptxlator.DPTXlator#getNumericValue()
	 * @see #getValueUnsigned()
	 */
	@Override
	public final double getNumericValue()
	{
		return getValueUnsigned();
	}

	@Override
	public String getValue()
	{
		return makeString(0);
	}

	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length / 4];
		for (int i = 0; i < s.length; ++i)
			s[i] = makeString(i);
		return s;
	}

	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * {@return the subtypes of the 4-byte unsigned translator type}
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
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

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			toDPT(Long.decode(removeUnit(value)), dst, index);
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}

	private short[] toDPT(final long value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (value < 0 || value > 0xFFFFFFFFL)
			throw newException("translation error, input value out of range", Long.toString(value));
		final int i = 4 * index;
		dst[i] = (short) ((value >> 24) & 0xFF);
		dst[i + 1] = (short) ((value >> 16) & 0xFF);
		dst[i + 2] = (short) ((value >> 8) & 0xFF);
		dst[i + 3] = (short) (value & 0xFF);
		return dst;
	}
}

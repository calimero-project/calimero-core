/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2019 B. Malinowsky

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
 * Translator for KNX DPTs with main number 7, type <b>2 byte unsigned value</b>.
 * <p>
 * The KNX data type width is 2 bytes.<br>
 * The default return value after creation is 0.
 * <p>
 * Supplied string value items might be formatted using decimal, hexadecimal, and octal
 * numbers, distinguished by using these prefixes:
 * <dl>
 * <dt>no prefix</dt><dd>for decimal numeral</dd>
 * <dt><code>0x</code>, <code>0X</code>, <code>#</code><dd>for hexadecimal numeral</dd>
 * <dt><code>0</code><dd>for octal numeral</dd>
 * </dl>
 */
public class DPTXlator2ByteUnsigned extends DPTXlator
{
	/**
	 * DPT ID 7.001, Unsigned count; values from <b>0</b> to <b>65535</b> pulses.
	 */
	public static final DPT DPT_VALUE_2_UCOUNT =
		new DPT("7.001", "Unsigned count", "0", "65535", "pulses");

	/**
	 * DPT ID 7.002, Time period in ms; values from <b>0</b> to <b>65535</b> ms.
	 */
	public static final DPT DPT_TIMEPERIOD =
		new DPT("7.002", "Time period in ms", "0", "65535", "ms");

	/**
	 * DPT ID 7.003, Time period (resolution 10 ms); values from <b>0</b> to <b>655350</b>
	 * ms.
	 */
	public static final DPT DPT_TIMEPERIOD_10 =
		new DPT("7.003", "Time period (resolution 10 ms)", "0", "655350", "ms");

	/**
	 * DPT ID 7.004, Time period (resolution 100 ms); values from <b>0</b> to <b>6553500</b>
	 * ms.
	 */
	public static final DPT DPT_TIMEPERIOD_100 =
		new DPT("7.004", "Time period (resolution 100 ms)", "0", "6553500", "ms");

	/**
	 * DPT ID 7.005, Time period in seconds; values from <b>0</b> to <b>65535</b> s
	 * (~18,2 hours).
	 */
	public static final DPT DPT_TIMEPERIOD_SEC =
		new DPT("7.005", "Time period in seconds", "0", "65535", "s");

	/**
	 * DPT ID 7.006, Time period in minutes; values from <b>0</b> to <b>65535</b> min
	 * (~45,5 days).
	 */
	public static final DPT DPT_TIMEPERIOD_MIN =
		new DPT("7.006", "Time period in minutes", "0", "65535", "min");

	/**
	 * DPT ID 7.007, Time period in hours; values from <b>0</b> to <b>65535</b> h (~7,4 years).
	 */
	public static final DPT DPT_TIMEPERIOD_HOURS =
		new DPT("7.007", "Time period in hours", "0", "65535", "h");

	/**
	 * DPT ID 7.010, Interface object property ID; values from <b>0</b> to <b>65535</b>.
	 */
	public static final DPT DPT_PROP_DATATYPE =
		new DPT("7.010", "Interface object property ID", "0", "65535", "");

	/**
	 * DPT ID 7.011, Length in millimeters; values from <b>0</b> to <b>65535</b>, resolution 1.
	 */
	public static final DPT DPT_LENGTH = new DPT("7.011", "Length in mm", "0", "65535", "mm");

	/**
	 * DPT ID 7.012, Electrical current; values from <b>0</b> to <b>65535</b> mA.
	 * <p>
	 * A value of 0 indicates no bus power supply functionality available, 1 to 65535 is
	 * the current in mA.
	 */
	public static final DPT DPT_ELECTRICAL_CURRENT =
		new DPT("7.012", "Electrical current", "0", "65535", "mA");

	/**
	 * DPT ID 7.013, Brightness (Lux); values from <b>0</b> to <b>65535</b> lx.
	 * <p>
	 * Note: the unit of measure symbol used here is "lx", and not "Lux" as originally
	 * proposed for this DPT.
	 */
	public static final DPT DPT_BRIGHTNESS = new DPT("7.013", "Brightness", "0", "65535", "lx");

	/**
	 * DPT ID 7.600, Absolute color temperature (Kelvin); values from <b>0</b> to <b>65535</b> K.
	 */
	public static final DPT DPT_ABSOLUTE_COLOR_TEMPERATURE = new DPT("7.600", "Absolute color temperature", "0", "65535", "K");


	private static final Map<String, DPT> types  = loadDatapointTypes(DPTXlator2ByteUnsigned.class);

	private final int min;
	private final int max;

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator2ByteUnsigned(final DPT dpt) throws KNXFormatException
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
	public DPTXlator2ByteUnsigned(final String dptID) throws KNXFormatException
	{
		super(2);
		setTypeID(types, dptID);
		min = getLimit(dpt.getLowerValue());
		max = getLimit(dpt.getUpperValue());
		data = new short[2];
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	@Override
	public String getValue()
	{
		return makeString(0);
	}

	/**
	 * Sets the value of the first translation item.
	 * <p>
	 * A value of DPT {@link #DPT_TIMEPERIOD_10} or {@link #DPT_TIMEPERIOD_100} is
	 * expected in unit millisecond, i.e., a value of DPT_TIMEPERIOD_10 gets divided by 10,
	 * a value of DPT_TIMEPERIOD_100 by 100. The result is rounded to the nearest
	 * representable value (with 0.5 rounded up). On any other DPT the value is expected
	 * according to its unit.
	 *
	 * @param value unsigned value, 0 &lt;= value &lt;= max, with
	 *        <ul>
	 *        <li>max = 655350 on DPT {@link #DPT_TIMEPERIOD_10}</li>
	 *        <li>max = 6553500 on DPT {@link #DPT_TIMEPERIOD_100}</li>
	 *        <li>max = 65535 otherwise</li>
	 *        </ul>
	 * @throws KNXFormatException on input value out of range for DPT
	 * @see #getType()
	 */
	public final void setValue(final int value) throws KNXFormatException
	{
		final short[] buf = new short[2];
		toDPT(value, buf, 0);
		data = buf;
	}

	/**
	 * Returns the first translation item as unsigned value.
	 * <p>
	 * A value of DPT {@link #DPT_TIMEPERIOD_10} or {@link #DPT_TIMEPERIOD_100} is
	 * returned in unit millisecond, i.e., a KNX DPT_TIMEPERIOD_10 data value is multiplied
	 * with 10, DPT_TIMEPERIOD_100 with 100.<br>
	 * On any other DPT the value is returned according to its unit.
	 *
	 * @return value as unsigned 16 Bit using type int
	 * @see #getType()
	 */
	public final int getValueUnsigned()
	{
		return fromDPT(0);
	}

	/**
	 * Returns the first translation item as unsigned value.
	 * <p>
	 * A value of DPT {@link #DPT_TIMEPERIOD_10} or {@link #DPT_TIMEPERIOD_100} is returned in unit
	 * millisecond, i.e., a KNX DPT_TIMEPERIOD_10 data value is multiplied with 10,
	 * DPT_TIMEPERIOD_100 with 100.<br>
	 * On any other DPT the value is returned according to its unit.
	 *
	 * @return numeric value
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 * @see #getValueUnsigned()
	 */
	@Override
	public final double getNumericValue()
	{
		return getValueUnsigned();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length / 2];
		for (int i = 0; i < s.length; ++i)
			s[i] = makeString(i);
		return s;
	}

	/**
	 * Sets the first translation item value from input of unit millisecond.
	 * <p>
	 * The method is for DPTs dealing with periods of time, in particular
	 * {@link #DPT_TIMEPERIOD}, {@link #DPT_TIMEPERIOD_10}, {@link #DPT_TIMEPERIOD_100}
	 * {@link #DPT_TIMEPERIOD_SEC}, {@link #DPT_TIMEPERIOD_MIN} and
	 * {@link #DPT_TIMEPERIOD_HOURS}. The milliseconds are converted to the unit of the
	 * set DPT, with the result rounded to the nearest representable value (with 0.5
	 * rounded up).<br>
	 * On any other DPT, the input is treated equal to {@link #setValue(int)}.
	 *
	 * @param milliseconds the value in milliseconds, 0 &lt;= <code>milliseconds</code>
	 * @throws KNXFormatException on milliseconds out of range for DPT
	 */
	public final void setTimePeriod(final long milliseconds) throws KNXFormatException
	{
		data = toDPT(milliseconds);
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
	 * @return the subtypes of the 2-byte unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private int fromDPT(final int index)
	{
		final int v = (data[2 * index] << 8) | data[2 * index + 1];
		if (dpt.equals(DPT_TIMEPERIOD_10))
			return v * 10;
		else if (dpt.equals(DPT_TIMEPERIOD_100))
			return v * 100;
		return v;
	}

	private String makeString(final int index)
	{
		return appendUnit(String.valueOf(fromDPT(index)));
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			toDPT(Integer.decode(removeUnit(value)).intValue(), dst, index);
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}

	private short[] toDPT(final long ms) throws KNXFormatException
	{
		// prevent round up to 0 from negative milliseconds
		if (ms < 0)
			throw newException("negative input value", Long.toString(ms));
		long v = ms;
		if (dpt.equals(DPT_TIMEPERIOD_SEC))
			v = Math.round(ms / 1000.0);
		else if (dpt.equals(DPT_TIMEPERIOD_MIN))
			v = Math.round(ms / 1000.0 / 60.0);
		else if (dpt.equals(DPT_TIMEPERIOD_HOURS))
			v = Math.round(ms / 1000.0 / 60.0 / 60.0);
		final short[] buf = new short[2];
		toDPT((int) v, buf, 0);
		return buf;
	}

	private void toDPT(final int value, final short[] dst, final int index) throws KNXFormatException
	{
		if (value < min || value > max)
			throw newException("translation error, input value out of range ["
							+ dpt.getLowerValue() + ".." + dpt.getUpperValue() + "]", Integer.toString(value));
		int v = value;
		if (dpt.equals(DPT_TIMEPERIOD_10))
			v = Math.round(value / 10.0f);
		else if (dpt.equals(DPT_TIMEPERIOD_100))
			v = Math.round(value / 100.0f);
		dst[2 * index] = ubyte(v >> 8);
		dst[2 * index + 1] = ubyte(v);
	}

	private int getLimit(final String limit) throws KNXFormatException
	{
		try {
			final int i = Integer.parseInt(limit);
			final int upper = dpt.equals(DPT_TIMEPERIOD_10) ? 655350 : dpt
					.equals(DPT_TIMEPERIOD_100) ? 6553500 : 65535;
			if (i >= 0 && i <= upper)
				return i;
		}
		catch (final NumberFormatException e) {}
		throw newException("limit not in valid DPT range", limit);
	}
}

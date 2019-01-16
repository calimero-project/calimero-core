/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2009, 2019 B. Malinowsky

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
 * Translator for KNX DPTs with main number 13, type <b>4 byte signed value</b>.
 * <p>
 * The KNX data type width is 4 bytes.<br>
 * The default return value after creation is 0.
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal,
 * hexadecimal, and octal numbers, distinguished by using these prefixes:
 * <dl>
 * <dt>no prefix</dt><dd>for decimal numeral</dd>
 * <dt><code>0x</code>, <code>0X</code>, <code>#</code><dd>for hexadecimal numeral</dd>
 * <dt><code>0</code><dd>for octal numeral</dd>
 * </dl>
 */
public class DPTXlator4ByteSigned extends DPTXlator
{
	/**
	 * DPT ID 13.001, Counter pulses; values from <b>-2147483648</b> to <b>2147483647</b> counter
	 * pulses.
	 */
	public static final DPT DPT_COUNT = new DPT("13.001", "Counter pulses", "-2147483648",
			"2147483647", "counter pulses");

	/**
	 * DPT ID 13.002, Flow rate in m3/h with high resolution; values from <b>-2147483648</b> to
	 * <b>2147483647</b>, resolution 0.0001 m3/h.
	 */
	public static final DPT DPT_FLOWRATE = new DPT("13.002", "Flow rate", "-2147483648",
			"2147483647", "m3/h");

	/**
	 * DPT ID 13.010, Active energy in watthours; values from <b>-2147483648</b> to
	 * <b>2147483647</b> Wh.
	 */
	public static final DPT DPT_ACTIVE_ENERGY = new DPT("13.010", "Active Energy", "-2147483648",
			"2147483647", "Wh");

	/**
	 * DPT ID 13.011, Apparent energy; values from <b>-2147483648</b> to <b>2147483647</b> VAh.
	 */
	public static final DPT DPT_APPARENT_ENERGY = new DPT("13.011", "Apparent energy",
			"-2147483648", "2147483647", "VAh");

	/**
	 * DPT ID 13.012, Reactive energy; values from <b>-2147483648</b> to <b>2147483647</b> VARh.
	 */
	public static final DPT DPT_REACTIVE_ENERGY = new DPT("13.012", "Reactive energy",
			"-2147483648", "2147483647", "VARh");

	/**
	 * DPT ID 13.013, Active energy in kilowatthours; values from <b>-2147483648</b> to
	 * <b>2147483647</b> kWh.
	 */
	public static final DPT DPT_ACTIVE_ENERGY_KWH = new DPT("13.013", "Active energy in kWh",
			"-2147483648", "2147483647", "kWh");

	/**
	 * DPT ID 13.014, Apparent energy in kilovolt-ampere-hours; values from <b>-2147483648</b> to
	 * <b>2147483647</b> kVAh.
	 */
	public static final DPT DPT_APPARENT_ENERGY_KVAH = new DPT("13.014", "Apparent energy in kVAh",
			"-2147483648", "2147483647", "kVAh");

	/**
	 * DPT ID 13.015, Reactive energy in kVARh; values from <b>-2147483648</b> to <b>2147483647</b>
	 * kVARh.
	 */
	public static final DPT DPT_REACTIVE_ENERGY_KVARH = new DPT("13.015",
			"Reactive energy in kVARh", "-2147483648", "2147483647", "kVARh");

	/**
	 * DPT ID 13.100, time lag in seconds; values from <b>-2147483648</b> to <b>2147483647</b> s.
	 */
	public static final DPT DPT_DELTA_TIME = new DPT("13.100", "time lag", "-2147483648", "2147483647", "s");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator4ByteSigned.class);

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator4ByteSigned(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available)
	 *         <code>dptID</code>
	 */
	public DPTXlator4ByteSigned(final String dptId) throws KNXFormatException
	{
		super(4);
		setTypeID(types, dptId);
		data = new short[4];
	}

	/**
	 * Sets the value of the first translation item.
	 *
	 * @param value signed value
	 * @see #getType()
	 */
	public final void setValue(final int value)
	{
		data = toDPT(value, new short[4], 0);
	}

	/**
	 * Returns the first translation item as signed 32 Bit value.
	 * <p>
	 *
	 * @return signed 32 Bit value
	 * @see #getType()
	 */
	public final int getValueSigned()
	{
		return fromDPT(0);
	}

	/**
	 * Returns the first translation item as signed 32 Bit value.
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
		final String[] s = new String[data.length / 4];
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

	private int fromDPT(final int index)
	{
		final int i = 4 * index;
		return data[i] << 24 | data[i + 1] << 16 | data[i + 2] << 8 | data[i + 3];
	}

	private String makeString(final int index)
	{
		return appendUnit(Integer.toString(fromDPT(index)));
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

	private static short[] toDPT(final int value, final short[] dst, final int index)
	{
		final int i = 4 * index;
		dst[i] = (short) ((value >> 24) & 0xFF);
		dst[i + 1] = (short) ((value >> 16) & 0xFF);
		dst[i + 2] = (short) ((value >> 8) & 0xFF);
		dst[i + 3] = (short) (value & 0xFF);
		return dst;
	}
}

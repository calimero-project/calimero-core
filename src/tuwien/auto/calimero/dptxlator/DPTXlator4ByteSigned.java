/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2009, 2011 B. Malinowsky

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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.log.LogLevel;

/**
 * Translator for KNX DPTs with main number 13, type <b>4 byte signed value</b>.
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
public class DPTXlator4ByteSigned extends DPTXlator
{
	/**
	 * DPT ID 13.001, Counter pulses; values from <b>-2147483648</b> to <b>2147483647</b> counter
	 * pulses.
	 * <p>
	 */
	public static final DPT DPT_COUNT = new DPT("13.001", "Counter pulses", "-2147483648",
			"2147483647", "counter pulses");

	/**
	 * DPT ID 13.002, Flow rate in m3/h with high resolution; values from <b>-2147483648</b> to
	 * <b>2147483647</b>, resolution 0.0001 m3/h.
	 * <p>
	 */
	public static final DPT DPT_FLOWRATE = new DPT("13.002", "Flow rate", "-2147483648",
			"2147483647", "m3/h");

	/**
	 * DPT ID 13.010, Active energy in watthours; values from <b>-2147483648</b> to
	 * <b>2147483647</b> Wh.
	 * <p>
	 */
	public static final DPT DPT_ACTIVE_ENERGY = new DPT("13.010", "Active Energy", "-2147483648",
			"2147483647", "Wh");

	/**
	 * DPT ID 13.011, Apparent energy; values from <b>-2147483648</b> to <b>2147483647</b> VAh.
	 * <p>
	 */
	public static final DPT DPT_APPARENT_ENERGY = new DPT("13.011", "Apparent energy",
			"-2147483648", "2147483647", "VAh");

	/**
	 * DPT ID 13.012, Reactive energy; values from <b>-2147483648</b> to <b>2147483647</b> VARh.
	 * <p>
	 */
	public static final DPT DPT_REACTIVE_ENERGY = new DPT("13.012", "Reactive energy",
			"-2147483648", "2147483647", "VARh");

	/**
	 * DPT ID 13.013, Active energy in kilowatthours; values from <b>-2147483648</b> to
	 * <b>2147483647</b> kWh.
	 * <p>
	 */
	public static final DPT DPT_ACTIVE_ENERGY_KWH = new DPT("13.013", "Active energy in kWh",
			"-2147483648", "2147483647", "kWh");

	/**
	 * DPT ID 13.014, Apparent energy in kilovolt-ampere-hours; values from <b>-2147483648</b> to
	 * <b>2147483647</b> kVAh.
	 * <p>
	 */
	public static final DPT DPT_APPARENT_ENERGY_KVAH = new DPT("13.014", "Apparent energy in kVAh",
			"-2147483648", "2147483647", "kVAh");

	/**
	 * DPT ID 13.015, Reactive energy in kVARh; values from <b>-2147483648</b> to <b>2147483647</b>
	 * kVARh.
	 * <p>
	 */
	public static final DPT DPT_REACTIVE_ENERGY_KVARH = new DPT("13.015",
			"Reactive energy in kVARh", "-2147483648", "2147483647", "kVARh");

	/**
	 * DPT ID 13.100, Delta time in seconds; values from <b>-2147483648</b> to <b>2147483647</b> s.
	 * <p>
	 */
	public static final DPT DPT_DELTA_TIME = new DPT("13.100", "Delta time in seconds",
			"-2147483648", "2147483647", "s");

	private static final Map types;
	
	static {
		types = new HashMap(15);
		final Field[] fields = DPTXlator4ByteSigned.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			try {
				final Object o = fields[i].get(null);
				if (o instanceof DPT) {
					types.put(((DPT) o).getID(), o);
				}
			}
			catch (final IllegalAccessException e) {}
		}
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
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
	 * <p>
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
	 * <p>
	 * 
	 * @param value signed value
	 * @throws KNXFormatException on input value out of range for DPT
	 * @see #getType()
	 */
	public final void setValue(final int value) throws KNXFormatException
	{
		data = toDPT(value, new short[4], 0);
	}

	/**
	 * Returns the first translation item as signed 32 Bit value.
	 * <p>
	 * 
	 * @return signed 32 Bit value using type long
	 * @see #getType()
	 */
	public final int getValueSigned()
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

	private int fromDPT(final int index)
	{
		final int i = 4 * index;
		return data[i] << 24 | data[i + 1] << 16 | data[i + 2] << 8 | data[i + 3];
	}

	private String makeString(final int index)
	{
		return appendUnit(Integer.toString(fromDPT(index)));
	}

	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			toDPT(Integer.decode(removeUnit(value)).intValue(), dst, index);
		}
		catch (final NumberFormatException e) {
			logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
		}
	}

	private short[] toDPT(final int value, final short[] dst, final int index)
		throws KNXFormatException
	{
		final int i = 4 * index;
		dst[i] = (short) ((value >> 24) & 0xFF);
		dst[i + 1] = (short) ((value >> 16) & 0xFF);
		dst[i + 2] = (short) ((value >> 8) & 0xFF);
		dst[i + 3] = (short) (value & 0xFF);
		return dst;
	}
}

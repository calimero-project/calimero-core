/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2012 B. Malinowsky

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
 * Translator for KNX DPTs with main number 5, type <b>8 Bit unsigned value</b>.
 * <p>
 * The KNX data type width is 1 byte.<br>
 * The default return value after creation is 0.<br>
 * Note, that {@link #DPT_SCALING} and {@link #DPT_ANGLE} are scaled representations,
 * which means the DPT's value ranges [0..100] respectively [0..360] are mapped to the 8
 * Bit KNX value range [0..255], with mapped values rounded to the next nearest integer
 * value.
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal,
 * hexadecimal, and octal numbers, distinguished by using these prefixes:
 * <dd>no prefix for decimal numeral
 * <dd><code>0x</code>, <code>0X</code> or <code>#</code> for hexadecimal
 * <dd><code>0</code> for octal numeral
 */
public class DPTXlator8BitUnsigned extends DPTXlator
{
	/**
	 * DPT ID 5.001, Scaling; values from <b>0</b> to <b>100</b> %.
	 * <p>
	 */
	public static final DPT DPT_SCALING = new DPT("5.001", "Scaling", "0", "100", "%");

	/**
	 * DPT ID 5.003, Angle; values from <b>0</b> to <b>360</b> \u00b0 (degree).
	 * <p>
	 */
	public static final DPT DPT_ANGLE = new DPT("5.003", "Angle", "0", "360", "\u00b0");

	/**
	 * DPT ID 5.004, Percent 8 Bit; values from <b>0</b> to <b>255</b> %.
	 * <p>
	 */
	public static final DPT DPT_PERCENT_U8 = new DPT("5.004", "Percent (8 Bit)", "0", "255", "%");

	/**
	 * DPT ID 5.005, Decimal factor; values from <b>0</b> to <b>255</b> ratio.
	 * <p>
	 * <b>This is a preliminary datapoint type, still in state "to be defined".</b>
	 */
	public static final DPT DPT_DECIMALFACTOR = new DPT("5.005", "Decimal factor", "0", "255",
			"ratio");

	/**
	 * DPT ID 5.006, Tariff information; values from <b>0</b> to <b>254</b>.
	 * <p>
	 * 0: no tariff available<br>
	 * 1 to 254: current or desired value<br>
	 * 255: reserved, shall not be transmitted
	 */
	public static final DPT DPT_TARIFF = new DPT("5.006", "Tariff information", "0", "254");
	
	/**
	 * DPT ID 5.010, Value 1 unsigned count; values from <b>0</b> to <b>255</b> counter
	 * pulses.
	 * <p>
	 */
	public static final DPT DPT_VALUE_1_UCOUNT = new DPT("5.010", "Unsigned count", "0", "255",
			"counter pulses");

	private static final Map types;

	static {
		types = new HashMap();
		types.put(DPT_SCALING.getID(), DPT_SCALING);
		types.put(DPT_ANGLE.getID(), DPT_ANGLE);
		types.put(DPT_PERCENT_U8.getID(), DPT_PERCENT_U8);
		types.put(DPT_DECIMALFACTOR.getID(), DPT_DECIMALFACTOR);
		types.put(DPT_VALUE_1_UCOUNT.getID(), DPT_VALUE_1_UCOUNT);
		types.put(DPT_TARIFF.getID(), DPT_TARIFF);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 * 
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator8BitUnsigned(final DPT dpt) throws KNXFormatException
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
	public DPTXlator8BitUnsigned(final String dptID) throws KNXFormatException
	{
		super(1);
		setSubType(dptID);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	public String getValue()
	{
		return makeString(0);
	}

	/**
	 * Sets one new translation item from a scaled unsigned value, replacing any old
	 * items.
	 * <p>
	 * The scale of the input value is according to the current DPT.
	 * 
	 * @param scaled scaled unsigned value, the dimension is determined by the set DPT, 0
	 *        &lt;= scaled value &lt;= defined maximum of DPT
	 * @throws KNXFormatException on wrong scaled value, if input doesn't conform to
	 *         subtype dimension
	 * @see #getType()
	 */
	public final void setValue(final int scaled) throws KNXFormatException
	{
		data = new short[] { toDPT(scaled) };
	}

	/**
	 * Returns the first translation item, the value scaled conforming to the range of the
	 * set DPT.
	 * <p>
	 * 
	 * @return scaled representation as unsigned 8 Bit using type short
	 * @see #getType()
	 */
	public final short getValueUnsigned()
	{
		return fromDPT(data[0]);
	}

	/**
	 * Sets one new translation item from an unsigned unscaled value, replacing any old
	 * items.
	 * <p>
	 * No scaling is performed during translation, the value is equal to the raw KNX data.
	 * 
	 * @param unscaled unscaled unsigned value, 0 &lt;= <code>unscaled</code> &lt;= 255,
	 *        the higher bytes are ignored
	 */
	public final void setValueUnscaled(final int unscaled)
	{
		data = new short[] { ubyte(unscaled) };
	}

	/**
	 * Returns the first translation item without any scaling.
	 * <p>
	 * The returned value is the raw KNX data value (0..255), not adjusted to the value
	 * range of the set DPT.
	 * 
	 * @return unscaled representation as unsigned byte
	 */
	public final short getValueUnscaled()
	{
		return data[0];
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i)
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
	 * @return the subtypes of the 8 Bit unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map getSubTypesStatic()
	{
		return types;
	}

	/**
	 * Sets a new subtype to use for translating items.
	 * <p>
	 * The translator is reset into default state, all currently contained items are
	 * removed (default value is set).
	 * 
	 * @param dptID new subtype ID to set
	 * @throws KNXFormatException on wrong formatted or not expected (available)
	 *         <code>dptID</code>
	 */
	private void setSubType(final String dptID) throws KNXFormatException
	{
		setTypeID(types, dptID);
		data = new short[1];
	}

	private short fromDPT(final short data)
	{
		short value = data;
		if (dpt.equals(DPT_SCALING))
			value = (short) Math.round(data * 100.0f / 255);
		else if (dpt.equals(DPT_ANGLE))
			value = (short) Math.round(data * 360.0f / 255);
		return value;
	}

	private String makeString(final int index)
	{
		return appendUnit(Short.toString(fromDPT(data[index])));
	}

	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			dst[index] = toDPT(Short.decode(removeUnit(value)).shortValue());
		}
		catch (final NumberFormatException e) {
			logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
		}
	}

	private short toDPT(final int value) throws KNXFormatException
	{
		try {
			if (value < 0 || value > Integer.parseInt(dpt.getUpperValue()))
				logThrow(LogLevel.WARN, "translation error for " + value,
					"input value out of range [" + dpt.getLowerValue() + ".."
						+ dpt.getUpperValue() + "]", Integer.toString(value));
		}
		catch (final NumberFormatException e) {
			logThrow(LogLevel.ERROR, "parsing " + dpt, null, dpt.getUpperValue());
		}
		int convert = value;
		if (dpt.equals(DPT_SCALING))
			convert = Math.round(value * 255.0f / 100);
		else if (dpt.equals(DPT_ANGLE))
			convert = Math.round(value * 255.0f / 360);
		return (short) convert;
	}
}

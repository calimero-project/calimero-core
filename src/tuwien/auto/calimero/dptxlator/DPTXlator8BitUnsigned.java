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

import java.text.DecimalFormat;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 5, type <b>8 Bit unsigned value</b>.
 * <p>
 * The KNX data type width is 1 byte.<br>
 * The default return value after creation is 0.<br>
 * Note, that {@link #DPT_SCALING} and {@link #DPT_ANGLE} are scaled representations, which means the DPT's value ranges
 * [0..100] respectively [0..360] are mapped to the 8 Bit KNX value range [0..255], with mapped values rounded to the
 * next nearest integer value.
 * <p>
 * In value methods expecting string items, an item containing a floating point number is parsed according to
 * {@link Double#parseDouble(String)}, an item containg a value without a fractional component might be formatted using
 * decimal, hexadecimal, and octal numbers, distinguished by using these prefixes:
 * <dl>
 * <dt>no prefix</dt>
 * <dd>for decimal numeral</dd>
 * <dt><code>0x</code>, <code>0X</code>, <code>#</code>
 * <dd>for hexadecimal numeral</dd>
 * <dt><code>0</code>
 * <dd>for octal numeral</dd>
 * </dl>
 */
public class DPTXlator8BitUnsigned extends DPTXlator
{
	/**
	 * DPT ID 5.001, Scaling; values from <b>0</b> to <b>100</b> %.
	 */
	public static final DPT DPT_SCALING = new DPT("5.001", "Scaling", "0", "100", "%");

	/**
	 * DPT ID 5.003, Angle; values from <b>0</b> to <b>360</b> \u00b0 (degree).
	 */
	public static final DPT DPT_ANGLE = new DPT("5.003", "Angle", "0", "360", "\u00b0");

	/**
	 * DPT ID 5.004, Percent 8 Bit; values from <b>0</b> to <b>255</b> %.
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
	 */
	public static final DPT DPT_VALUE_1_UCOUNT = new DPT("5.010", "Unsigned count", "0", "255",
			"counter pulses");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator8BitUnsigned.class);

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
	@Override
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
	 * Returns the first translation item, the value scaled conforming to the range of the set DPT.
	 *
	 * @return scaled numeric value
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 * @see #getValueUnsigned()
	 */
	@Override
	public final double getNumericValue()
	{
		return toValue(data[0]);
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
	@Override
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
	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 8 Bit unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
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

	private double toValue(final short data)
	{
		final double maxPercent = 100.0d;
		final double maxAngle = 360.0d;

		if (dpt.equals(DPT_SCALING))
			return data * maxPercent / 255;
		if (dpt.equals(DPT_ANGLE))
			return data * maxAngle / 255;
		return data;
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
		final String s = new DecimalFormat("##.#").format(toValue(data[index]));
		return appendUnit(s);
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			try {
				dst[index] = toDPT(Double.parseDouble(removeUnit(value).replace(',', '.')));
			}
			catch (final NumberFormatException e) {
				dst[index] = toDPT(Short.decode(removeUnit(value)).shortValue());
			}
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}

	private short toDPT(final double value) throws KNXFormatException
	{
		try {
			if (value < 0 || value > Integer.parseInt(dpt.getUpperValue()))
				throw newException("translation error, input value out of range ["
						+ dpt.getLowerValue() + ".." + dpt.getUpperValue() + "]", Double.toString(value));
		}
		catch (final NumberFormatException e) {
			throw newException("parsing upper limit of " + dpt, dpt.getUpperValue());
		}
		if (dpt.equals(DPT_SCALING))
			return (short) Math.round(value * 255 / 100);
		if (dpt.equals(DPT_ANGLE))
			return (short) Math.round(value * 255 / 360);
		return (short) value;
	}
}

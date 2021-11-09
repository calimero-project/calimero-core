/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2021 B. Malinowsky

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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 8, type <b>2 byte signed value</b>.
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
public class DptXlator2ByteSigned extends DPTXlator
{
	/**
	 * DPT ID 8.001, signed count; values from <b>-32768</b> to <b>32767</b> pulses.
	 */
	public static final DPT DptValueCount = new DPT("8.001", "signed count", "-32768", "32767", "pulses");

	/**
	 * DPT ID 8.002, delta time in ms; values from <b>-32768</b> to <b>32767</b> ms.
	 */
	public static final DPT DptDeltaTime = new DPT("8.002", "delta time in ms", "-32768", "32767", "ms");

	/**
	 * DPT ID 8.003, delta time in ms (resolution 10 ms); values from <b>-327680</b> to <b>327670</b> ms.
	 */
	public static final DPT DptDeltaTime10 = new DPT("8.003", "delta time in ms (resolution 10 ms)", "-327680",
			"327670", "ms");

	/**
	 * DPT ID 8.004, delta time in ms (resolution 100 ms); values from <b>-3276800</b> to <b>3276700</b> ms.
	 */
	public static final DPT DptDeltaTime100 = new DPT("8.004", "delta time in ms (resolution 100 ms)", "-3276800",
			"3276700", "ms");

	/**
	 * DPT ID 8.005, delta time in seconds; values from <b>-32768</b> to <b>32767</b> s (~18,2 hours).
	 */
	public static final DPT DptDeltaTimeSec = new DPT("8.005", "delta time in seconds", "-32768", "32767", "s");

	/**
	 * DPT ID 8.006, delta time in minutes; values from <b>-32768</b> to <b>32767</b> min (~45,5 days).
	 */
	public static final DPT DptDeltaTimeMin = new DPT("8.006", "delta time in minutes", "-32768", "32767", "min");

	/**
	 * DPT ID 8.007, delta time in hours; values from <b>-32768</b> to <b>32767</b> h (~7,4 years).
	 */
	public static final DPT DptDeltaTimeHours = new DPT("8.007", "delta time in hours", "-32768", "32767", "h");

	/**
	 * DPT ID 8.010, percent; values from <b>-327.68</b> to <b>327.67</b> %, resolution 0.01.
	 */
	public static final DPT DptPercent = new DPT("8.010", "percent", "-327.68", "327.67", "%");

	/**
	 * DPT ID 8.011, rotation angle in degrees; values from <b>-32768</b> to <b>32767</b> °, resolution 1.
	 */
	public static final DPT DptRotationAngle = new DPT("8.011", "rotation angle", "-32768", "32767", "°");

	/**
	 * DPT ID 8.012, length in meters; values from <b>-32768</b> to <b>32767</b> m, resolution 1.
	 */
	public static final DPT DptLength = new DPT("8.012", "length in m", "-32768", "32767", "m");


	private static final Map<String, DPT> types  = loadDatapointTypes(DptXlator2ByteSigned.class);


	private final NumberFormat formatter = NumberFormat.getNumberInstance();

	private final double min;
	private final double max;

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlator2ByteSigned(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlator2ByteSigned(final String dptId) throws KNXFormatException
	{
		super(2);
		setTypeID(types, dptId);
		min = getLimit(dpt.getLowerValue());
		max = getLimit(dpt.getUpperValue());
		data = new short[2];
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
		formatter.setParseIntegerOnly(dpt.equals(DptPercent) ? false : true);
	}

	@Override
	public String getValue()
	{
		return makeString(0);
	}

	/**
	 * Sets the value of the first translation item.
	 * <p>
	 * Note, that a value of DPT {@link #DptDeltaTime10} or {@link #DptDeltaTime100} is
	 * expected in unit millisecond, i.e., a value of DptDeltaTime10 gets divided by 10,
	 * a value of DptDeltaTime100 by 100. The result is rounded to the nearest
	 * representable value (with 0.5 rounded up).
	 *
	 * @param value signed value, min &lt;= value &lt;= max, with
	 *        <ul>
	 *        <li>min = -327680 for DPT {@link #DptDeltaTime10}</li>
	 *        <li>min = -3276800 for DPT {@link #DptDeltaTime100}</li>
	 *        <li>min = -327.68 for DPT {@link #DptPercent}</li>
	 *        <li>min = -32768 otherwise</li>
	 *        </ul>
	 *        <ul>
	 *        <li>max = 327670 for DPT {@link #DptDeltaTime10}</li>
	 *        <li>max = 3276700 for DPT {@link #DptDeltaTime100}</li>
	 *        <li>max = 327.67 for DPT {@link #DptPercent}</li>
	 *        <li>max = 32767 otherwise</li>
	 *        </ul>
	 * @throws KNXFormatException on input value out of range for DPT
	 * @see #getType()
	 */
	@Override
	public final void setValue(final double value) throws KNXFormatException
	{
		final short[] buf = new short[2];
		toDPT(value, buf, 0);
		data = buf;
	}

	/**
	 * Returns the first translation item as signed value.
	 * <p>
	 * A value of DPT {@link #DptDeltaTime10} or {@link #DptDeltaTime100} is returned in unit
	 * millisecond, i.e., a KNX DptDeltaTime10 data value is multiplied with 10,
	 * DptDeltaTime100 with 100.<br>
	 * On any other DPT the value is returned according to its unit.
	 *
	 * @return numeric value
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 */
	@Override
	public final double getNumericValue()
	{
		return fromDPT(0);
	}

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
	 * This method is for DPTs dealing with delta time, in particular
	 * {@link #DptDeltaTime}, {@link #DptDeltaTime10}, {@link #DptDeltaTime100}
	 * {@link #DptDeltaTimeSec}, {@link #DptDeltaTimeMin} and
	 * {@link #DptDeltaTimeHours}. The milliseconds are converted to the unit of the
	 * set DPT, with the result rounded to the nearest representable value (with 0.5
	 * rounded up).<br>
	 * On any other DPT, the input is treated equal to {@link #setValue(int)}.
	 *
	 * @param milliseconds the value in milliseconds
	 * @throws KNXFormatException on milliseconds out of range for DPT
	 */
	public final void setDeltaTime(final long milliseconds) throws KNXFormatException {
		data = toDPT(milliseconds);
	}

	@Override
	public final Map<String, DPT> getSubTypes() {
		return types;
	}

	/**
	 * @return the subtypes of the 2-byte signed translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic() {
		return types;
	}

	private double fromDPT(final int index) {
		final int v = (short) ((data[2 * index] << 8) | data[2 * index + 1]);
		if (dpt.equals(DptDeltaTime10))
			return v * 10;
		else if (dpt.equals(DptDeltaTime100))
			return v * 100;
		else if (dpt.equals(DptPercent))
			return v / 100d;
		return v;
	}

	private String makeString(final int index) {
		return appendUnit(formatter.format(fromDPT(index)));
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		try {
			final String s = removeUnit(value);
			double v;
			if (dpt.equals(DptPercent))
				v = formatter.parse(s).doubleValue();
			else {
				try {
					v = Integer.decode(s).intValue();
				}
				catch (final NumberFormatException e) {
					v = formatter.parse(s).longValue();
				}
			}
			toDPT(v, dst, index);
		}
		catch (NumberFormatException | ParseException e) {
			throw newException("wrong value format", value);
		}
	}

	private short[] toDPT(final long ms) throws KNXFormatException {
		double v = ms;
		if (dpt.equals(DptDeltaTimeSec))
			v = Math.round(ms / 1000.0);
		else if (dpt.equals(DptDeltaTimeMin))
			v = Math.round(ms / 1000.0 / 60.0);
		else if (dpt.equals(DptDeltaTimeHours))
			v = Math.round(ms / 1000.0 / 60.0 / 60.0);
		final short[] buf = new short[2];
		toDPT(v, buf, 0);
		return buf;
	}

	private void toDPT(final double value, final short[] dst, final int index) throws KNXFormatException {
		if (value < min || value > max)
			throw newException("translation error, input value out of range [" + dpt.getLowerValue() + ".."
					+ dpt.getUpperValue() + "]", Double.toString(value));
		final int v;
		if (dpt.equals(DptDeltaTime10))
			v = (int) Math.round(value / 10);
		else if (dpt.equals(DptDeltaTime100))
			v = (int) Math.round(value / 100);
		else if (dpt.equals(DptPercent))
			v = (int) Math.round(value * 100);
		else
			v = (int) value;
		dst[2 * index] = ubyte(v >> 8);
		dst[2 * index + 1] = ubyte(v);
	}

	private double getLimit(final String limit) throws KNXFormatException {
		try {
			final double d = Double.parseDouble(limit);
			final double lower = dpt.equals(DptDeltaTime10) ? -327680L
					: dpt.equals(DptDeltaTime100) ? -3276800L : -32768;
			final double upper = dpt.equals(DptDeltaTime10) ? 327670L : dpt.equals(DptDeltaTime100) ? 3276700L : 32767;
			if (d >= lower && d <= upper)
				return d;
		}
		catch (final NumberFormatException e) {}
		throw newException("limit not in valid DPT range", limit);
	}
}

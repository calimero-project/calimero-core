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
 * Translator for KNX DPTs with main number 9, type <b>2-byte float</b>.
 * <p>
 * The KNX data type width is 2 bytes.<br>
 * This type is a two byte floating format with a maximum usable range of -671088.64 to
 * +670760.96. DPTs adjust the usable range to reasonable limits for its values, the
 * translator will check and enforce those DPT specific limits in all methods working with
 * java values (e.g. {@link #setValue(double)}). Data methods for KNX data (e.g.
 * {@link #setData(byte[])} accept all data within the maximum usable range.<br>
 * In value methods expecting a string type, the value is a floating type representation.
 * <p>
 * The default return value after creation is <code>0.0</code>.<br>
 * Note, that the floating type structure specified by this data type isn't really
 * precise, especially for bigger floating numbers, so you have to expect certain rounding
 * deviations.
 *
 * @author B. Malinowsky
 */
public class DPTXlator2ByteFloat extends DPTXlator
{
	/**
	 * DPT ID 9.001, Temperature; values from <b>-273</b> to <b>+670760</b> \u00b0C.
	 */
	public static final DPT DPT_TEMPERATURE =
		new DPT("9.001", "Temperature", "-273", "+670760", "\u00b0C");

	/**
	 * DPT ID 9.002, Temperature difference; value range <b>+/-670760</b> K.
	 */
	public static final DPT DPT_TEMPERATURE_DIFFERENCE =
		new DPT("9.002", "Temperature difference", "-670760", "+670760", "K");

	/**
	 * DPT ID 9.003, Temperature gradient; value range <b>+/-670760</b> K/h.
	 */
	public static final DPT DPT_TEMPERATURE_GRADIENT =
		new DPT("9.003", "Temperature gradient", "-670760", "+670760", "K/h");

	/**
	 * DPT ID 9.004, Intensity of light (Lux); values from <b>0</b> to <b>+670760</b> lx. Note, the
	 * unit of measure symbol used here is "lx", and not "Lux" as originally proposed for this DPT.
	 */
	public static final DPT DPT_INTENSITY_OF_LIGHT =
		new DPT("9.004", "Light intensity", "0", "+670760", "lx");

	/**
	 * DPT ID 9.005, Wind speed; values from <b>0</b> to <b>+670760</b> m/s.
	 */
	public static final DPT DPT_WIND_SPEED = new DPT("9.005", "Wind speed", "0", "+670760", "m/s");

	/**
	 * DPT ID 9.006, Air pressure; values from <b>0</b> to <b>+670760</b> Pa.
	 */
	public static final DPT DPT_AIR_PRESSURE = new DPT("9.006", "Air pressure", "0", "+670760",
			"Pa");

	/**
	 * DPT ID 9.007, Humidity; values from <b>0</b> to <b>+670760</b> %.
	 */
	public static final DPT DPT_HUMIDITY = new DPT("9.007", "Humidity", "0", "+670760", "%");

	/**
	 * DPT ID 9.008, Air quality; values from <b>0</b> to <b>+670760</b> ppm.
	 */
	public static final DPT DPT_AIRQUALITY = new DPT("9.008", "Air quality", "0", "+670760", "ppm");

	/**
	 * DPT ID 9.009, Air flow; value range <b>+/-670760</b> m&sup3;/h; resolution 0.01 m&sup3;/h.
	 */
	public static final DPT DPT_AIR_FLOW = new DPT("9.009", "Air flow", "-670760", "+670760",
			"m\u00b3/h");

	/**
	 * DPT ID 9.010, Time difference in seconds; value range <b>+/-670760</b> s.
	 */
	public static final DPT DPT_TIME_DIFFERENCE1 =
		new DPT("9.010", "Time difference 1", "-670760", "+670760", "s");

	/**
	 * DPT ID 9.011, Time difference in milliseconds; value range <b>+/-670760</b> ms.
	 */
	public static final DPT DPT_TIME_DIFFERENCE2 =
		new DPT("9.011", "Time difference 2", "-670760", "+670760", "ms");

	/**
	 * DPT ID 9.020, Voltage; value range <b>+/-670760</b> mV.
	 */
	public static final DPT DPT_VOLTAGE = new DPT("9.020", "Voltage", "-670760", "+670760", "mV");

	/**
	 * DPT ID 9.021, Electrical current; value range <b>+/-670760</b> mA.
	 */
	public static final DPT DPT_ELECTRICAL_CURRENT =
		new DPT("9.021", "Electrical current", "-670760", "+670760", "mA");

	/**
	 * DPT ID 9.022, Power density; value range <b>+/-670760</b> W/m<sup>2</sup>.
	 */
	public static final DPT DPT_POWERDENSITY =
		new DPT("9.022", "Power density", "-670760", "+670760", "W/m\u00b2");

	/**
	 * DPT ID 9.023, Kelvin/percent; value range <b>+/-670760</b> K/%.
	 */
	public static final DPT DPT_KELVIN_PER_PERCENT =
		new DPT("9.023", "Kelvin/percent", "-670760", "+670760", "K/%");

	/**
	 * DPT ID 9.024, Power; value range <b>+/-670760</b> kW.
	 */
	public static final DPT DPT_POWER = new DPT("9.024", "Power", "-670760", "+670760", "kW");

	/**
	 * DPT ID 9.025, Volume flow in liter/hour; value range <b>+/-670760</b> l/h, resolution 0.01.
	 */
	public static final DPT DPT_VOLUME_FLOW = new DPT("9.025", "Volume flow", "-670760", "+670760",
			"l/h");

	/**
	 * DPT ID 9.026, Rain amount in liters per square meter; values from <b>-671088.64</b> to
	 * <b>670760.96</b> l/m<sup>2</sup>, resolution 0.01.
	 */
	public static final DPT DPT_RAIN_AMOUNT = new DPT("9.026", "Rain amount", "-671088.64",
			"670760.96", "l/m\u00b2");

	/**
	 * DPT ID 9.027, Temperature in Degree Fahrenheit; values from <b>+/-459.6</b> to
	 * <b>670760.96</b> \u00b0F, resolution 0.01.
	 */
	public static final DPT DPT_TEMP_F = new DPT("9.027", "Temperature", "-459.6", "670760.96",
			"\u00b0F");

	/**
	 * DPT ID 9.028, Wind speed in km/h; values from <b>0</b> to <b>670760.96</b> km/h, resolution
	 * 0.01.
	 */
	public static final DPT DPT_WIND_SPEED_KMH = new DPT("9.028", "Wind speed", "0", "670760.96",
			"km/h");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator2ByteFloat.class);

	private final double min;
	private final double max;

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator2ByteFloat(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for <code>dptID</code>.
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) DPT
	 */
	public DPTXlator2ByteFloat(final String dptID) throws KNXFormatException
	{
		super(2);
		setTypeID(types, dptID);
		min = getLimit(dpt.getLowerValue());
		max = getLimit(dpt.getUpperValue());
		data = new short[2];
	}

	/**
	 * Sets the translation value from a double.
	 * <p>
	 * If succeeded, any other items in the translator are discarded.
	 *
	 * @param value the double value
	 * @throws KNXFormatException if <code>value</code>doesn't fit into KNX data type
	 */
	public void setValue(final double value) throws KNXFormatException
	{
		final short[] buf = new short[2];
		toDPT(value, buf, 0);
		data = buf;
	}

	/**
	 * Returns the first translation item formatted as double.
	 *
	 * @return numeric value as double
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 */
	@Override
	public final double getNumericValue()
	{
		return fromDPT(0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] buf = new String[data.length / 2];
		for (int i = 0; i < buf.length; ++i)
			buf[i] = makeString(i);
		return buf;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
	@Override
	public Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 2-byte float translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String makeString(final int index)
	{
		return appendUnit(String.valueOf(fromDPT(index)));
	}

	private double fromDPT(final int index)
	{
		final int i = 2 * index;
		// DPT bits high byte: MEEEEMMM, low byte: MMMMMMMM
		// left align all mantissa bits
		int v = ((data[i] & 0x80) << 24) | ((data[i] & 0x7) << 28) | (data[i + 1] << 20);
		// normalize
		v >>= 20;
		final int exp = (data[i] & 0x78) >> 3;
		return (1 << exp) * v * 0.01;
	}

	private void toDPT(final double value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (value < min || value > max)
			throw newException("translation error, value out of range [" + dpt.getLowerValue()
					+ ".." + dpt.getUpperValue() + "]", Double.toString(value));
		// encoding: value = (0.01*M)*2^E
		double v = value * 100.0f;
		int e = 0;
		for (; v < -2048.0f; v /= 2)
			e++;
		for (; v > 2047.0f; v /= 2)
			e++;
		final int m = (int) Math.round(v) & 0x7FF;
		short msb = (short) (e << 3 | m >> 8);
		if (value < 0.0)
			msb |= 0x80;
		dst[2 * index] = msb;
		dst[2 * index + 1] = ubyte(m);
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException
	{
		try {
			toDPT(Double.parseDouble(removeUnit(value)), dst, index);
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value, e);
		}
	}

	private static final double negLimit = -671088.64d;
	private static final double posLimit = 670760.96d;

	private double getLimit(final String limit) throws KNXFormatException
	{
		try {
			final double d = Double.parseDouble(limit);
			if (d >= negLimit && d <= posLimit)
				return d;
		}
		catch (final NumberFormatException e) {}
		throw newException("limit in valid DPT range", limit);
	}
}

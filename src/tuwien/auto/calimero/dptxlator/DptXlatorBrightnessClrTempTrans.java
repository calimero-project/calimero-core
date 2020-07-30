/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2020 B. Malinowsky

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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 249, type <b>brightness, color temperature, transition</b>. The KNX data
 * type width is 6 bytes. The default return value after creation is the value with all parts not valid
 * (<code>- % - K - s</code>).
 * <p>
 * In value methods expecting string items, brightness and transition are parsed as floating point value.
 */
public class DptXlatorBrightnessClrTempTrans extends DPTXlator {
	public static final String Description = "brightness & color temperature transition";

	/**
	 * DPT ID 249.600, brightness, color temperature, transition; values from <b>0 % 0 K 0 s</b> to <b>100 % 65535
	 * K 6553.5 s</b>.
	 */
	public static final DPT DptBrightnessClrTempTrans = new DPT("249.600", "brightness & color temperature transition",
			"0 0 0 0", "100 100 100 100", "%");

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>();
		types.put(DptBrightnessClrTempTrans.getID(), DptBrightnessClrTempTrans);
	}

	private final NumberFormat formatter = NumberFormat.getNumberInstance();


	/**
	 * Creates a translator for {@link #DptBrightnessClrTempTrans}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorBrightnessClrTempTrans() throws KNXFormatException {
		this(DptBrightnessClrTempTrans);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorBrightnessClrTempTrans(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlatorBrightnessClrTempTrans(final String dptId) throws KNXFormatException {
		super(6);
		setTypeID(types, dptId);
		data = new short[6];
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(1);
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param brightness absolute brightness, <code>0 &le; brightness &le; 100 %</code>
	 * @param clrTemperature absolute color temperature, <code>0 &le; temperature &le; 65535</code>
	 * @param transition duration of transition <code>0 &le; transition &le; 6553500 ms</code> (100 ms resolution)
	 */
	public final void setValue(final double brightness, final int clrTemperature, final Duration transition) {
		data = toDpt(brightness, clrTemperature, transition);
	}

	@Override
	public String[] getAllValues() {
		final int items = getItems();
		final String[] s = new String[items];
		for (int i = 0; i < items; ++i)
			s[i] = fromDpt(i);
		return s;
	}

	@Override
	public final Map<String, DPT> getSubTypes() {
		return types;
	}

	/**
	 * @return the subtypes of this translator
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic() {
		return types;
	}

	private String fromDpt(final int index) {
		int offset = index * 6;
		final double time = ((data[offset++] << 8) | data[offset++]) / 10d;
		final int temperature = (data[offset++] << 8) | data[offset++];
		final double brightness = data[offset++] * 100d / 255;
		final int valid = data[offset++];

		final StringBuilder sb = new StringBuilder();
		sb.append((valid & 1) == 1 ? formatter.format(brightness) : "-").append(appendUnit ? " % " : " ");
		sb.append((valid & 2) == 2 ? temperature : "-").append(appendUnit ? " K " : " ");
		sb.append((valid & 4) == 4 ? formatter.format(time) : "-").append(appendUnit ? " s" : "");
		return sb.toString();
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		final String[] fields = value.split(" ");
		// either all fields have unit suffix, or none
		if (fields.length != 6 && fields.length != 3)
			throw newException("unsupported format for brightness, color temperature, transition", value);

		// set increment, to skip units if in fields
		final int inc = fields.length / 3;

		int valid = 0;

		String field = fields[0];
		boolean invalid = field.equals("-");
		final double brightness = invalid ? 0 : parse(field);
		valid |= invalid ? 0 : 1;

		field = fields[inc];
		invalid = field.equals("-");
		final int temperature = invalid ? 0 : parseInt(field);
		valid |= invalid ? 0 : 2;

		field = fields[2 * inc];
		invalid = field.equals("-");
		final double time = invalid ? 0 : parse(field);
		valid |= invalid ? 0 : 4;

		try {
			rangeCheck(brightness, temperature, Duration.ofMillis((long) (time * 1000)));
		}
		catch (final KNXIllegalArgumentException e) {
			throw newException(e.getMessage(), value);
		}

		final int brightnessRaw = (int) Math.round(brightness * 255 / 100);
		final int timeRaw = (int) Math.round(time * 10);

		int offset = index * 6;
		dst[offset++] = (short) (timeRaw >> 8);
		dst[offset++] = ubyte(timeRaw);
		dst[offset++] = (short) (temperature >> 8);
		dst[offset++] = ubyte(temperature);
		dst[offset++] = ubyte(brightnessRaw);
		dst[offset] = (short) valid;
	}

	private short[] toDpt(final double brightness, final int temperature, final Duration timePeriod) {
		rangeCheck(brightness, temperature, timePeriod);

		final int valid = 0b111;
		final int time = (int) (timePeriod.toMillis() / 100);
		return new short[] { (short) (time >> 8), ubyte(time), (short) (temperature >> 8), ubyte(temperature),
			(short) Math.round(brightness * 255 / 100), valid };
	}

	private void rangeCheck(final double brightness, final int temperature, final Duration timePeriod) {
		if (brightness < 0 || brightness > 100)
			throw new KNXIllegalArgumentException("absolute brightness " + brightness + " out of range [0..100]");
		if (temperature < 0 || temperature > 65535)
			throw new KNXIllegalArgumentException("absolute color temperature " + temperature + " out of range [0..65535]");
		if (timePeriod.isNegative() || timePeriod.toMillis() > 65535 * 100)
			throw new KNXIllegalArgumentException("time period " + timePeriod + " out of range [0..6553500]");
	}

	private double parse(final String value) throws KNXFormatException {
		try {
			return formatter.parse(value).doubleValue();
//			return Double.parseDouble(value.replace(',', '.'));
		}
		catch (final ParseException e) {
			throw newException("wrong value format", value);
		}
	}

	private int parseInt(final String value) throws KNXFormatException {
		try {
			return Integer.parseInt(value);
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}
}

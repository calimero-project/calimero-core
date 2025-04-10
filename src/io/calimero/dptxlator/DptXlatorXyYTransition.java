/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2020, 2024 B. Malinowsky

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

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 243, type <b>color transition xyY</b>. The KNX data
 * type width is 8 bytes. The default return value after creation is a time period of 0 milliseconds with invalid
 * coordinates and brightness ({@code 0 ms}).
 */
public class DptXlatorXyYTransition extends DPTXlator {
	public static final String Description = "color transition xyY";

	/**
	 * DPT ID 243.600, color transition xyY; values from <b>(0, 0) 0 % 0 ms</b> to <b>(1, 1) 100 % 6553500 ms</b>.
	 */
	public static final DPT DptXyYTransition = new DPT("243.600", "color transition xyY",
			"(0, 0) 0 % 0 ms", "(1, 1) 100 % 6553500 ms");

	private static final Map<String, DPT> types = loadDatapointTypes(DptXlatorXyYTransition.class);

	private final NumberFormat formatter = NumberFormat.getNumberInstance();

	private final DPTXlator2ByteUnsigned t = new DPTXlator2ByteUnsigned(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_100);
	private final DPTXlator8BitUnsigned scaled = new DPTXlator8BitUnsigned(DPTXlator8BitUnsigned.DPT_SCALING);

	/**
	 * Creates a translator for {@link #DptXyYTransition}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorXyYTransition() throws KNXFormatException {
		this(DptXyYTransition);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorXyYTransition(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) {@code dptId}
	 */
	public DptXlatorXyYTransition(final String dptId) throws KNXFormatException {
		super(8);
		setTypeID(types, dptId);
		data = new short[typeSize];
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(4);
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	public final Optional<Double> x() {
		final double x = fromDptDouble(0)[2];
		return x == -1 ? Optional.empty() : Optional.of(x);
	}

	public final Optional<Double> y() {
		final double y = fromDptDouble(0)[1];
		return y == -1 ? Optional.empty() : Optional.of(y);
	}

	public final Optional<Double> brightness() {
		final double b = fromDptDouble(0)[3];
		return b == -1 ? Optional.empty() : Optional.of(b);
	}

	public final Duration fadingTime() {
		return Duration.ofMillis((long) (fromDptDouble(0)[0] * 100));
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param x x coordinate of the color information, {@code 0 ≤ x ≤ 1}
	 * @param y y coordinate of the color information, {@code 0 ≤ y ≤ 1}
	 * @param brightness color brightness in percent, {@code 0 ≤ brightness ≤ 100 %}
	 * @param fadingTime fading time in 100 ms steps, {@code 0 ≤ duration ≤ 6553.5 s}
	 */
	public final void setValue(final double x, final double y, final double brightness, final Duration fadingTime) {
		toDpt(x, y, brightness, fadingTime);
	}

	public final void setChromaticity(final double x, final double y, final Duration fadingTime) {
		setCoordinates(x, y);
		setFadingTime(fadingTime);
	}

	public final void setBrightness(final double brightness, final Duration fadingTime) {
		setBrightness(brightness);
		setFadingTime(fadingTime);
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
	 * {@return the subtypes of this translator}
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic() {
		return types;
	}

	private String fromDpt(final int index) {
		final int offset = index * typeSize;
		final var joiner = new StringJoiner(" ");

		final int valid = data[offset + 7];
		if ((valid & 0x2) == 2) {
			final double y = ushort(offset + 2) / 65_535d;
			final double x = ushort(offset + 4) / 65_535d;
			joiner.add("(" + formatter.format(x) + ", " + formatter.format(y) + ")");
		}
		if ((valid & 0x1) == 1) {
			final int brightness = data[offset + 6];
			scaled.setValueUnscaled(brightness);
			scaled.setAppendUnit(appendUnit);
			joiner.add(scaled.getValue());
		}

		final int dur = ushort(offset);
		try {
			t.setTimePeriod(dur * 100L);
			t.setAppendUnit(appendUnit);
			joiner.add(t.getValue());
		}
		catch (final KNXFormatException e) {}

		return joiner.toString();
	}

	private int ushort(final int offset) { return  (data[offset] << 8) | data[offset + 1]; }

	private double[] fromDptDouble(final int index) {
		int offset = index * typeSize;

		final double time = ((data[offset++] << 8) | data[offset++]);
		final double y = ((data[offset++] << 8) | data[offset++]) / 65_535d;
		final double x = ((data[offset++] << 8) | data[offset++]) / 65_535d;
		final double brightness = data[offset++] * 100d / 255;

		final int valid = data[offset];
		final boolean clrValid = (valid & 2) == 2;
		final boolean brightnessValid = (valid & 1) == 1;

		return new double[] { time, clrValid ? y : -1, clrValid ? x : -1, brightnessValid ? brightness : -1 };
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		if (value.isEmpty())
			return;
		final String[] fields = value.split("[ ]+", 0);
		// max fields defined by either "(x, y) Y % fade ms"
		if (fields.length > 6 || fields.length < 1)
			throw newException("unsupported format for color transition xyY", value);

		int i = 0;
		int valid = 0;
		final String f = fields[0];

		double x = 0;
		double y = 0;
		int brightness = 0;
		short brightnessRaw = 0;

		if (f.startsWith("(")) {
			final int sep = f.endsWith(",") ? 1 : 0;
			x = parse(f.substring(1, f.length() - sep));
			if (fields[++i].isEmpty())
				i++;
			y = parse(fields[i].substring(0, fields[i].length() - 1));
			valid = 2;
			i++;
		}
		if (fields.length > i + 1 && "%".equals(fields[i + 1])) {
			scaled.setValue(fields[i] + " %");
			brightness = scaled.getValueUnsigned();
			brightnessRaw = scaled.getValueUnscaled();
			i += 2;
			valid |= 1;
		}

		try {
			rangeCheck(x, y, brightness);
		}
		catch (final KNXIllegalArgumentException e) {
			throw newException(e.getMessage(), value);
		}
		t.setValue(fields[i]);
		i++;
		if (fields.length > i && "ms".equals(fields[i]))
			i++;

		if (i < fields.length)
			throw newException("value contains excessive components", fields[i]);

		final int ms = t.getValueUnsigned() / 100;
		final int xRaw = (int) Math.round(x * 65_535);
		final int yRaw = (int) Math.round(y * 65_535);

		int offset = index * typeSize;
		dst[offset++] = (short) (ms >> 8);
		dst[offset++] = (short) (ms & 0xff);
		dst[offset++] = (short) (yRaw >> 8);
		dst[offset++] = (short) (yRaw & 0xff);
		dst[offset++] = (short) (xRaw >> 8);
		dst[offset++] = (short) (xRaw & 0xff);
		dst[offset++] = brightnessRaw;
		dst[offset] = (short) valid;
	}

	private void toDpt(final double x, final double y, final double brightness, final Duration fadingTime) {
		data = new short[8];
		setFadingTime(fadingTime);
		setCoordinates(x, y);
		setBrightness(brightness);
	}

	private void setFadingTime(final Duration fadingTime) {
		try {
			t.setTimePeriod(fadingTime.toMillis());
		}
		catch (final KNXFormatException e) {
			throw new KNXIllegalArgumentException(e.getMessage());
		}
		final var d = t.getData();

		data[0] = d[0];
		data[1] = d[1];
	}

	private void setCoordinates(final double x, final double y) {
		final int xUnscaled = unscaled(x);
		final int yUnscaled = unscaled(y);

		data[2] = ubyte(yUnscaled >> 8);
		data[3] = ubyte(yUnscaled);
		data[4] = ubyte(xUnscaled >> 8);
		data[5] = ubyte(xUnscaled);
		data[7] |= 0b10;
	}

	private void setBrightness(final double brightness) {
		try {
			scaled.setValue(brightness);
		}
		catch (final KNXFormatException e) {
			throw new KNXIllegalArgumentException(e.getMessage());
		}
		final var bright = scaled.getValueUnscaled();
		data[6] = ubyte(bright);
		data[7] |= 0b01;
	}

	private static int unscaled(final double value) {
		if (value < 0 || value > 1)
			throw new KNXIllegalArgumentException("coordinate " + value + " out of range [0..1]");
		return (int) (value * 65_535);
	}

	private static void rangeCheck(final double x, final double y, final int brightness) {
		if (x < 0 || x > 1)
			throw new KNXIllegalArgumentException("x " + x + " out of range [0..1]");
		if (y < 0 || y > 1)
			throw new KNXIllegalArgumentException("y " + y + " out of range [0..1]");
		if (brightness < 0 || brightness > 100)
			throw new KNXIllegalArgumentException("brightness " + brightness + " out of range [0..100]");
	}

	private double parse(final String value) throws KNXFormatException {
		try {
			return Double.parseDouble(value.replace(',', '.'));
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}
}

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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 242, type <b>color xyY</b>. The KNX data type width is 6 bytes. The default
 * return value after creation is the value with all components invalid (<code>""</code>).
 * <p>
 * In value methods expecting string items, x, y, and brightness are parsed as floating point value.
 */
public class DptXlatorXyY extends DPTXlator {
	public static final String Description = "xyY color";

	/**
	 * DPT ID 242.600, color xyY; values from <b>(0 0) 0 %</b> to <b>(1 1) 100 %</b>.
	 */
	public static final DPT DptXyY = new DPT("242.600", "color xyY", "(0 0) 0", "(1 1) 100", "%");

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>();
		types.put(DptXyY.getID(), DptXyY);
	}

	private final NumberFormat formatter = NumberFormat.getNumberInstance();


	/**
	 * Creates a translator for {@link DptXyY}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorXyY() throws KNXFormatException {
		this(DptXyY);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorXyY(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlatorXyY(final String dptId) throws KNXFormatException {
		super(6);
		setTypeID(types, dptId);
		data = new short[6];
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(4);
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	public final Optional<Double> x() {
		final double x = fromDptDouble(0)[0];
		return x == -1 ? Optional.empty() : Optional.of(x);
	}

	public final Optional<Double> y() {
		final double y = fromDptDouble(0)[1];
		return y == -1 ? Optional.empty() : Optional.of(y);
	}

	public final Optional<Double> brightness() {
		final double b = fromDptDouble(0)[2];
		return b == -1 ? Optional.empty() : Optional.of(b);
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param x x coordinate, <code>0 &le; x &le; 1</code>
	 * @param y y coordinate, <code>0 &le; y &le; 1</code>
	 * @param brightness absolute brightness <code>0 &le; brightness &le; 100 %</code>
	 */
	public final void setValue(final double x, final double y, final double brightness) {
		data = toDpt(x, y, brightness);
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
		final double[] values = fromDptDouble(index);

		final StringBuilder sb = new StringBuilder();
		if (values[0] != -1) {
			sb.append("(");
			sb.append(formatter.format(values[0])).append(" ");
			sb.append(formatter.format(values[1]));
			sb.append(")");
		}
		if (values[2] != -1) {
			if (sb.length() > 0)
				sb.append(" ");
			sb.append(new DecimalFormat("##.#").format(values[2])).append(appendUnit ? " %" : "");
		}
		return sb.toString();
	}

	private double[] fromDptDouble(final int index) {
		int offset = index * 6;

		final double x = ((data[offset++] << 8) | data[offset++]) / 65_535d;
		final double y = ((data[offset++] << 8) | data[offset++]) / 65_535d;
		final double brightness = data[offset++] * 100d / 255; // ??? scaling might be wrong

		final int valid = data[offset++];
		final boolean clrValid = (valid & 2) == 2;
		final boolean brightnessValid = (valid & 1) == 1;

		return new double[] { clrValid ? x : -1, clrValid ? y : -1, brightnessValid ? brightness : -1 };
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		final String[] fields = value.split(" ");
		int i = 0;

		// max fields defined by either "(x, y) Y" or "(x, y) Y %"
		if (fields.length > 3 && !(fields.length == 4 && "%".equals(fields[3])))
			throw newException("unsupported format for color (x y) Y", value);

		int valid = 0;
		double x = 0;
		double y = 0;
		double brightness = 0;

		final boolean clrValid = fields.length >= 2 && !"%".equals(fields[1]) || fields[0].startsWith("(");
		if (clrValid) {
			valid |= 2;

			String field = fields[i++];
			if (field.charAt(field.length() - 1) == ',')
				field = field.substring(0, field.length() - 1);
			x = parse(field.replace("(", ""));

			field = fields[i++];
			y = parse(field.replace(")", ""));
		}
		final boolean brightnessValid = fields.length > 2 || "%".equals(fields[fields.length - 1]);
		if (brightnessValid) {
			valid |= 1;

			final String field = fields[i++];
			brightness = parse(field);
		}
		try {
			rangeCheck(x, y, brightness);
		}
		catch (final KNXIllegalArgumentException e) {
			throw newException(e.getMessage(), value);
		}

		final int xRaw = (int) Math.round(x * 65_535);
		final int yRaw = (int) Math.round(y * 65_535);
		final int brightnessRaw = (int) Math.round(brightness * 255 / 100);

		int offset = index * 6;
		dst[offset++] = (short) (xRaw >> 8);
		dst[offset++] = ubyte(xRaw);
		dst[offset++] = (short) (yRaw >> 8);
		dst[offset++] = ubyte(yRaw);
		dst[offset++] = ubyte(brightnessRaw);
		dst[offset++] = (short) valid;
	}

	private short[] toDpt(final double x, final double y, final double brightness) {
		rangeCheck(x, y, brightness);

		final int valid = 0b11;
		final int xRaw = (int) Math.round(x * 65_535);
		final int yRaw = (int) Math.round(y * 65_535);

		return new short[] { (short) (xRaw >> 8), ubyte(xRaw), (short) (yRaw >> 8), ubyte(yRaw),
			(short) Math.round(brightness * 255 / 100), valid };
	}

	private void rangeCheck(final double x, final double y, final double brightness) {
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

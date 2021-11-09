/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2020, 2021 B. Malinowsky

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
import java.util.Optional;

import tuwien.auto.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 252, type <b>relative control RGBW</b>. The KNX data
 * type width is 5 bytes. The default return value after creation is the value with all parts not valid
 * (<code>- - - -</code>).
 */
public class DptXlatorRelativeControlRgbw extends DPTXlator {
	public static final String Description = "relative control RGBW";

	/**
	 * DPT ID 252.600, relative control RGBW; values from <b>R decrease 0 G decrease 0 G decrease 0 W decrease 0</b> to
	 * <b>R increase 7 G increase 7 B increase 7 W increase 7</b>.
	 */
	public static final DPT DptRelativeControlRgbw = new DPT("252.600", "relative control RGBW",
			"R decrease 0 G decrease 0 B decrease 0 W decrease 0", "R increase 7 G increase 7 B increase 7 W increase 7");

	private static final Map<String, DPT> types = loadDatapointTypes(DptXlatorRelativeControlRgbw.class);

	private static final String Red = "R";
	private static final String Green = "G";
	private static final String Blue = "B";
	private static final String White = "W";

	private enum Component { Red, Green, Blue, White }

	private final DPTXlator3BitControlled t = new DPTXlator3BitControlled(DPTXlator3BitControlled.DPT_CONTROL_DIMMING);

	/**
	 * Creates a translator for {@link #DptRelativeControlRgbw}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorRelativeControlRgbw() throws KNXFormatException {
		this(DptRelativeControlRgbw);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorRelativeControlRgbw(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlatorRelativeControlRgbw(final String dptId) throws KNXFormatException {
		super(5);
		setTypeID(types, dptId);
		data = new short[typeSize];
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	public final Optional<StepControl> red() {
		return component(Component.Red);
	}

	public final Optional<StepControl> green() {
		return component(Component.Green);
	}

	public final Optional<StepControl> blue() {
		return component(Component.Blue);
	}

	public final Optional<StepControl> white() {
		return component(Component.White);
	}

	private Optional<StepControl> component(final Component what) {
		int offset;
		int validBit;
		if (what == Component.Red) {
			offset = 0;
			validBit = 8;
		}
		else if (what == Component.Green) {
			offset = 1;
			validBit = 4;
		}
		else if (what == Component.Blue) {
			offset = 2;
			validBit = 2;
		}
		else if (what == Component.White) {
			offset = 3;
			validBit = 1;
		}
		else
			throw new Error("illegal control value");

		if ((data[4] & validBit) == 0)
			return Optional.empty();
		return Optional.of(StepControl.from(data[offset]));
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param red step control for red
	 * @param green step control for green
	 * @param blue step control for blue
	 * @param white step control for white
	 */
	public final void setValue(final StepControl red, final StepControl green, final StepControl blue,
			final StepControl white) {
		data = toDpt(red, green, blue, white);
	}

	public final void setRed(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 0;
		final int validBit = 8;
		data[offset] = d;
		data[4] |= validBit;
	}

	public final void setGreen(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 1;
		final int validBit = 4;
		data[offset] = d;
		data[4] |= validBit;
	}

	public final void setBlue(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 2;
		final int validBit = 2;
		data[offset] = d;
		data[4] |= validBit;
	}

	public final void setWhite(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 3;
		final int validBit = 1;
		data[offset] = d;
		data[4] |= validBit;
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
		final int offset = index * typeSize;

		final int valid = data[offset + typeSize - 1];
		if (valid == 0)
			return Red + " - " + Green + " - " + Blue + " - " + White + " -";

		t.setAppendUnit(appendUnit);
		String s = component(offset, 3);
		s += component(offset, 2);
		s += component(offset, 1);
		s += component(offset, 0);
		return s.trim();
	}

	private String component(final int offset, final int component) {
		final String[] prefixes = { White, Blue, Green, Red};
		final int mask = data[offset + typeSize - 1];
		if (((mask >> component) & 1) == 1) {
			t.setData(new byte[] { (byte) data[offset + 3 - component] });
			return " " + prefixes[component] + " " + t.getValue();
		}
		return "";
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		if (value.isEmpty())
			return;
		final String[] fields = value.split(" ", 0);
		// 1 to 4 control with stepcodes
		if (fields.length > 16 || fields.length < 4)
			throw newException("unsupported format for relative control RGBW", value);

		int valid = 0;
		final int offset = index * typeSize;
		int i = 0;

		final String[] components = { Red, Green, Blue, White };
		int maskBit = 8;
		int clrOffset = 0;
		for (final var c : components) {
			if (c.equals(fields[i])) {
				final int clr = component(fields, ++i);
				dst[offset + clrOffset] = ubyte(clr);
				final boolean invalid = clr == -1;
				valid |= invalid ? 0 : maskBit;

				i += invalid || clr == 0 ? 1 : 2;
				if (i < fields.length && "steps".equals(fields[i]))
					i++;
			}
			if (i >= fields.length)
				break;
			maskBit >>= 1;
			clrOffset++;
		}

		if (i < fields.length)
			throw newException("value contains excessive components", fields[i]);
		dst[offset + typeSize - 1] = (short) valid;
	}

	private int component(final String[] fields, final int i) throws KNXFormatException {
		final String first = fields[i];
		if ("-".equals(first))
			return -1;
		if (fields.length > i + 1)
			t.setValue(first + " " + fields[i + 1]);
		else
			t.setValue(first);
		return t.getData()[0];
	}

	private short[] toDpt(final StepControl red, final StepControl green, final StepControl blue,
			final StepControl white) {
		t.setValue(red);
		final short r = ubyte(t.getData()[0]);
		t.setValue(green);
		final short g = ubyte(t.getData()[0]);
		t.setValue(blue);
		final short b = ubyte(t.getData()[0]);
		t.setValue(white);
		final short w = ubyte(t.getData()[0]);

		final int valid = 0b1111;
		return new short[] { r, g, b, w, valid };
	}
}

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
 * Translator for KNX DPTs with main number 253, type <b>relative control xyY</b>. The KNX data
 * type width is 4 bytes. The default return value after creation is the value with all parts not valid
 * (<code>x - y - Y -</code>).
 */
public class DptXlatorRelativeControlXyY extends DPTXlator {
	public static final String Description = "relative control xyY";

	/**
	 * DPT ID 253.600, relative control xyY; values from <b>x decrease 0 y decrease 0 Y decrease 0</b> to
	 * <b>x increase 7 y increase 7 Y increase 7</b>.
	 */
	public static final DPT DptRelativeControlXyY = new DPT("253.600", "relative control xyY",
			"x decrease 0 y decrease 0 Y decrease 0", "x increase 7 y increase 7 Y increase 7");

	private static final Map<String, DPT> types = loadDatapointTypes(DptXlatorRelativeControlXyY.class);

	private final DPTXlator3BitControlled t = new DPTXlator3BitControlled(DPTXlator3BitControlled.DPT_CONTROL_DIMMING);

	/**
	 * Creates a translator for {@link #DptRelativeControlXyY}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorRelativeControlXyY() throws KNXFormatException {
		this(DptRelativeControlXyY);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorRelativeControlXyY(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlatorRelativeControlXyY(final String dptId) throws KNXFormatException {
		super(4);
		setTypeID(types, dptId);
		data = new short[typeSize];
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	public final Optional<StepControl> x() {
		return component(x);
	}

	public final Optional<StepControl> y() {
		return component(y);
	}

	public final Optional<StepControl> brightness() {
		return component(Y);
	}

	// component id with shift for valid Bit field
	private static final int x = 2;
	private static final int y = 1;
	private static final int Y = 0;

	private Optional<StepControl> component(final int c) {
		final int validBit = 1 << c;
		if ((data[3] & validBit) == 0)
			return Optional.empty();

		final int offset = 2 - c;
		return Optional.of(StepControl.from(data[offset]));
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param x step control for x
	 * @param y step control for y
	 * @param brightness step control for brightness
	 */
	public final void setValue(final StepControl x, final StepControl y, final StepControl brightness) {
		data = toDpt(x, y, brightness);
	}

	/**
	 * Sets the step control for the x component of chromaticity.
	 *
	 * @param value step control of the x component
	 */
	public final void setX(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 0;
		final int validBit = 4;
		data[offset] = d;
		data[3] |= validBit;
	}

	/**
	 * Sets the step control for the y component of chromaticity.
	 *
	 * @param value step control of the y component
	 */
	public final void setY(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 1;
		final int validBit = 2;
		data[offset] = d;
		data[3] |= validBit;
	}

	/**
	 * Sets the step control for the Y component.
	 *
	 * @param value step control of the Y component
	 */
	public final void setBrightness(final StepControl value) {
		t.setValue(value);
		final short d = ubyte(t.getData()[0]);

		final int offset = 2;
		final int validBit = 1;
		data[offset] = d;
		data[3] |= validBit;
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
			return "x - y - Y -";

		t.setAppendUnit(appendUnit);
		String s = component(offset, 2);
		s += component(offset, 1);
		s += component(offset, 0);
		return s.trim();
	}

	private String component(final int offset, final int component) {
		final String[] prefixes = { "Y", "y", "x" };
		final int mask = data[offset + typeSize - 1];
		if (((mask >> component) & 1) == 1) {
			t.setData(new byte[] { (byte) data[offset + 2 - component] });
			return " " + prefixes[component] + " " + t.getValue();
		}
		return "";
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		if (value.isEmpty())
			return;
		final String[] fields = value.split(" ", 0);
		// 1 to 3 control with stepcodes
		if (fields.length > 12 || fields.length < 3)
			throw newException("unsupported format for relative control xyY", value);

		int valid = 0;
		final int offset = index * typeSize;
		int i = 0;

		final String[] components = { "x", "y", "Y" };
		int maskBit = 4;
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

	private short[] toDpt(final StepControl x, final StepControl y, final StepControl brightness) {
		t.setValue(x);
		final short x_ = ubyte(t.getData()[0]);
		t.setValue(y);
		final short y_ = ubyte(t.getData()[0]);
		t.setValue(brightness);
		final short br = ubyte(t.getData()[0]);

		final int valid = 0b111;
		return new short[] { x_, y_, br, valid };
	}
}

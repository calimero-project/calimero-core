/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2020 B. Malinowsky

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
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 254, type <b>relative control RGB</b>. The KNX data
 * type width is 3 bytes. The default return value after creation is the value with all color components stop fading
 * (<code>break break break</code>).
 */
public class DptXlatorRelativeControlRgb extends DPTXlator {
	public static final String Description = "relative control RGB";

	/**
	 * DPT ID 254.600, relative control RGB; values from <b>decrease 0 decrease 0 decrease 0</b> to <b>increase
	 * 7 increase 7 increase 7</b>.
	 */
	public static final DPT DptRelativeControlRgb = new DPT("254.600", "relative control RGB",
			"decrease 0 decrease 0 decrease 0", "increase 7 increase 7 increase 7");

	private static final Map<String, DPT> types = loadDatapointTypes(DptXlatorRelativeControlRgb.class);

	private static final String Red = "R";
	private static final String Green = "G";
	private static final String Blue = "B";

	private final DPTXlator3BitControlled t = new DPTXlator3BitControlled(DPTXlator3BitControlled.DPT_CONTROL_DIMMING);

	/**
	 * Creates a translator for {@link #DptRelativeControlRgb}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorRelativeControlRgb() throws KNXFormatException {
		this(DptRelativeControlRgb);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorRelativeControlRgb(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlatorRelativeControlRgb(final String dptId) throws KNXFormatException {
		super(3);
		setTypeID(types, dptId);
		data = new short[typeSize];
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param increaseRed increase or decrease value
	 * @param redStepcode stepcode, <code>0 &le; clrTempStepcode &le; 7</code>
	 * @param increaseGreen increase or decrease value
	 * @param greenStepcode stepcode, <code>0 &le; clrTempStepcode &le; 7</code>
	 * @param increaseBlue increase or decrease value
	 * @param blueStepcode stepcode, <code>0 &le; clrTempStepcode &le; 7</code>
	 */
	public final void setValue(final boolean increaseRed, final int redStepcode, final boolean increaseGreen,
			final int greenStepcode, final boolean increaseBlue, final int blueStepcode) {
		data = toDpt(increaseRed, redStepcode, increaseGreen, greenStepcode, increaseBlue, blueStepcode);
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

		t.setAppendUnit(appendUnit);
		String s = component(offset, 2);
		s += component(offset, 1);
		s += component(offset, 0);
		return s.trim();
	}

	private String component(final int offset, final int component) {
		final String[] prefixes = { Blue, Green, Red};
		t.setData(new byte[] { (byte) data[offset + 2 - component] });
		return " " + prefixes[component] + " " + t.getValue();
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		if (value.isEmpty())
			return;
		final String[] fields = value.split(" ", 0);
		// 3 control with stepcodes
		if (fields.length > 12 || fields.length < 3)
			throw newException("unsupported format for relative control RGB", value);

		final int offset = index * typeSize;
		int i = 0;

		final String[] components = { Red, Green, Blue };
		int clrOffset = 0;
		int colors = 0;
		for (final var c : components) {
			if (c.equals(fields[i]))
				++i;

			if ("break".equals(fields[i])) {
				dst[offset + clrOffset] = 0;
				i++;
			}
			else {
				final int clr = component(fields, i);
				dst[offset + clrOffset] = ubyte(clr);
				i += 2;
			}
			if (i < fields.length && "steps".equals(fields[i]))
				i++;
			colors++;
			if (i >= fields.length)
				break;
			clrOffset++;
		}
		if (colors < 3)
			throw newException("value contains not all color components", "missing " + (3 - colors));

		if (i < fields.length)
			throw newException("value contains excessive components", fields[i]);
	}

	private int component(final String[] fields, final int i) throws KNXFormatException {
		final String first = fields[i];
		if (fields.length > i + 1)
			t.setValue(first + " " + fields[i + 1]);
		else
			t.setValue(first);
		return t.getData()[0];
	}

	private short[] toDpt(final boolean increaseRed, final int redStepcode, final boolean increaseGreen,
			final int greenStepcode, final boolean increaseBlue, final int blueStepcode) {
		rangeCheck(redStepcode);
		rangeCheck(greenStepcode);
		rangeCheck(blueStepcode);

		t.setValue(increaseRed, redStepcode);
		final short red = ubyte(t.getData()[0]);
		t.setValue(increaseGreen, greenStepcode);
		final short green = ubyte(t.getData()[0]);
		t.setValue(increaseBlue, blueStepcode);
		final short blue = ubyte(t.getData()[0]);

		return new short[] { red, green, blue };
	}

	private void rangeCheck(final int clrStepcode) {
		if (clrStepcode < 0 || clrStepcode > 7)
			throw new KNXIllegalArgumentException("stepcode " + clrStepcode + " out of range [0..7]");
	}
}

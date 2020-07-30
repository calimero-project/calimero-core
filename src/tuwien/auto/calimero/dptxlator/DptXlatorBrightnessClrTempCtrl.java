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

import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 250, type <b>brightness &amp; color temperature control</b>. The KNX data
 * type width is 3 bytes. The default return value after creation is the value with all parts not valid
 * (<code>- -</code>).
 */
public class DptXlatorBrightnessClrTempCtrl extends DPTXlator {
	public static final String Description = "brightness & color temperature control";

	/**
	 * DPT ID 250.600, brightness &amp; color temperature control; values from <b>decrease 1 decrease 1</b> to <b>increase
	 * 7 increase 7</b>.
	 */
	public static final DPT DptBrightnessClrTempCtrl = new DPT("250.600", "brightness & color temperature control",
			"decrease 1 decrease 1", "increase 7 increase 7");

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>();
		types.put(DptBrightnessClrTempCtrl.getID(), DptBrightnessClrTempCtrl);
	}

	private static final String clrTempSuffix = "CT";
	private static final String brightnessSuffix = "BRT";

	private final DPTXlator3BitControlled t = new DPTXlator3BitControlled(DPTXlator3BitControlled.DPT_CONTROL_DIMMING);

	/**
	 * Creates a translator for {@link #DptBrightnessClrTempCtrl}.
	 *
	 * @throws KNXFormatException on not available DPT
	 */
	public DptXlatorBrightnessClrTempCtrl() throws KNXFormatException {
		this(DptBrightnessClrTempCtrl);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlatorBrightnessClrTempCtrl(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptId</code>
	 */
	public DptXlatorBrightnessClrTempCtrl(final String dptId) throws KNXFormatException {
		super(3);
		setTypeID(types, dptId);
		data = new short[3];
	}

	@Override
	public String getValue() {
		return fromDpt(0);
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 *
	 * @param increaseClrTemp increase or decrease value
	 * @param clrTempStepcode color temperature stepcode, <code>1 &le; clrTempStepcode &le; 7</code>
	 * @param increaseBrightness increase or decrease value
	 * @param brightnessStepcode brightness stepcode, <code>1 &le; brightnessStepcode &le; 7</code>
	 */
	public final void setValue(final boolean increaseClrTemp, final int clrTempStepcode, final boolean increaseBrightness,
			final int brightnessStepcode) {
		data = toDpt(increaseClrTemp, clrTempStepcode, increaseBrightness, brightnessStepcode);
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
		final int offset = index * 3;
		String s = "";

		t.setAppendUnit(appendUnit);
		final int valid = data[offset + 2];
		// check valid stepcode for clr temp
		if ((valid & 2) == 2) {
			t.setData(new byte[] { (byte) data[offset] });
			s = clrTempSuffix + " " + t.getValue();
		}
		else
			s = clrTempSuffix + " -";
		// check valid stepcode for brightness
		if ((valid & 1) == 1) {
			t.setData(new byte[] { (byte) data[offset + 1] });
			if (!s.isEmpty())
				s += " ";
			s += brightnessSuffix + " " + t.getValue();
		}
		else
			s += " " + brightnessSuffix + " -";
		return s;
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		if (value.isEmpty())
			return;
		final String[] fields = value.split(" ", 0);
		// either 1 or 2 control with stepcodes, optionally component prefix
		if (fields.length > 8 || fields.length < 2)
			throw newException("unsupported format for brightness & color temperature control", value);

		int valid = 0;
		final int offset = index * 3;
		int i = 0;
		if (clrTempSuffix.equals(fields[i])) {
			final int clr = component(true, fields, ++i);
			valid |= clr == 0 ? 0 : 2;
			dst[offset] = ubyte(clr);

			i += clr == 0 ? 1 : 2;
			if ("steps".equals(fields[i]))
				i++;
			if (!brightnessSuffix.equals(fields[i]))
				throw newException("no brightness", fields[i]);
			i++;

			final int brt = component(false, fields, i);
			valid |= brt == 0 ? 0 : 1;
			dst[offset + 1] = ubyte(brt);
			i += brt == 0 ? 1 : 2;
			if (i < fields.length && "steps".equals(fields[i]))
				i++;
		}
		else if (fields.length >= 2 && fields.length <= 6) {
			final int clr = component(true, fields, i);
			valid |= clr == 0 ? 0 : 2;
			dst[offset] = ubyte(clr);

			i += clr == 0 ? 1 : 2;
			if ("steps".equals(fields[i]))
				i++;

			final int brt = component(false, fields, i);
			valid |= brt == 0 ? 0 : 1;
			dst[offset + 1] = ubyte(brt);
			i += brt == 0 ? 1 : 2;
			if (i < fields.length && "steps".equals(fields[i]))
				i++;
		}
		if (i < fields.length)
			throw newException("value contains excessive components", fields[i]);
		dst[offset + 2] = (short) valid;
	}

	private int component(final boolean color, final String[] fields, final int i) throws KNXFormatException {
		final String first = fields[i];
		if ("-".equals(first))
			return 0;
		t.setValue(first + " " + fields[i + 1]);
		return t.getData()[0];
	}

	private short[] toDpt(final boolean increaseClrTemp, final int clrTempStepcode, final boolean increaseBrightness,
			final int brightnessStepcode) {
		rangeCheck(clrTempStepcode, brightnessStepcode);

		t.setValue(increaseClrTemp, clrTempStepcode);
		final short clrtemp = ubyte(t.getData()[0]);
		t.setValue(increaseBrightness, brightnessStepcode);
		final short bright = ubyte(t.getData()[0]);
		final int valid = 0b11;

		return new short[] { clrtemp, bright, valid };
	}

	private void rangeCheck(final int clrTempStepcode, final int brightnessStepcode) {
		if (clrTempStepcode < 1 || clrTempStepcode > 7)
			throw new KNXIllegalArgumentException("color temperature stepcode " + clrTempStepcode + " out of range [1..7]");
		if (brightnessStepcode < 1 || brightnessStepcode > 7)
			throw new KNXIllegalArgumentException("brightness stepcode " + brightnessStepcode + " out of range [1..7]");
	}
}

/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;


class DPTXlator8BitUnsignedTest
{
	private DPTXlator8BitUnsigned t;

	// scaled values/data
	private final String min = "0";
	private final String max = "255";
	private final String maxScale = "100";
	private final String maxAngle = "360";
	private final String value1 = "13";
	private final String value2 = "89";
	private final String[] strings = { value1, min, value2, };
	private final int[] values = { 13, 0, 89, };

	// unscaled values/data
	private final byte[] dataMin = { 0, };
	private final byte[] dataMax = { (byte) 0xff, };
	private final byte[] dataValue1 = { 34 };
	private final byte[] dataValue2 = { 0, 0, (byte) 128, };
	private final int[] valuesRaw = { 255, 34, 0, 128 };
	private final String[] stringsRaw = { "255", "34", "0", "128" };
	private final byte[] data = { (byte) 0xff, 34, 0, (byte) 128, };

	private final DPT[] dpts = {
		DPTXlator8BitUnsigned.DPT_SCALING, DPTXlator8BitUnsigned.DPT_ANGLE,
		DPTXlator8BitUnsigned.DPT_PERCENT_U8,
		DPTXlator8BitUnsigned.DPT_DECIMALFACTOR, DPTXlator8BitUnsigned.DPT_TARIFF,
		DPTXlator8BitUnsigned.DPT_VALUE_1_UCOUNT, DPTXlator8BitUnsigned.DptFanStage };


	@BeforeEach
	void init() throws Exception
	{
		t = new DPTXlator8BitUnsigned(dpts[5]);
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValues();
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		final String[] s = { value1 };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(t.getValue(), t.getValue());
	}

	@Test
	void getAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		Helper.assertSimilar("0", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(stringsRaw, t.getAllValues());

		t.setValue(values[0]);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(strings[0], t.getAllValues()[0]);
	}

	@Test
	void setValueString() throws KNXFormatException
	{
		t.setValue(value1);
		Helper.assertSimilar(value1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());
	}

	@Test
	void getValue() throws KNXFormatException
	{
		Helper.assertSimilar("0", t.getValue());
		t.setValues();
		Helper.assertSimilar("0", t.getValue());
		t.setValue(values[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(stringsRaw[3], t.getValue());
		t.setData(data);
		Helper.assertSimilar(max, t.getValue());
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(dataMax, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertArrayEquals(dataMax, t.getData());
		t.setData(dataValue2, 2);
		byte[] d = t.getData();
		assertEquals(1, d.length);
		assertEquals(d[0], dataValue2[2]);

		final byte[] array = new byte[data.length + 1];
		System.arraycopy(data, 0, array, 1, data.length);
		t.setData(array, 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertArrayEquals(data, d);
		Helper.assertSimilar(stringsRaw, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException
	{
		assertEquals(2, t.getData(new byte[2], 1).length);
		final byte[] empty = new byte[2];
		assertArrayEquals(empty, t.getData(new byte[2], 1));

		t.setData(data);
		byte[] d = new byte[10];
		Arrays.fill(d, (byte) 0xCC);
		t.getData(d, 3);
		for (int i = 0; i < 3; i++)
			assertEquals((byte) 0xCC, d[i]);
		for (int i = 3; i < 7; i++)
			assertEquals(data[i - 3], d[i]);
		for (int i = 7; i < 10; i++)
			assertEquals((byte) 0xCC, d[i]);

		for (int i = 0; i < valuesRaw.length; i++) {
			t.setValue(valuesRaw[i]);
			d = t.getData(new byte[2 + i], i);
			assertEquals(data[i], d[i]);
			assertEquals(0, d[i + 1]);
		}

		final DPTXlator8BitUnsigned x = new DPTXlator8BitUnsigned(dpts[4]);
		try {
			// reserved, shall not be used
			x.setValue(255);
			fail("255 is reserved, shall not be used for tariff");
		}
		catch (final Exception e) {
			// fine
		}
	}

	@Test
	void getSubTypes()
	{
		assertEquals(dpts.length, t.getSubTypes().size());
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length - 1, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
	}

	@Test
	void dptXlator8BitUnsignedDPT()
	{
		Helper.checkDPTs(dpts, true);
	}

	@Test
	void getValueUnsigned() throws KNXFormatException
	{
		assertEquals(0, t.getValueUnsigned());

		t.setData(dataMax);
		assertEquals(valuesRaw[0], t.getValueUnsigned());
		t.setData(dataValue1);
		assertEquals(valuesRaw[1], t.getValueUnsigned());
		t.setData(dataMin);
		assertEquals(valuesRaw[2], t.getValueUnsigned());
		t.setData(dataValue2, 2);
		assertEquals(valuesRaw[3], t.getValueUnsigned());

		for (int i = 0; i < stringsRaw.length; i++) {
			t.setValue(stringsRaw[i]);
			assertEquals(valuesRaw[i], t.getValueUnsigned());
		}
	}

	@Test
	void getValueShortUnscaled() throws KNXFormatException
	{
		assertEquals(0, t.getValueUnscaled());
		t.setData(dataMax);
		assertEquals(255, t.getValueUnscaled());
		t.setData(dataMin);
		assertEquals(0, t.getValueUnscaled());
		for (final int value : values) {
			t.setValue(value);
			assertEquals(value, t.getValueUnscaled());
		}
		final DPTXlator8BitUnsigned tscaled = new DPTXlator8BitUnsigned(dpts[0]);
		tscaled.setValue(maxScale);
		assertEquals(100, tscaled.getValueUnsigned());
		assertEquals(255, tscaled.getValueUnscaled());
		final int[] scaledRaw = { 33, 0, 227 };
		for (int i = 0; i < values.length; i++) {
			tscaled.setValue(values[i]);
			assertEquals(scaledRaw[i], tscaled.getValueUnscaled());
		}
		for (int i = 0; i < data.length; i++) {
			tscaled.setData(data, i);
			assertEquals(valuesRaw[i], tscaled.getValueUnscaled());
		}

		final DPTXlator8BitUnsigned tangle = new DPTXlator8BitUnsigned(dpts[1]);
		tangle.setValue(maxAngle);
		assertEquals(360, tangle.getValueUnsigned());
		assertEquals(255, tangle.getValueUnscaled());
		final int[] angleRaw = { 9, 0, 63 };
		for (int i = 0; i < values.length; i++) {
			tangle.setValue(values[i]);
			assertEquals(angleRaw[i], tangle.getValueUnscaled());
		}
		for (int i = 0; i < data.length; i++) {
			tangle.setData(data, i);
			assertEquals(valuesRaw[i], tangle.getValueUnscaled());
		}
	}

	@Test
	void setValueInt() throws KNXFormatException
	{
		for (final DPT dpt : dpts) {
			setValueIntFail(new DPTXlator8BitUnsigned(dpt), Integer.parseInt(dpt
					.getLowerValue()) - 1);
			setValueIntFail(new DPTXlator8BitUnsigned(dpt), Integer.parseInt(dpt
					.getUpperValue()) + 1);
		}
	}

	private static void setValueIntFail(final DPTXlator8BitUnsigned tr, final int v)
	{
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) {}
	}

	@Test
	void setValueUnscaled() throws KNXFormatException
	{
		t.setData(data);
		assertEquals(data.length, t.getItems());
		t.setValueUnscaled(0);
		assertEquals(1, t.getItems());

		for (final int j : valuesRaw) {
			t.setValueUnscaled(j);
			assertEquals(j, t.getValueUnscaled());
		}

		final DPTXlator8BitUnsigned tscaled = new DPTXlator8BitUnsigned(dpts[0]);
		tscaled.setValueUnscaled(255);
		assertEquals(100, tscaled.getValueUnsigned());
		assertEquals(255, tscaled.getValueUnscaled());
		Helper.assertSimilar(maxScale, tscaled.getValue());
		final int[] scaledRaw = { 34, 0, 227 };
		for (int i = 0; i < scaledRaw.length; i++) {
			tscaled.setValueUnscaled(scaledRaw[i]);
			assertEquals(values[i], tscaled.getValueUnsigned());
			Helper.assertSimilar(strings[i], tscaled.getValue());
		}
		for (int i = 0; i < valuesRaw.length; i++) {
			tscaled.setValueUnscaled(valuesRaw[i]);
			assertEquals(data[i], tscaled.getData()[0]);
			assertEquals(1, tscaled.getData().length);
		}

		final DPTXlator8BitUnsigned tangle = new DPTXlator8BitUnsigned(dpts[1]);
		tangle.setValueUnscaled(255);
		assertEquals(360, tangle.getValueUnsigned());
		assertEquals(255, tangle.getValueUnscaled());
		final int[] angleRaw = { 9, 0, 63 };
		final DecimalFormat fmt = new DecimalFormat("##.#");
		final String[] scaled = { fmt.format(12.7d), min, fmt.format(88.9d) };
		for (int i = 0; i < angleRaw.length; i++) {
			tangle.setValueUnscaled(angleRaw[i]);
			assertEquals(values[i], tangle.getValueUnsigned());
			Helper.assertSimilar(scaled[i], tangle.getValue());
		}
		for (int i = 0; i < valuesRaw.length; i++) {
			tangle.setValueUnscaled(valuesRaw[i]);
			assertEquals(data[i], tangle.getData()[0]);
			assertEquals(1, tscaled.getData().length);
		}
	}

	@Test
	void setValuesWithFloatingPointNumbers() throws KNXFormatException {
		final String[] values = { "0.0", "1.0e0", "11.1" };
		for (final DPT dpt : dpts) {
			final DPTXlator x = new DPTXlator8BitUnsigned(dpt);
			x.setValues(values);
		}
	}

	@Test
	void scalingWithFloatingPointNumbers() throws KNXFormatException {
		final DPTXlator x = new DPTXlator8BitUnsigned(DPTXlator8BitUnsigned.DPT_SCALING);
		final double max = 100d;
		x.setValue(max + " %");
		assertEquals("100 %", x.getValue());
		assertEquals(max, x.getNumericValue());

		final double scaled = 80d * 100 / 255;
		x.setValue(String.valueOf(scaled));
		assertEquals(80, x.getData()[0] & 0xff);
		assertEquals(String.format("%.1f %%", scaled), x.getValue());
		assertEquals(scaled, x.getNumericValue());
	}

	@Test
	void getNumericValue() throws KNXFormatException {
		for (final DPT dpt : dpts) {
			final DPTXlator x = new DPTXlator8BitUnsigned(dpt);
			assertEquals(0d,  x.getNumericValue(), 0);
		}
		final double scaled = (Math.round(12.345d * 255 / 100)) * 100d / 255;
		final double angle = (Math.round(12.345d * 255 / 360)) * 360d / 255;
		final double[] values = { scaled, angle, 12, 12, 12, 12, 12 };
		for (int i = 0; i < dpts.length; i++) {
			final DPTXlator x = new DPTXlator8BitUnsigned(dpts[i]);
			x.setValue("12.345");
			assertEquals(values[i],  x.getNumericValue(), x.getType().getDescription());
		}
	}
}

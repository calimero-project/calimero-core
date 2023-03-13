/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

import static org.junit.jupiter.api.Assertions.*;


class DPTXlator2ByteUnsignedTest
{
	private DPTXlator2ByteUnsigned t;

	private final String min = format(0);
	private final String max = format(65535);
	private final String value1 = format(735);
	private final String value2 = format(54732);
	private final String[] strings = { value1, min, value2, max, };
	private final int[] ints = { 735, 0, 54732, 65535, };
	private final byte[] dataMin = { 0, 0 };
	private final byte[] dataMax = { (byte) 0xff, (byte) 0xff };
	private final byte[] dataValue1 = { 2, (byte) 0xDF };
	private final byte[] dataValue2 = { 0, 0, (byte) 0xd5, (byte) 0xcc, 0 };
	private final byte[] data =
		{ 2, (byte) 0xDF, 0, 0, (byte) 0xd5, (byte) 0xcc, (byte) 0xff, (byte) 0xff };

	private final DPT[] dpts = {
		DPTXlator2ByteUnsigned.DPT_VALUE_2_UCOUNT,
		DPTXlator2ByteUnsigned.DPT_PROP_DATATYPE,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_10,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_100,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_SEC,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_MIN,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_HOURS,
		DPTXlator2ByteUnsigned.DPT_ELECTRICAL_CURRENT,
		DPTXlator2ByteUnsigned.DPT_BRIGHTNESS,
		DPTXlator2ByteUnsigned.DPT_LENGTH,
		DPTXlator2ByteUnsigned.DPT_ABSOLUTE_COLOR_TEMPERATURE
	};


	@BeforeEach
	void init() throws Exception
	{
		t = new DPTXlator2ByteUnsigned(dpts[0]);
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
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(format(0), t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length / 2, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValue(5000);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(format(5000), t.getAllValues()[0]);
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
		Helper.assertSimilar(format(0), t.getValue());
		t.setValues();
		Helper.assertSimilar(format(0), t.getValue());
		t.setValue(ints[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(value2, t.getValue());
		t.setData(data);
		Helper.assertSimilar(value1, t.getValue());
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(dataMin, 0);
		try {
			t.setData(new byte[] {}, 0);
		fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertArrayEquals(dataMin, t.getData());
		t.setData(dataValue2, 2);
		byte[] d = t.getData();
		assertEquals(2, d.length);
		assertEquals(d[0], dataValue2[2]);
		assertEquals(d[1], dataValue2[3]);

		final byte[] array = new byte[data.length + 4];
		System.arraycopy(data, 0, array, 3, data.length);
		t.setData(array, 3);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertArrayEquals(data, d);
		Helper.assertSimilar(strings, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException
	{
		assertEquals(4, t.getData(new byte[4], 1).length);
		final byte[] empty = new byte[4];
		assertArrayEquals(empty, t.getData(new byte[4], 1));

		t.setData(data);
		byte[] d = t.getData(new byte[20], 10);
		for (int i = 0; i < 10; i++)
			assertEquals(0, d[i]);
		for (int i = 10; i < 18; i++)
			assertEquals(data[i - 10], d[i]);
		for (int i = 18; i < 20; i++)
			assertEquals(0, d[i]);

		for (int i = 0; i < ints.length; i++) {
			t.setValue(ints[i]);
			d = t.getData(new byte[2 + i], i);
			assertEquals(data[2 * i], d[i]);
			assertEquals(data[2 * i + 1], d[i + 1]);
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
	void dptXlator2ByteUnsignedDPT() throws KNXFormatException
	{
		checkDPTs(dpts, true);
		for (DPT dpt : dpts) {
			setValueIntFail(new DPTXlator2ByteUnsigned(dpt), Integer.parseInt(dpt
					.getLowerValue()) - 1);
			setValueDoubleFail(new DPTXlator2ByteUnsigned(dpt), Double.parseDouble(dpt
					.getUpperValue()) + 1);
		}
	}

	private void setValueIntFail(final DPTXlator2ByteUnsigned tr, final int v)
	{
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) {}
	}

	private void setValueDoubleFail(final DPTXlator2ByteUnsigned tr, final double v)
	{
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) {}
	}

	@Test
	void getValueUnsigned() throws KNXFormatException
	{
		assertEquals(0, t.getValueUnsigned());

		t.setData(dataValue1);
		assertEquals(ints[0], t.getValueUnsigned());
		t.setData(dataMin);
		assertEquals(ints[1], t.getValueUnsigned());
		t.setData(dataValue2, 2);
		assertEquals(ints[2], t.getValueUnsigned());
		t.setData(dataMax);
		assertEquals(ints[3], t.getValueUnsigned());

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(ints[i], t.getValueUnsigned());
		}
	}

	@Test
	void setValueInt() throws KNXFormatException
	{
		for (int i = 0; i < ints.length; i++) {
			t.setValue(ints[i]);
			assertEquals(ints[i], t.getValueUnsigned());
			Helper.assertSimilar(strings[i], t.getValue());
		}
		// check 10ms and 100ms timeperiods
		DPTXlator2ByteUnsigned tr = new DPTXlator2ByteUnsigned(dpts[3]);
		tr.setValue(655.35);
		assertEquals(655350, tr.getValueUnsigned());
		Helper.assertSimilar(format(655.35), tr.getValue());
		// round up
		tr.setValue(337777);
		assertEquals(337780, tr.getValueUnsigned());
		Helper.assertSimilar(format(337.78), tr.getValue());
		// round off
		tr.setValue(332222);
		assertEquals(332220, tr.getValueUnsigned());
		Helper.assertSimilar(format(332.22), tr.getValue());

		tr = new DPTXlator2ByteUnsigned(dpts[4]);
		tr.setValue(6553500);
		assertEquals(6553500, tr.getValueUnsigned());
		Helper.assertSimilar(format(6553.5), tr.getValue());
		// round up
		tr.setValue(3377751);
		assertEquals(3377800, tr.getValueUnsigned());
		Helper.assertSimilar(format(3377.8), tr.getValue());
		// round off
		tr.setValue(3322249);
		assertEquals(3322200, tr.getValueUnsigned());
		Helper.assertSimilar(format(3322.2), tr.getValue());
	}

	@Test
	void setTimePeriod() throws KNXFormatException
	{
		final long[] max =
			{ 65535L, 655350L, 6553500L, 65535000L, 65535L * 60000, 65535L * 3600000, };
		for (int i = 2; i < 8; i++) {
			final DPTXlator2ByteUnsigned tr = new DPTXlator2ByteUnsigned(dpts[i]);
			tr.setTimePeriod(max[i - 2]);
			if (i == 3)
				assertEquals(655350, tr.getValueUnsigned());
			else if (i == 4)
				assertEquals(6553500, tr.getValueUnsigned());
			else
				assertEquals(65535, tr.getValueUnsigned());
		}
	}

	@Test
	void setHexString() throws KNXFormatException {
		final var t = new DPTXlator2ByteUnsigned(DPTXlator2ByteUnsigned.DPT_VALUE_2_UCOUNT);
		t.setValue("0x10");
		assertEquals(16, (int) t.getNumericValue());
	}

	private static String format(final double v) {
		final var formatter = NumberFormat.getNumberInstance();
		return formatter.format(v);
	}

	private static void checkDPTs(final DPT[] dpts, final boolean testSimilarity)
	{
		try {
			for (DPT dpt : dpts) {
				final DPTXlator t = TranslatorTypes.createTranslator(0, dpt.getID());

				final String lower = format(Double.parseDouble(dpt.getLowerValue()));
				t.setValue(lower);
				if (testSimilarity)
					Helper.assertSimilar(lower, t.getValue());

				final String upper = format(Double.parseDouble(dpt.getUpperValue()));
				t.setValue(upper);
				if (testSimilarity)
					Helper.assertSimilar(upper, t.getValue());
			}
		}
		catch (final KNXException e) {
			fail(e.getMessage());
		}
	}
}

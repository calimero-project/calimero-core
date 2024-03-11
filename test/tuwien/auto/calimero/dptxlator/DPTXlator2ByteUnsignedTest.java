/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;


class DPTXlator2ByteUnsignedTest
{
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

	private static final DPT[] dpts = {
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


	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void setValues(final DPTXlator2ByteUnsigned t) throws KNXFormatException
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

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void getAllValues(final DPTXlator2ByteUnsigned t) throws KNXFormatException
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

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void setValueString(final DPTXlator2ByteUnsigned t) throws KNXFormatException
	{
		t.setValue(value1);
		Helper.assertSimilar(value1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());
	}

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void getValue(final DPTXlator2ByteUnsigned t) throws KNXFormatException
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

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void setDataByteArrayInt(final DPTXlator2ByteUnsigned t)
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

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void getDataByteArrayInt(final DPTXlator2ByteUnsigned t) throws KNXFormatException
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
	void getSubTypes() throws KNXFormatException
	{
		final var t = new DPTXlator2ByteUnsigned(dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length - 1, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
	}

	@ParameterizedTest
	@MethodSource("allXlators")
	void dptXlator2ByteUnsignedDPT(final DPTXlator2ByteUnsigned t) throws KNXFormatException
	{
		final String lower = format(Double.parseDouble(t.getType().getLowerValue()));
		t.setValue(lower);
		Helper.assertSimilar(lower, t.getValue());

		final String upper = format(Double.parseDouble(t.getType().getUpperValue()));
		t.setValue(upper);
		Helper.assertSimilar(upper, t.getValue());

		assertThrows(KNXFormatException.class,() -> t.setValue(Integer.parseInt(t.getType().getLowerValue()) - 1));
		assertThrows(KNXFormatException.class,() -> t.setValue(Math.nextDown(Double.parseDouble(t.getType().getLowerValue()))));
		assertThrows(KNXFormatException.class,() -> t.setValue(Math.nextUp(Double.parseDouble(t.getType().getUpperValue()))));
	}

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void getValueUnsigned(final DPTXlator2ByteUnsigned t) throws KNXFormatException
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

	@ParameterizedTest
	@MethodSource("xlatorsNoScaled")
	void setValueInt(final DPTXlator2ByteUnsigned t) throws KNXFormatException
	{
		for (int i = 0; i < ints.length; i++) {
			t.setValue(ints[i]);
			assertEquals(ints[i], t.getValueUnsigned());
			Helper.assertSimilar(strings[i], t.getValue());
		}
	}

	@Test
	void setValueTimeperiod_10() throws KNXFormatException {
		final var t = new DPTXlator2ByteUnsigned(dpts[3]);
		t.setValue(655350);
		assertEquals(655350, t.getValueUnsigned());
		// round up
		t.setValue(337777);
		assertEquals(337780, t.getValueUnsigned());
		// round off
		t.setValue(332222);
		assertEquals(332220, t.getValueUnsigned());
	}

	@Test
	void setValueTimeperiod_100() throws KNXFormatException {
		final var t = new DPTXlator2ByteUnsigned(dpts[4]);
		t.setValue(6553500);
		assertEquals(6553500, t.getValueUnsigned());
		// round up
		t.setValue(3377751);
		assertEquals(3377800, t.getValueUnsigned());
		// round off
		t.setValue(3322249);
		assertEquals(3322200, t.getValueUnsigned());
	}

	@Test
	void setTimePeriod() throws KNXFormatException
	{
		final long[] max = { 65535L, 655350L, 6553500L, 65535000L, 65535L * 60000, 65535L * 3600000, };
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

	private static Stream<DPTXlator2ByteUnsigned> allXlators() {
		return Stream.of(dpts).map(DPTXlator2ByteUnsignedTest::create);
	}

	private static Stream<DPTXlator2ByteUnsigned> xlatorsNoScaled() {
		final var noScaled = new ArrayList<>(Arrays.asList(dpts));
		noScaled.remove(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_10);
		noScaled.remove(DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_100);
		return noScaled.stream().map(DPTXlator2ByteUnsignedTest::create);
	}

	private static DPTXlator2ByteUnsigned create(final DPT dpt) {
		try {
			return new DPTXlator2ByteUnsigned(dpt.getID());
		} catch (final KNXFormatException e) {
			throw new IllegalStateException();
		}
	}
}

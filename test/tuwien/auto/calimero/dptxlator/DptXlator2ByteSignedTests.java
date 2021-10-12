/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2021 B. Malinowsky

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General @Test License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General @Test License for more details.

    You should have received a copy of the GNU General @Test License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    Linking this library statically or dynamically with other modules is
    making a combined work based on this library. Thus, the terms and
    conditions of the GNU General @Test License cover the whole
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.NumberFormat;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

class DptXlator2ByteSignedTests {
	private DptXlator2ByteSigned t;

	private final String min = format(-32768);
	private final String max = format(32767);
	private final String value1 = format(735);
	private final String value2 = format(-4732);
	private final String[] strings = { value1, min, value2, max, };
	private final int[] ints = { 735, -32768, -4732, 32767, };
	private final byte[] dataMin = { (byte) 0x80, (byte) 0x00 };
	private final byte[] dataMax = { (byte) 0x7f, (byte) 0xff };
	private final byte[] dataValue1 = { 2, (byte) 0xDF };
	private final byte[] dataValue2 = { 0, 0, (byte) 0xed, (byte) 0x84, 0 };
	private final byte[] data = { 2, (byte) 0xDF, (byte) 0x80, 0, (byte) 0xed, (byte) 0x84, (byte) 0x7f, (byte) 0xff };

	private final DPT[] dpts = {
		DptXlator2ByteSigned.DptValueCount,
		DptXlator2ByteSigned.DptDeltaTime,
		DptXlator2ByteSigned.DptDeltaTime10,
		DptXlator2ByteSigned.DptDeltaTime100,
		DptXlator2ByteSigned.DptDeltaTimeSec,
		DptXlator2ByteSigned.DptDeltaTimeMin,
		DptXlator2ByteSigned.DptDeltaTimeHours,
		DptXlator2ByteSigned.DptRotationAngle,
		DptXlator2ByteSigned.DptPercent,
		DptXlator2ByteSigned.DptLength };

	@BeforeEach
	void setUp() throws Exception {
		t = new DptXlator2ByteSigned(dpts[0]);
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValues(new String[0]);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		final String[] s = { value1 };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(new String[] { t.getValue(), t.getValue() });
	}

	@Test
	void getAllValues() throws KNXFormatException {
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
	void setValueString() throws KNXFormatException {
		t.setValue(value1);
		Helper.assertSimilar(value1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());
	}

	@Test
	void getValue() throws KNXFormatException {
		Helper.assertSimilar(format(0), t.getValue());
		t.setValues(new String[0]);
		Helper.assertSimilar(format(0), t.getValue());
		t.setValue(ints[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(value2, t.getValue());
		t.setData(data);
		Helper.assertSimilar(value1, t.getValue());
	}

	@Test
	void setDataByteArrayInt() {
		t.setData(dataMin, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {
		}
		assertTrue(Arrays.equals(dataMin, t.getData()));
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
		assertTrue(Arrays.equals(data, d));
		Helper.assertSimilar(strings, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException {
		assertEquals(4, t.getData(new byte[4], 1).length);
		final byte[] empty = new byte[4];
		assertTrue(Arrays.equals(empty, t.getData(new byte[4], 1)));

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
	void getSubTypes() {
		assertEquals(dpts.length, t.getSubTypes().size());
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length - 1, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
	}

	@Test
	void dptLimits() throws KNXFormatException {
		checkDPTs(dpts, true);
		for (int i = 0; i < dpts.length; i++) {
			final var tr = new DptXlator2ByteSigned(dpts[i]);
			if (dpts[i].equals(DptXlator2ByteSigned.DptPercent)) {
				setValueFail(tr, Double.parseDouble(dpts[i].getLowerValue()) - 1);
				setValueFail(tr, Double.parseDouble(dpts[i].getUpperValue()) + 1);
			}
			else {
				setValueFail(tr, Long.parseLong(dpts[i].getLowerValue()) - 1);
				setValueFail(tr, Long.parseLong(dpts[i].getUpperValue()) + 1);
			}
		}
	}

	private void setValueFail(final DptXlator2ByteSigned tr, final double v) {
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) {}
	}

	@Test
	void getNumericValue() throws KNXFormatException {
		assertEquals(0, t.getNumericValue());

		t.setData(dataValue1);
		assertEquals(ints[0], t.getNumericValue());
		t.setData(dataMin);
		assertEquals(ints[1], t.getNumericValue());
		t.setData(dataValue2, 2);
		assertEquals(ints[2], t.getNumericValue());
		t.setData(dataMax);
		assertEquals(ints[3], t.getNumericValue());

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(ints[i], t.getNumericValue());
		}
	}

	@Test
	void setValue() throws KNXFormatException {
		for (int i = 0; i < ints.length; i++) {
			t.setValue(ints[i]);
			assertEquals(ints[i], t.getNumericValue());
			Helper.assertSimilar(strings[i], t.getValue());
		}
		// check 10 ms and 100 ms delta time
		var tr = new DptXlator2ByteSigned(DptXlator2ByteSigned.DptDeltaTime10);
		tr.setValue(327670);
		assertEquals(327670, tr.getNumericValue());
		Helper.assertSimilar(format(327670), tr.getValue());
		// round up
		tr.setValue(317777);
		assertEquals(317780, tr.getNumericValue());
		Helper.assertSimilar(format(317780), tr.getValue());
		// round off
		tr.setValue(312224);
		assertEquals(312220, tr.getNumericValue());
		Helper.assertSimilar(format(312220), tr.getValue());

		tr = new DptXlator2ByteSigned(DptXlator2ByteSigned.DptDeltaTime100);
		tr.setValue(3276700);
		assertEquals(3276700, tr.getNumericValue());
		Helper.assertSimilar(format(3276700), tr.getValue());
		// round up
		tr.setValue(337777);
		assertEquals(337800, tr.getNumericValue());
		Helper.assertSimilar(format(337800), tr.getValue());
		// round off
		tr.setValue(332249);
		assertEquals(332200, tr.getNumericValue());
		Helper.assertSimilar(format(332200), tr.getValue());
	}

	@Test
	void setDeltaTime() throws KNXFormatException {
		final long[] max = { 32767L, 327670L, 3276700L, 32767000L, 32767L * 60000, 32767L * 3600000, };
		for (int i = 1; i < 7; i++) {
			final DptXlator2ByteSigned tr = new DptXlator2ByteSigned(dpts[i]);
			tr.setDeltaTime(max[i - 1]);
			if (i == 2)
				assertEquals(327670, tr.getNumericValue(), "i=" + i);
			else if (i == 3)
				assertEquals(3276700, tr.getNumericValue(), "i=" + i);
			else
				assertEquals(32767L, tr.getNumericValue(), "i=" + i);
		}
	}

	@Test
	void setHexString() throws KNXFormatException {
		final var t = new DptXlator2ByteSigned(DptXlator2ByteSigned.DptValueCount);
		t.setValue("0x10");
		assertEquals(16, (int) t.getNumericValue());
	}

	private static String format(final double v) {
		final var formatter = NumberFormat.getNumberInstance();
		return formatter.format(v);
	}

	// TODO copied from Helper because we need to adjust numbers for current locale
	private static void checkDPTs(final DPT[] dpts, final boolean testSimilarity) {
		try {
			for (int i = 0; i < dpts.length; i++) {
				final DPTXlator t = TranslatorTypes.createTranslator(0, dpts[i].getID());
				t.setValue(dpts[i].getLowerValue());
				if (testSimilarity)
					Helper.assertSimilar(format(Double.parseDouble(dpts[i].getLowerValue())), t.getValue());
				t.setValue(dpts[i].getUpperValue());
				if (testSimilarity)
					Helper.assertSimilar(format(Double.parseDouble(dpts[i].getUpperValue())), t.getValue());
			}
		}
		catch (final KNXException e) {
			Assert.fail(e.getMessage());
		}
	}
}

/*
    Calimero 3 - A library for KNX network access
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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

import static org.junit.jupiter.api.Assertions.*;

class DPTXlator4ByteUnsignedTest {
	private DPTXlator4ByteUnsigned t;

	private final String min = "0";
	private final String max = "4294967295";
	private final String value1 = "412";
	private final String value2 = "3294961111";

	private final byte[] dataMin = { 0, 0, 0, 0 };
	private final byte[] dataMax = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
	private final byte[] dataValue1 = { 0, 0, 1, (byte) 0x9c };
	// offset = 2, 3 empty bytes at end
	private final byte[] dataValue2 = { 0, 0, (byte) 0xC4, (byte) 0x65, (byte) 0x1D, (byte) 0xD7,
		0, 0, 0 };

	private final String[] strings = { max, min, value1, value2, };
	private final long[] uints = { 4294967295L, 0, 412, 3294961111L, };
	private final byte[] data = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0, 0, 0, 0,
		0, 0, 1, (byte) 0x9c, (byte) 0xC4, (byte) 0x65, (byte) 0x1D, (byte) 0xD7 };

	@BeforeEach
	void setUp() throws Exception {
		t = new DPTXlator4ByteUnsigned(DPTXlator4ByteUnsigned.DPT_VALUE_4_UCOUNT);
	}

	@Test
	void setValues() throws KNXFormatException {
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
	void getAllValues() throws KNXFormatException {
		assertEquals(1, t.getItems());
		Helper.assertSimilar("0", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length / 4, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());
		// value = 0xCCCCCCCC
		t.setValue(3435973836L);
		assertEquals(1, t.getItems());
		Helper.assertSimilar("3435973836", t.getAllValues()[0]);

		t.setData(dataValue1);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(value1, t.getAllValues()[0]);
	}

	@Test
	void setValueString() throws KNXFormatException {
		t.setValue(value2);
		Helper.assertSimilar(value2, t.getValue());
		String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value2, t.getValue());
		assertEquals(s, t.getValue());

		t.setValue(value1);
		Helper.assertSimilar(value1, t.getValue());
		s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());
	}

	@Test
	void getValue() throws KNXFormatException {
		Helper.assertSimilar("0", t.getValue());
		t.setValues();
		Helper.assertSimilar("0", t.getValue());
		t.setValue(uints[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(value2, t.getValue());
		t.setData(data);
		Helper.assertSimilar(max, t.getValue());
	}

	@Test
	void setDataByteArrayInt() {
		t.setData(dataMin, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertArrayEquals(dataMin, t.getData());

		t.setData(dataMax, 0);
		assertArrayEquals(dataMax, t.getData());

		t.setData(dataValue2, 2);
		byte[] d = t.getData();
		assertEquals(4, d.length);
		for (int i = 0; i < d.length; i++) {
			assertEquals(d[i], dataValue2[i + 2]);
		}

		final byte[] array = new byte[data.length + 6];
		System.arraycopy(data, 0, array, 3, data.length);
		t.setData(array, 3);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertArrayEquals(data, d);
		Helper.assertSimilar(strings, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException {
		byte[] d = new byte[5];
		Arrays.fill(d, (byte) 0xAA);
		assertEquals(5, t.getData(d, 1).length);
		try {
			// usable range too short
			t.getData(new byte[4], 1);
			fail("usable range too short");
		}
		catch (final KNXIllegalArgumentException expected) {}
		final byte[] empty = new byte[5];
		assertArrayEquals(empty, t.getData(new byte[5], 1));

		t.setData(data);
		d = t.getData(new byte[25], 6);
		for (int i = 0; i < 6; i++)
			assertEquals(0, d[i]);
		for (int i = 6; i < 22; i++)
			assertEquals(data[i - 6], d[i]);
		for (int i = 22; i < 25; i++)
			assertEquals(0, d[i]);

		for (int i = 0; i < uints.length; i++) {
			t.setValue(uints[i]);
			d = t.getData(new byte[4 + i], i);
			for (int j = 0; j < 4; j++) {
				assertEquals(data[4 * i + j], d[i + j]);
			}
		}
	}

	@Test
	void getSubTypes() {
		assertEquals(6, t.getSubTypes().size());
	}

	@Test
	void getValueUnsigned() throws KNXFormatException {
		assertEquals(0, t.getValueUnsigned());

		for (int i = 0; i < uints.length - 1; i++) {
			t.setData(data, 4 * i);
			assertEquals(uints[i], t.getValueUnsigned());
		}
		t.setData(dataValue2, 2);
		assertEquals(uints[3], t.getValueUnsigned());

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(uints[i], t.getValueUnsigned());
		}
	}

	@Test
	void setValueLong() throws KNXFormatException {
		for (int i = 0; i < uints.length; i++) {
			t.setValue(uints[i]);
			assertEquals(uints[i], t.getValueUnsigned());
			Helper.assertSimilar(strings[i], t.getValue());
		}
	}
}

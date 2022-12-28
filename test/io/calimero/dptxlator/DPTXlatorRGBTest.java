/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2014, 2021 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

class DPTXlatorRGBTest {
	private DPTXlatorRGB t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DPTXlatorRGB();
	}

	@Test
	void constructWithDptIdString() throws KNXFormatException {
		new DPTXlatorRGB("232.600");
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DPTXlatorRGB.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@ParameterizedTest
	@CsvSource({"0,0,0", "20, 21, 22", "255, 255, 255"})
	void setValue(final int red, final int green, final int blue) {
		t.setValue(red, green, blue);
		assertEquals(red, t.red());
		assertEquals(green, t.green());
		assertEquals(blue, t.blue());
	}

	@ParameterizedTest
	@CsvSource({"-1,0,0", "0,-1,0", "0,0,-1", "256, 0, 0", "0, 256, 0", "0, 0, 256"})
	void setIllegalValue(final int red, final int green, final int blue) {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(red, green, blue));
	}

	@Test
	void red() {
		assertEquals(0, t.red());
	}

	@Test
	void green() {
		assertEquals(0, t.green());
	}

	@Test
	void blue() {
		assertEquals(0, t.blue());
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String first = "r:1 g:2 b:3";
		t.setValues(first, "4 5 6");
		final String[] all = t.getAllValues();
		assertEquals(first, all[0]);
		assertEquals("r:4 g:5 b:6", all[1]);
	}

	@Test
	void setValueString() throws KNXFormatException {
		t.setValue("1 2 3");
		final String v = t.getValue();
		assertEquals("r:1 g:2 b:3", v);
	}

	@Test
	void setData() {
		final byte[] data = { 1, 2, 3 };
		t.setData(data);
		assertArrayEquals(data, t.getData());
	}

	@Test
	void getData() {
		assertArrayEquals(new byte[] { 0, 0, 0 }, t.getData());
	}

	@Test
	void setDataByteArrayInt() {
		final byte[] data = { 0, 0, 1, 2, 3, 0 };
		t.setData(data, 2);
		assertArrayEquals(Arrays.copyOfRange(data, 2, 2 + 3), t.getData());
	}

	@Test
	void getDataByteArrayInt() {
		final byte[] data = { 1, 2, 3 };
		t.setData(data);
		final byte[] dst = new byte[6];
		t.getData(dst, 2);
		assertArrayEquals(data, Arrays.copyOfRange(dst, 2, 2 + 3));
	}
}

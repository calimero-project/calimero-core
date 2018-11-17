/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018 B. Malinowsky

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

class DptXlatorXyYTest {

	private DptXlatorXyY t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorXyY(DptXlatorXyY.DptColorXyY);
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { "(0,01, 1) 0", "(0.6, 0.5) 40", "(0.7 0.8) 90" };
		t.setValues(values);
		t.setAppendUnit(false);
		assertArrayEquals(values, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("", t.getValue());
		final String value = "(0.100, 0.50) 50";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DptXlatorXyY.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@Test
	void setValidxyYValue() {
		t.setValue(0, 0, 0);
		t.setValue(1, 1, 100);
		t.setValue(0.1, 0.1, 10);
		t.setValue(0.97, 0.98, 99);
		assertEquals(0.97, t.x().get().doubleValue(), 0.2);
		assertEquals(0.98, t.y().get().doubleValue(), 0.2);
		assertEquals(99, t.brightness().get().doubleValue(), 0.2);
	}

	@Test
	void setIllegalxyYValue() {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(1, 1, 101));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(-1, 1, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(1, -100, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(0, 1, -100));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues("0 0 0", "1 1 3 %", "0,30 0,30 0 %");
		assertEquals(3, t.getItems());
		assertEquals("0,30 0,30 0 %", t.getAllValues()[2]);
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, () -> t.getNumericValue());
	}

	@Test
	void setData() {
		final byte[] data = { 0b11, 25, 50, 77, 102, 50 };
		t.setData(data);
		final double x = ((25 << 8) | 50) / 65_535d;
		final double y = ((77 << 8) | 102) / 65_535d;
		final double b = 50 * 100d / 255;
		assertEquals("(" + x + ", " + y + ") " + b + " %", t.getValue());
	}

	@Test
	void getData() {
		assertArrayEquals(new byte[6], t.getData());
		final byte[] data = { 0, 0b1111, 25, 50, 30, 40 };
		t.setData(data);
		assertArrayEquals(data, t.getData());
	}

	@Test
	void getItems() {
		assertEquals(1, t.getItems());
		t.setValue(0, 1, 2);
		assertEquals(1, t.getItems());
	}

	@Test
	void correctTypeSize() {
		assertEquals(6, t.getTypeSize());
	}

	@Test
	void useWrongNumberComponents() {
		assertThrows(KNXFormatException.class, () -> t.setValue("()"));
		assertThrows(KNXFormatException.class, () -> t.setValue("(1.0, 1.0) %"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1.0 1.0 1.0 100"));
	}

	@Test
	void componentOutOfRange() {
		assertThrows(KNXFormatException.class, () -> t.setValue("1.0001 1 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 -2 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 1 -3"));
	}

	@Test
	void illegalCharacter() {
		assertThrows(KNXFormatException.class, () -> t.setValue("[1 1] 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 1 (100)"));
	}

	@Test
	void setEmptyValue() throws KNXFormatException {
		t.setValue("");
	}

	@Test
	void setUnitOnly() {
		assertThrows(KNXFormatException.class, () -> t.setValue("%"));
	}

	@Test
	void useNotValidComponent() throws KNXFormatException {
		String value = "100 %";
		t.setValue(value);
		assertEquals(value, t.getValue());

		value = "1, 1";
		t.setValue(value);
		assertEquals("(" + value + ")", t.getValue());
	}
}

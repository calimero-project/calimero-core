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

class DptXlatorRgbwTest {
	private DptXlatorRgbw t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorRgbw(DptXlatorRgbw.DptColorRgbw);
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { "0 1 2 3", "6 5 4 3", "7 8 9 0" };
		t.setValues(values);
		t.setAppendUnit(false);
		assertArrayEquals(values, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("- - - - %", t.getValue());
		final String value = "100 50 50 0";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DptXlatorRgbw.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@Test
	void setValidRgbwValue() {
		t.setValue(0, 0, 0, 0);
		t.setValue(1, 2, 3, 4);
		t.setValue(100, 100, 100, 100);
		t.setValue(97, 98, 99, 100);
		assertEquals(97, t.red().get().doubleValue(), 0.2);
		assertEquals(98, t.green().get().doubleValue(), 0.2);
		assertEquals(99, t.blue().get().doubleValue(), 0.2);
		assertEquals(100, t.white().get().doubleValue());
	}

	@Test
	void setIllegalRgbwValue() {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(100, 100, 100, 101));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(-1, 100, 100, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(-100, 100, 100, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(0, 100, 100, -100));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues("0 0 0 0", "1 2 3 4 %", "30 30 30 0 %");
		assertEquals(3, t.getItems());
		assertEquals("30 30 30 0 %", t.getAllValues()[2]);
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, () -> t.getNumericValue());
	}

	@Test
	void setData() {
		final byte[] data = { 0, 0b1111, 25, 50, 77, 102 };
		t.setData(data);
		assertEquals("10 20 30 40 %", t.getValue());
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
		t.setValue(0, 1, 2, 3);
		assertEquals(1, t.getItems());
	}

	@Test
	void correctTypeSize() {
		assertEquals(6, t.getTypeSize());
	}

	@Test
	void useWrongNumberComponents() {
		assertThrows(KNXFormatException.class, () -> t.setValue("-"));
		assertThrows(KNXFormatException.class, () -> t.setValue("- - - - -"));
		assertThrows(KNXFormatException.class, () -> t.setValue("0"));
		assertThrows(KNXFormatException.class, () -> t.setValue("0 1 2 3 4"));
	}

	@Test
	void componentOutOfRange() {
		assertThrows(KNXFormatException.class, () -> t.setValue("-1 2 3 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 -2 3 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 2 -3 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 2 3 -4"));
	}

	@Test
	void illegalCharacter() {
		assertThrows(KNXFormatException.class, () -> t.setValue("1 2 x 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 2 3 *"));
	}

	@Test
	void setNotValidValue() throws KNXFormatException {
		t.setValue("- - - - %");
	}

	@Test
	void useNotValidComponent() throws KNXFormatException {
		final String value = "100 - - 0 %";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void useReservedBits() {
		t.setData(new byte[] { (byte) 0xff, (byte) 0xf0, 1, 2, 3, 4 });
		t.getValue();
	}
}

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

class DptXlatorRelativeControlRgbwTest {
	private static final String value = "R decrease 1 G decrease 2 B decrease 3 W decrease 4";

	private DptXlatorRelativeControlRgbw t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorRelativeControlRgbw();
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { value, "R increase 7 G increase 7 B increase 7 W increase 7", };
		t.setValues(values);
		t.setAppendUnit(false);
		assertArrayEquals(values, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("R - G - B - W -", t.getValue());
		final String value = "G increase 3 W decrease 6";
		t.setValue(value);
		final String expected = "G increase 3 steps W decrease 6 steps";
		assertEquals(expected, t.getValue());
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DptXlatorBrightnessClrTempTrans.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@Test
	void setValidValue() {
		t.setValue(false, 0, false, 0, false, 0, false, 0);
		t.setValue(false, 1, false, 1, false, 1, false, 1);
		t.setValue(true, 7, true, 7, true, 7, true, 7);
		t.setValue(true, 3, false, 5, true, 4, false, 7);
		t.setValue(false, 1, true, 6, false, 2, true, 4);
	}

	@Test
	void setIllegalValue() {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(false, -1, false, 7, false, 7, false, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 7, true, -1, false, 7, false, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 8, true, 7, false, 7, false, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 7, true, 8, false, 7, false, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 7, true, 5, false, 8, false, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 7, true, 5, false, 7, false, 8));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues(value, "R decrease 1 G decrease 3 B increase 7 W increase 7");
		assertEquals(2, t.getItems());
		t.setAppendUnit(false);
		assertEquals("R decrease 1 G decrease 3 B increase 7 W increase 7", t.getAllValues()[1]);
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, () -> t.getNumericValue());
	}

	@Test
	void setData() {
		final byte[] data = { 8 + 5, 6, 7, 1, (byte) 0b1111 };
		t.setData(data);
		assertEquals("R increase 5 steps G decrease 6 steps B decrease 7 steps W decrease 1 steps", t.getValue());
	}

	@Test
	void getData() {
		assertArrayEquals(new byte[5], t.getData());
		final byte[] data = { 4, 5, 6, 7, 0b1111 };
		t.setData(data);
		assertArrayEquals(data, t.getData());
	}

	@Test
	void getItems() {
		assertEquals(1, t.getItems());
		t.setValue(true, 7, true, 7, true, 7, true, 7);
		assertEquals(1, t.getItems());
	}

	@Test
	void correctTypeSize() {
		assertEquals(5, t.getTypeSize());
	}

	@Test
	void useWrongNumberFields() {
		assertThrows(KNXFormatException.class, () -> t.setValue("-"));
		assertThrows(KNXFormatException.class, () -> t.setValue("- - -"));
		assertThrows(KNXFormatException.class, () -> t.setValue("up 0 increase 6"));
		assertThrows(KNXFormatException.class, () -> t.setValue("0 1"));
	}

	@Test
	void fieldOutOfRange() {
		assertThrows(KNXFormatException.class, () -> t.setValue("decrease -1 increase 5"));
		assertThrows(KNXFormatException.class, () -> t.setValue("decrease 3 increase -2"));
		assertThrows(KNXFormatException.class, () -> t.setValue("decrease 8 increase 5"));
		assertThrows(KNXFormatException.class, () -> t.setValue("decrease 3 increase 9"));
	}

	@Test
	void illegalControl() {
		assertThrows(KNXFormatException.class, () -> t.setValue("up 2 increase 7"));
		assertThrows(KNXFormatException.class, () -> t.setValue("decrease 1 down 3"));
	}

	@Test
	void setNotValidValue() throws KNXFormatException {
		final String value = "R - G - B - W -";
		t.setValue(value);
		final String expected = "R - G - B - W -";
		assertEquals(expected, t.getValue());
	}

	@Test
	void useNotValidField() throws KNXFormatException {
		String value = "G increase 7 W decrease 4";
		t.setValue(value);
		t.setAppendUnit(false);
		assertEquals(value, t.getValue());

		value = "R increase 7 B increase 1";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void min() throws KNXFormatException {
		final String value = "R decrease 7 G decrease 7 B decrease 7 W decrease 7";
		t.setValue(value);
		String expected = value;
		t.setAppendUnit(false);
		assertEquals(expected, t.getValue());
		t.setValue(false, 0, false, 0, false, 0, false, 0);
		expected = "R decrease break G decrease break B decrease break W decrease break";
		assertEquals(expected, t.getValue());
	}

	@Test
	void max() throws KNXFormatException {
		final String value = "R increase 7 steps G increase 7 steps B increase 7 steps W increase 7 steps";
		t.setValue(value);
		final String expected = value;
		assertEquals(expected, t.getValue());
		t.setValue(true, 7, true, 7, true, 7, true, 7);
		assertEquals(expected, t.getValue());
	}

	@Test
	void stopFading() throws KNXFormatException {
		final String value = "R break W break";
		t.setValue(value);
		final String expected = "R decrease break W decrease break";
		assertEquals(expected, t.getValue());
	}
}

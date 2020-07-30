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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

class DptXlatorBrightnessClrTempCtrlTest {
	private DptXlatorBrightnessClrTempCtrl t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorBrightnessClrTempCtrl();
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { "decrease 1 decrease 2", "increase 7 increase 7", "decrease 3 increase 5" };
		t.setValues(values);
		t.setAppendUnit(false);
		final String[] expected = { "CT decrease 1 BRT decrease 2", "CT increase 7 BRT increase 7", "CT decrease 3 BRT increase 5" };
		assertArrayEquals(expected, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("CT - BRT -", t.getValue());
		final String value = "increase 3 decrease 6";
		t.setValue(value);
		final String expected = "CT increase 3 steps BRT decrease 6 steps";
		assertEquals(expected, t.getValue());
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DptXlatorBrightnessClrTempTrans.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@Test
	void setValidValue() {
		t.setValue(false, 1, false, 1);
		t.setValue(true, 7, true, 7);
		t.setValue(true, 3, false, 5);
		t.setValue(false, 1, true, 6);
	}

	@Test
	void setIllegalValue() {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(false, -1, false, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 7, true, -1));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 8, true, 7));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(true, 7, true, 8));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues("decrease 1 decrease 3", "increase 7 increase 7", "decrease 3 increase 5");
		assertEquals(3, t.getItems());
		t.setAppendUnit(false);
		assertEquals("CT decrease 3 BRT increase 5", t.getAllValues()[2]);
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, () -> t.getNumericValue());
	}

	@Test
	void setData() {
		final byte[] data = { (byte) 3, 8 + 5, (byte) 0b11 };
		t.setData(data);
		assertEquals("CT decrease 3 steps BRT increase 5 steps", t.getValue());
	}

	@Test
	void getData() {
		assertArrayEquals(new byte[3], t.getData());
		final byte[] data = { 0, 0b1111, 25, 50, 30, 40 };
		t.setData(data);
		assertArrayEquals(data, t.getData());
	}

	@Test
	void getItems() {
		assertEquals(1, t.getItems());
		t.setValue(true, 7, true, 7);
		assertEquals(1, t.getItems());
	}

	@Test
	void correctTypeSize() {
		assertEquals(3, t.getTypeSize());
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
		final String value = "- -";
		t.setValue(value);
		final String expected = "CT - BRT -";
		assertEquals(expected, t.getValue());
	}

	@Test
	void useNotValidField() throws KNXFormatException {
		String value = "CT - BRT increase 7";
		t.setValue(value);
		t.setAppendUnit(false);
		assertEquals(value, t.getValue());

		value = "CT increase 7 BRT -";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void min() throws KNXFormatException {
		final String value = "decrease 1 decrease 1";
		t.setValue(value);
		final String expected = "CT decrease 1 BRT decrease 1";
		t.setAppendUnit(false);
		assertEquals(expected, t.getValue());
		t.setValue(false, 1, false, 1);
		assertEquals(expected, t.getValue());
	}

	@Test
	void max() throws KNXFormatException {
		final String value = "increase 7 steps increase 7 steps";
		t.setValue(value);
		final String expected = "CT increase 7 steps BRT increase 7 steps";
		assertEquals(expected, t.getValue());
		t.setValue(true, 7, true, 7);
		assertEquals(expected, t.getValue());
	}
}

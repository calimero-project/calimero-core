/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2020, 2021 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.calimero.dptxlator.StepControl.Break;
import static io.calimero.dptxlator.StepControl.decrease;
import static io.calimero.dptxlator.StepControl.increase;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

class DptXlatorRelativeControlXyYTest {
	private static final String value = "x decrease 1 y decrease 2 Y decrease 3";

	private DptXlatorRelativeControlXyY t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorRelativeControlXyY();
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { value, "x increase 7 y increase 7 Y increase 7", };
		t.setValues(values);
		t.setAppendUnit(false);
		assertArrayEquals(values, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("x - y - Y -", t.getValue());
		final String value = "y increase 3 Y decrease 6";
		t.setValue(value);
		final String expected = "y increase 3 steps Y decrease 6 steps";
		assertEquals(expected, t.getValue());
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DptXlatorBrightnessClrTempTrans.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@Test
	void setValidValue() {
		t.setValue(Break, Break, Break);
		t.setValue(decrease(1), decrease(1), decrease(1));
		t.setValue(increase(7), increase(7), increase(7));
		t.setValue(increase(3), decrease(5), increase(4));
		t.setValue(decrease(1), increase(6), decrease(2));
	}

	@Test
	void setIllegalValue() {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(decrease(-1), decrease(7), decrease(7)));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(increase(7), increase(-1), decrease(7)));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(increase(8), increase(7), decrease(7)));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(increase(7), increase(8), decrease(7)));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(increase(7), increase(5), decrease(8)));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(increase(7), increase(5), decrease(-1)));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues(value, "x decrease 1 y decrease 3 Y increase 7");
		assertEquals(2, t.getItems());
		t.setAppendUnit(false);
		assertEquals("x decrease 1 y decrease 3 Y increase 7", t.getAllValues()[1]);
	}

	@ParameterizedTest
	@MethodSource("stepControlProvider")
	void setx(final StepControl value) {
		t.setX(value);
		assertEquals(value, t.x().get());
		assertTrue(t.y().isEmpty());
		assertTrue(t.brightness().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("stepControlProvider")
	void sety(final StepControl value) {
		t.setY(value);
		assertEquals(value, t.y().get());
		assertTrue(t.x().isEmpty());
		assertTrue(t.brightness().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("stepControlProvider")
	void setBrightness(final StepControl value) {
		t.setBrightness(value);
		assertEquals(value, t.brightness().get());
		assertTrue(t.x().isEmpty());
		assertTrue(t.y().isEmpty());
	}

	private static Stream<StepControl> stepControlProvider() {
		return Stream.of(StepControl.Break, StepControl.increase(1), StepControl.decrease(7));
	}

	@Test
	void x() {
		assertTrue(t.x().isEmpty());
	}

	@Test
	void y() {
		assertTrue(t.y().isEmpty());
	}

	@Test
	void brightness() {
		assertTrue(t.brightness().isEmpty());
	}

	@Test
	void setAllComponents() {
		final StepControl x = StepControl.increase(1);
		final StepControl y = StepControl.increase(2);
		final StepControl Y = StepControl.increase(3);

		t.setX(x);
		t.setY(y);
		t.setBrightness(Y);

		assertEquals(x, t.x().get());
		assertEquals(y, t.y().get());
		assertEquals(Y, t.brightness().get());
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, t::getNumericValue);
	}

	@Test
	void setData() {
		final byte[] data = { 8 + 5, 6, 7, (byte) 0b111 };
		t.setData(data);
		assertEquals("x increase 5 steps y decrease 6 steps Y decrease 7 steps", t.getValue());
	}

	@Test
	void getData() {
		assertArrayEquals(new byte[4], t.getData());
		final byte[] data = { 4, 5, 6, 0b111 };
		t.setData(data);
		assertArrayEquals(data, t.getData());
	}

	@Test
	void getItems() {
		assertEquals(1, t.getItems());
		t.setValue(increase(7), increase(7), increase(7));
		assertEquals(1, t.getItems());
	}

	@Test
	void correctTypeSize() {
		assertEquals(4, t.getTypeSize());
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
		final String value = "x - y - Y -";
		t.setValue(value);
		final String expected = "x - y - Y -";
		assertEquals(expected, t.getValue());
	}

	@Test
	void useNotValidField() throws KNXFormatException {
		String value = "y increase 7 Y decrease 4";
		t.setValue(value);
		t.setAppendUnit(false);
		assertEquals(value, t.getValue());

		value = "x increase 7 Y increase 1";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void min() throws KNXFormatException {
		final String value = "x decrease 7 y decrease 7 Y decrease 7";
		t.setValue(value);
		String expected = value;
		t.setAppendUnit(false);
		assertEquals(expected, t.getValue());
		t.setValue(Break, Break, Break);
		expected = "x decrease break y decrease break Y decrease break";
		assertEquals(expected, t.getValue());
	}

	@Test
	void max() throws KNXFormatException {
		final String value = "x increase 7 steps y increase 7 steps Y increase 7 steps";
		t.setValue(value);
		final String expected = value;
		assertEquals(expected, t.getValue());
		t.setValue(increase(7), increase(7), increase(7));
		assertEquals(expected, t.getValue());
	}

	@Test
	void stopFading() throws KNXFormatException {
		final String value = "x break Y break";
		t.setValue(value);
		final String expected = "x decrease break Y decrease break";
		assertEquals(expected, t.getValue());
	}
}

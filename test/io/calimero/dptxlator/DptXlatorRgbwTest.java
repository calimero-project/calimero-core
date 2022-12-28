/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2023 B. Malinowsky

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

import java.text.NumberFormat;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

class DptXlatorRgbwTest {
	private final NumberFormat formatter = NumberFormat.getNumberInstance();

	private DptXlatorRgbw t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorRgbw();
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(1);
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { "0 1 2 3", "6 5 4 3", "7 8 9 0" };
		t.setValues(values);
		assertArrayEquals(new String[] { format(0, 1.2, 2, 3.1), format(5.9, 5.1, 3.9, 3.1), format(7.1, 7.8, 9, 0) },
				t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("- - - - %", t.getValue());
		final String value = "100 50 50 0";
		t.setValue(value);
		assertEquals(format(100d, 50.2, 50.2, 0d), t.getValue());
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
		assertEquals(97, t.red().get(), 0.2);
		assertEquals(98, t.green().get(), 0.2);
		assertEquals(99, t.blue().get(), 0.2);
		assertEquals(100, t.white().get().doubleValue());
	}

	@Test
	void setIllegalRgbwValue() {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(100, 100, 100, 101));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(-1, 100, 100, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(100, -1, 100, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(0, 100, 101, 100));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(0, 100, 100, -100));
	}

	@ParameterizedTest
	@ValueSource(doubles = { -0.1, 100.1})
	void setIllegalColorValue(final double value) {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setRed(value));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setGreen(value));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setBlue(value));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setWhite(value));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues("0 0 0 0", "1 2 3 4 %", "30 30 30 0 %");
		assertEquals(3, t.getItems());
		assertEquals(format(30.2, 30.2, 30.2, 0d), t.getAllValues()[2]);
	}

	@ParameterizedTest
	@MethodSource("colorValueProvider")
	void setRed(final double value) {
		t.setRed(value);
		assertEquals(value, t.red().get(), 0.2);
		assertTrue(t.green().isEmpty());
		assertTrue(t.blue().isEmpty());
		assertTrue(t.white().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("colorValueProvider")
	void setGreen(final double value) {
		t.setGreen(value);
		assertEquals(value, t.green().get(), 0.2);
		assertTrue(t.red().isEmpty());
		assertTrue(t.blue().isEmpty());
		assertTrue(t.white().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("colorValueProvider")
	void setBlue(final double value) {
		t.setBlue(value);
		assertEquals(value, t.blue().get(), 0.2);
		assertTrue(t.red().isEmpty());
		assertTrue(t.green().isEmpty());
		assertTrue(t.white().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("colorValueProvider")
	void setWhite(final double value) {
		t.setWhite(value);
		assertEquals(value, t.white().get(), 0.2);
		assertTrue(t.red().isEmpty());
		assertTrue(t.green().isEmpty());
		assertTrue(t.blue().isEmpty());
	}

	private static Stream<Double> colorValueProvider() {
		return Stream.of(0d, 100d, 12.34d, 99.9d, 50d);
	}

	@Test
	void red() {
		assertTrue(t.red().isEmpty());
	}

	@Test
	void green() {
		assertTrue(t.green().isEmpty());
	}

	@Test
	void blue() {
		assertTrue(t.blue().isEmpty());
	}

	@Test
	void white() {
		assertTrue(t.white().isEmpty());
	}

	@Test
	void setAllComponents() {
		final double red = 3;
		final double green = 4;
		final double blue = 5;
		final double white = 6;

		t.setRed(red);
		t.setGreen(green);
		t.setBlue(blue);
		t.setWhite(white);

		assertEquals(red, t.red().get(), 0.2);
		assertEquals(green, t.green().get(), 0.2);
		assertEquals(blue, t.blue().get(), 0.2);
		assertEquals(white, t.white().get(), 0.2);
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, () -> t.getNumericValue());
	}

	@Test
	void setData() {
		final byte[] data = { 25, 50, 77, 102, 0, 0b1111};
		t.setData(data);
		assertEquals(format(9.8, 19.6, 30.2, 40d), t.getValue());
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

	private String format(final double r, final double g, final double b, final double w) {
		return String.format("%s %s %s %s %%", formatter.format(r), formatter.format(g), formatter.format(b), formatter.format(w));
	}
}

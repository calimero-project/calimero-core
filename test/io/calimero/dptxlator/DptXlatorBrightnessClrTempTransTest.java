/*
    Calimero 3 - A library for KNX network access
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
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

class DptXlatorBrightnessClrTempTransTest {
	private DptXlatorBrightnessClrTempTrans t;

	@BeforeEach
	void init() throws KNXFormatException {
		t = new DptXlatorBrightnessClrTempTrans();
	}

	@Test
	void getAllValues() throws KNXFormatException {
		final String[] values = { "0 1 2", localize(5.9) + " 5 4", "7 8 " + localize(6500) };
		t.setValues(values);
		t.setAppendUnit(false);
		String[] expected = { "0 1 2", localize(5.9) + " 5 4", localize(7.1) + " 8 " + localize(6500) };
		assertArrayEquals(expected, t.getAllValues());

		t.setAppendUnit(true);
		expected = new String[]{ "0 % 1 K 2 s", localize(5.9) + " % 5 K 4 s", localize(7.1) + " % 8 K " + localize(6500) + " s" };
		assertArrayEquals(expected, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("- % - K - s", t.getValue());
		final String value = "100 % 65535 K " + localize(6553.5) + " s";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void onlyOneSubtype() {
		assertEquals(1, DptXlatorBrightnessClrTempTrans.getSubTypesStatic().size());
		assertEquals(1, t.getSubTypes().size());
	}

	@Test
	void setValidValue() {
		t.setValue(0, 0, Duration.ofMillis(1000));
		t.setValue(1, 2, Duration.ofSeconds(3));
		t.setValue(100, 100, Duration.ofSeconds(100));
		t.setValue(98, 99, Duration.ofMillis(65535 * 100));
	}

	@Test
	void setIllegalValue() {
		final Duration transition = Duration.ofSeconds(100);
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(100, -65535, transition));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(-1, 100, transition));
		assertThrows(KNXIllegalArgumentException.class, () -> t.setValue(0, 100, transition.negated()));
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues("0 0 0", "1 2 3", localize(30.2) + " % 30 K 30 s");
		assertEquals(3, t.getItems());
		assertEquals(localize(30.2) + " % 30 K 30 s", t.getAllValues()[2]);
	}

	@Test
	void withoutUnit() throws KNXFormatException {
		t.setValue(localize(40.4) + " % 56000 K " + localize(10.1) + " s");
		t.setAppendUnit(false);
		assertEquals(localize(40.4) + " 56000 " + localize(10.1), t.getValue());
	}

	@Test
	void getNumericValue() {
		assertThrows(KNXFormatException.class, t::getNumericValue);
	}

	@Test
	void setData() {
		final byte[] data = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0b111 };
		t.setData(data);
		assertEquals("100 % 65535 K " + localize(6553.5) + " s", t.getValue());
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
		t.setValue(0, 1, Duration.ofSeconds(5));
		assertEquals(1, t.getItems());
	}

	@Test
	void correctTypeSize() {
		assertEquals(6, t.getTypeSize());
	}

	@Test
	void useWrongNumberFields() {
		assertThrows(KNXFormatException.class, () -> t.setValue("-"));
		assertThrows(KNXFormatException.class, () -> t.setValue("- - - - -"));
		assertThrows(KNXFormatException.class, () -> t.setValue("0"));
		assertThrows(KNXFormatException.class, () -> t.setValue("0 1 2 3 4"));
	}

	@Test
	void fieldOutOfRange() {
		assertThrows(KNXFormatException.class, () -> t.setValue("-1 2 3 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 -2 3 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 2 -3 4"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 2 3 -4"));
	}

	@Test
	void illegalCharacter() {
		assertThrows(KNXFormatException.class, () -> t.setValue("1 % 2 K x s"));
		assertThrows(KNXFormatException.class, () -> t.setValue("1 % * K 2 s"));
	}

	@Test
	void setNotValidValue() throws KNXFormatException {
		final String value = "- % - K - s";
		t.setValue(value);
		assertEquals(value, t.getValue());
	}

	@Test
	void useNotValidField() throws KNXFormatException {
		final String value = localize(60.8) + " % - K " + localize(100.9) + " s";
		t.setValue(value);
		assertEquals(localize(60.8) + " % - K " + localize(100.9) + " s", t.getValue());
	}

	@Test
	void min() throws KNXFormatException {
		t.setValue("0 0 0");
		t.setAppendUnit(false);
		assertEquals("0 0 0", t.getValue());
		t.setValue(0, 0, Duration.ofMillis(0));
		assertEquals("0 0 0", t.getValue());
	}

	@Test
	void max() throws KNXFormatException {
		t.setValue("100 65535 " + localize(6553.5));
		t.setAppendUnit(false);
		assertEquals("100 65535 " + localize(6553.5), t.getValue());
		t.setValue(100, 65535, Duration.ofSeconds(6553, 500_000_000L));
		assertEquals("100 65535 " + localize(6553.5), t.getValue());
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 90, 3000 })
	void setFadingTime(final int time) {
		t.setFadingTime(Duration.ofMillis(time));
		assertEquals(Math.round(time / 100d) * 100, t.fadingTime().get().toMillis());
	}

	@ParameterizedTest
	@ValueSource(ints = { -10, 0xffff*100+1 })
	void fadingTimeOutOfRange(final int time) {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setFadingTime(Duration.ofMillis(time)));
	}

	@Test
	void fadingTime() {
		assertTrue(t.fadingTime().isEmpty());
	}

	@ParameterizedTest
	@ValueSource(doubles = { 0, 30.5, 100 })
	void setBrightness(final double brightness) {
		t.setBrightness(brightness);
		assertEquals(brightness, t.brightness().get(), 0.1);
	}

	@ParameterizedTest
	@ValueSource(doubles = { -1, 100.1 })
	void brightnessOutOfRange(final double brightness) {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setBrightness(brightness));
	}

	@Test
	void brightness() {
		assertTrue(t.brightness().isEmpty());
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 40000 })
	void setColorTemp(final int colorTemp) {
		t.setColorTemp(colorTemp);
		assertEquals(colorTemp, t.colorTemp().get());
	}

	@ParameterizedTest
	@ValueSource(ints = { -10, 0x10000 })
	void colorTempOutOfRange(final int colorTemp) {
		assertThrows(KNXIllegalArgumentException.class, () -> t.setColorTemp(colorTemp));
	}

	@Test
	void colorTemp() {
		assertTrue(t.colorTemp().isEmpty());
	}

	@Test
	void setAllComponents() {
		final double brightness = 22.2;
		final int colorTemp = 333;
		final var time = Duration.ofMillis(555);

		t.setBrightness(brightness);
		t.setColorTemp(colorTemp);
		t.setFadingTime(time);
		assertEquals(brightness, t.brightness().get(), 0.2);
		assertEquals(colorTemp, t.colorTemp().get());
		assertEquals(Math.round(time.toMillis() / 100d) * 100, t.fadingTime().get().toMillis());
	}

	private static String localize(final double v) {
		final var fmt = NumberFormat.getNumberInstance();
		fmt.setMinimumFractionDigits(0);
		fmt.setMaximumFractionDigits(1);
		return fmt.format(v);
	}
}

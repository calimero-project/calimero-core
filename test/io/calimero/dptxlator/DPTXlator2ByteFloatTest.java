/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;


class DPTXlator2ByteFloatTest
{
	private final String min = "-671088.64";
	private final String max = "670433.28";
	private final String maxAlt = "670760";
	private final String zero = "0.0";

	private final String value1 = "735.763";
	// the encoded value 1
	private final String value1Enc = "736.0";
	private final String value2 = "100.0";

	private final String[] strings = { min, max, zero, "736.0", value2 };
	private final double[] floats = { Double.parseDouble(min), Double.parseDouble(max),
		Double.parseDouble(zero), Double.parseDouble(value1), Double.parseDouble(value2), };

	private final byte[] dataMin = { (byte) 0xf8, 0 };
	private final byte[] dataMax = { (byte) 0x7f, (byte) 0xfe }; // 670433.28
	private final byte[] invalid = { 0x7f, (byte) 0xff };
	private final byte[] dataZero = { 0, 0 };
	private final byte[] dataValue1 = { (byte) 0x34, (byte) 0x7e };
	// 2 byte offset, 1 byte appended
	private final byte[] dataValue2 = { 0, 0, (byte) 0x1c, (byte) 0xe2, 0 };

	private static final DPT[] dpts = { DPTXlator2ByteFloat.DPT_TEMPERATURE,
		DPTXlator2ByteFloat.DPT_TEMPERATURE_DIFFERENCE,
		DPTXlator2ByteFloat.DPT_TEMPERATURE_GRADIENT, DPTXlator2ByteFloat.DPT_INTENSITY_OF_LIGHT,
		DPTXlator2ByteFloat.DPT_WIND_SPEED, DPTXlator2ByteFloat.DPT_AIR_PRESSURE,
		DPTXlator2ByteFloat.DPT_HUMIDITY, DPTXlator2ByteFloat.DPT_AIRQUALITY, DPTXlator2ByteFloat.DPT_AIR_FLOW,
		DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE1, DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE2,
		DPTXlator2ByteFloat.DPT_VOLTAGE, DPTXlator2ByteFloat.DPT_ELECTRICAL_CURRENT,
		DPTXlator2ByteFloat.DPT_POWERDENSITY, DPTXlator2ByteFloat.DPT_KELVIN_PER_PERCENT,
		DPTXlator2ByteFloat.DPT_POWER, DPTXlator2ByteFloat.DPT_VOLUME_FLOW,
		DPTXlator2ByteFloat.DPT_RAIN_AMOUNT, DPTXlator2ByteFloat.DPT_TEMP_F,
		DPTXlator2ByteFloat.DPT_WIND_SPEED_KMH, DPTXlator2ByteFloat.DptAbsoluteHumidity,
		DPTXlator2ByteFloat.DptConcentration };

	private static final DPT[] dptsLowerZero = { DPTXlator2ByteFloat.DPT_INTENSITY_OF_LIGHT,
			DPTXlator2ByteFloat.DPT_WIND_SPEED, DPTXlator2ByteFloat.DPT_AIR_PRESSURE,
			DPTXlator2ByteFloat.DPT_HUMIDITY, DPTXlator2ByteFloat.DPT_AIRQUALITY,
			DPTXlator2ByteFloat.DPT_WIND_SPEED_KMH, DPTXlator2ByteFloat.DptAbsoluteHumidity,
			DPTXlator2ByteFloat.DptConcentration };


	@ParameterizedTest
	@MethodSource("allXlators")
	void invalidData(final DPTXlator2ByteFloat t) {
		t.setData(invalid);
	}

	@ParameterizedTest
	@MethodSource("allXlators")
	void invalidValue(final DPTXlator2ByteFloat t) {
		t.setData(invalid);
		// TODO indicate invalid value in get(Numeric)Value
		t.getValue();
		t.getNumericValue();
	}

	@ParameterizedTest
	@MethodSource("xlatorsLowerZero")
	void min0(final DPTXlator2ByteFloat t) throws KNXFormatException {
		t.setValue(0);
		t.setValue(zero);

		assertThrows(KNXFormatException.class, () -> t.setValue(Math.nextDown(0d)));
		assertThrows(KNXFormatException.class, () -> t.setValue("-0.1"));
		assertThrows(KNXFormatException.class, () -> t.setValue(min));
		assertThrows(KNXFormatException.class, () -> t.setValue(Double.parseDouble(min)));
	}

	@ParameterizedTest
	@MethodSource("tempXlators")
	void minTemp(final DPTXlator2ByteFloat t, final double lowerLimit, final String sLowerLimit) throws KNXFormatException {
		t.setValue(lowerLimit);
		t.setValue(sLowerLimit);

		assertThrows(KNXFormatException.class, () -> t.setValue(Math.nextDown(lowerLimit)));
		assertThrows(KNXFormatException.class, () -> t.setValue("" + Math.nextDown(lowerLimit)));
		assertThrows(KNXFormatException.class, () -> t.setValue(min));
		assertThrows(KNXFormatException.class, () -> t.setValue(Double.parseDouble(min)));
	}

	@ParameterizedTest
	@MethodSource("xlatorsMaxAlt")
	void maxAlt(final DPTXlator2ByteFloat t) throws KNXFormatException {
		t.setValue(maxAlt);
		final double d = Double.parseDouble(maxAlt);
		t.setValue(d);

		assertThrows(KNXFormatException.class, () -> t.setValue(Math.nextUp(d)));
		assertThrows(KNXFormatException.class, () -> t.setValue("" + Math.nextUp(d)));
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void setValues(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		t.setValues();
		assertEquals(1, t.getItems());
		assertEquals(0.0, t.getNumericValue(), 0);
		t.setValues(min, max, zero, value1, value2);
		assertEquals(5, t.getItems());
		assertEquals(-671088.64, t.getNumericValue(), 1.0);
		t.setValue(100);
		t.setValues(t.getValue(), t.getValue());
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void getAllValues(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		assertEquals(t.getItems(), t.getItems());
		assertEquals(0.0, t.getNumericValue(), 0);
		t.setValues(strings);
		final String[] returned = t.getAllValues();
		assertEquals(strings.length, returned.length);
		assertEquals(t.getItems(), returned.length);
		for (int i = 0; i < strings.length; ++i)
			assertTrue(returned[i].contains(strings[i]));
	}

	@ParameterizedTest
	@MethodSource("allXlators")
	void setValueString(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		t.setValue(value1);
		assertEquals(1, t.getItems());
		assertEquals(floats[3], t.getNumericValue(), 1);
		assertTrue(t.getValue().startsWith(value1Enc));

		t.setValue(t.getValue());
		assertTrue(t.getValue().startsWith(value1Enc));
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void setDataByteArrayInt(final DPTXlator2ByteFloat t)
	{
		t.setData(dataMin, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertArrayEquals(dataMin, t.getData());
		t.setData(dataValue2, 2);
		byte[] data = t.getData();
		assertEquals(2, data.length);
		assertEquals(data[0], dataValue2[2]);
		assertEquals(data[1], dataValue2[3]);

		final byte[] array = { 0, dataMax[0], dataMax[1], dataValue1[0], dataValue1[1], 0 };
		t.setData(array, 1);
		data = t.getData();
		assertEquals(4, data.length);
		for (int i = 0; i < 4; ++i)
			assertEquals(array[i + 1], data[i]);
		Helper.assertSimilar(new String[] { max, value1Enc }, t.getAllValues());
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void getDataByteArrayInt(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		t.setData(dataValue2, 2);
		final byte[] data = t.getData(new byte[5], 2);
		assertEquals(5, data.length);
		assertArrayEquals(dataValue2, data);

		try {
			// usable range too short
			t.getData(new byte[2], 1);
			fail("usable range too short");
		}
		catch (final KNXIllegalArgumentException expected) {}
		assertArrayEquals(dataValue2, t.getData(new byte[5], 2));
		assertNotNull(t.getData(new byte[0], 0));

		final byte[] array = { 0, dataValue1[0], dataValue1[1], dataMin[0], dataMin[1], 0 };
		t.setData(array, 1);
		assertArrayEquals(array, t.getData(new byte[6], 1));

		t.setValues(value1, min);
		assertArrayEquals(array, t.getData(new byte[6], 1));
	}

	@Test
	void getSubTypes() throws KNXFormatException {
		final var t = new DPTXlator2ByteFloat(dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length - 1, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
	}

	@ParameterizedTest
	@MethodSource("allXlators")
	void lowerUpperLimit(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		final DPT dpt = t.getType();
		t.setValue(dpt.getLowerValue());
		t.setAppendUnit(false);
		double lower = Double.parseDouble(dpt.getLowerValue());
		assertEquals(lower, Double.parseDouble(t.getValue()), 0.09);

		t.setValue(dpt.getUpperValue());
		double upper = Double.parseDouble(dpt.getUpperValue());
		assertEquals(upper, Double.parseDouble(t.getValue()), 0.96);

		assertThrows(KNXFormatException.class, () -> t.setValue(Math.nextDown(lower)));
		assertThrows(KNXFormatException.class, () -> t.setValue(Math.nextUp(upper)));
	}

	@Test
	void invalid() {
		final DPT invalid = new DPT("0.00", "invalid", "invalid", "invalid", "invalid");
		assertThrows(KNXFormatException.class, () -> new DPTXlator2ByteFloat(invalid));
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void getNumericValue(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		assertEquals(0.0, t.getNumericValue(), 0);

		t.setData(dataMin);
		assertEquals(floats[0], t.getNumericValue(), 1.0);
		t.setData(dataMax);
		assertEquals(floats[1], t.getNumericValue(), 1.0);
		t.setData(dataZero);
		assertEquals(floats[2], t.getNumericValue(), 0);
		t.setData(dataValue1);
		assertEquals(floats[3], t.getNumericValue(), 1.0);
		t.setData(dataValue2, 2);
		assertEquals(floats[4], t.getNumericValue(), 1.0);

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(floats[i], t.getNumericValue(), 1.0);
		}
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void setValueDouble(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		for (int i = 0; i < floats.length; i++) {
			t.setValue(floats[i]);
			assertEquals(floats[i], t.getNumericValue(), 1.0);
			assertTrue(t.getValue().startsWith(strings[i]));
		}
	}

	@ParameterizedTest
	@MethodSource("xlatorsMinMax")
	void testToString(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		assertTrue(t.toString().contains("0.0"));
		t.setValues(strings);
		final String s = t.toString();
		for (final String string : strings) {
			assertTrue(s.contains(string));
		}
	}

	@ParameterizedTest
	@MethodSource("allXlators")
	void getValue(final DPTXlator2ByteFloat t) throws KNXFormatException
	{
		assertTrue(t.getValue().contains("0"));
		assertTrue(t.getValue().contains(t.getType().getUnit()));

		t.setValue(265);
		final double d = t.getNumericValue();
		assertEquals(265, d, 1.0);
		final String s = String.valueOf(d);
		assertTrue(t.getValue().contains(s));
	}

	@ParameterizedTest
	@MethodSource("allXlators")
	void getType(final DPTXlator2ByteFloat t) throws KNXFormatException {
		assertEquals(t.getType(), new DPTXlator2ByteFloat(t.getType().getID()).getType());
	}


	private static Stream<DPTXlator2ByteFloat> allXlators() {
		return Stream.of(dpts).map(DPTXlator2ByteFloatTest::create);
	}

	// lower limit 0
	private static Stream<DPTXlator2ByteFloat> xlatorsLowerZero() {
		return Stream.of(dptsLowerZero).map(DPTXlator2ByteFloatTest::create);
	}

	// temperature DPTs have different lower limit
	private static Stream<Arguments> tempXlators() throws KNXFormatException {
		return Stream.of(
				Arguments.of(new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_TEMPERATURE), -273, "-273.0"),
				Arguments.of(new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_TEMP_F), -459.6, "-459.6"));
	}

	// alternative upper limit 670760
	private static Stream<DPTXlator2ByteFloat> xlatorsMaxAlt() {
		return Stream.of(DPTXlator2ByteFloat.DptAbsoluteHumidity, DPTXlator2ByteFloat.DptConcentration)
				.map(DPTXlator2ByteFloatTest::create);
	}

	// default lower/upper limit
	private static Stream<DPTXlator2ByteFloat> xlatorsMinMax() {
		final var minMax = new ArrayList<>(Arrays.asList(dpts));
		minMax.removeAll(Arrays.asList(dptsLowerZero));
		minMax.remove(DPTXlator2ByteFloat.DPT_TEMPERATURE);
		minMax.remove(DPTXlator2ByteFloat.DPT_TEMP_F);
		return minMax.stream().map(DPTXlator2ByteFloatTest::create);
	}

	private static DPTXlator2ByteFloat create(final DPT dpt) {
		try {
			return new DPTXlator2ByteFloat(dpt.getID());
		} catch (final KNXFormatException e) {
			throw new IllegalStateException();
		}
	}
}

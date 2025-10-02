/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2015, 2025 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.dptxlator.DPTXlator8BitEnum.ApplicationArea;
import io.calimero.dptxlator.DPTXlator8BitEnum.OccupancyMode;

class DPTXlator8BitEnumTest {
	private DPTXlator8BitEnum t;

	private final String e1 = "no fault";
	private final String e2 = "11";
	private final String e3 = "12";

	private final String outOfRangeMin = "-1";
	private final String outOfRangeMax = "51";

	private final String[] strings = { e1, e2, e3 };
	private final String[] stringsDesc = { "no fault", "HVAC Hot Water Heating",
		"HVAC Direct Electrical Heating" };
	private final String[] stringsName = { ApplicationArea.NoFault.name(),
		ApplicationArea.HvacHotWaterHeating.name(),
		ApplicationArea.HvacDirectElectricalHeating.name() };

	private final int[] values = { 0, 11, 12 };

	private final byte[] e1Data = { 0, };
	private final byte[] e2Data = { 11 };
	private final byte[] e3Data = { 12 };
	private final byte[] outOfRangeMinData = { (byte) 0xff };
	private final byte[] outOfRangeMaxData = { 51 };
	private final byte[] eAllData = { e1Data[0], e2Data[0], e3Data[0] };


	@BeforeEach
	void setUp() throws Exception {
		TranslatorTypes.createTranslator(0, "20.003");
		t = new DPTXlator8BitEnum(DPTXlator8BitEnum.DptApplicationArea.getID());
	}

	@Test
	void getAllValues() throws KNXFormatException {
		assertEquals(1, t.getAllValues().length);
		assertEquals("no fault", t.getAllValues()[0]);

		t.setValues(strings);
		assertArrayEquals(stringsDesc, t.getAllValues());
	}

	@Test
	void getValue() throws KNXFormatException {
		// translator specific
		assertEquals("no fault", t.getValue());
		t.setValue(1);
		assertNotEquals("no fault", t.getValue(), "should not equal \"no fault\"");

		// try a enum with no 0 element
		final DPTXlator8BitEnum x = new DPTXlator8BitEnum(DPTXlator8BitEnum.DptAlarmClassType);
		final int v = x.getValueUnsigned();
		assertEquals(1, v);
		final int element = DPTXlator8BitEnum.AlarmClassType.ExtendedAlarm.value();
		x.setValue(element);
		assertEquals(element, x.getValueUnsigned());
	}

	@Test
	void value() throws KNXFormatException {
		final Enum<?> v = t.value();
		assertInstanceOf(ApplicationArea.class, v);
		assertEquals(ApplicationArea.NoFault, v);

		t.setValue(ApplicationArea.HvacTerminalUnits);
		assertEquals(ApplicationArea.HvacTerminalUnits, t.value());

		t.setData(outOfRangeMaxData);
		assertThrows(KNXFormatException.class, () -> t.value());
	}

	@Test
	void getSubTypes() {
		final Map<String, DPT> subTypes = t.getSubTypes();
		assertEquals(70, subTypes.size());
	}

	@Test
	void setValueInt() throws KNXFormatException {
		t.setValue(OccupancyMode.Occupied.value());
		t.setValue(e2);
		try {
			t.setValue(outOfRangeMin);
			fail("out of range min");
		}
		catch (final Exception expected) {}
		try {
			t.setValue(outOfRangeMax);
			fail("out of range max");
		}
		catch (final Exception expected) {}

		final Map<String, DPT> subTypes = DPTXlator8BitEnum.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			new DPTXlator8BitEnum(dpt).setValue("1");
		}
	}

	@Test
	void getValueUnsigned() throws KNXFormatException {
		final Map<String, DPT> subTypes = DPTXlator8BitEnum.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			final DPTXlator8BitEnum x = new DPTXlator8BitEnum(dpt);
			x.setValue("1");
			assertEquals(1, x.getValueUnsigned());
		}
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues(strings);
		t.setValues(stringsDesc);
		t.setValues(stringsName);

		assertEquals(values[0], t.getValueUnsigned());

		try {
			t.setValues("xyz");
			fail("element does not exist");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void setDataByteArrayInt() throws KNXFormatException {
		t.setData(e1Data, 0);
		assertEquals(0, t.getValueUnsigned());

		final byte[] data = new byte[] { -1, -1, e1Data[0], e2Data[0], e3Data[0] };
		t.setData(data, 2);
		assertArrayEquals(eAllData, t.getData());

		try {
			// TODO setData actually does not validate any input
			t.setData(outOfRangeMinData, 0);
			t.getValueUnsigned();
			fail("out of range");
		}
		catch (final KNXFormatException expected) {}
		try {
			// TODO setData actually does not validate any input
			t.setData(outOfRangeMaxData, 0);
			t.getValueUnsigned();
			fail("out of range");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException {
		t.setValue(e1);
		assertArrayEquals(e1Data, t.getData());
		t.setData(eAllData);
		final byte[] dst = new byte[6];
		Arrays.fill(dst, (byte) -1);
		final byte[] data = t.getData(dst, 2);
		assertEquals(-1, data[0]);
		assertEquals(-1, data[1]);

		assertEquals(e1Data[0], data[2]);
		assertEquals(e2Data[0], data[3]);
		assertEquals(e3Data[0], data[4]);

		assertEquals(-1, data[5]);
	}

	@Test
	void getTypeSize() {
		assertEquals(1, t.getTypeSize());
	}
}

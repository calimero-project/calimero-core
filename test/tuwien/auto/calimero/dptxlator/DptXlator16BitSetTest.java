/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2019 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static tuwien.auto.calimero.dptxlator.DptXlator16BitSet.RhccStatus.EarlyEveningShutdown;
import static tuwien.auto.calimero.dptxlator.DptXlator16BitSet.RhccStatus.Fault;
import static tuwien.auto.calimero.dptxlator.DptXlator16BitSet.RhccStatus.FrostAlarm;
import static tuwien.auto.calimero.dptxlator.DptXlator16BitSet.RhccStatus.HeatingEcoMode;
import static tuwien.auto.calimero.dptxlator.DptXlator16BitSet.RhccStatus.OverheatAlarm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.dptxlator.DptXlator16BitSet.Medium;
import tuwien.auto.calimero.dptxlator.DptXlator16BitSet.RhccStatus;

class DptXlator16BitSetTest {
	private DptXlator16BitSet t;

	private final String e1 = "LimitFlowTemperature";
	private final String e2 = "0x08";
	private final String e3 = "Fault";
	private final String[] strValues = new String[] { e1, e2, e3 };
	private final String[] elements = new String[] { "Limit Flow Temperature", "Limit Return Temperature", e3 };

	private final byte[] e1Data = { 0, 4, };
	private final byte[] e2Data = { 0, 8 };
	private final byte[] e3Data = { 0, 1 };
	private final byte[] eAllData = { e1Data[0], e1Data[1], e2Data[0], e2Data[1], e3Data[0], e3Data[1] };

	@BeforeEach
	void init() throws Exception {
		t = new DptXlator16BitSet(DptXlator16BitSet.DptRhccStatus);
	}

	@Test
	void getAllValues() throws KNXFormatException {
		assertEquals(1, t.getAllValues().length);
		assertEquals("Cooling Mode", t.getAllValues()[0]);

		t.setValues(strValues);
		assertArrayEquals(elements, t.getAllValues());
	}

	@Test
	void setMultipleElements() throws KNXFormatException {
		t.setValue("1 1 0 1");
		assertEquals(13, t.getNumericValue());
		assertEquals("Limit Return Temperature, Limit Flow Temperature, Fault", t.getValue());
	}

	@Test
	void getValue() throws KNXFormatException {
		assertEquals("Cooling Mode", t.getValue());
		t.setValue(1);
		assertTrue(Fault.name().equals(t.getValue()));

		final DptXlator16BitSet x = new DptXlator16BitSet(DptXlator16BitSet.DptRhccStatus);
		final int v = (int) x.getNumericValue();
		assertEquals(0, v);
		final int element = DptXlator16BitSet.RhccStatus.LimitFlowTemperature.value();
		x.setValue(element);
		assertEquals(element, x.getNumericValue());
	}

	@Test
	void implementedSubTypes() {
		final Map<String, DPT> subTypes = t.getSubTypes();
		assertEquals(2, subTypes.size());
	}

	@Test
	void setValue() throws KNXFormatException {
		t.setValue(Fault.value());
		t.setValue(e2);
		try {
			t.setValue(-1);
			fail("lower bound out of range");
		}
		catch (final Exception expected) {}
		try {
			t.setValue(0x8000);
			fail("upper bound out of range");
		}
		catch (final Exception expected) {}
		try {
			t.setValue("-1");
			fail("lower bound out of range");
		}
		catch (final Exception expected) {}
		try {
			t.setValue("32768");
			fail("upper bound out of range");
		}
		catch (final Exception expected) {}

		final Map<String, DPT> subTypes = DptXlator16BitSet.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			new DptXlator16BitSet(dpt).setValue("1");
		}
	}

	@Test
	void getNumericValue() throws KNXFormatException {
		final Map<String, DPT> subTypes = DptXlator16BitSet.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			final DptXlator16BitSet x = new DptXlator16BitSet(dpt);
			x.setValue("1");
			assertEquals(1, x.getNumericValue());
		}
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues(strValues);
		t.setValues(strValues);
		t.setValues(HeatingEcoMode.name(), Fault.name(), FrostAlarm.name(), OverheatAlarm.name(), EarlyEveningShutdown.name());

		assertEquals(2, t.getNumericValue());

		try {
			t.setValues(new String[] { "xyz" });
			fail("element does not exist");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void setData() throws KNXFormatException {
		t.setData(e1Data, 0);
		assertEquals(4, t.getNumericValue());

		final byte[] data = new byte[] { -1, -1, e1Data[0], e1Data[1], e2Data[0], e2Data[1], e3Data[0], e3Data[1] };
		t.setData(data, 2);
		assertArrayEquals(eAllData, t.getData());

		try {
			t.setData(new byte[] { (byte) 0xff, 0 }, 0);
			t.getNumericValue();
			fail("out of range");
		}
		catch (final KNXFormatException expected) {}
		try {
			t.setData(new byte[] { (byte) 0x80, (byte) 0xff }, 0);
			t.getNumericValue();
			fail("out of range");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void getData() throws KNXFormatException {
		t.setValue(e1);
		assertArrayEquals(e1Data, t.getData());
		t.setData(eAllData);
		final byte[] dst = new byte[9];
		Arrays.fill(dst, (byte) -1);
		final byte[] data = t.getData(dst, 2);
		assertEquals(-1, data[0]);
		assertEquals(-1, data[1]);

		assertEquals(e1Data[0], data[2]);
		assertEquals(e1Data[1], data[3]);
		assertEquals(e2Data[0], data[4]);
		assertEquals(e2Data[1], data[5]);
		assertEquals(e3Data[0], data[6]);
		assertEquals(e3Data[1], data[7]);

		assertEquals(-1, data[8]);
	}

	@Test
	void typeSize() {
		assertEquals(2, t.getTypeSize());
	}

	@Test
	void useDptMedia() throws KNXFormatException {
		t = new DptXlator16BitSet(DptXlator16BitSet.DptMedia);
		for (final Medium m : DptXlator16BitSet.Medium.values()) {
			t.setValue(m.name());
		}

		t.setValue("TP1");
	}

	@Test
	void mediumNames() throws KNXFormatException {
		t = new DptXlator16BitSet(DptXlator16BitSet.DptMedia);
		t.setValue("1 1 0 1 1 0");
		assertEquals("Knxip, RF, PL110, TP1", t.getValue());
	}

	@Test
	void useNoMedium() throws KNXFormatException {
		t = new DptXlator16BitSet(DptXlator16BitSet.DptMedia);

		t.setValue(t.getValue());
		assertEquals("", t.getValue());
		assertEquals(0, t.getNumericValue());

		t.setValue(EnumSet.noneOf(DptXlator16BitSet.Medium.class));
		assertEquals("", t.getValue());
		assertEquals(0, t.getNumericValue());
	}

	@Test
	void setPrettyPrintedConstants() throws KNXFormatException {
		t.setValue(t.getValue());
		assertEquals(0, t.getNumericValue());

		t.setValue(EnumSet.of(RhccStatus.EarlyMorningStart));
		t.setValue(t.getValue());

		t.setValue(EnumSet.of(RhccStatus.EarlyMorningStart, RhccStatus.FrostAlarm));
		t.setValue(t.getValue());
	}

	@Test
	void stringifyEnumConstants() throws KNXFormatException {
		t.setValue(RhccStatus.EarlyEveningShutdown.name());
		t.setValue(RhccStatus.EarlyEveningShutdown.name() + " " + RhccStatus.OverheatAlarm.name());
	}
}

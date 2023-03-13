/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2017, 2023 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.fail;
import static io.calimero.dptxlator.DptXlator8BitSet.GeneralStatus.AlarmUnAck;
import static io.calimero.dptxlator.DptXlator8BitSet.GeneralStatus.Fault;
import static io.calimero.dptxlator.DptXlator8BitSet.GeneralStatus.InAlarm;
import static io.calimero.dptxlator.DptXlator8BitSet.GeneralStatus.OutOfService;
import static io.calimero.dptxlator.DptXlator8BitSet.GeneralStatus.Overridden;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;

class DptXlator8BitSetTest
{
	private DptXlator8BitSet t;

	private final String e1 = "overridden";
	private final String e2 = "0x08";
	private final String e3 = "out of service";
	private final String[] strValues = new String[] { e1, e2, e3 };
	private final String[] elements = new String[] { e1, "in alarm", e3 };

	private final byte[] e1Data = { 4, };
	private final byte[] e2Data = { 8 };
	private final byte[] e3Data = { 1 };
	private final byte[] eAllData = { e1Data[0], e2Data[0], e3Data[0] };

	@BeforeEach
	void init() throws Exception
	{
		t = new DptXlator8BitSet(DptXlator8BitSet.DptGeneralStatus);
	}

	@Test
	void testSet() throws KNXFormatException {
		t = new DptXlator8BitSet(DptXlator8BitSet.DptChannelActivation);
		t.setValue("1 0 0 0 1 1 0 1");
		assertEquals("channel8, channel4, channel3, channel1", t.getValue());

		t = new DptXlator8BitSet(DptXlator8BitSet.DptChannelActivation);
		t.setValue("channel1");
		assertEquals("channel1", t.getValue());
	}

	@Test
	void getAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getAllValues().length);
		assertEquals("", t.getAllValues()[0]);

		t.setValues(strValues);
		assertArrayEquals(elements, t.getAllValues());
	}

	@Test
	void setMultipleElements() throws KNXFormatException
	{
		t.setValue("1 1 0 1");
		assertEquals(13, t.getNumericValue());
		assertEquals("in alarm, overridden, out of service", t.getValue());
	}

	@Test
	void getValue() throws KNXFormatException
	{
		assertEquals("", t.getValue());
		t.setValue(1);
		assertEquals(e3, t.getValue());

		final DptXlator8BitSet x = new DptXlator8BitSet(DptXlator8BitSet.DptDeviceControl);
		final int v = (int) x.getNumericValue();
		assertEquals(0, v);
		final int element = DptXlator8BitSet.DeviceControl.VerifyMode.value();
		x.setValue(element);
		assertEquals(element, x.getNumericValue());
	}

	@Test
	void getSubTypes()
	{
		final Map<String, DPT> subTypes = t.getSubTypes();
		assertEquals(14, subTypes.size());
	}

	@Test
	void setValue() throws KNXFormatException
	{
		t.setValue(Fault.value());
		t.setValue(e2);
		try {
			t.setValue(-1);
			fail("lower bound out of range");
		}
		catch (final Exception expected) {}
		try {
			t.setValue(32);
			fail("upper bound out of range");
		}
		catch (final Exception expected) {}
		try {
			t.setValue("-1");
			fail("lower bound out of range");
		}
		catch (final Exception expected) {}
		try {
			t.setValue("32");
			fail("upper bound out of range");
		}
		catch (final Exception expected) {}

		final Map<String, DPT> subTypes = DptXlator8BitSet.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			new DptXlator8BitSet(dpt).setValue("1");
		}
	}

	@Test
	void getNumericValue() throws KNXFormatException
	{
		final Map<String, DPT> subTypes = DptXlator8BitSet.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			final DptXlator8BitSet x = new DptXlator8BitSet(dpt);
			x.setValue("1");
			assertEquals(1, x.getNumericValue());
		}
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(strValues);
		t.setValues(strValues);
		t.setValues(OutOfService.name(), Fault.name(), Overridden.name(), InAlarm.name(),
				AlarmUnAck.name());

		assertEquals(1, t.getNumericValue());

		try {
			t.setValues("xyz");
			fail("element does not exist");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void setData() throws KNXFormatException
	{
		t.setData(e1Data, 0);
		assertEquals(4, t.getNumericValue());

		final byte[] data = new byte[] { -1, -1, e1Data[0], e2Data[0], e3Data[0] };
		t.setData(data, 2);
		assertArrayEquals(eAllData, t.getData());

		try {
			t.setData(new byte[] { (byte) 0xff }, 0);
			t.getNumericValue();
			fail("out of range");
		}
		catch (final KNXFormatException expected) {}
		try {
			t.setData(new byte[] { 32 }, 0);
			t.getNumericValue();
			fail("out of range");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void getData() throws KNXFormatException
	{
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
	void getTypeSize()
	{
		assertEquals(1, t.getTypeSize());
	}
}

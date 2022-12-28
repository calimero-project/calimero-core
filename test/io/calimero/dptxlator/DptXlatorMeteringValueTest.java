/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2017, 2020 B. Malinowsky

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
import static io.calimero.dptxlator.DptXlatorMeteringValue.DptMeteringValue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;

class DptXlatorMeteringValueTest
{
	private static final String fault = DptXlator8BitSet.GeneralStatus.Fault.name().toLowerCase();
	private static final String outOfService = "out of service";

	private DptXlatorMeteringValue t;

	private final String e1 = "1.2 GJ";
	private final String e2 = "1234 Wh";
	private final String e3 = "0.001 kJ";
	private final String[] strValues = new String[] { e1, e2, e3 };
	private final String[] elements = new String[] { "1.2E9 J", "1234.0 Wh", "1.0 J" };

	private final byte[] e1Data = { 0, 0, 0, 12, (byte) 0x88, 0 };
	private final byte[] e2Data = { 0, 0x12, (byte) 0xd4, (byte) 0x50, 0, 2 };
	private final byte[] e3Data = { 1, 0, 0, 0, 0b00010111, 0 }; // volume [l] with exp 7
	private byte[] eAllData;

	@BeforeEach
	void init() throws Exception
	{
		t = new DptXlatorMeteringValue(DptMeteringValue);
		final int length = e1Data.length;
		eAllData = new byte[3 * length];
		System.arraycopy(e1Data, 0, eAllData, 0, length);
		System.arraycopy(e2Data, 0, eAllData, length, length);
		System.arraycopy(e3Data, 0, eAllData, 2 * length, length);
	}

	@Test
	void getAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getAllValues().length);
		assertEquals("0.0", t.getAllValues()[0]);

		t.setValues(strValues);
		assertArrayEquals(elements, t.getAllValues());
	}

	@Test
	void getValue()
	{
		assertEquals("0.0", t.getValue());
		t.setValue(1);
		assertEquals("1.0", t.getValue());

		// XXX any value we set in status is ignored
		t.status().setValue(EnumSet.of(DptXlator8BitSet.GeneralStatus.OutOfService));
		assertEquals("1.0", t.getValue());
	}

	@Test
	void setValueWithCoding() throws KNXFormatException
	{
		t.setValue(0b00000001, 333.5);
		assertEquals("333.5 Wh", t.getValue());
		t.setAppendUnit(false);
		assertEquals("333.5", t.getValue());
	}

	@Test
	void getValueWithStatus()
	{
		// coding 'mass' kg with nnn = 2, i.e., exponent = 2 - 3 = 1
		t.setData(new byte[] { 0, 1, 0, 0, 0b00011010, 0b00011111 });
		assertEquals("alarm un ack, in alarm, overridden, " + fault + ", " + outOfService + ", 6553.6 kg", t.getValue());
	}

	@Test
	void getSubTypes()
	{
		final Map<String, DPT> subTypes = t.getSubTypes();
		assertEquals(1, subTypes.size());
	}

	@Test
	void setNumericValue()
	{
		t.setValue(0);
		t.setValue(Integer.MIN_VALUE);
		t.setValue(Integer.MAX_VALUE);
	}

	@Test
	void setUnsupportedUnit()
	{
		try {
			t.setValue("555.5 TB");
			fail("unsupported unit");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void setStringValue() throws KNXFormatException
	{
		t.setValue("555");
		assertEquals("555.0", t.getValue());
		assertEquals(555.0, t.getNumericValue());

		t.setValue("555.123 W");
		assertEquals("555.123 W", t.getValue());
		assertEquals(555.123, t.getNumericValue());

		t.setValue("555.123 l/h");
		final String[] split = t.getValue().split(" ", -1);
		assertEquals("0.55512", split[0].substring(0, 7));
		assertEquals("m³/h", split[1]);
		assertEquals(0.555123, t.getNumericValue(), 0.000001);
	}

	@Test
	void setInvalidStringValue()
	{
		try {
			t.setValue("12.3.5");
			fail("not a parsable number");
		}
		catch (final KNXFormatException expected) {}
		try {
			t.setValue(fault + " Wh");
			fail("no number");
		}
		catch (final KNXFormatException expected) {}
		try {
			t.setValue("0x1E3 Wh");
			fail("not a parsable number");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void setStringValueWithStatus() throws KNXFormatException
	{
		t.setValue("OutOfService 555.123 kJ/h");
		final String[] split = t.getValue().split(",", -1);
		assertEquals(outOfService, split[0]);
		assertEquals("555123.0 J/h", split[1].trim());
		assertEquals(555123.0, t.getNumericValue());
	}

	@Test
	void setStringValueWithMultiStatus() throws KNXFormatException
	{
		t.setValue(outOfService + ", Fault 555.123 kJ/h");
		final String[] split = t.getValue().split(",", -1);
		assertEquals(fault, split[0]);
		assertEquals(outOfService, split[1].trim());
		assertEquals("555123.0 J/h", split[2].trim());
		assertEquals(555123.0, t.getNumericValue());
	}

	@Test
	void getNumericValue() throws KNXFormatException
	{
		final Map<String, DPT> subTypes = DptXlatorMeteringValue.getSubTypesStatic();
		final Set<String> keySet = subTypes.keySet();
		for (final String s : keySet) {
			final DPT dpt = subTypes.get(s);
			final DptXlatorMeteringValue x = new DptXlatorMeteringValue(dpt);
			x.setValue("1");
			assertEquals(1, x.getNumericValue());
		}
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(strValues);
		assertEquals(1200000000, t.getNumericValue());

		try {
			t.setValues(new String[] { "xyz" });
			fail("not a number");
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void setData()
	{
		t.setData(e1Data, 0);
		assertEquals(1200000000, t.getNumericValue());

		final byte[] data = new byte[] { -1, -1, e1Data[0], e1Data[1], e1Data[2], e1Data[3], e1Data[4], e1Data[5],
			e2Data[0], e2Data[1], e2Data[2], e2Data[3], e2Data[4], e2Data[5], e3Data[0], e3Data[1], e3Data[2],
			e3Data[3], e3Data[4], e3Data[5] };
		t.setData(data, 2);
		assertArrayEquals(eAllData, t.getData());

		try {
			t.setData(new byte[] { (byte) 0xff }, 0);
			fail("wrong data size");
		}
		catch (final Exception expected) {}

		t.setData(e2Data);
		assertEquals(fault + ", 1234.0 Wh", t.getValue());

		t.setData(e3Data);
		assertEquals("1.6777216E8 m³", t.getValue());

		final byte[] hca = { 1, 0, 0, 0, 0b01101110, 0 }; // units for Heat Cost Allocator
		t.setData(hca);
		assertEquals(16777216.0, t.getNumericValue());

		final byte[] allset = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0, 31 };
		t.setData(allset);
		assertEquals("alarm un ack, in alarm, overridden, " + fault + ", " + outOfService + ", -0.001 Wh", t.getValue());

		final byte[] reserved = { 0, 0, 0, 0, (byte) 0b11101110, 0 };
		// TODO no data input validation for reserved codings
		t.setData(reserved);
	}

	@Test
	void getData() throws KNXFormatException
	{
		t.setValue(e1);
		assertArrayEquals(e1Data, t.getData());
		t.setData(eAllData);
		final byte[] dst = new byte[eAllData.length + 3];
		Arrays.fill(dst, (byte) -1);
		final byte[] data = Arrays.copyOfRange(t.getData(dst, 2), 2, eAllData.length + 2);
		assertEquals(-1, dst[0]);
		assertEquals(-1, dst[1]);

		assertArrayEquals(eAllData, data);

		assertEquals(-1, dst[eAllData.length + 2]);
	}

	@Test
	void status() throws KNXFormatException
	{
		assertEquals(0, t.status().getNumericValue());
		t.setData(e1Data, 0);
		assertEquals(0, t.status().getNumericValue());
		t.setData(e2Data, 0);
		assertEquals(fault, t.status().getValue());
	}

	@Test
	void getTypeSize()
	{
		assertEquals(6, t.getTypeSize());
	}
}

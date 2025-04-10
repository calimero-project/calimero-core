/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2009, 2023 B. Malinowsky

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

import static org.junit.jupiter.api.Assertions.*;


@Isolated("modifies Locale")
class DPTXlator4ByteFloatTest
{
	private DPTXlator4ByteFloat x;
	private DPTXlator4ByteFloat t;


	private final String min = "1.40239846e-45";
	private final String max = "3.40282347e+38";
	private final String value1 = "735";
	private final String value2 = "54732.33334";

	private final String value1Enc = "735";

	private final String zero = "0.0";
	private final String[] strings = { min, max, zero, value1, value2};
	private final String[] strCmp = { "1.4E-45", "3.40282E38", "0.0", "735", "54732.332"};

	private final float[] floats =
		{ Float.parseFloat(min), Float.parseFloat(max), Float.parseFloat(zero),
			Float.parseFloat(value1), Float.parseFloat(value2), };

	private final byte[] dataMin = toBytes(Float.floatToIntBits(floats[0]));
	private final byte[] dataMax = toBytes(Float.floatToIntBits(floats[1]));
	private final byte[] dataZero = toBytes(Float.floatToIntBits(floats[2]));
	private final byte[] dataValue1 = toBytes(Float.floatToIntBits(floats[3]));
	// we put the value on offset 4
	private final byte[] dataValue2 = new byte[] { 0, 0, 0, 0,
		toBytes(Float.floatToIntBits(floats[4]))[0], toBytes(Float.floatToIntBits(floats[4]))[1],
		toBytes(Float.floatToIntBits(floats[4]))[2], toBytes(Float.floatToIntBits(floats[4]))[3] };

	private final DPT[] dpts = DPTXlator4ByteFloat.getSubTypesStatic().values()
			.toArray(new DPT[1]);

	private static byte[] toBytes(final int i)
	{
		return new byte[] {(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) (i) };
	}


	@BeforeEach
	void init() throws Exception
	{
		x = new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_ACCELERATION);
		t = new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_ELECTRIC_FLUX);
	}

	@Test
	void setValues() throws KNXFormatException
	{
		final DPTXlator4ByteFloat t = new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_VOLUME);
		t.setValues(strings);
		assertEquals(strCmp.length, t.getItems());
		Helper.assertSimilar(strCmp, t.getAllValues());

		t.setValues();
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strCmp, t.getAllValues());

		final String[] s = { value1 };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(t.getValue(), t.getValue());
	}

	@Test
	void getAllValues() throws KNXFormatException
	{
		assertEquals(t.getItems(), t.getItems());
		assertEquals(0.0, t.getValueFloat(), 0);
		t.setValues(strings);
		final String[] returned = t.getAllValues();
		assertEquals(strings.length, returned.length);
		assertEquals(t.getItems(), returned.length);
		for (int i = 0; i < strings.length; ++i)
			assertTrue(returned[i].contains(strCmp[i]));
	}

	@Test
	void getSubTypes()
	{
		final Map<String, DPT> types = x.getSubTypes();
		assertEquals(83, types.size());
	}

	@Test
	void getSubTypesStatic()
	{
		final Map<String, DPT> types = DPTXlator4ByteFloat.getSubTypesStatic();
		assertEquals(83, types.size());
	}

	@Test
	void dptXlator4ByteFloatDPT()
	{
		Helper.checkDPTs(dpts, false);
	}

	@Test
	void setValueFloat() throws KNXFormatException
	{
		for (int i = 0; i < floats.length; i++) {
			t.setValue(floats[i]);
			assertEquals(floats[i], t.getValueFloat(), 1.0);
			assertTrue(t.getValue().startsWith(strCmp[i]));
		}
	}

	@Test
	void getValueFloat() throws KNXFormatException
	{
		assertEquals(0.0, t.getValueFloat(), 0);

		t.setData(dataMin);
		assertEquals(floats[0], t.getValueFloat(), 1.0);
		t.setData(dataMax);
		assertEquals(floats[1], t.getValueFloat(), 1.0);
		t.setData(dataZero);
		assertEquals(floats[2], t.getValueFloat(), 0);
		t.setData(dataValue1);
		assertEquals(floats[3], t.getValueFloat(), 1.0);
		t.setData(dataValue2, 4);
		assertEquals(floats[4], t.getValueFloat(), 1.0);

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(floats[i], t.getValueFloat(), 1.0);
		}
	}

	@Test
	void setValueString() throws KNXFormatException
	{
		t.setValue(value1);
		assertEquals(1, t.getItems());
		assertEquals(floats[3], t.getValueFloat(), 1);
		assertTrue(t.getValue().startsWith(value1Enc));

		t.setValue(t.getValue());
		assertTrue(t.getValue().startsWith(value1Enc));
	}

	@Test
	void getValue() throws KNXFormatException
	{
		assertTrue(t.getValue().contains("0"));
		assertTrue(t.getValue().contains(t.getType().getUnit()));
		t.setValue(265f);
		final float f = t.getValueFloat();
		assertEquals(265, f, 1.0);
		assertTrue(t.getValue().contains(String.valueOf(f)));

		// test non-localized formatted output for bigger floating point values
		// that use scientific notation
		// the difference is basically the decimal separator '.' vs ',' (depending on locale)
		// we compare for all available locales
		final float bigvalue = 123456.78f;
		final Locale saved = Locale.getDefault();
		final Locale[] locales = Locale.getAvailableLocales();
		final List<String> output = new ArrayList<>();
		for (final Locale l : locales) {
			Locale.setDefault(l);
			//System.out.println("test language " + l);
			final DPTXlator4ByteFloat t = new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_ACCELERATION);
			t.setValue(bigvalue);
			//System.out.println(t.getValue());
			output.add(t.getValue());
		}
		Locale.setDefault(saved);
		// check if outputs are the same
		for (int i = 1; i < output.size(); ++i) {
			final String first = output.get(i - 1);
			final String second = output.get(i);
			assertEquals(first, second);
		}
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(dataMin, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertArrayEquals(dataMin, t.getData());
		t.setData(dataValue2, 4);
		byte[] data = t.getData();
		assertEquals(4, data.length);
		assertEquals(data[0], dataValue2[4]);
		assertEquals(data[1], dataValue2[5]);
		assertEquals(data[2], dataValue2[6]);
		assertEquals(data[3], dataValue2[7]);

		final byte[] array =
			{ 0, dataMax[0], dataMax[1], dataMax[2], dataMax[3],
				dataValue1[0], dataValue1[1], dataValue1[2], dataValue1[3], 0 };
		t.setData(array, 1);
		data = t.getData();
		assertEquals(8, data.length);
		for (int i = 0; i < 8; ++i)
			assertEquals(array[i + 1], data[i]);
		Helper.assertSimilar(new String[] { strCmp[1], value1Enc }, t.getAllValues());
	}

	@Test
	void getType()
	{
		assertEquals(DPTXlator4ByteFloat.DPT_ACCELERATION, x.getType());
	}

	@Test
	void getTypeSize()
	{
		assertEquals(4, x.getTypeSize());
	}

	@Test
	void testToString() throws KNXFormatException
	{
		assertTrue(t.toString().contains("0.0"));
		t.setValues(strings);
		final String s = t.toString();
		for (int i = 0; i < strings.length; i++) {
			assertTrue(s.contains(strCmp[i]));
		}
	}

}

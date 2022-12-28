/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;


class DPTXlatorBooleanTest
{
	private DPTXlatorBoolean t;
	private DPT[] dpts;

	private final boolean[] values = { true, false, true };
	private final String[] strings = { "TRUE", "false", "true" };
	private final byte[] data = { 1, 0, 1 };

	private final byte[] dataValue1 = { 0, 0, 1 };


	@BeforeEach
	void init() throws Exception
	{
		t = new DPTXlatorBoolean(DPTXlatorBoolean.DPT_BOOL);
		dpts = t.getSubTypes().values().toArray(new DPT[0]);
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValues(new String[0]);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		final String[] s = { "true" };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(new String[] { t.getValue(), t.getValue() });
	}

	@Test
	void getAllValues()
	{
		assertEquals(1, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar("false", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValue(values[0]);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(strings[0], t.getAllValues()[0]);
	}

	@Test
	void setValueString() throws KNXFormatException
	{
		t.setValue(values[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(strings[0], t.getValue());
		assertEquals(s, t.getValue());

		setValueStringFail("start");
		setValueStringFail("stop");
		setValueStringFail("open");
		setValueStringFail("close");
	}

	private void setValueStringFail(final String string)
	{
		try {
			t.setValue(string);
			fail("should fail");
		}
		catch (final KNXFormatException e) {}
	}

	@Test
	void getValue() throws KNXFormatException
	{
		Helper.assertSimilar("false", t.getValue());
		t.setValues(new String[0]);
		Helper.assertSimilar("false", t.getValue());
		t.setValue(values[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue1, 2);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(data);
		Helper.assertSimilar(strings[0], t.getValue());
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(data, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertTrue(Arrays.equals(data, t.getData()));
		t.setData(dataValue1, 2);
		byte[] d = t.getData();
		assertEquals(1, d.length);
		assertEquals(d[0], dataValue1[2]);

		final byte[] array = new byte[data.length + 1];
		System.arraycopy(data, 0, array, 1, data.length);
		t.setData(array, 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertTrue(Arrays.equals(data, d));
		Helper.assertSimilar(strings, t.getAllValues());

		t.setData(new byte[] { (byte) 0xFF, (byte) 0xFF });
		d = t.getData();
		assertEquals(1, d[0]);
		assertEquals(1, d[1]);
		assertTrue(t.getValueBoolean());
	}

	@Test
	void getDataByteArrayInt()
	{
		assertEquals(2, t.getData(new byte[2], 1).length);
		final byte[] empty = new byte[2];
		assertTrue(Arrays.equals(empty, t.getData(new byte[2], 1)));

		t.setData(data);
		byte[] d = new byte[10];
		Arrays.fill(d, (byte) 0xCC);
		t.getData(d, 3);
		for (int i = 0; i < 3; i++)
			assertEquals((byte) 0xCC, d[i]);
		for (int i = 3; i < 6; i++)
			assertEquals(0xCC | data[i - 3], d[i] & 0xFF);
		for (int i = 6; i < 10; i++)
			assertEquals((byte) 0xCC, d[i]);

		for (int i = 0; i < values.length; i++) {
			t.setValue(values[i]);
			d = t.getData(new byte[2 + i], i);
			assertEquals(data[i], d[i]);
			assertEquals(0, d[i + 1]);
		}
	}

	@Test
	void getSubTypes()
	{
		assertEquals(dpts.length, t.getSubTypes().size());
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length - 1, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
	}

	@Test
	void dptXlatorBooleanDPT()
	{
		Helper.checkDPTs(dpts, true);
	}

	@Test
	void setValueBoolean()
	{
		assertFalse(t.getValueBoolean());
		t.setValue(true);
		assertTrue(t.getValueBoolean());
		t.setValue(false);
		assertFalse(t.getValueBoolean());
	}

	@Test
	void getValueBoolean() throws KNXFormatException
	{
		assertEquals(false, t.getValueBoolean());

		t.setData(new byte[] { 1 });
		assertEquals(true, t.getValueBoolean());
		t.setData(new byte[] { 0 });
		assertEquals(false, t.getValueBoolean());
		t.setData(data);
		assertEquals(values[0], t.getValueBoolean());
		assertEquals(data.length, t.getItems());
		t.setData(dataValue1, 2);
		assertEquals(values[0], t.getValueBoolean());
		assertEquals(1, t.getItems());

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(values[i], t.getValueBoolean());
		}
	}

}

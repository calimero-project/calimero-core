/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2013, 2023 B. Malinowsky

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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

import static org.junit.jupiter.api.Assertions.*;


class DPTXlatorSceneNumberTest
{
	private DPTXlatorSceneNumber t;

	// min max scenes
	private final String min = "0";
	private final String max = "63";

	private final String invalid = "64";
	private final String invalid2 = "-1";

	private final String[] invalidStrings = { invalid, invalid2, "activate " + min, "learn " + max,
		"act " + min, "lean" + max };

	private final String value1 = "13";
	private final String[] strings = { "63", "34", "0", "30" };

	private final byte[] dataMax = { (byte) 63, };
	private final byte[] dataValue2 = { 0, 0, (byte) 30, };
	private final byte[] data = { (byte) 63, 34, 0, (byte) 30, };

	final DPT sc = DPTXlatorSceneNumber.DPT_SCENE_NUMBER;
	final DPT[] dpts = { DPTXlatorSceneNumber.DPT_SCENE_NUMBER };


	@BeforeEach
	void init() throws Exception
	{
		t = new DPTXlatorSceneNumber(sc);
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValues();
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		final String[] s = { value1 };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(t.getValue(), t.getValue());
	}

	@Test
	void getAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		Helper.assertSimilar("0", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValue(strings[0]);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(strings[0], t.getAllValues()[0]);
	}

	@Test
	void setValueString() throws KNXFormatException
	{
		// pre and post spaces should be accepted
		t.setValue(" " + value1 + " ");
		Helper.assertSimilar(value1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());

		for (String invalidString : invalidStrings) {
			try {
				t.setValue(invalidString);
				fail("should not succeed: " + invalidString);
			} catch (final Exception e) {
				// fine
			}
		}
	}

	@Test
	void getValue() throws KNXFormatException
	{
		Helper.assertSimilar("0", t.getValue());
		t.setValues();
		Helper.assertSimilar("0", t.getValue());
		t.setValue(strings[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(strings[3], t.getValue());
		t.setData(data);
		Helper.assertSimilar(max, t.getValue());
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(dataMax, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertArrayEquals(dataMax, t.getData());
		t.setData(dataValue2, 2);
		byte[] d = t.getData();
		assertEquals(1, d.length);
		assertEquals(d[0], dataValue2[2]);

		final byte[] array = new byte[data.length + 1];
		System.arraycopy(data, 0, array, 1, data.length);
		t.setData(array, 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertArrayEquals(data, d);
		Helper.assertSimilar(strings, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException
	{
		assertEquals(2, t.getData(new byte[2], 1).length);
		final byte[] empty = new byte[2];
		assertArrayEquals(empty, t.getData(new byte[2], 1));

		t.setData(data);
		byte[] d = new byte[10];
		Arrays.fill(d, (byte) 0xCC);
		t.getData(d, 3);
		for (int i = 0; i < 3; i++)
			assertEquals((byte) 0xCC, d[i]);
		for (int i = 3; i < 7; i++)
			assertEquals(data[i - 3], d[i]);
		for (int i = 7; i < 10; i++)
			assertEquals((byte) 0xCC, d[i]);

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
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
	void dptXlatorSceneNumberDPT()
	{
		Helper.checkDPTs(dpts, true);
	}

	@Test
	void setValueInt()
	{
		t.setValue(13);
		Helper.assertSimilar(value1, t.getValue());
		int scene = t.getSceneNumber();
		assertEquals(13, scene);

		t.setValue(0);
		Helper.assertSimilar(strings[2], t.getValue());
		scene = t.getSceneNumber();
		assertEquals(0, scene);

		t.setValue(34);
		Helper.assertSimilar(strings[1], t.getValue());
		scene = t.getSceneNumber();
		assertEquals(34, scene);

		try {
			t.setValue(64);
			fail("scene number too big");
		}
		catch (final Exception e) {
			// fine
		}

		try {
			t.setValue(-1);
			fail("scene number negative");
		}
		catch (final Exception e) {
			// fine
		}
	}
}

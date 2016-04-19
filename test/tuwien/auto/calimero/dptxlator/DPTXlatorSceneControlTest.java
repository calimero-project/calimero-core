/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2013, 2016 B. Malinowsky

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

import java.util.Arrays;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class DPTXlatorSceneControlTest extends TestCase
{
	private DPTXlatorSceneControl t;

	// min max scenes
	private final String min = "0";
	private final String max = "63";

	private final String invalid = "64";
	private final String invalid2 = "-1";

	private final String[] invalidStrings = { "learn " + invalid, "activate " + invalid2, min, max,
		"act " + min, "lean" + max };

	private final String value1 = "learn 13";
	private final String[] strings = { "learn 63", "activate 34", "activate 0", "learn 30" };

	private final byte[] dataMax = { (byte) (0x80 | 63), };
	private final byte[] dataValue2 = { 0, 0, (byte) (128 + 30), };
	private final byte[] data = { (byte) (128 + 63), 34, 0, (byte) (128 + 30), };

	DPT sc = DPTXlatorSceneControl.DPT_SCENE_CONTROL;
	DPT[] dpts = { DPTXlatorSceneControl.DPT_SCENE_CONTROL };

	/**
	 * @param name
	 */
	public DPTXlatorSceneControlTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		Util.setupLogging("DPTXlator");
		t = new DPTXlatorSceneControl(sc);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#setValues(java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValues(new String[0]);
		assertEquals(strings.length, t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		final String[] s = { value1 };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(new String[] { t.getValue(), t.getValue() });
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public void testGetAllValues() throws KNXFormatException
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

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#setValue(java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public void testSetValueString() throws KNXFormatException
	{
		t.setValue("  " + value1 + "  ");
		Helper.assertSimilar(value1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());

		for (int i = 0; i < invalidStrings.length; i++) {
			try {
				t.setValue(invalidStrings[i]);
				fail("should not succeed: " + invalidStrings[i]);
			}
			catch (final Exception e) {
				// fine
			}
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#getValue()}.
	 *
	 * @throws KNXFormatException
	 */
	public void testGetValue() throws KNXFormatException
	{
		Helper.assertSimilar("0", t.getValue());
		t.setValues(new String[0]);
		Helper.assertSimilar("0", t.getValue());
		t.setValue(strings[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(strings[3], t.getValue());
		t.setData(data);
		Helper.assertSimilar(max, t.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#setData(byte[], int)}.
	 */
	public void testSetDataByteArrayInt()
	{
		t.setData(dataMax, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertTrue(Arrays.equals(dataMax, t.getData()));
		t.setData(dataValue2, 2);
		byte[] d = t.getData();
		assertEquals(1, d.length);
		assertEquals(d[0], dataValue2[2]);

		final byte[] array = new byte[data.length + 1];
		System.arraycopy(data, 0, array, 1, data.length);
		t.setData(array, 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertTrue(Arrays.equals(data, d));
		Helper.assertSimilar(strings, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#getData(byte[], int)}.
	 *
	 * @throws KNXFormatException
	 */
	public void testGetDataByteArrayInt() throws KNXFormatException
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

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#getSubTypes()}.
	 */
	public void testGetSubTypes()
	{
		assertEquals(dpts.length, t.getSubTypes().size());
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length - 1, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#DPTXlatorSceneControl(
	 * tuwien.auto.calimero.dptxlator.DPT)}.
	 */
	public void testDPTXlatorSceneControlDPT()
	{
		Helper.checkDPTs(dpts, true);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl#setValue(boolean, int)}.
	 */
	public void testSetValueBooleanInt()
	{
		t.setValue(true, 13);
		Helper.assertSimilar(value1, t.getValue());
		int scene = t.getSceneNumber();
		assertEquals(13, scene);

		t.setValue(false, 0);
		Helper.assertSimilar(strings[2], t.getValue());
		scene = t.getSceneNumber();
		assertEquals(0, scene);

		t.setValue(false, 34);
		Helper.assertSimilar(strings[1], t.getValue());
		scene = t.getSceneNumber();
		assertEquals(34, scene);

		try {
			t.setValue(false, 64);
			fail("scene number too big");
		}
		catch (final Exception e) {
			// fine
		}

		try {
			t.setValue(false, -1);
			fail("scene number negative");
		}
		catch (final Exception e) {
			// fine
		}
	}
}

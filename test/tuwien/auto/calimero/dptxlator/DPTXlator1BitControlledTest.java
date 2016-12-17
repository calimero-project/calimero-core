/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2016 B. Malinowsky

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
import java.util.Map;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class DPTXlator1BitControlledTest extends TestCase
{
	private DPTXlator1BitControlled t;
	private DPTXlator1BitControlled t7;
	private final DPT step = DPTXlator1BitControlled.DPT_STEP_CONTROL;

	private DPT[] dpts;

	private final boolean[] control = { false, true, true, false};
	private final boolean[] values = { true, false, true, false };
	private final String[] strings = { "0 TRUE", "1 false", "1 true", "0 FALSE" };
	private final byte[] data = { 1, 2, 3, 0 };

	private final byte[] dataValue1 = { 0, 0, 1 };

	private final byte[] stepData = { 0xF, 0x1, 0x3, };

	/**
	 * @param name
	 */
	public DPTXlator1BitControlledTest(final String name)
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
		t = new DPTXlator1BitControlled(DPTXlator1BitControlled.DPT_BOOL_CONTROL);
		t7 = new DPTXlator1BitControlled(step);
		dpts = t.getSubTypes().values().toArray(new DPT[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator#setValues
	 * (java.lang.String[])}.
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

		final String[] s = { "1 true" };
		t.setValues(s);
		assertEquals(s.length, t.getItems());
		Helper.assertSimilar(s, t.getAllValues());

		t.setValues(new String[] { t.getValue(), t.getValue() });
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#getAllValues()}.
	 */
	public void testGetAllValues()
	{
		assertEquals(1, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar("false", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValue(control[0], values[0]);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(strings[0], t.getAllValues()[0]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#setData(byte[], int)}.
	 */
	public void testSetDataByteArrayInt()
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
		assertEquals(3, d[0]);
		assertEquals(3, d[1]);
		assertTrue(t.getValueBit());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#getData(byte[], int)}.
	 */
	public void testGetDataByteArrayInt()
	{
		assertEquals(4, t7.getData(new byte[4], 1).length);
		final byte[] empty = new byte[4];
		assertTrue(Arrays.equals(empty, t7.getData(new byte[4], 1)));

		t7.setData(stepData);
		byte[] d = new byte[10];
		final byte pattern = (byte) 0xAA;
		Arrays.fill(d, pattern);
		t7.getData(d, 5);
		for (int i = 0; i < 5; i++)
			assertEquals(pattern, d[i]);
		for (int i = 5; i < 8; i++) {
			assertEquals((byte) (0xA8 | (stepData[i - 5] & 0x3)), d[i]);
		}
		for (int i = 8; i < 10; i++)
			assertEquals(pattern, d[i]);

		final boolean[] ctrls = { true, false, true, };
		final boolean[] values = { true, true, true, };
		for (int i = 0; i < ctrls.length; i++) {
			t7.setValue(ctrls[i], values[i]);
			d = t7.getData(new byte[1 + i], i);
			assertEquals(stepData[i] & 0x3, d[i]);
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#getSubTypes()}.
	 */
	public void testGetSubTypes()
	{
		final Map<String, DPT> types = t.getSubTypes();
		assertEquals(12, types.size());
	}

	/**
	 * Test method for {@link DPTXlator1BitControlled#DPTXlator1BitControlled(tuwien.auto.calimero.dptxlator.DPT)}.
	 */
	public void testDPTXlator1BitControlledDPT()
	{
		Helper.checkDPTs(dpts, false);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#setValue(boolean, boolean)}.
	 */
	public void testSetValueBooleanBoolean()
	{
		assertEquals(false, t.getControlBit());
		assertEquals(false, t.getValueBit());

		t.setValue(true, false);
		assertEquals(true, t.getControlBit());
		assertEquals(false, t.getValueBit());

		t.setValue(false, true);
		assertEquals(false, t.getControlBit());
		assertEquals(true, t.getValueBit());

		t.setValue(true, true);
		assertEquals(true, t.getControlBit());
		assertEquals(true, t.getValueBit());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#setControlBit(boolean)}.
	 * @throws KNXFormatException
	 */
	public void testSetControlBit() throws KNXFormatException
	{
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		t7.setValue("0 increase");
		assertFalse(t7.getControlBit());
		boolean b = true;
		assertEquals(b, t7.getValueBit());
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		assertEquals(b, t7.getValueBit());
		b = false;
		t7.setValueBit(b);
		t7.setControlBit(false);
		assertFalse(t7.getControlBit());
		assertEquals(b, t7.getValueBit());
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		assertEquals(b, t7.getValueBit());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#getControlBit()}.
	 * @throws KNXFormatException
	 */
	public void testGetControlBit() throws KNXFormatException
	{
		assertFalse(t7.getControlBit());
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		t7.setValue("0 increase");
		assertFalse(t7.getControlBit());
		t7.setData(data, 2);
		assertTrue(t7.getControlBit());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled#getValueBit()}.
	 * @throws KNXFormatException
	 */
	public void testGetValueBit() throws KNXFormatException
	{
		assertFalse(t7.getValueBit());
		t7.setControlBit(true);
		assertFalse(t7.getValueBit());
		t7.setValueBit(true);
		assertTrue(t7.getValueBit());
		t7.setValue("0 increase");
		assertTrue(t7.getValueBit());
		t7.setData(data, 2);
		assertTrue(t7.getValueBit());
	}

}

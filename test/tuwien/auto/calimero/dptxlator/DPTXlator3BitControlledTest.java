/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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
import tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled.DPT3BitControlled;

/**
 * @author B. Malinowsky
 */
public class DPTXlator3BitControlledTest extends TestCase
{
	// translator for DPT 3.007 dimming
	private DPTXlator3BitControlled t7;
	// translator for DPT 3.008 blinds
	private DPTXlator3BitControlled t8;

	private final DPT dim = DPTXlator3BitControlled.DPT_CONTROL_DIMMING;
	private final DPT dimCtrl =
		((DPT3BitControlled) DPTXlator3BitControlled.DPT_CONTROL_DIMMING).getControlDPT();
	private final DPT blind = DPTXlator3BitControlled.DPT_CONTROL_BLINDS;
	private final DPT blindCtrl =
		((DPT3BitControlled) DPTXlator3BitControlled.DPT_CONTROL_BLINDS).getControlDPT();

	private final String dimValue1 = dimCtrl.getLowerValue().toUpperCase() + " " + 1;
	private final String dimValue2 = dim.getUpperValue();
	private final String dimValue3 = dimCtrl.getLowerValue().toLowerCase() + " " + 3;
	private final String[] dims = { dimValue2, dimValue1, dimValue3 };
	private final byte[] dimData = { 0xF, 0x1, 0x3, };

	private final String blindValueBr = blindCtrl.getLowerValue().toUpperCase() + " break";
	private final String blindValue1 = blindCtrl.getLowerValue().toUpperCase() + " " + 1;
	private final String blindValue2 = blind.getUpperValue();
	private final String blindValue3 = blindCtrl.getUpperValue() + " " + 5;
	private final String[] blinds = { blindValue2, blindValue1, blindValue3 };
	private final byte[] blindData = { 0xF, 0x1, 0x0D, };
	private final byte[] dataBlindValue3 = { 0x0D };

	// for all DPTs: offset = 2, items: value2, valueBreak, value1,
	private final byte[] data = { 0, 0, 0x0F, 0x0, 0x1, };

	/**
	 * @param name name of test case
	 */
	public DPTXlator3BitControlledTest(final String name)
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
		t7 = new DPTXlator3BitControlled(dim);
		t8 = new DPTXlator3BitControlled(blind);
	}

	/**
	 * Test method for {@link DPTXlator3BitControlled#DPTXlator3BitControlled(DPT)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testDPTXlator3BitControlled() throws KNXFormatException
	{
		new DPTXlator3BitControlled(DPTXlator3BitControlled.DPT_CONTROL_BLINDS);
		new DPTXlator3BitControlled(DPTXlator3BitControlled.DPT_CONTROL_DIMMING);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#setValues(java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t7.setValues(dims);
		assertEquals(dims.length, t7.getItems());
		Helper.assertSimilar(dims, t7.getAllValues());

		t7.setValues(new String[0]);
		assertEquals(dims.length, t7.getItems());
		Helper.assertSimilar(dims, t7.getAllValues());

		final String[] s = { dimValue1 };
		t7.setValues(s);
		assertEquals(s.length, t7.getItems());
		Helper.assertSimilar(s, t7.getAllValues());

		t7.setValues(new String[] { t7.getValue(), t7.getValue() });
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#getAllValues()}.
	 */
	public final void testGetAllValues()
	{
		assertEquals(1, t8.getItems());
		assertEquals(t8.getItems(), t8.getItems());
		Helper.assertSimilar(blindValueBr, t8.getAllValues()[0]);

		t8.setData(blindData);
		assertEquals(blindData.length, t8.getItems());
		assertEquals(t8.getItems(), t8.getItems());
		Helper.assertSimilar(blinds, t8.getAllValues());

		t8.setValue(true, 7);
		assertEquals(1, t8.getItems());
		Helper.assertSimilar(blindValue2, t8.getAllValues()[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#setValue
	 * (java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueString() throws KNXFormatException
	{
		t7.setValue(DPTXlator3BitControlled.DPT_CONTROL_DIMMING.getLowerValue());
		t7.setValue(DPTXlator3BitControlled.DPT_CONTROL_DIMMING.getUpperValue());
		t8.setValue(DPTXlator3BitControlled.DPT_CONTROL_BLINDS.getLowerValue());
		t8.setValue(DPTXlator3BitControlled.DPT_CONTROL_BLINDS.getUpperValue());

		t7.setValue(dimValue3);
		Helper.assertSimilar(dimValue3, t7.getValue());
		final String s = t7.getValue();
		t7.setValue(s);
		Helper.assertSimilar(dimValue3, t7.getValue());
		assertEquals(s, t7.getValue());

		t8.setValue(blindValueBr);

		// check tolerant whitespace behavior
		t7.setValue(" " + dim.getUpperValue() + "    " + 7);
		t7.setValue("\t" + dim.getUpperValue() + "\t" + 3 + "\t");

		setValueFail(t7, "start 6");
		setValueFail(t8, "upp 2");
		setValueFail(t7, "increase 8");
		setValueFail(t8, "up -1");
	}

	private void setValueFail(final DPTXlator3BitControlled t, final String value)
	{
		try {
			t.setValue(value);
			fail("invalid ctrl");
		}
		catch (final KNXFormatException e) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
	{
		t8.setData(blindData, 0);
		try {
			t8.setData(new byte[] {}, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertTrue(Arrays.equals(blindData, t8.getData()));
		t8.setData(dataBlindValue3);
		byte[] d = t8.getData();
		assertEquals(1, d.length);
		assertEquals(d[0], dataBlindValue3[0]);

		t8.setData(data, 2);
		d = t8.getData();
		assertEquals(data.length - 2, d.length);
		for (int i = 0; i < d.length; i++) {
			assertEquals(data[i + 2], d[i]);
		}
		Helper.assertSimilar(new String[] { blindValue2, blindValueBr, blindValue1 }, t8
			.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#getData(byte[], int)}.
	 */
	public final void testGetDataByteArrayInt()
	{
		assertEquals(4, t7.getData(new byte[4], 1).length);
		final byte[] empty = new byte[4];
		assertTrue(Arrays.equals(empty, t7.getData(new byte[4], 1)));

		t7.setData(dimData);
		byte[] d = new byte[10];
		final byte pattern = (byte) 0xAA;
		Arrays.fill(d, pattern);
		t7.getData(d, 5);
		for (int i = 0; i < 5; i++)
			assertEquals(pattern, d[i]);
		for (int i = 5; i < 8; i++)
			assertEquals((byte) (0xA0 | dimData[i - 5]), d[i]);
		for (int i = 8; i < 10; i++)
			assertEquals(pattern, d[i]);

		final boolean[] ctrls = { true, false, false, };
		final int[] values = { 7, 1, 3, };
		for (int i = 0; i < ctrls.length; i++) {
			t7.setValue(ctrls[i], values[i]);
			d = t7.getData(new byte[1 + i], i);
			assertEquals(dimData[i], d[i]);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#getSubTypes()}.
	 */
	public final void testGetSubTypes()
	{
		assertEquals(2, t7.getSubTypes().size());
		t7.getSubTypes().remove(dim.getID());
		assertEquals(1, t7.getSubTypes().size());
		assertEquals(1, t8.getSubTypes().size());

		t8.getSubTypes().put(blind.getID(), blind);
		assertEquals(1, t7.getSubTypes().size());
		assertEquals(1, t8.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t7.getSubTypes().put(dim.getID(), dim);
		assertEquals(2, t7.getSubTypes().size());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#getControlBit()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetControlBit() throws KNXFormatException
	{
		assertFalse(t7.getControlBit());
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		t7.setValue(dimValue3);
		assertFalse(t7.getControlBit());
		t7.setData(data, 2);
		assertTrue(t7.getControlBit());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#setControlBit(boolean)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetControlBit() throws KNXFormatException
	{
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		t7.setValue(dimValue3);
		assertFalse(t7.getControlBit());
		assertEquals(3, t7.getStepCode());
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		assertEquals(3, t7.getStepCode());
		t7.setStepCode(7);
		t7.setControlBit(false);
		assertFalse(t7.getControlBit());
		assertEquals(7, t7.getStepCode());
		t7.setControlBit(true);
		assertTrue(t7.getControlBit());
		assertEquals(7, t7.getStepCode());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#getStepCode()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetStepCode() throws KNXFormatException
	{
		assertEquals(0, t8.getStepCode());
		t8.setData(blindData);
		assertEquals(7, t8.getStepCode());
		t8.setStepCode(4);
		assertEquals(4, t8.getStepCode());
		t8.setValues(blinds);
		assertEquals(7, t8.getStepCode());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#getIntervals()}.
	 */
	public final void testGetIntervals()
	{
		assertEquals(0, t8.getIntervals());
		t8.setStepCode(7);
		assertEquals(64, t8.getIntervals());
		t8.setStepCode(2);
		assertEquals(2, t8.getIntervals());
		t8.setValue(false, 3);
		assertEquals(4, t8.getIntervals());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#setIntervals(int)}.
	 */
	public final void testSetIntervals()
	{
		try {
			t8.setIntervals(0);
			fail("invalid value");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			t8.setIntervals(65);
			fail("invalid value");
		}
		catch (final KNXIllegalArgumentException e) {}
		t8.setIntervals(60);
		assertEquals(7, t8.getStepCode());
		assertEquals(64, t8.getIntervals());
		t8.setIntervals(48);
		assertEquals(6, t8.getStepCode());
		assertEquals(32, t8.getIntervals());
		t8.setIntervals(24);
		assertEquals(5, t8.getStepCode());
		assertEquals(16, t8.getIntervals());
		t8.setIntervals(1);
		assertEquals(1, t8.getStepCode());
		assertEquals(1, t8.getIntervals());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled#setStepCode(int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetStepCode() throws KNXFormatException
	{
		try {
			t8.setStepCode(8);
			fail("invalid value");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			t8.setStepCode(-1);
			fail("invalid value");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertFalse(t8.getControlBit());
		t8.setStepCode(1);
		assertEquals(1, t8.getStepCode());
		assertFalse(t8.getControlBit());
		t8.setStepCode(7);
		assertEquals(7, t8.getStepCode());
		assertFalse(t8.getControlBit());
		t8.setValue(blindValue3);
		assertTrue(t8.getControlBit());
		t8.setStepCode(2);
		assertTrue(t8.getControlBit());
		assertEquals(2, t8.getStepCode());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getValue()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValue() throws KNXFormatException
	{
		Helper.assertSimilar("decrease break", t7.getValue());
		Helper.assertSimilar("up break", t8.getValue());
		t7.setValues(new String[0]);
		Helper.assertSimilar("decrease break", t7.getValue());

		t7.setValue(false, 3);
		Helper.assertSimilar(dimValue3, t7.getValue());
		t7.setData(data, 2);
		Helper.assertSimilar(dimValue2, t7.getValue());
		t8.setData(dataBlindValue3);
		Helper.assertSimilar(blindValue3, t8.getValue());
	}

	/**
	 * Test method for {@link DPTXlator3BitControlled#setValue(int)}.
	 */
	public final void testSetValueInt()
	{
		t7.setValue(3);
		assertEquals(3, t7.getValueSigned());
		assertEquals(3, t7.getStepCode());
		assertTrue(t7.getControlBit());
		t7.setValue(-3);
		assertEquals(-3, t7.getValueSigned());
		assertFalse(t7.getControlBit());

		t8.setValue(3);
		assertEquals(3, t8.getValueSigned());
		assertTrue(t8.getControlBit());
		t8.setValue(-3);
		assertEquals(-3, t8.getValueSigned());
		assertFalse(t8.getControlBit());

		t8.setValue(0);
		assertEquals(0, t8.getValueSigned());
		assertTrue(t8.getControlBit());
		try {
			t8.setValue(8);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertEquals(0, t8.getValueSigned());
		try {
			t8.setValue(-8);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertEquals(0, t8.getValueSigned());

		t7.setControlBit(false);
		t7.setStepCode(5);
		assertEquals(-5, t7.getValueSigned());
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2018 B. Malinowsky

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
public class DPTXlator2ByteUnsignedTest extends TestCase
{
	private DPTXlator2ByteUnsigned t;

	private final String min = "0";
	private final String max = "65535";
	private final String value1 = "735";
	private final String value2 = "54732";
	private final String[] strings = { value1, min, value2, max, };
	private final int[] ints = { 735, 0, 54732, 65535, };
	private final byte[] dataMin = { 0, 0 };
	private final byte[] dataMax = { (byte) 0xff, (byte) 0xff };
	private final byte[] dataValue1 = { 2, (byte) 0xDF };
	private final byte[] dataValue2 = { 0, 0, (byte) 0xd5, (byte) 0xcc, 0 };
	private final byte[] data =
		{ 2, (byte) 0xDF, 0, 0, (byte) 0xd5, (byte) 0xcc, (byte) 0xff, (byte) 0xff };

	private final DPT[] dpts ={
		DPTXlator2ByteUnsigned.DPT_VALUE_2_UCOUNT,
		DPTXlator2ByteUnsigned.DPT_PROP_DATATYPE,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_10,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_100,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_SEC,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_MIN,
		DPTXlator2ByteUnsigned.DPT_TIMEPERIOD_HOURS,
		DPTXlator2ByteUnsigned.DPT_ELECTRICAL_CURRENT,
		DPTXlator2ByteUnsigned.DPT_BRIGHTNESS,
		DPTXlator2ByteUnsigned.DPT_LENGTH,
		DPTXlator2ByteUnsigned.DPT_ABSOLUTE_COLOR_TEMPERATURE
	};

	/**
	 * @param name name of test case
	 */
	public DPTXlator2ByteUnsignedTest(final String name)
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
		t = new DPTXlator2ByteUnsigned(dpts[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#setValues(java.lang.String[])}.
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
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar("0", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length / 2, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(strings, t.getAllValues());

		t.setValue(5000);
		assertEquals(1, t.getItems());
		Helper.assertSimilar("5000", t.getAllValues()[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#setValue(java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueString() throws KNXFormatException
	{
		t.setValue(value1);
		Helper.assertSimilar(value1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		Helper.assertSimilar(value1, t.getValue());
		assertEquals(s, t.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#getValue()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValue() throws KNXFormatException
	{
		Helper.assertSimilar("0", t.getValue());
		t.setValues(new String[0]);
		Helper.assertSimilar("0", t.getValue());
		t.setValue(ints[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(value2, t.getValue());
		t.setData(data);
		Helper.assertSimilar(value1, t.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
	{
		t.setData(dataMin, 0);
		try {
			t.setData(new byte[] {}, 0);
		fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertTrue(Arrays.equals(dataMin, t.getData()));
		t.setData(dataValue2, 2);
		byte[] d = t.getData();
		assertEquals(2, d.length);
		assertEquals(d[0], dataValue2[2]);
		assertEquals(d[1], dataValue2[3]);

		final byte[] array = new byte[data.length + 4];
		System.arraycopy(data, 0, array, 3, data.length);
		t.setData(array, 3);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertTrue(Arrays.equals(data, d));
		Helper.assertSimilar(strings, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#getData(byte[], int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetDataByteArrayInt() throws KNXFormatException
	{
		assertEquals(4, t.getData(new byte[4], 1).length);
		final byte[] empty = new byte[4];
		assertTrue(Arrays.equals(empty, t.getData(new byte[4], 1)));

		t.setData(data);
		byte[] d = t.getData(new byte[20], 10);
		for (int i = 0; i < 10; i++)
			assertEquals(0, d[i]);
		for (int i = 10; i < 18; i++)
			assertEquals(data[i - 10], d[i]);
		for (int i = 18; i < 20; i++)
			assertEquals(0, d[i]);

		for (int i = 0; i < ints.length; i++) {
			t.setValue(ints[i]);
			d = t.getData(new byte[2 + i], i);
			assertEquals(data[2 * i], d[i]);
			assertEquals(data[2 * i + 1], d[i + 1]);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#getSubTypes()}.
	 */
	public final void testGetSubTypes()
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#DPTXlator2ByteUnsigned
	 * (tuwien.auto.calimero.dptxlator.DPT)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testDPTXlator2ByteUnsignedDPT() throws KNXFormatException
	{
		Helper.checkDPTs(dpts, true);
		for (int i = 0; i < dpts.length; i++) {
			setValueIntFail(new DPTXlator2ByteUnsigned(dpts[i]), Integer.parseInt(dpts[i]
				.getLowerValue()) - 1);
			setValueIntFail(new DPTXlator2ByteUnsigned(dpts[i]), Integer.parseInt(dpts[i]
				.getUpperValue()) + 1);
		}
	}

	private void setValueIntFail(final DPTXlator2ByteUnsigned tr, final int v)
	{
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#getValueUnsigned()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValueUnsigned() throws KNXFormatException
	{
		assertEquals(0, t.getValueUnsigned());

		t.setData(dataValue1);
		assertEquals(ints[0], t.getValueUnsigned());
		t.setData(dataMin);
		assertEquals(ints[1], t.getValueUnsigned());
		t.setData(dataValue2, 2);
		assertEquals(ints[2], t.getValueUnsigned());
		t.setData(dataMax);
		assertEquals(ints[3], t.getValueUnsigned());

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(ints[i], t.getValueUnsigned());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#setValue(int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueInt() throws KNXFormatException
	{
		for (int i = 0; i < ints.length; i++) {
			t.setValue(ints[i]);
			assertEquals(ints[i], t.getValueUnsigned());
			Helper.assertSimilar(strings[i], t.getValue());
		}
		// check 10ms and 100ms timeperiods
		DPTXlator2ByteUnsigned tr = new DPTXlator2ByteUnsigned(dpts[3]);
		tr.setValue(655350);
		assertEquals(655350, tr.getValueUnsigned());
		Helper.assertSimilar("655350", tr.getValue());
		// round up
		tr.setValue(337777);
		assertEquals(337780, tr.getValueUnsigned());
		Helper.assertSimilar("337780", tr.getValue());
		// round off
		tr.setValue(332222);
		assertEquals(332220, tr.getValueUnsigned());
		Helper.assertSimilar("332220", tr.getValue());

		tr = new DPTXlator2ByteUnsigned(dpts[4]);
		tr.setValue(6553500);
		assertEquals(6553500, tr.getValueUnsigned());
		Helper.assertSimilar("6553500", tr.getValue());
		// round up
		tr.setValue(3377751);
		assertEquals(3377800, tr.getValueUnsigned());
		Helper.assertSimilar("3377800", tr.getValue());
		// round off
		tr.setValue(3322249);
		assertEquals(3322200, tr.getValueUnsigned());
		Helper.assertSimilar("3322200", tr.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned#setTimePeriod(long)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetTimePeriod() throws KNXFormatException
	{
		final long[] max =
			{ 65535L, 655350L, 6553500L, 65535000L, 65535L * 60000, 65535L * 3600000, };
		for (int i = 2; i < 8; i++) {
			final DPTXlator2ByteUnsigned tr = new DPTXlator2ByteUnsigned(dpts[i]);
			tr.setTimePeriod(max[i - 2]);
			if (i == 3)
				assertEquals("i=" + i, 655350, tr.getValueUnsigned());
			else if (i == 4)
				assertEquals("i=" + i, 6553500, tr.getValueUnsigned());
			else
				assertEquals("i=" + i, 65535, tr.getValueUnsigned());
		}
	}

}

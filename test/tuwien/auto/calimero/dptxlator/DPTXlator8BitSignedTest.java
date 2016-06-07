/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class DPTXlator8BitSignedTest extends TestCase
{
	private DPTXlator8BitSigned t;

	private final String min = "-128";
	private final String max = "127";
	private final String value1 = "-13";
	private final String value2 = "89";
	private final String[] strings = { value1, min, value2, };
	private final int[] values = { -13, 0, 89, };

	private final byte[] dataMin = { -128, };
	private final byte[] dataMax = { 127, };
	private final byte[] dataValue1 = { 34 };
	private final byte[] dataValue2 = { 0, 0, 127, };
	private final int[] valuesRaw = { 127, 34, -128, 127 };
	private final String[] stringsRaw = { "127", "34", "-128", "127" };
	private final byte[] data = { 127, 34, -128, 127, };

	private final DPT[] dpts = {
		DPTXlator8BitSigned.DPT_PERCENT_V8,
		DPTXlator8BitSigned.DPT_VALUE_1_UCOUNT };

	private final DPT sm = DPTXlator8BitSigned.DPT_STATUS_MODE3;

	/**
	 * @param name name of test case
	 */
	public DPTXlator8BitSignedTest(final String name)
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
		TranslatorTypes.createTranslator(0, "6.001");
		t = new DPTXlator8BitSigned(dpts[1]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#setValues(java.lang.String[])}.
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		Helper.assertSimilar("0", t.getAllValues()[0]);

		t.setData(data);
		assertEquals(data.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		Helper.assertSimilar(stringsRaw, t.getAllValues());

		t.setValue(values[0]);
		assertEquals(1, t.getItems());
		Helper.assertSimilar(strings[0], t.getAllValues()[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#setValue(java.lang.String)}.
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

		final DPTXlator8BitSigned t2 = new DPTXlator8BitSigned(sm);
		// we basically don't care about the status mode separators
		t2.setValue("0:0:0:0:0-1");

		try {
			t2.setValue("0/0/0/0 0");
			fail("wrong status mode");
		}
		catch (final KNXFormatException expected) {}
		try {
			t2.setValue("0/0/0/0/0/0 0");
			fail("wrong status mode");
		}
		catch (final KNXFormatException expected) {}
		try {
			t2.setValue("0/0/0/0/2 0");
			fail("wrong status mode");
		}
		catch (final KNXFormatException expected) {}
		try {
			t2.setValue("0/0/0/0/0 3");
			fail("wrong status mode");
		}
		catch (final KNXFormatException expected) {}
		try {
			t2.setValue("0/2/0/0/0 1");
			fail("wrong status mode");
		}
		catch (final KNXFormatException expected) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getValue()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValue() throws KNXFormatException
	{
		Helper.assertSimilar("0", t.getValue());
		t.setValues(new String[0]);
		Helper.assertSimilar("0", t.getValue());
		t.setValue(values[0]);
		Helper.assertSimilar(strings[0], t.getValue());
		t.setData(dataValue2, 2);
		Helper.assertSimilar(stringsRaw[3], t.getValue());
		t.setData(data);
		Helper.assertSimilar(max, t.getValue());

		final DPTXlator8BitSigned t2 = new DPTXlator8BitSigned(sm);
		assertEquals("0/0/0/0/0 0", t2.getValue());

		t2.setValue(DPTXlator8BitSigned.DPT_STATUS_MODE3.getUpperValue());
		assertEquals(DPTXlator8BitSigned.DPT_STATUS_MODE3.getUpperValue(), t2.getValue());
		t2.setValue(DPTXlator8BitSigned.DPT_STATUS_MODE3.getLowerValue());
		assertEquals(DPTXlator8BitSigned.DPT_STATUS_MODE3.getLowerValue(), t2.getValue());
		t2.setStatusMode(false, true, true, false, true, 1);
		assertEquals("0/1/1/0/1 1", t2.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
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
		Helper.assertSimilar(stringsRaw, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getData(byte[], int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetDataByteArrayInt() throws KNXFormatException
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

		for (int i = 0; i < valuesRaw.length; i++) {
			t.setValue(valuesRaw[i]);
			d = t.getData(new byte[2 + i], i);
			assertEquals(data[i], d[i]);
			assertEquals(0, d[i + 1]);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getSubTypes()}.
	 */
	public final void testGetSubTypes()
	{
		// the status-mode DPT is excluded, because of its different interpretation
		assertEquals(dpts.length, t.getSubTypes().size() - 1);
		t.getSubTypes().remove(dpts[0].getID());
		assertEquals(dpts.length, t.getSubTypes().size());
		// type map is static member in translator, so add DPT again
		t.getSubTypes().put(dpts[0].getID(), dpts[0]);
		assertEquals(dpts.length, t.getSubTypes().size() - 1);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#DPTXlator8BitSigned
	 * (tuwien.auto.calimero.dptxlator.DPT)}.
	 */
	public final void testDPTXlator8BitSignedDPT()
	{
		Helper.checkDPTs(dpts, true);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getValueSigned()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValueSigned() throws KNXFormatException
	{
		assertEquals(0, t.getValueSigned());

		t.setData(dataMax);
		assertEquals(valuesRaw[0], t.getValueSigned());
		t.setData(dataValue1);
		assertEquals(valuesRaw[1], t.getValueSigned());
		t.setData(dataMin);
		assertEquals(valuesRaw[2], t.getValueSigned());
		t.setData(dataValue2, 2);
		assertEquals(valuesRaw[3], t.getValueSigned());

		for (int i = 0; i < stringsRaw.length; i++) {
			t.setValue(stringsRaw[i]);
			assertEquals(valuesRaw[i], t.getValueSigned());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getNumericValue()}.
	 */
	public final void testGetNumericValue()
	{
		t.setData(new byte[] { (byte) 0xff}, 0);
		assertEquals(-1, (int) t.getNumericValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#setValue(int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueInt() throws KNXFormatException
	{
		for (int i = 0; i < dpts.length; i++) {
			setValueIntFail(new DPTXlator8BitSigned(dpts[i]),
					Integer.parseInt(dpts[i].getLowerValue()) - 1);
			setValueIntFail(new DPTXlator8BitSigned(dpts[i]),
					Integer.parseInt(dpts[i].getUpperValue()) + 1);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#setStatusMode
	 * (boolean, boolean, boolean, boolean, boolean, int))}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetStatusModeBooleanBooleanBooleanBooleanBooleanInt()
		throws KNXFormatException
	{
		try {
			t.setStatusMode(true, true, true, true, true, 2);
			fail("wrong xlator");
		}
		catch (final KNXIllegalStateException expected) {}

		final DPTXlator8BitSigned t2 = new DPTXlator8BitSigned(sm);
		assertEquals(0, t2.getMode());
		t2.setStatusMode(true, true, true, true, true, 2);
		assertEquals(2, t2.getMode());
		assertEquals((byte) 0xfc, t2.getValueSigned());

		t2.setStatusMode(false, false, false, false, false, 0);
		assertEquals(0, t2.getMode());
		assertEquals(1, t2.getValueSigned());

		try {
			t2.setStatusMode(true, true, true, true, true, -1);
			fail("out of range");
		}
		catch (final KNXIllegalArgumentException expected) {}
		try {
			t2.setStatusMode(true, true, true, true, true, 3);
			fail("out of range");
		}
		catch (final KNXIllegalArgumentException expected) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned#getMode()}.
	 */
	public final void testGetMode()
	{
		try {
			t.getMode();
			fail("wrong xlator");
		}
		catch (final KNXIllegalStateException expected) {}
	}

	private void setValueIntFail(final DPTXlator8BitSigned tr, final int v)
	{
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) { }
	}
}

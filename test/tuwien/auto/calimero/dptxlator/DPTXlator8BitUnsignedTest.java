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

/**
 * @author B. Malinowsky
 */
public class DPTXlator8BitUnsignedTest extends TestCase
{
	private DPTXlator8BitUnsigned t;

	// scaled values/data
	private final String min = "0";
	private final String max = "255";
	private final String maxScale = "100";
	private final String maxAngle = "360";
	private final String value1 = "13";
	private final String value2 = "89";
	private final String[] strings = { value1, min, value2, };
	private final int[] values = { 13, 0, 89, };

	// unscaled values/data
	private final byte[] dataMin = { 0, };
	private final byte[] dataMax = { (byte) 0xff, };
	private final byte[] dataValue1 = { 34 };
	private final byte[] dataValue2 = { 0, 0, (byte) 128, };
	private final int[] valuesRaw = { 255, 34, 0, 128 };
	private final String[] stringsRaw = { "255", "34", "0", "128" };
	private final byte[] data = { (byte) 0xff, 34, 0, (byte) 128, };

	private final DPT[] dpts = {
		DPTXlator8BitUnsigned.DPT_SCALING, DPTXlator8BitUnsigned.DPT_ANGLE,
		DPTXlator8BitUnsigned.DPT_PERCENT_U8,
		DPTXlator8BitUnsigned.DPT_DECIMALFACTOR, DPTXlator8BitUnsigned.DPT_TARIFF,
		DPTXlator8BitUnsigned.DPT_VALUE_1_UCOUNT };

	/**
	 * @param name name of test case
	 */
	public DPTXlator8BitUnsignedTest(final String name)
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
		t = new DPTXlator8BitUnsigned(dpts[5]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#setValues(java.lang.String[])}.
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#getAllValues()}.
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#setValue(java.lang.String)}.
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#getValue()}.
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
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#setData(byte[], int)}.
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#getData(byte[], int)}.
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

		final DPTXlator8BitUnsigned x = new DPTXlator8BitUnsigned(dpts[4]);
		try {
			// reserved, shall not be used
			x.setValue(255);
			fail("255 is reserved, shall not be used for tariff");
		}
		catch (final Exception e) {
			// fine
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#getSubTypes()}.
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
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#DPTXlator8BitUnsigned
	 * (tuwien.auto.calimero.dptxlator.DPT)}.
	 */
	public final void testDPTXlator8BitUnsignedDPT()
	{
		Helper.checkDPTs(dpts, true);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#getValueUnsigned()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValueUnsigned() throws KNXFormatException
	{
		assertEquals(0, t.getValueUnsigned());

		t.setData(dataMax);
		assertEquals(valuesRaw[0], t.getValueUnsigned());
		t.setData(dataValue1);
		assertEquals(valuesRaw[1], t.getValueUnsigned());
		t.setData(dataMin);
		assertEquals(valuesRaw[2], t.getValueUnsigned());
		t.setData(dataValue2, 2);
		assertEquals(valuesRaw[3], t.getValueUnsigned());

		for (int i = 0; i < stringsRaw.length; i++) {
			t.setValue(stringsRaw[i]);
			assertEquals(valuesRaw[i], t.getValueUnsigned());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#getValueUnscaled()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValueShortUnscaled() throws KNXFormatException
	{
		assertEquals(0, t.getValueUnscaled());
		t.setData(dataMax);
		assertEquals(255, t.getValueUnscaled());
		t.setData(dataMin);
		assertEquals(0, t.getValueUnscaled());
		for (int i = 0; i < values.length; i++) {
			t.setValue(values[i]);
			assertEquals(values[i], t.getValueUnscaled());
		}
		final DPTXlator8BitUnsigned tscaled = new DPTXlator8BitUnsigned(dpts[0]);
		tscaled.setValue(maxScale);
		assertEquals(100, tscaled.getValueUnsigned());
		assertEquals(255, tscaled.getValueUnscaled());
		final int[] scaledRaw = { 33, 0, 227 };
		for (int i = 0; i < values.length; i++) {
			tscaled.setValue(values[i]);
			assertEquals(scaledRaw[i], tscaled.getValueUnscaled());
		}
		for (int i = 0; i < data.length; i++) {
			tscaled.setData(data, i);
			assertEquals(valuesRaw[i], tscaled.getValueUnscaled());
		}

		final DPTXlator8BitUnsigned tangle = new DPTXlator8BitUnsigned(dpts[1]);
		tangle.setValue(maxAngle);
		assertEquals(360, tangle.getValueUnsigned());
		assertEquals(255, tangle.getValueUnscaled());
		final int[] angleRaw = { 9, 0, 63 };
		for (int i = 0; i < values.length; i++) {
			tangle.setValue(values[i]);
			assertEquals(angleRaw[i], tangle.getValueUnscaled());
		}
		for (int i = 0; i < data.length; i++) {
			tangle.setData(data, i);
			assertEquals(valuesRaw[i], tangle.getValueUnscaled());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#setValue(int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueInt() throws KNXFormatException
	{
		for (int i = 0; i < dpts.length; i++) {
			setValueIntFail(new DPTXlator8BitUnsigned(dpts[i]), Integer.parseInt(dpts[i]
				.getLowerValue()) - 1);
			setValueIntFail(new DPTXlator8BitUnsigned(dpts[i]), Integer.parseInt(dpts[i]
				.getUpperValue()) + 1);
		}
	}

	private void setValueIntFail(final DPTXlator8BitUnsigned tr, final int v)
	{
		try {
			tr.setValue(v);
			fail("set value should fail: " + v);
		}
		catch (final KNXFormatException e) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned#setValueUnscaled(int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueUnscaled() throws KNXFormatException
	{
		t.setData(data);
		assertEquals(data.length, t.getItems());
		t.setValueUnscaled(0);
		assertEquals(1, t.getItems());

		for (int i = 0; i < valuesRaw.length; i++) {
			t.setValueUnscaled(valuesRaw[i]);
			assertEquals(valuesRaw[i], t.getValueUnscaled());
		}

		final DPTXlator8BitUnsigned tscaled = new DPTXlator8BitUnsigned(dpts[0]);
		tscaled.setValueUnscaled(255);
		assertEquals(100, tscaled.getValueUnsigned());
		assertEquals(255, tscaled.getValueUnscaled());
		Helper.assertSimilar(maxScale, tscaled.getValue());
		final int[] scaledRaw = { 33, 0, 227 };
		for (int i = 0; i < scaledRaw.length; i++) {
			tscaled.setValueUnscaled(scaledRaw[i]);
			assertEquals(values[i], tscaled.getValueUnsigned());
			Helper.assertSimilar(strings[i], tscaled.getValue());
		}
		for (int i = 0; i < valuesRaw.length; i++) {
			tscaled.setValueUnscaled(valuesRaw[i]);
			assertEquals(data[i], tscaled.getData()[0]);
			assertEquals(1, tscaled.getData().length);
		}

		final DPTXlator8BitUnsigned tangle = new DPTXlator8BitUnsigned(dpts[1]);
		tangle.setValueUnscaled(255);
		assertEquals(360, tangle.getValueUnsigned());
		assertEquals(255, tangle.getValueUnscaled());
		final int[] angleRaw = { 9, 0, 63 };
		for (int i = 0; i < angleRaw.length; i++) {
			tangle.setValueUnscaled(angleRaw[i]);
			assertEquals(values[i], tangle.getValueUnsigned());
			Helper.assertSimilar(strings[i], tangle.getValue());
		}
		for (int i = 0; i < valuesRaw.length; i++) {
			tangle.setValueUnscaled(valuesRaw[i]);
			assertEquals(data[i], tangle.getData()[0]);
			assertEquals(1, tscaled.getData().length);
		}
	}
}

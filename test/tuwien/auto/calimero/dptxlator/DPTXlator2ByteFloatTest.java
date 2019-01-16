/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2019 B. Malinowsky

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
public class DPTXlator2ByteFloatTest extends TestCase
{
	private DPTXlator2ByteFloat t;
	private final String min = "-671088.64";
	private final String max = "670760.96";
	private final String zero = "0.0";

	private final String value1 = "735.763";
	// the encoded value 1
	private final String value1Enc = "736.0";
	private final String value2 = "100.0";

	private final String[] strings = { min, max, zero, "736.0", value2 };
	private final double[] floats = { Double.parseDouble(min), Double.parseDouble(max),
		Double.parseDouble(zero), Double.parseDouble(value1), Double.parseDouble(value2), };

	private final byte[] dataMin = { (byte) 0xf8, 0, };
	private final byte[] dataMax = { 0x7f, -1 };
	private final byte[] dataZero = { 0, 0 };
	private final byte[] dataValue1 = { (byte) 0x34, (byte) 0x7e, };
	// 2 byte offset, 1 byte appended
	private final byte[] dataValue2 = { 0, 0, (byte) 0x1c, (byte) 0xe2, 0 };

	private final DPT[] dpts = { DPTXlator2ByteFloat.DPT_TEMPERATURE,
		DPTXlator2ByteFloat.DPT_TEMPERATURE_DIFFERENCE,
		DPTXlator2ByteFloat.DPT_TEMPERATURE_GRADIENT, DPTXlator2ByteFloat.DPT_INTENSITY_OF_LIGHT,
		DPTXlator2ByteFloat.DPT_WIND_SPEED, DPTXlator2ByteFloat.DPT_AIR_PRESSURE,
		DPTXlator2ByteFloat.DPT_HUMIDITY, DPTXlator2ByteFloat.DPT_AIRQUALITY, DPTXlator2ByteFloat.DPT_AIR_FLOW,
		DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE1, DPTXlator2ByteFloat.DPT_TIME_DIFFERENCE2,
		DPTXlator2ByteFloat.DPT_VOLTAGE, DPTXlator2ByteFloat.DPT_ELECTRICAL_CURRENT,
		DPTXlator2ByteFloat.DPT_POWERDENSITY, DPTXlator2ByteFloat.DPT_KELVIN_PER_PERCENT,
		DPTXlator2ByteFloat.DPT_POWER, DPTXlator2ByteFloat.DPT_VOLUME_FLOW,
		DPTXlator2ByteFloat.DPT_RAIN_AMOUNT, DPTXlator2ByteFloat.DPT_TEMP_F,
		DPTXlator2ByteFloat.DPT_WIND_SPEED_KMH };

	/**
	 * @param name name of test case
	 */
	public DPTXlator2ByteFloatTest(final String name)
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
		t = new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_RAIN_AMOUNT);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#setValues (java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t.setValues(new String[] {});
		assertEquals(1, t.getItems());
		assertEquals(0.0, t.getNumericValue(), 0);
		t.setValues(new String[] { min, max, zero, value1, value2, });
		assertEquals(5, t.getItems());
		assertEquals(-671088.64, t.getNumericValue(), 1.0);
		t.setValue(100);
		t.setValues(new String[] { t.getValue(), t.getValue() });
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		assertEquals(t.getItems(), t.getItems());
		assertEquals(0.0, t.getNumericValue(), 0);
		t.setValues(strings);
		final String[] returned = t.getAllValues();
		assertEquals(strings.length, returned.length);
		assertEquals(t.getItems(), returned.length);
		for (int i = 0; i < strings.length; ++i)
			assertTrue(returned[i].indexOf(strings[i]) >= 0);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#setValue (java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueString() throws KNXFormatException
	{
		t.setValue(value1);
		assertEquals(1, t.getItems());
		assertEquals(floats[3], t.getNumericValue(), 1);
		assertTrue(t.getValue().startsWith(value1Enc));

		t.setValue(t.getValue());
		assertTrue(t.getValue().startsWith(value1Enc));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#setData(byte[], int)}.
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
		byte[] data = t.getData();
		assertEquals(2, data.length);
		assertEquals(data[0], dataValue2[2]);
		assertEquals(data[1], dataValue2[3]);

		final byte[] array = { 0, dataMax[0], dataMax[1], dataValue1[0], dataValue1[1], 0 };
		t.setData(array, 1);
		data = t.getData();
		assertEquals(4, data.length);
		for (int i = 0; i < 4; ++i)
			assertEquals(array[i + 1], data[i]);
		Helper.assertSimilar(new String[] { max, value1Enc }, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#getData(byte[], int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetDataByteArrayInt() throws KNXFormatException
	{
		t.setData(dataValue2, 2);
		final byte[] data = t.getData(new byte[5], 2);
		assertEquals(5, data.length);
		assertTrue(Arrays.equals(dataValue2, data));

		try {
			// usable range too short
			t.getData(new byte[2], 1);
			fail("usable range too short");
		}
		catch (final KNXIllegalArgumentException expected) {}
		assertTrue(Arrays.equals(dataValue2, t.getData(new byte[5], 2)));
		assertNotNull(t.getData(new byte[0], 0));

		final byte[] array = { 0, dataValue1[0], dataValue1[1], dataMin[0], dataMin[1], 0 };
		t.setData(array, 1);
		assertTrue(Arrays.equals(array, t.getData(new byte[6], 1)));

		t.setValues(new String[] { value1, min });
		assertTrue(Arrays.equals(array, t.getData(new byte[6], 1)));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#getSubTypes()}.
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
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#DPTXlator2ByteFloat
	 * (tuwien.auto.calimero.dptxlator.DPT)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testDPTXlator2ByteFloatDPT() throws KNXFormatException
	{
		// do no similarity test because float type rounding issues
		Helper.checkDPTs(dpts, false);

		for (int i = 0; i < dpts.length; i++) {
			setValueFloatFail(new DPTXlator2ByteFloat(dpts[i]),
					Double.parseDouble(dpts[i].getLowerValue()) - 0.1d);
			setValueFloatFail(new DPTXlator2ByteFloat(dpts[i]),
					Double.parseDouble(dpts[i].getUpperValue()) + 0.1d);
		}

		final DPT dpt = new DPT("0.00", "invalid", "invalid", "invalid", "invalid");
		boolean failed = false;
		try {
			new DPTXlator2ByteFloat(dpt);
		}
		catch (final KNXFormatException e) {
			failed = true;
		}
		assertTrue(failed);
	}

	private void setValueFloatFail(final DPTXlator2ByteFloat tr, final double d)
	{
		try {
			tr.setValue(d);
			fail("set value should fail: " + d);
		}
		catch (final KNXFormatException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#getNumericValue()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetNumericValue() throws KNXFormatException
	{
		assertEquals(0.0, t.getNumericValue(), 0);

		t.setData(dataMin);
		assertEquals(floats[0], t.getNumericValue(), 1.0);
		t.setData(dataMax);
		assertEquals(floats[1], t.getNumericValue(), 1.0);
		t.setData(dataZero);
		assertEquals(floats[2], t.getNumericValue(), 0);
		t.setData(dataValue1);
		assertEquals(floats[3], t.getNumericValue(), 1.0);
		t.setData(dataValue2, 2);
		assertEquals(floats[4], t.getNumericValue(), 1.0);

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			assertEquals(floats[i], t.getNumericValue(), 1.0);
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat#setValue(double)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueDouble() throws KNXFormatException
	{
		for (int i = 0; i < floats.length; i++) {
			t.setValue(floats[i]);
			assertEquals(floats[i], t.getNumericValue(), 1.0);
			assertTrue(t.getValue().startsWith(strings[i]));
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#toString()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testToString() throws KNXFormatException
	{
		assertTrue(t.toString().indexOf("0.0") >= 0);
		t.setValues(strings);
		final String s = t.toString();
		for (int i = 0; i < strings.length; i++) {
			assertTrue(s.indexOf(strings[i]) >= 0);
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getValue()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValue() throws KNXFormatException
	{
		assertTrue(t.getValue().indexOf("0") >= 0);
		assertTrue(t.getValue().indexOf(t.getType().getUnit()) >= 0);

		t.setValue(265);
		final double d = t.getNumericValue();
		assertEquals(265, d, 1.0);
		final String s = String.valueOf(d);
		assertTrue(t.getValue().indexOf(s) >= 0);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getType()}.
	 */
	public final void testGetType()
	{
		assertEquals(t.getType(), dpts[17]);
	}
}

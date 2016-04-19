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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * Test for DPTXlatorTime.
 * <p>
 *
 * @author B. Malinowsky
 */
public class DPTXlatorTimeTest extends TestCase
{
	private DPTXlatorTime t;
	private final String[] values = new String[] { "mon, 00:00:00", "WED, 23:59:59", "  12:43:23  " };
	private final DPT dpt = DPTXlatorTime.DPT_TIMEOFDAY;

	// wed, 22:33:44, offset 1
	private final byte[] data = new byte[] { 0, 2 << 5 | 22, 33, 44, -1, -1 };

	/**
	 * @param name
	 */
	public DPTXlatorTimeTest(final String name)
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
		t = new DPTXlatorTime(DPTXlatorTime.DPT_TIMEOFDAY);
		// reset to default to not interfere with tests
		DPTXlatorTime.useValueFormat(null);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#setValues(java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t.setValues(values);
		assertEquals(3, t.getItems());
		assertEquals(0, t.getHour());
		assertEquals(0, t.getMinute());
		assertEquals(0, t.getSecond());
		assertEquals(1, t.getDayOfWeek());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getAllValues().length);
		Helper.assertSimilar("no-day, 00:00:00", t.getAllValues()[0]);
		t.setValues(values);
		Assert.assertEquals(values.length, t.getAllValues().length);
		final String[] all = t.getAllValues();
		for (int i = 0; i < values.length; i++)
			Assert.assertTrue(all[i].toLowerCase()
				.indexOf(values[i].toLowerCase().trim()) > -1);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#setValue(java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueString() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		t.setValue(dpt.getLowerValue());
		assertEquals(1, t.getItems());
		assertEquals(0, t.getHour());
		assertEquals(0, t.getMinute());
		assertEquals(0, t.getSecond());
		assertEquals(0, t.getDayOfWeek());

		t.setValue(dpt.getUpperValue());
		assertEquals(1, t.getItems());
		assertEquals(23, t.getHour());
		assertEquals(59, t.getMinute());
		assertEquals(59, t.getSecond());
		assertEquals(7, t.getDayOfWeek());

		try {
			t.setValue("wed 23:24");
			fail("not valid");
		}
		catch (final KNXFormatException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
	{
		t.setData(data, 1);
		assertEquals(1, t.getItems());
		assertEquals(22, t.getHour());
		assertEquals(33, t.getMinute());
		assertEquals(44, t.getSecond());
		assertEquals(2, t.getDayOfWeek());

		// on last value we use a reserved bit and therefore will see a warning
		final byte[] d = new byte[] { 1 << 5 | 4, 5, 6, 2 << 5 | 7, 8, 9,
			10, 1 << 6 | 11, 12 };
		t.setData(d, 0);
		assertEquals(3, t.getItems());
		Helper.assertSimilar(new String[] { "mon, 04:05:06", "tue, 07:08:09",
			"no-day, 10:11:12" }, t.getAllValues());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#getData(byte[], int)}.
	 */
	public final void testGetDataByteArrayInt()
	{
		byte[] d = t.getData(new byte[5], 2);
		for (int i = 0; i < d.length; ++i)
			assertEquals(0, d[i]);
		t.setData(data, 1);
		d = t.getData(new byte[5], 1);
		for (int i = 0; i < 4; ++i)
			assertEquals(data[i], d[i]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#getSubTypes()}.
	 */
	public final void testGetSubTypes()
	{
		assertEquals(1, t.getSubTypes().size());
		t.getSubTypes().containsKey(dpt.getID());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#setValue(int, int, int, int)}.
	 */
	public final void testSetValueIntIntIntInt()
	{
		t.setValue(6, 15, 16, 17);
		assertEquals(15, t.getHour());
		assertEquals(16, t.getMinute());
		assertEquals(17, t.getSecond());
		assertEquals(6, t.getDayOfWeek());

		timeHelperThrow(-1, 15, 16, 17);
		timeHelperThrow(8, 15, 16, 17);
		timeHelperThrow(2, -1, 16, 17);
		timeHelperThrow(2, 24, 16, 17);
		timeHelperThrow(2, 22, -1, 17);
		timeHelperThrow(2, 22, 60, 17);
		timeHelperThrow(2, 22, 17, -1);
		timeHelperThrow(2, 22, 17, 60);
	}

	private void timeHelperThrow(final int dow, final int hr, final int min, final int sec)
	{
		try {
			t.setValue(dow, hr, min, sec);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#getDayOfWeek()}.
	 */
	public final void testInitialGet()
	{
		assertEquals(1, t.getItems());
		assertEquals(0, t.getDayOfWeek());
		assertEquals(0, t.getHour());
		assertEquals(0, t.getMinute());
		assertEquals(0, t.getSecond());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#setValue(long)}.
	 */
	public final void testSetValueLong()
	{
		final Calendar c = Calendar.getInstance();
		t.setValue(c.getTimeInMillis());
		assertEquals(1, t.getItems());
		// adjust offset
		int dow = c.get(Calendar.DAY_OF_WEEK);
		dow = dow == 1 ? 7 : dow - 1;
		assertEquals(dow, t.getDayOfWeek());
		assertEquals(c.get(Calendar.HOUR_OF_DAY), t.getHour());
		assertEquals(c.get(Calendar.MINUTE), t.getMinute());
		assertEquals(c.get(Calendar.SECOND), t.getSecond());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#getValueMilliseconds()}.
	 */
	public final void testGetValueMilliseconds()
	{
		final Calendar c = Calendar.getInstance();
		final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		final long init = t.getValueMilliseconds();
		c.setTimeInMillis(init);
		assertEquals(sdf.format(c.getTime()), sdf.format(new Date(init)));

		t.setValue(System.currentTimeMillis());
		final long time = t.getValueMilliseconds();
		c.setTimeInMillis(time);
		assertEquals(sdf.format(c.getTime()), sdf.format(new Date(time)));

		int dow = c.get(Calendar.DAY_OF_WEEK);
		dow = dow == 1 ? 7 : dow - 1;
		assertEquals(dow, t.getDayOfWeek());
		assertEquals(c.get(Calendar.HOUR_OF_DAY), t.getHour());
		assertEquals(c.get(Calendar.MINUTE), t.getMinute());
		assertEquals(c.get(Calendar.SECOND), t.getSecond());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorTime#useValueFormat (java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testUseValueFormat() throws KNXFormatException
	{
		DPTXlatorTime.useValueFormat("yyMMdd HHmmss");
		final String v = "071020 223344";
		t.setValue(v);
		assertEquals(22, t.getHour());
		assertEquals(33, t.getMinute());
		assertEquals(44, t.getSecond());
		assertEquals(6, t.getDayOfWeek());
		final String s = t.getValue();
		assertEquals(v.substring(v.indexOf(' ')), s.substring(s.indexOf(' ')));
	}
}

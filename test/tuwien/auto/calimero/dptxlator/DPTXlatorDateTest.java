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

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * Test for DPTXlatorDate.
 * <p>
 *
 * @author B. Malinowsky
 */
public class DPTXlatorDateTest extends TestCase
{
	private DPTXlatorDate t;
	private final String[] values =
		new String[] { "1999-09-10", "2000-01-01", "  2015-02-31  " };
	private final DPT dpt = DPTXlatorDate.DPT_DATE;

	// 2007-08-09
	private final byte[] data = new byte[] { 0, 9, 8, 2007 - 2000, -1, -1 };

	/**
	 * @param name
	 */
	public DPTXlatorDateTest(final String name)
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
		t = new DPTXlatorDate(dpt);
		// reset to default to not interfere with tests
		DPTXlatorDate.useValueFormat(null);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#setValues(java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t.setValues(values);
		assertEquals(3, t.getItems());
		assertEquals(1999, t.getYear());
		assertEquals(9, t.getMonth());
		assertEquals(10, t.getDay());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getAllValues().length);
		Helper.assertSimilar("2000-01-01", t.getAllValues()[0]);
		assertEquals(2000, t.getYear());
		assertEquals(1, t.getMonth());
		assertEquals(1, t.getDay());

		t.setValues(values);
		assertEquals(values.length, t.getAllValues().length);
		final String[] all = t.getAllValues();
		Helper.assertSimilar(all, values);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#setValue(java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueString() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		t.setValue(dpt.getLowerValue());
		assertEquals(1, t.getItems());
		assertEquals(1990, t.getYear());
		assertEquals(1, t.getMonth());
		assertEquals(1, t.getDay());

		t.setValue(dpt.getUpperValue());
		assertEquals(1, t.getItems());
		assertEquals(2089, t.getYear());
		assertEquals(12, t.getMonth());
		assertEquals(31, t.getDay());

		dateHelperThrow("1989-01-01");
		dateHelperThrow("2090-01-01");
		dateHelperThrow("2007-00-01");
		dateHelperThrow("2007 13-01");
		dateHelperThrow("2015-01-0");
		dateHelperThrow("2015-01-32");
	}

	private void dateHelperThrow(final String value)
	{
		try {
			t.setValue(value);
			fail("should throw");
		}
		catch (final KNXFormatException e) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
	{
		t.setData(data, 1);
		assertEquals(1, t.getItems());
		assertEquals(2007, t.getYear());
		assertEquals(8, t.getMonth());
		assertEquals(9, t.getDay());

		// on last value we use a reserved bit and therefore will see a warning
		final byte[] d =
			new byte[] { 15, 3, 2025 - 2000, 31, 12, 1999 - 1900, 31, 4, 2012 - 2000 };
		t.setData(d, 0);
		assertEquals(3, t.getItems());
		Helper.assertSimilar(new String[] { "2025-03-15", "1999-12-31", "2012-04-31" }, t
			.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#getData(byte[], int)}.
	 */
	public final void testGetDataByteArrayInt()
	{
		byte[] d = t.getData(new byte[5], 2);
		final byte[] expected = new byte[] { 0, 0, 1, 1, 0, };
		for (int i = 0; i < d.length; ++i)
			assertEquals(expected[i], d[i]);
		t.setData(data, 1);
		d = t.getData(new byte[5], 1);
		for (int i = 0; i < 3; ++i)
			assertEquals(data[i], d[i]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#getSubTypes()}.
	 */
	public final void testGetSubTypes()
	{
		assertEquals(1, t.getSubTypes().size());
		t.getSubTypes().containsKey(dpt.getID());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#useValueFormat
	 * (java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testUseValueFormat() throws KNXFormatException
	{
		final String pattern = "EEEE, MMMMM dd, yyyy HH:mm";
		DPTXlatorDate.useValueFormat(pattern);
		final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		final DateFormatSymbols sym = sdf.getDateFormatSymbols();
		final String v = sym.getWeekdays()[Calendar.SATURDAY] + ", "
			+ sym.getMonths()[Calendar.APRIL]+ " 28, 2007 12:13";
		t.setValue(v);
		assertEquals(1, t.getItems());
		assertEquals(2007, t.getYear());
		assertEquals(4, t.getMonth());
		assertEquals(28, t.getDay());
		final String s = t.getValue();
		assertEquals(v.substring(0, v.lastIndexOf(' ')), s.substring(0, s
			.lastIndexOf(' ')));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#setValue(int, int, int)}.
	 */
	public final void testSetValueIntIntInt()
	{
		t.setValue(1990, 1, 1);
		assertEquals(1990, t.getYear());
		assertEquals(1, t.getMonth());
		assertEquals(1, t.getDay());

		t.setValue(2089, 12, 31);
		assertEquals(2089, t.getYear());
		assertEquals(12, t.getMonth());
		assertEquals(31, t.getDay());

		t.setValue(2008, 6, 31);
		assertEquals(2008, t.getYear());
		assertEquals(6, t.getMonth());
		assertEquals(31, t.getDay());

		dateHelperThrow(1989, 15, 16);
		dateHelperThrow(2090, 15, 16);
		dateHelperThrow(2007, 0, 16);
		dateHelperThrow(2007, 13, 16);
		dateHelperThrow(2007, 11, 0);
		dateHelperThrow(2000, 11, 32);
	}

	private void dateHelperThrow(final int yr, final int mth, final int day)
	{
		try {
			t.setValue(yr, mth, day);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#setValue(long)}.
	 */
	public final void testSetValueLong()
	{
		final Calendar c = Calendar.getInstance();
		t.setValue(c.getTimeInMillis());
		assertEquals(1, t.getItems());
		assertEquals(c.get(Calendar.YEAR), t.getYear());
		assertEquals(c.get(Calendar.MONTH) + 1, t.getMonth());
		assertEquals(c.get(Calendar.DAY_OF_MONTH), t.getDay());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDate#getValueMilliseconds()}.
	 * @throws KNXFormatException
	 */
	public final void testGetValueMilliseconds() throws KNXFormatException
	{
		final Calendar c = Calendar.getInstance();
		final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
		final long init = t.getValueMilliseconds();
		c.setTimeInMillis(init);
		assertEquals(sdf.format(c.getTime()), sdf.format(new Date(init)));

		t.setValue(System.currentTimeMillis());
		final long time = t.getValueMilliseconds();
		c.setTimeInMillis(time);
		assertEquals(sdf.format(c.getTime()), sdf.format(new Date(time)));

		assertEquals(c.get(Calendar.YEAR), t.getYear());
		assertEquals(c.get(Calendar.MONTH) + 1, t.getMonth());
		assertEquals(c.get(Calendar.DAY_OF_MONTH), t.getDay());

		c.clear(Calendar.HOUR);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		assertEquals(c.getTimeInMillis(), t.getValueMilliseconds());

		t.setValue(2007, 2, 30);
		try {
			t.getValueMilliseconds();
			fail("invalid time");
		}
		catch (final KNXFormatException e) {}
	}
}

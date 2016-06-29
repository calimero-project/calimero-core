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

import java.util.Calendar;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class DPTXlatorDateTimeTest extends TestCase
{
	private DPTXlatorDateTime t;

	private String def;

	private final String work = "-workday-";
	private final String dst = "(dst)";
	private final String insync = ",in sync ";
	private final String nosync = ",no sync ";

	private final String time = "12.45.33";
	private final String date = "7/22";
	private final String dateday = "7/22 Tue";
	private final String value = "Wed, 8-27, 2007 " + time + work + dst + insync;
	private final String value2 = "2007-6-5 (Tue) 23:22:00 DST workday";
	private final String value3 = "8:30:0 any day, " + work;
	private final String value4 = "any day, 1900" + work + nosync;

	// matching to 'value' Strings
	private final byte[] data = { 107, 8, 27, 108, 45, 33, 0x41, (byte) 0x80 };
	private final byte[] data2 = { 107, 6, 5, 0x57, 22, 0, 0x41, 0 };
	private final byte[] data3 = { 0, 0, 0, 8, 30, 0, 0x58, 0 };
	private final byte[] data4 = { 0, 0, 0, 0, 0, 0, 0x4a, 0 };

	// if we're in a time zone without DST, date/time values with a set DST field will fail
	private final boolean useDaylightTime = Calendar.getInstance().getTimeZone().useDaylightTime();

	/**
	 * @param name name of test case
	 */
	public DPTXlatorDateTimeTest(final String name)
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
		t = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME.getID());
		def = t.getValue();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#setValues
	 * (java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t.setValues(new String[0]);
		assertTrue(t.getItems() == 1);
		assertTrue(t.getValue().equals(def));
		t.setValues(new String[] { value2, value3 });
		assertTrue(t.getItems() == 2);
		assertFalse(t.getValue().equals(def));
		assertFalse(t.getAllValues()[1].equals(def));

		t.setValues(new String[] { t.getValue(), t.getValue() });
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		String[] v = new String[] { time, date, dateday, value, value2, value3 };
		t.setValues(v);
		assertEquals(v.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		checkTime(12, 45, 33);
		v = t.getAllValues();
		final String item = v[4].toLowerCase();
		assertFind(item, new String[] { "2007", "tue", "6", "5", "23", "22", "0", "dst",
			"workday", "no", "sync" });
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#setValue(java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValue() throws KNXFormatException
	{
		t.setValue(dateday);
		assertEquals(t.getItems(), 1);
		assertFalse(t.getValue().equals(def));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
	{
		t.setData(data);
		assertEquals(2007, t.getYear());
		assertEquals(8, t.getMonth());
		assertEquals(27, t.getDay());
		checkTime(12, 45, 33);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));

		t.setData(new byte[] { 107, 8, 31, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 8, 0, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 0, 30, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 13, 30, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 5, 30, (byte) ((5 << 5) | 24), 45, 33, 0x41, (byte) 0x80 });
		t.setData(new byte[] { 107, 9, 30, (byte) ((5 << 5) | 22), 1 << 6 | 62, 40, 0x43,
			(byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 9, 30, (byte) ((5 << 5) | 22), 1 << 6 | 62, 40, 0x41,
			(byte) 0x80 });
	}

	private void dataHelperThrow(final byte[] data)
	{
		try {
			t.setData(data);
			fail("invalid data");
		}
		catch (final KNXIllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#getData(byte[], int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetDataByteArrayInt() throws KNXFormatException
	{
		testGetByteArrayHelper(value, data);
		testGetByteArrayHelper(value2, data2);
		testGetByteArrayHelper(value3, data3);
		testGetByteArrayHelper(value4, data4);
	}

	private void testGetByteArrayHelper(final String test, final byte[] cmp)
		throws KNXFormatException
	{
		final int offset = 2;
		final byte[] dst = new byte[offset + 10];
		t.setValue(test);
		final String std = t.getValue();
		final byte[] d = t.getData(dst, offset);
		assertSame(dst, d);
		assertTrue(dst[0] == 0 && dst[1] == 0 && dst[10] == 0 && dst[11] == 0);
		for (int i = 0; i < 8; ++i)
			assertEquals(cmp[i], d[offset + i]);
		t.setData(d, offset);
		assertEquals(std, t.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#getSubTypes()}.
	 */
	public final void testGetSubTypes()
	{
		assertEquals(1, t.getSubTypes().size());
		assertEquals(DPTXlatorDateTime.DPT_DATE_TIME, t.getSubTypes().values().iterator()
			.next());
		t.getSubTypes().clear();
		assertEquals(0, t.getSubTypes().size());
		t.getSubTypes().put(DPTXlatorDateTime.DPT_DATE_TIME.getID(),
			DPTXlatorDateTime.DPT_DATE_TIME);
		assertEquals(1, t.getSubTypes().size());
		assertEquals(DPTXlatorDateTime.DPT_DATE_TIME, t.getSubTypes().values().iterator()
			.next());
	}

	/**
	 * Test method for {@link DPTXlatorDateTime#setValue(String)},
	 * {@link DPTXlatorDateTime#setData(byte[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValueAndData() throws KNXFormatException
	{
		t.setValue("24:00:00");
		t.setValue("00:00:00");
		t.setValue("13.56.43");
		try {
			t.setValue("24:01:00");
			fail("should throw");
		}
		catch (final KNXFormatException e) {}

		try {
			t.setValue("24:00:10");
			fail("should throw");
		}
		catch (final KNXFormatException e) {}

		t.setData(new byte[] { 0, 0, 0, 24, 0, 0, 0x3c, 0 });
		try {
			t.setData(new byte[] { 0, 0, 0, 24, 1, 0, 0x3c, 0 });
			fail("invalid data");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			t.setData(new byte[] { 0, 0, 0, 24, 0, 1, 0x3c, 0 });
			fail("invalid data");
		}
		catch (final KNXIllegalArgumentException e) {}

		t.setData(new byte[] { 0, 0, 0, 23, 0, 1, 0x3c, 0 });
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#useValueFormat(boolean)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testUseValueFormat() throws KNXFormatException
	{
		t.setValue(value);
		assertFind(t.getValue(), new String[] { "wed", "8", "27", "2007", "workday",
			"dst", "in", "sync", });
		t.useValueFormat(false);
		assertFind(t.getValue(), new String[] { "8", "27", "2007" });
		assertFindNot(t.getValue(), new String[] { "workday", "dst", "in", "sync", });

		t.setValue(value3);
		assertFind(t.getValue(), new String[] { "8", "30", "0", });
		assertFindNot(t.getValue(), new String[] { "any", "day", "workday", });
		t.useValueFormat(true);
		assertFind(t.getValue(), new String[] { "any", "day", "workday", });
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#getValueMilliseconds()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetValueMilliseconds() throws KNXFormatException
	{
		long ms;
		t.setValue(time);
		try {
			t.getValueMilliseconds();
			fail("invalid time");
		}
		catch (final KNXFormatException e) {}
		final Calendar c = Calendar.getInstance();

		if (useDaylightTime)
			t.setValue(value2);
		else
			t.setValue(value2.replace("DST", ""));
		ms = t.getValueMilliseconds();
		c.setTimeInMillis(ms);
		assertEquals(c.get(Calendar.YEAR), t.getYear());
		assertEquals(c.get(Calendar.MONTH) + 1, t.getMonth());
		assertEquals(c.get(Calendar.DAY_OF_MONTH), t.getDay());
		assertEquals(c.get(Calendar.HOUR_OF_DAY), t.getHour());
		assertEquals(c.get(Calendar.MINUTE), t.getMinute());
		assertEquals(c.get(Calendar.SECOND), t.getSecond());

		t.setValue("2007/2/27 00:00:00");
		t.setValue("2007/2/29 00:00:00");
		assertFalse(t.validate());

		t.setValue("2007/2/29 00:00:00");
		try {
			t.getValueMilliseconds();
			fail("invalid time");
		}
		catch (final KNXFormatException e) {}


		t.setValue("2007/2/28 24:00:00");
		ms = t.getValueMilliseconds();
		c.clear();
		c.set(2007, 1/* 0 based */, 28, 23, 59, 59);
		c.set(Calendar.MILLISECOND, 999);
		assertEquals(c.getTimeInMillis(), ms);
		assertEquals(24, t.getHour());
		assertEquals(0, t.getMinute());
		assertEquals(0, t.getSecond());

		assertTrue(t.validate());
	}

	/**
	 * Test method for setWorkday.
	 */
	public final void testSetWorkday()
	{
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
		t.setDateTimeFlag(DPTXlatorDateTime.WORKDAY, true);
		t.setValidField(DPTXlatorDateTime.WORKDAY, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
		t.setDateTimeFlag(DPTXlatorDateTime.WORKDAY, false);
		t.setValidField(DPTXlatorDateTime.WORKDAY, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
	}

	/**
	 * Test method for setDayOfWeek.
	 */
	public final void testSetDayOfWeek()
	{
		assertFalse(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(0, t.getDayOfWeek());
		t.setDayOfWeek(0);
		t.setValidField(DPTXlatorDateTime.DAY_OF_WEEK, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(0, t.getDayOfWeek());
		t.setDayOfWeek(5);
		t.setValidField(DPTXlatorDateTime.DAY_OF_WEEK, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(5, t.getDayOfWeek());
	}

	/**
	 * Test method for setYear.
	 */
	public final void testSetYear()
	{
		yearHelper(1900, 0);
		yearHelper(2155, 255);
		yearHelper(2007, 107);

		yearHelperThrow(1899);
		yearHelperThrow(2156);
		yearHelperThrow(0);
		yearHelperThrow(2200);
	}

	private void yearHelper(final int yr, final int data)
	{
		t.setDate(yr, 1, 1);
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(yr, t.getYear());
		assertEquals((byte) data, t.getData()[0]);
	}

	private void yearHelperThrow(final int yr)
	{
		try {
			yearHelper(yr, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for setDate.
	 */
	public final void testSetDate()
	{
		dateHelper(1, 1, 1, 1);
		dateHelper(12, 31, 12, 31);
		dateHelper(7, 25, 7, 25);

		dateHelperThrow(0, 1);
		dateHelperThrow(1, 0);
		dateHelperThrow(0, 0);
		dateHelperThrow(13, 1);
		dateHelperThrow(12, 32);
		dateHelperThrow(13, 32);
		dateHelperThrow(-1, 15);
		dateHelperThrow(11, -15);
	}

	private void dateHelper(final int mth, final int day, final int dataMth, final int dataDay)
	{
		t.setValidField(DPTXlatorDateTime.DATE, true);
		t.setDate(2000, mth, day);
		assertTrue(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(mth, t.getMonth());
		assertEquals((byte) dataMth, t.getData()[1]);
		assertEquals(day, t.getDay());
		assertEquals((byte) dataDay, t.getData()[2]);
		t.setDate(2000, mth, day);
		t.setValidField(DPTXlatorDateTime.DATE, false);
		assertEquals(mth, t.getMonth());
		assertEquals((byte) dataMth, t.getData()[1]);
		assertEquals(day, t.getDay());
		assertEquals((byte) dataDay, t.getData()[2]);
	}

	private void dateHelperThrow(final int mth, final int day)
	{
		try {
			dateHelper(mth, day, 0, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for setTime.
	 */
	public final void testSetTime()
	{
		timeHelper(0, 0, 0, 0, 0, 0);
		timeHelper(24, 0, 0, 24, 0, 0);
		timeHelper(12, 59, 59, 12, 59, 59);
		timeHelper(23, 59, 59, 23, 59, 59);
		timeHelper(17, 53, 21, 17, 53, 21);

		timeHelperThrow(24, 0, 1);
		timeHelperThrow(24, 1, 0);
		timeHelperThrow(24, 10, 10);
		timeHelperThrow(1, 60, 30);
		timeHelperThrow(1, 30, 60);
		timeHelperThrow(1, 60, 60);
		timeHelperThrow(25, 0, 0);
		timeHelperThrow(-1, 0, 0);
		timeHelperThrow(0, -4, 0);
		timeHelperThrow(0, 0, -55);
	}

	private void timeHelper(final int hr, final int min, final int sec, final int dataHr, final int dataMin,
		final int dataSec)
	{
		t.setTime(hr, min, sec);
		t.setValidField(DPTXlatorDateTime.TIME, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(hr, t.getHour());
		assertEquals((byte) dataHr, t.getData()[3]);
		assertEquals(min, t.getMinute());
		assertEquals((byte) dataMin, t.getData()[4]);
		assertEquals(sec, t.getSecond());
		assertEquals((byte) dataSec, t.getData()[5]);

		t.setTime(hr, min, sec);
		t.setValidField(DPTXlatorDateTime.TIME, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(hr, t.getHour());
		assertEquals((byte) dataHr, t.getData()[3]);
		assertEquals(min, t.getMinute());
		assertEquals((byte) dataMin, t.getData()[4]);
		assertEquals(sec, t.getSecond());
		assertEquals((byte) dataSec, t.getData()[5]);
	}

	private void timeHelperThrow(final int hr, final int min, final int sec)
	{
		try {
			timeHelper(hr, min, sec, 0, 0, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for setDaylightTime.
	 */
	public final void testSetDaylightTime()
	{
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
		t.setDateTimeFlag(DPTXlatorDateTime.DAYLIGHT, true);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
		t.setDateTimeFlag(DPTXlatorDateTime.DAYLIGHT, false);
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
	}

	/**
	 * Test method for setQualityOfClock.
	 */
	public final void testSetQualityOfClock()
	{
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
		t.setDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC, true);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
		t.setDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC, false);
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#setValue(long)}.
	 * @throws KNXFormatException
	 */
	public final void testSetValueMilliseconds() throws KNXFormatException
	{
		final Calendar c = Calendar.getInstance();
		c.clear();
		c.set(2007, 7, 15, 20, 30, 10);
		final long ms = c.getTimeInMillis();

		t.setValidField(DPTXlatorDateTime.TIME, false);
		t.setValidField(DPTXlatorDateTime.DATE, false);
		t.setValidField(DPTXlatorDateTime.YEAR, false);
		t.setValue(ms);
		assertTrue(t.isValidField(DPTXlatorDateTime.DATE));
		assertTrue(t.isValidField(DPTXlatorDateTime.TIME));
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));

		assertEquals(ms, t.getValueMilliseconds());
		assertEquals(c.get(Calendar.YEAR), t.getYear());
		assertEquals(c.get(Calendar.MONTH) + 1, t.getMonth());
		assertEquals(c.get(Calendar.DAY_OF_MONTH), t.getDay());
		checkTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c
			.get(Calendar.SECOND));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#isFaultyClock()}.
	 */
	public final void testIsFaultyClock()
	{
		assertFalse(t.isFaultyClock());
		assertEquals(t.getData()[6] & 0x80, 0x00);
		t.setFaultyClock(true);
		assertTrue(t.isFaultyClock());
		assertEquals(t.getData()[6] & 0x80, 0x80);
	}

	/**
	 * Test method for isWorkdaySet.
	 */
	public final void testIsWorkdaySet()
	{
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertEquals(t.getData()[6] & 0x20, 0x20);
		t.setValidField(DPTXlatorDateTime.WORKDAY, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertEquals(t.getData()[6] & 0x20, 0x00);
		t.setValidField(DPTXlatorDateTime.WORKDAY, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertEquals(t.getData()[6] & 0x20, 0x20);
	}

	/**
	 * Test method for isYearSet.
	 */
	public final void testIsYearSet()
	{
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(t.getData()[6] & 0x10, 0x00);
		t.setValidField(DPTXlatorDateTime.YEAR, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(t.getData()[6] & 0x10, 0x10);
		t.setValidField(DPTXlatorDateTime.YEAR, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(t.getData()[6] & 0x10, 0x00);
	}

	/**
	 * Test method for isDateSet.
	 */
	public final void testIsDateSet()
	{
		assertTrue(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(t.getData()[6] & 0x08, 0x00);
		t.setValidField(DPTXlatorDateTime.DATE, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(t.getData()[6] & 0x08, 0x08);
		t.setValidField(DPTXlatorDateTime.DATE, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(t.getData()[6] & 0x08, 0x00);
	}

	/**
	 * Test method for isDayOfWeekSet.
	 */
	public final void testIsDayOfWeekSet()
	{
		assertFalse(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(t.getData()[6] & 0x04, 0x04);
		t.setValidField(DPTXlatorDateTime.DAY_OF_WEEK, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(t.getData()[6] & 0x04, 0x00);
		t.setValidField(DPTXlatorDateTime.DAY_OF_WEEK, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(t.getData()[6] & 0x04, 0x04);
	}

	/**
	 * Test method for isTimeSet.
	 */
	public final void testIsTimeSet()
	{
		assertTrue(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(t.getData()[6] & 0x02, 0x00);
		t.setValidField(DPTXlatorDateTime.TIME, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(t.getData()[6] & 0x02, 0x02);
		t.setValidField(DPTXlatorDateTime.TIME, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(t.getData()[6] & 0x02, 0x00);
	}

	/**
	 * Test method for inDaylightTime.
	 *
	 * @throws KNXFormatException
	 */
	public final void testInDaylightTime() throws KNXFormatException
	{
		t.setValue(value);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
		t.setData(data2);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
		t.setValue(time);
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
	}

	/**
	 * Test method for hasExternalSyncSignal.
	 *
	 * @throws KNXFormatException
	 */
	public final void testHasExternalSyncSignal() throws KNXFormatException
	{
		t.setValue(value);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
		t.setData(data);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
		t.setValue(value2);
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
		t.setData(data2);
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.CLOCK_SYNC));
	}

	/**
	 * Test method for isWorkday.
	 */
	public final void testIsWorkday()
	{
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
		t.setDateTimeFlag(DPTXlatorDateTime.WORKDAY, true);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
		t.setDateTimeFlag(DPTXlatorDateTime.WORKDAY, true);
		assertTrue(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
		t.setDateTimeFlag(DPTXlatorDateTime.WORKDAY, false);
		assertFalse(t.getDateTimeFlag(DPTXlatorDateTime.WORKDAY));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorDateTime#validate()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testValidate() throws KNXFormatException
	{
		final Calendar c = Calendar.getInstance();
		t.setValue(c.getTimeInMillis());
		assertTrue(t.validate());

		t.setDateTimeFlag(DPTXlatorDateTime.DAYLIGHT, !t
			.getDateTimeFlag(DPTXlatorDateTime.DAYLIGHT));
		assertFalse(t.validate());

		t.setValue(c.getTimeInMillis());
		t.setDayOfWeek(c.get(Calendar.DAY_OF_WEEK));
		assertFalse(t.validate());

		t.setValue("2007/2/29");
		assertFalse(t.validate());

		t.setValue("23:00:00");
		assertTrue(t.validate());

		if (useDaylightTime)
			t.setValues(new String[] { "2002/12/31 23:59:59", "Sat, 2007/5/12 24:00:00 DST ", "2007/2/28 in sync" });
		else
			t.setValues(new String[] { "2002/12/31 23:59:59", "Sat, 2007/5/12 24:00:00 ", "2007/2/28 in sync" });
		assertTrue(t.validate());
		t.setValues(new String[] { "2002/12/31 23:59:59",
			"Sat, 2007/5/12 24:00:00 DST ", "2007/2/29" });
		assertFalse(t.validate());
	}

	private void checkTime(final int hr, final int min, final int sec)
	{
		assertEquals(hr, t.getHour());
		assertEquals(min, t.getMinute());
		assertEquals(sec, t.getSecond());
	}

	private void assertFind(final String text, final String[] find)
	{
		for (int i = 0; i < find.length; ++i)
			assertTrue(text.toLowerCase().indexOf(find[i].toLowerCase()) >= 0);
	}

	private void assertFindNot(final String text, final String[] find)
	{
		for (int i = 0; i < find.length; ++i)
			assertTrue(text.toLowerCase().indexOf(find[i].toLowerCase()) == -1);
	}
}

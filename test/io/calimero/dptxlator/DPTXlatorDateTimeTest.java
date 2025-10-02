/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.dptxlator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

@Isolated("modifies Locale")
class DPTXlatorDateTimeTest {
	private DPTXlatorDateTime t;

	private String def;

	private final String work = "-workday-";
	private final String dst = "(dst)";
	private final String insync = " (sync)";
	private final String nosync = "";

	private final String time = "12:45:33";
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


	@BeforeEach
	void setUp() throws Exception {
		t = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME.getID());
		def = t.getValue();
	}

	@Test
	void defaultConstructor() {
		final var dateTime = new DPTXlatorDateTime();
		assertEquals(1900, dateTime.getYear());
		assertEquals(1, dateTime.getMonth());
		assertEquals(1, dateTime.getDay());
		assertEquals(0, dateTime.getHour());
		assertEquals(0, dateTime.getMinute());
		assertEquals(0, dateTime.getSecond());
		assertEquals(0, dateTime.getDayOfWeek());
	}

	@Test
	void setValues() throws KNXFormatException {
		t.setValues();
		assertEquals(1, t.getItems());
		assertEquals(t.getValue(), def);
		t.setValues(value2, value3);
		assertEquals(2, t.getItems());
		assertNotEquals(t.getValue(), def);
		assertNotEquals(t.getAllValues()[1], def);

		t.setValues(t.getValue(), t.getValue());
	}

	@Test
	void getAllValues() throws KNXFormatException {
		String[] v = new String[] { time, date, dateday, value, value2, value3 };
		t.setValues(v);
		assertEquals(v.length, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		checkTime(12, 45, 33);
		v = t.getAllValues();
		final String item = v[4].toLowerCase();

		final String day = localizedDayOfWeek(DayOfWeek.TUESDAY.getValue());
		assertFind(item, new String[] { "2007", day, "6", "5", "23", "22", "0", "dst", "workday" });
	}

	private static String localizedDayOfWeek(final int dayOfWeek) {
		return DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.SHORT, Locale.getDefault());
	}

	@Test
	void setValue() throws KNXFormatException {
		t.setValue(dateday);
		assertEquals(1, t.getItems());
		assertNotEquals(t.getValue(), def);
	}

	@Test
	void setDataByteArrayInt() {
		t.setData(data);
		assertEquals(2007, t.getYear());
		assertEquals(8, t.getMonth());
		assertEquals(27, t.getDay());
		checkTime(12, 45, 33);
		assertTrue(t.isDst());
		assertTrue(t.isSyncClock());
		assertTrue(t.isWorkday());

		t.setData(new byte[] { 107, 8, 31, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 8, 0, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 0, 30, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 13, 30, (byte) ((5 << 5) | 22), 45, 33, 0x41, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 5, 30, (byte) ((5 << 5) | 24), 45, 33, 0x41, (byte) 0x80 });
		t.setData(new byte[] { 107, 9, 30, (byte) ((5 << 5) | 22), 1 << 6 | 62, 40, 0x43, (byte) 0x80 });
		dataHelperThrow(new byte[] { 107, 9, 30, (byte) ((5 << 5) | 22), 1 << 6 | 62, 40, 0x41, (byte) 0x80 });
	}

	private void dataHelperThrow(final byte[] data) {
		try {
			t.setData(data);
			fail("invalid data");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	@Test
	void getDataByteArrayInt() throws KNXFormatException {
		testGetByteArrayHelper(value, data);
		testGetByteArrayHelper(value2, data2);
		testGetByteArrayHelper(value3, data3);
		testGetByteArrayHelper(value4, data4);
	}

	private void testGetByteArrayHelper(final String test, final byte[] cmp) throws KNXFormatException {
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

	@Test
	void getSubTypes() {
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

	@Test
	void setValueAndData() throws KNXFormatException {
		t.setValue("24:00:00");
		t.setValue("00:00:00");
		t.setValue("13:56:43");
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

	@Test
	void useValueFormat() throws KNXFormatException {
		t.setValue(value);
		final String day = localizedDayOfWeek(DayOfWeek.WEDNESDAY.getValue());
		assertFind(t.getValue(), new String[] { day, "8", "27", "2007", "workday", "dst", "sync", });
		t.useValueFormat(false);
		assertFind(t.getValue(), new String[] { "8", "27", "2007" });
		assertFindNot(t.getValue(), new String[] { "workday", "dst", "in", "sync", });

		t.setValue(value3);
		assertFind(t.getValue(), new String[] { "8", "30", "0", });
		assertFindNot(t.getValue(), new String[] { "any", "day", "workday", });
		t.useValueFormat(true);
		assertFind(t.getValue(), new String[] { "any", "day", "workday", });
	}

	@Test
	void getValueMilliseconds() throws KNXFormatException {
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

	@Test
	void localDateTime() throws KNXFormatException {
		t.setValue(time);
		try {
			t.localDateTime();
			fail("invalid time");
		}
		catch (final KNXFormatException e) {}

		if (useDaylightTime)
			t.setValue(value2);
		else
			t.setValue(value2.replace("DST", ""));
		final var ldt = t.localDateTime();

		assertEquals(ldt.getYear(), t.getYear());
		assertEquals(ldt.getMonth().getValue(), t.getMonth());
		assertEquals(ldt.getDayOfMonth(), t.getDay());
		assertEquals(ldt.getDayOfWeek().getValue(), t.getDayOfWeek());
		assertEquals(ldt.getHour(), t.getHour());
		assertEquals(ldt.getMinute(), t.getMinute());
		assertEquals(ldt.getSecond(), t.getSecond());

		t.setValue("2007/2/29 00:00:00"); // no leap year
		assertThrows(DateTimeException.class, t::localDateTime);

		t.setValue("2007/2/28 00:00:00");
		final var zero = t.localDateTime();
		assertEquals(LocalTime.MIN, zero.toLocalTime());

		t.setValue("2007/2/28 24:00:00");
		final var cheat = t.localDateTime();
		assertEquals(LocalTime.MAX, cheat.toLocalTime());

		t.setFaultyClock(true);
		assertThrows(KNXFormatException.class, t::localDateTime);

		final var noYear = new DPTXlatorDateTime();
		noYear.setValue(dateday);
		assertThrows(KNXFormatException.class, noYear::localDateTime);
	}

	@Test
	void dateWithDotSeparators() throws KNXFormatException {
		t.setValue("2021.2.3");
		assertEquals("2021/2/3", t.getValue());
	}

	@Test
	void setWorkday() {
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertFalse(t.isWorkday());
		t.setWorkday(true);
		t.setValidField(DPTXlatorDateTime.WORKDAY, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertTrue(t.isWorkday());
		t.setWorkday(false);
		t.setValidField(DPTXlatorDateTime.WORKDAY, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertFalse(t.isWorkday());
	}

	@Test
	void setDayOfWeek() {
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

	@ParameterizedTest
	@MethodSource("localeProvider")
	void localizedDayOfWeek(final Locale locale) {
		final var current = Locale.getDefault();
		Locale.setDefault(locale);
		try {
			for (final var dow : DayOfWeek.values()) {
				final var now = LocalDateTime.now().with(ChronoField.DAY_OF_WEEK, dow.getValue());
				t.setValue(now);
				final var v = t.getValue();
				final var day = localizedDayOfWeek(now.getDayOfWeek().getValue());
				assertTrue(v.contains(day));
			}
		}
		finally {
			Locale.setDefault(current);
		}
	}

	private static Locale[] localeProvider() {
		return Locale.getAvailableLocales();
	}

	@Test
	void setYear() {
		yearHelper(1900, 0);
		yearHelper(2155, 255);
		yearHelper(2007, 107);

		yearHelperThrow(1899);
		yearHelperThrow(2156);
		yearHelperThrow(0);
		yearHelperThrow(2200);
	}

	private void yearHelper(final int yr, final int data) {
		t.setDate(yr, 1, 1);
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(yr, t.getYear());
		assertEquals((byte) data, t.getData()[0]);
	}

	private void yearHelperThrow(final int yr) {
		try {
			yearHelper(yr, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	@Test
	void setDate() {
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

	private void dateHelper(final int mth, final int day, final int dataMth, final int dataDay) {
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

	private void dateHelperThrow(final int mth, final int day) {
		try {
			dateHelper(mth, day, 0, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	@Test
	void setTime() {
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
			final int dataSec) {
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

	private void timeHelperThrow(final int hr, final int min, final int sec) {
		try {
			timeHelper(hr, min, sec, 0, 0, 0);
			fail("should throw");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	@Test
	void setDaylightTime() {
		assertFalse(t.isDst());
		t.setDst(true);
		assertTrue(t.isDst());
		t.setDst(false);
		assertFalse(t.isDst());
	}

	@Test
	void setQualityOfClock() {
		assertFalse(t.isSyncClock());
		t.setClockSync(true);
		assertTrue(t.isSyncClock());
		t.setClockSync(false);
		assertFalse(t.isSyncClock());
	}

	@Test
	void setValueMilliseconds() throws KNXFormatException {
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
		checkTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
	}

	@Test
	void isFaultyClock() {
		assertFalse(t.isFaultyClock());
		assertEquals(0x00, t.getData()[6] & 0x80);
		t.setFaultyClock(true);
		assertTrue(t.isFaultyClock());
		assertEquals(0x80, t.getData()[6] & 0x80);
	}

	@Test
	void isWorkdaySet() {
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertEquals(0x20, t.getData()[6] & 0x20);
		t.setValidField(DPTXlatorDateTime.WORKDAY, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertEquals(0x00, t.getData()[6] & 0x20);
		t.setValidField(DPTXlatorDateTime.WORKDAY, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertEquals(0x20, t.getData()[6] & 0x20);
	}

	@Test
	void isYearSet() {
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(0x00, t.getData()[6] & 0x10);
		t.setValidField(DPTXlatorDateTime.YEAR, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(0x10, t.getData()[6] & 0x10);
		t.setValidField(DPTXlatorDateTime.YEAR, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.YEAR));
		assertEquals(0x00, t.getData()[6] & 0x10);
	}

	@Test
	void isDateSet() {
		assertTrue(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(0x00, t.getData()[6] & 0x08);
		t.setValidField(DPTXlatorDateTime.DATE, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(0x08, t.getData()[6] & 0x08);
		t.setValidField(DPTXlatorDateTime.DATE, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.DATE));
		assertEquals(0x00, t.getData()[6] & 0x08);
	}

	@Test
	void isDayOfWeekSet() {
		assertFalse(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(0x04, t.getData()[6] & 0x04);
		t.setValidField(DPTXlatorDateTime.DAY_OF_WEEK, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(0x00, t.getData()[6] & 0x04);
		t.setValidField(DPTXlatorDateTime.DAY_OF_WEEK, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.DAY_OF_WEEK));
		assertEquals(0x04, t.getData()[6] & 0x04);
	}

	@Test
	void isTimeSet() {
		assertTrue(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(0x00, t.getData()[6] & 0x02);
		t.setValidField(DPTXlatorDateTime.TIME, false);
		assertFalse(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(0x02, t.getData()[6] & 0x02);
		t.setValidField(DPTXlatorDateTime.TIME, true);
		assertTrue(t.isValidField(DPTXlatorDateTime.TIME));
		assertEquals(0x00, t.getData()[6] & 0x02);
	}

	@Test
	void inDaylightTime() throws KNXFormatException {
		t.setValue(value);
		assertTrue(t.isDst());
		t.setData(data2);
		assertTrue(t.isDst());
		t.setValue(time);
		assertFalse(t.isDst());
	}

	@Test
	void hasExternalSyncSignal() throws KNXFormatException {
		t.setValue(value);
		assertTrue(t.isSyncClock());
		t.setData(data);
		assertTrue(t.isSyncClock());
		t.setValue(value2);
		assertFalse(t.isSyncClock());
		t.setData(data2);
		assertFalse(t.isSyncClock());
	}

	@Test
	void isWorkday() {
		assertFalse(t.isWorkday());
		t.setWorkday(true);
		assertTrue(t.isWorkday());
		t.setWorkday(true);
		assertTrue(t.isWorkday());
		t.setWorkday(false);
		assertFalse(t.isWorkday());
	}

	@Test
	void isHoliday() throws KNXFormatException {
		final var value = t.getValue();
		assertFalse(t.isValidField(DPTXlatorDateTime.WORKDAY));

		t.setWorkday(false);
		assertTrue(t.isValidField(DPTXlatorDateTime.WORKDAY));
		assertFalse(t.isWorkday());

		final var parser = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME);
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
		parser.setValue(value + " (holiday)");
		assertTrue(parser.isValidField(DPTXlatorDateTime.WORKDAY));
		assertFalse(parser.isWorkday());
		assertTrue(parser.getValue().contains("holiday"));
	}

	@Test
	void holidayAndWorkingDay() throws KNXFormatException {
		final var value = t.getValue() + " holiday, workday";
		final var parser = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME);
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
		assertThrows(KNXFormatException.class, () -> parser.setValue(value));
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
	}

	@Test
	void workingDayAndHoliday() throws KNXFormatException {
		final var value = t.getValue() + " workday, holiday";
		final var parser = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME);
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
		assertThrows(KNXFormatException.class, () -> parser.setValue(value));
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
	}

	@Test
	void duplicateHoliday() throws KNXFormatException {
		final var value = t.getValue() + " holiday (holiday)";
		final var parser = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME);
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
		assertThrows(KNXFormatException.class, () -> parser.setValue(value));
		assertFalse(parser.isValidField(DPTXlatorDateTime.WORKDAY));
	}

	@Test
	void useAllFlags() throws KNXFormatException {
		t.setFaultyClock(false); // needs to be false
		t.setWorkday(false);
		t.setClockSync(true);
		t.setDst(true);

		final var parser = new DPTXlatorDateTime(DPTXlatorDateTime.DPT_DATE_TIME);
		parser.setValue(t.getValue());
	}

	@Test
	void validate() throws KNXFormatException {
		final Calendar c = Calendar.getInstance();
		t.setValue(c.getTimeInMillis());
		assertTrue(t.validate());

		t.setDst(!t.isDst());
		assertFalse(t.validate());

		t.setValue(c.getTimeInMillis());
		t.setDayOfWeek(c.get(Calendar.DAY_OF_WEEK));
		assertFalse(t.validate());

		t.setValue("2007/2/29");
		assertFalse(t.validate());

		t.setValue("23:00:00");
		assertTrue(t.validate());

		if (useDaylightTime)
			t.setValues("2002/12/31 23:59:59", "Sat, 2007/5/12 24:00:00 DST ", "2007/2/28 sync");
		else
			t.setValues("2002/12/31 23:59:59", "Sat, 2007/5/12 24:00:00 ", "2007/2/28 sync");
		assertTrue(t.validate());
		t.setValues("2002/12/31 23:59:59", "Sat, 2007/5/12 24:00:00 DST ", "2007/2/29");
		assertFalse(t.validate());
	}

	@Test
	void reliableSyncSource() {
		t.setData(data);
		assertFalse(t.isReliableSyncSource());

		final byte[] withReliableSyncSource = { 107, 8, 27, 108, 45, 33, 0x41, (byte) 0xc0 };
		t.setData(withReliableSyncSource);
		assertTrue(t.isReliableSyncSource());
	}

	private void checkTime(final int hr, final int min, final int sec) {
		assertEquals(hr, t.getHour());
		assertEquals(min, t.getMinute());
		assertEquals(sec, t.getSecond());
	}

	private static void assertFind(final String text, final String[] find) {
		for (final String s : find)
			assertTrue(text.toLowerCase().contains(s.toLowerCase()));
	}

	private static void assertFindNot(final String text, final String[] find) {
		for (final String s : find)
			assertFalse(text.toLowerCase().contains(s.toLowerCase()));
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

@ResourceLock(value = "calimero.dptxlator.date", mode = READ)
class DPTXlatorDateTest
{
	private DPTXlatorDate t;
	private final String[] values = new String[] { "1999-09-10", "2000-01-01", "  2015-02-28  " };
	private final DPT dpt = DPTXlatorDate.DPT_DATE;

	// 2007-08-09
	private final byte[] data = new byte[] { 0, 9, 8, 2007 - 2000, -1, -1 };

	@BeforeEach
	void init() throws Exception
	{
		t = new DPTXlatorDate(dpt);
		// reset to default to not interfere with tests
		final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE
				.withResolverStyle(DPTXlatorDate.defaultResolverStyle);
		DPTXlatorDate.useValueFormat(dtf);
	}

	@Test
	void defaultConstructor() {
		final var date = new DPTXlatorDate();
		assertEquals(2000, date.getYear());
		assertEquals(1, date.getMonth());
		assertEquals(1, date.getDay());
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(values);
		assertEquals(3, t.getItems());
		assertEquals(1999, t.getYear());
		assertEquals(9, t.getMonth());
		assertEquals(10, t.getDay());
	}

	@Test
	void setInvalidValue()
	{
		try {
			final String invalidDate = "2015-02-29";
			t.setValues(new String[] { invalidDate });
			fail("invalid day in date " + invalidDate);
		}
		catch (final KNXFormatException expected) {}
	}

	@Test
	void getAllValues() throws KNXFormatException
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

	@Test
	void setValueString() throws KNXFormatException
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
			fail("setting value '" + value + "' should throw");
		}
		catch (final KNXFormatException e) {}
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(data, 1);
		assertEquals(1, t.getItems());
		assertEquals(2007, t.getYear());
		assertEquals(8, t.getMonth());
		assertEquals(9, t.getDay());

		t.setData(new byte[] { 31, 4, 2012 - 2000 }, 0);
		try {
			t.getAllValues();
			fail("date 31 of April is not possible");
		}
		catch (final DateTimeException expected) {}

		// on last value we use a reserved bit and therefore will see a warning
		final byte[] d = new byte[] { 15, 3, 2025 - 2000, 31, 12, 1999 - 1900, 30, 4, 2012 - 2000 };
		t.setData(d, 0);
		assertEquals(3, t.getItems());
		Helper.assertSimilar(new String[] { "2025-03-15", "1999-12-31", "2012-04-30" }, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt()
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

	@Test
	void getSubTypes()
	{
		assertEquals(1, t.getSubTypes().size());
		t.getSubTypes().containsKey(dpt.getID());
	}

	@Test
	@ResourceLock(value = "calimero.dptxlator.date", mode = READ_WRITE)
	void useValueFormat() throws KNXFormatException
	{
		final String pattern = "EEEE, MMMM dd, yyyy[ HH:mm]";
		DPTXlatorDate.useValueFormat(pattern);
		final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		final DateFormatSymbols sym = sdf.getDateFormatSymbols();
		final String v = sym.getWeekdays()[Calendar.SATURDAY] + ", " + sym.getMonths()[Calendar.APRIL]
				+ " 28, 2007 12:13";
		t.setValue(v);
		assertEquals(1, t.getItems());
		assertEquals(2007, t.getYear());
		assertEquals(4, t.getMonth());
		assertEquals(28, t.getDay());
		final String s = t.getValue();
		assertEquals(v.substring(0, v.lastIndexOf(' ')), s);
	}

	@Test
	void setValueIntIntInt()
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

	@Test
	void setValueLong()
	{
		final Calendar c = Calendar.getInstance();
		t.setValue(c.getTimeInMillis());
		assertEquals(1, t.getItems());
		assertEquals(c.get(Calendar.YEAR), t.getYear());
		assertEquals(c.get(Calendar.MONTH) + 1, t.getMonth());
		assertEquals(c.get(Calendar.DAY_OF_MONTH), t.getDay());
	}

	@Test
	void getValueMilliseconds() throws KNXFormatException
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
		catch (final KNXFormatException expected) {}
	}

	@Test
	void localDate()
	{
		final LocalDate date = t.localDate();
		assertEquals(date.getYear(), t.getYear());
		assertEquals(date.getMonthValue(), t.getMonth());
		assertEquals(date.getDayOfMonth(), t.getDay());
	}
}

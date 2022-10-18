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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

@ResourceLock(value = "useValueFormat", mode = ResourceAccessMode.READ)
class DPTXlatorTimeTest
{
	private DPTXlatorTime t;
	private final String[] values = new String[] { "mon, 00:00:00", "WED, 23:59:59", "  12:43:23  " };
	private final DPT dpt = DPTXlatorTime.DPT_TIMEOFDAY;

	// wed, 22:33:44, offset 1
	private final byte[] data = new byte[] { 0, 2 << 5 | 22, 33, 44, -1, -1 };

	@BeforeEach
	void init() throws Exception
	{
		t = new DPTXlatorTime(DPTXlatorTime.DPT_TIMEOFDAY);
		// reset to default to not interfere with tests
		DPTXlatorTime.useValueFormat(DPTXlatorTime.builder.toFormatter());
	}

	@Test
	void defaultConstructor() {
		final var time = new DPTXlatorTime();
		assertEquals(0, time.getHour());
		assertEquals(0, time.getMinute());
		assertEquals(0, time.getSecond());
		assertEquals(0, time.getDayOfWeek());
	}

	@Test
	void setValues() throws KNXFormatException
	{
		t.setValues(values);
		assertEquals(3, t.getItems());
		assertEquals(0, t.getHour());
		assertEquals(0, t.getMinute());
		assertEquals(0, t.getSecond());
		assertEquals(1, t.getDayOfWeek());
	}

	@Test
	void getAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getAllValues().length);
		Helper.assertSimilar("00:00:00", t.getAllValues()[0]);
		t.setValues(values);
		assertEquals(values.length, t.getAllValues().length);
		final String[] all = t.getAllValues();
		for (int i = 0; i < values.length; i++)
			assertTrue(all[i].toLowerCase().indexOf(values[i].toLowerCase().trim()) > -1);
	}

	@Test
	void setValueString() throws KNXFormatException
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

		t.setValue("wed 23:24");
		assertEquals(3, t.getDayOfWeek());
		assertEquals(23, t.getHour());
		assertEquals(24, t.getMinute());
		assertEquals(0, t.getSecond());
	}

	@Test
	void setDataByteArrayInt()
	{
		t.setData(data, 1);
		assertEquals(1, t.getItems());
		assertEquals(22, t.getHour());
		assertEquals(33, t.getMinute());
		assertEquals(44, t.getSecond());
		assertEquals(2, t.getDayOfWeek());

		// on last value we use a reserved bit and therefore will see a warning
		final byte[] d = new byte[] { 1 << 5 | 4, 5, 6, 2 << 5 | 7, 8, 9, 10, 1 << 6 | 11, 12 };
		t.setData(d, 0);
		assertEquals(3, t.getItems());
		Helper.assertSimilar(new String[] { "mon, 04:05:06", "tue, 07:08:09", "10:11:12" }, t.getAllValues());
	}

	@Test
	void getDataByteArrayInt()
	{
		byte[] d = t.getData(new byte[5], 2);
		for (int i = 0; i < d.length; ++i)
			assertEquals(0, d[i]);
		t.setData(data, 1);
		d = t.getData(new byte[5], 1);
		for (int i = 0; i < 4; ++i)
			assertEquals(data[i], d[i]);
	}

	@Test
	void getSubTypes()
	{
		assertEquals(1, t.getSubTypes().size());
		t.getSubTypes().containsKey(dpt.getID());
	}

	@Test
	void setValueIntIntIntInt()
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

	@Test
	void initialGet()
	{
		assertEquals(1, t.getItems());
		assertEquals(0, t.getDayOfWeek());
		assertEquals(0, t.getHour());
		assertEquals(0, t.getMinute());
		assertEquals(0, t.getSecond());
		assertEquals("00:00:00", t.getValue());
	}

	@Test
	void setValueLong()
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

	@Test
	void getValueMilliseconds()
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

	@Test
	void localTime()
	{
		final LocalTime time = t.localTime();
		assertEquals(time.getHour(), t.getHour());
		assertEquals(time.getMinute(), t.getMinute());
		assertEquals(time.getSecond(), t.getSecond());
	}

	@Test
	@ResourceLock(value = "useValueFormat", mode = ResourceAccessMode.READ_WRITE)
	void useValueFormat() throws KNXFormatException
	{
		DPTXlatorTime.useValueFormat("[uuMMdd ]HHmmss");
		final String v = "071020 223344";
		t.setValue(v);
		assertEquals(22, t.getHour());
		assertEquals(33, t.getMinute());
		assertEquals(44, t.getSecond());
		assertEquals(6, t.getDayOfWeek());
		final String s = t.getValue();
		assertEquals(v.substring(v.indexOf(' ')), s.substring(s.indexOf(' ')));
	}

	@Test
	void dayOfWeek()
	{
		assertFalse(t.dayOfWeek().isPresent());
		for (int dow = 1; dow < 8; dow++) {
			t.setValue(dow, 0, 0, 0);
			assertEquals(DayOfWeek.of(dow), t.dayOfWeek().get());
		}
	}
}

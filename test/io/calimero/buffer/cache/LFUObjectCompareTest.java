/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.buffer.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;

import org.junit.jupiter.api.Test;


class LFUObjectCompareTest
{
	final Cache lfu = new LFUCache(0, 0);

	// !! This is a copy of the private LFUObjectCompare class in LFUCache !!
	private static class LFUObjectCompare implements Comparator<CacheObject>
	{
		@Override
		public int compare(final CacheObject o1, final CacheObject o2)
		{
			if (o1.getUsage() > o2.getUsage())
				return 1;
			if (o1.getUsage() < o2.getUsage())
				return -1;
			return Integer.compare(o1.getCount(), o2.getCount());
		}
	}

	@Test
	void compare()
	{
		final LFUObjectCompare cmp = new LFUObjectCompare();
		final CacheObject o1 = new CacheObject("1", "value 1");
		final CacheObject o1equal = new CacheObject("equal", "equal value 1");
		final CacheObject o2 = new CacheObject("2", "value 2");

		assertEquals(0, cmp.compare(o1, o1equal));
		assertEquals(0, cmp.compare(o1equal, o1));
		assertEquals(0, cmp.compare(o1, o2));
		assertEquals(0, cmp.compare(o2, o1));

		lfu.put(o1);
		lfu.put(o1equal);
		lfu.put(o2);
		assertEquals(0, cmp.compare(o1, o1equal));
		assertEquals(0, cmp.compare(o1equal, o1));
		assertEquals(0, cmp.compare(o1, o2));
		assertEquals(0, cmp.compare(o2, o1));

		lfu.get("1");
		assertEquals(1, cmp.compare(o1, o1equal));
		assertEquals(-1, cmp.compare(o1equal, o1));
		assertEquals(1, cmp.compare(o1, o2));
		assertEquals(-1, cmp.compare(o2, o1));
		assertEquals(0, cmp.compare(o1equal, o2));
		lfu.get("1");
		assertEquals(1, cmp.compare(o1, o1equal));
		assertEquals(-1, cmp.compare(o1equal, o1));
		assertEquals(1, cmp.compare(o1, o2));
		assertEquals(-1, cmp.compare(o2, o1));
		assertEquals(0, cmp.compare(o1equal, o2));
		lfu.get("equal");
		assertEquals(1, cmp.compare(o1, o1equal));
		assertEquals(-1, cmp.compare(o1equal, o1));
		assertEquals(1, cmp.compare(o1, o2));
		assertEquals(-1, cmp.compare(o2, o1));
		assertEquals(1, cmp.compare(o1equal, o2));
		lfu.get("2");
		assertEquals(1, cmp.compare(o1, o1equal));
		assertEquals(-1, cmp.compare(o1equal, o1));
		assertEquals(1, cmp.compare(o1, o2));
		assertEquals(-1, cmp.compare(o2, o1));
		assertEquals(0, cmp.compare(o1equal, o2));
		lfu.get("2");
		assertEquals(1, cmp.compare(o1, o1equal));
		assertEquals(-1, cmp.compare(o1equal, o1));
		assertEquals(0, cmp.compare(o1, o2));
		assertEquals(0, cmp.compare(o2, o1));
		assertEquals(-1, cmp.compare(o1equal, o2));
	}
}

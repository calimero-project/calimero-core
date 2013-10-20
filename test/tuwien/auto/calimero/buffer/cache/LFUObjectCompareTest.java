/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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
*/

package tuwien.auto.calimero.buffer.cache;

import java.util.Comparator;

import junit.framework.TestCase;

/**
 * @author B. Malinowsky
 */
public class LFUObjectCompareTest extends TestCase
{
	Cache lfu = new LFUCache(0, 0);

	// !! This is a copy of the private LFUObjectCompare class in LFUCache !!
	private static class LFUObjectCompare implements Comparator
	{
		public int compare(final Object o1, final Object o2)
		{
			final CacheObject cmp1 = (CacheObject) o1;
			final CacheObject cmp2 = (CacheObject) o2;
			if (cmp1.getUsage() > cmp2.getUsage())
				return 1;
			if (cmp1.getUsage() < cmp2.getUsage())
				return -1;
			if (cmp1.getCount() > cmp2.getCount())
				return 1;
			if (cmp1.getCount() < cmp2.getCount())
				return -1;
			return 0;
		}
	}

	/**
	 * @param name name of test case
	 */
	public LFUObjectCompareTest(final String name)
	{
		super(name);
	}

	/**
	 * Test method for
	 * tuwien.auto.calimero.cache.LFUObjectCompare#compare(java.lang.Object,
	 * java.lang.Object).
	 */
	public void testCompare()
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

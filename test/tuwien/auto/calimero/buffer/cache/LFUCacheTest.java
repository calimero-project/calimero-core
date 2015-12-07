/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

package tuwien.auto.calimero.buffer.cache;

import junit.framework.TestCase;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.buffer.LDataObject;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * @author B. Malinowsky
 */
public class LFUCacheTest extends TestCase
{
	Cache var, fix, exp;
	CacheObject o1, o2, o3, o4, o5, o6;
	LDataObject co;

	private class ExpCache extends LFUCache
	{
		ExpCache(final int cacheSize, final int timeToExpire)
		{
			super(cacheSize, timeToExpire);
			sweepInterval = 1;
		}
	}

	/**
	 * @param name name of test case
	 */
	public LFUCacheTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		var = new LFUCache(0, 0);
		fix = new LFUCache(5, 0);
		exp = new ExpCache(0, 1);
		o1 = new CacheObject("1", "value 1");
		o2 = new CacheObject("2", "value 2");
		o3 = new CacheObject("3", "value 3");
		o4 = new CacheObject("4", "value 4");
		o5 = new CacheObject("5", "value 5");
		o6 = new CacheObject("6", "value 6");
		co =
			new LDataObject(new CEMILData(CEMILData.MC_LDATA_IND,
				new IndividualAddress(0), new IndividualAddress("1.1.1"),
				new byte[] { 10 }, Priority.NORMAL));
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.cache.LFUCache#clear()}.
	 */
	public void testClear()
	{
		var.put(o1);
		var.put(o2);
		assertEquals(o1, var.get("1"));
		assertEquals(o2, var.get("2"));
		var.clear();
		assertNull(var.get("1"));
		assertNull(var.get("2"));

		fix.put(o1);
		fix.put(o2);
		assertEquals(o1, fix.get("1"));
		assertEquals(o2, fix.get("2"));
		fix.clear();
		assertNull(fix.get("1"));
		assertNull(fix.get("2"));

		exp.put(o1);
		exp.put(o2);
		assertEquals(o1, exp.get("1"));
		assertEquals(o2, exp.get("2"));
		exp.clear();
		assertNull(exp.get("1"));
		assertNull(exp.get("2"));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.LFUCache#get(java.lang.Object)}.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	public void testGet() throws InterruptedException
	{
		// var
		var.put(o1);
		var.put(o2);
		assertEquals(o1, var.get("1"));
		assertEquals(o2, var.get("2"));

		assertNull(var.get(" 1 "));
		assertNull(var.get("1 "));
		assertNull(var.get(" 1"));
		assertNull(var.get(""));

		var.put(o3);
		var.put(o4);
		assertEquals(o3, var.get("3"));
		assertEquals(o4, var.get("4"));
		var.put(o5);
		var.put(o6);
		assertEquals(o5, var.get("5"));
		assertEquals(o6, var.get("6"));

		var.put(co);
		assertEquals(co, var.get(co.getKey()));

		// fix
		fix.put(o1);
		fix.put(o2);
		assertNull(fix.get(" 1 "));
		assertNull(fix.get("1 "));
		assertNull(fix.get(" 1"));
		assertNull(fix.get(""));

		assertEquals(o1, fix.get("1"));
		// put object count to 3
		assertEquals(o1, fix.get("1"));
		assertEquals(o1, fix.get("1"));
		// put object count to 2
		assertEquals(o2, fix.get("2"));
		assertEquals(o2, fix.get("2"));

		fix.put(o3);
		fix.put(o4);
		// count to 2
		assertEquals(o3, fix.get("3"));
		assertEquals(o3, fix.get("3"));
		assertEquals(o4, fix.get("4"));
		assertEquals(o4, fix.get("4"));

		fix.put(o5);
		assertEquals(o5, fix.get("5"));
		// removes o5
		fix.put(o6);
		assertEquals(o6, fix.get("6"));
		assertNull(fix.get("5"));
		// check rest
		assertEquals(o1, fix.get("1"));
		assertEquals(o2, fix.get("2"));
		assertEquals(o3, fix.get("3"));
		assertEquals(o4, fix.get("4"));
		// removes o6
		fix.put(co);
		assertEquals(co, fix.get(co.getKey()));
		assertNull(fix.get("6"));
		// check rest
		assertEquals(o1, fix.get("1"));
		assertEquals(o2, fix.get("2"));
		assertEquals(o3, fix.get("3"));
		assertEquals(o4, fix.get("4"));
		assertEquals(co, fix.get(co.getKey()));
		// put co again, remove old co
		fix.put(co);
		assertEquals(o1, fix.get("1"));
		assertEquals(o2, fix.get("2"));
		assertEquals(o3, fix.get("3"));
		assertEquals(o4, fix.get("4"));
		assertEquals(co, fix.get(co.getKey()));

		// exp
		exp.put(o1);
		exp.put(o2);
		assertEquals(o1, exp.get("1"));
		assertEquals(o2, exp.get("2"));
		Thread.sleep(500);
		exp.put(new CacheObject("new 1", "x"));
		exp.put(new CacheObject("new 2", "y"));
		Thread.sleep(700);
		assertNull("sweepInterval in our test cache ExpCache not adjusted to 1 second?",
			exp.get("1"));
		assertNull(exp.get("2"));
		assertEquals("x", exp.get("new 1").getValue());
		assertEquals("y", exp.get("new 2").getValue());
		Thread.sleep(1000);
		assertNull(exp.get("new 1"));
		assertNull(exp.get("new 2"));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.cache.LFUCache#put
	 * (tuwien.auto.calimero.buffer.cache.CacheObject)}.
	 */
	public void testPut()
	{
		assertEquals(0, o1.getCount());
		assertEquals(0, o2.getCount());
		var.put(o1);
		assertEquals(1, var.get(o1.getKey()).getCount());
		var.put(o2);
		assertEquals(1, var.get(o2.getKey()).getCount());
		var.put(o1);
		assertEquals(1, o1.getCount());
		assertEquals(2, var.get(o1.getKey()).getCount());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.LFUCache#remove(java.lang.Object)}.
	 */
	public void testRemove()
	{
		// var
		var.put(o1);
		var.put(o2);
		assertEquals(o1, var.get("1"));
		assertEquals(o2, var.get("2"));
		var.remove("1");
		assertNull(var.get("1"));
		assertEquals(o2, var.get("2"));
		var.remove("2");
		assertNull(var.get("2"));
		// fix
		fix.put(o1);
		fix.put(o2);
		assertEquals(o1, fix.get("1"));
		assertEquals(o2, fix.get("2"));
		fix.remove("1");
		assertNull(fix.get("1"));
		assertEquals(o2, fix.get("2"));
		fix.remove("2");
		assertNull(fix.get("2"));
		// exp
		exp.put(o1);
		exp.put(o2);
		assertEquals(o1, exp.get("1"));
		assertEquals(o2, exp.get("2"));
		exp.remove("1");
		assertNull(exp.get("1"));
		assertEquals(o2, exp.get("2"));
		exp.remove("2");
		assertNull(exp.get("2"));

	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.cache.LFUCache#removeExpired()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	public void testRemoveExpired() throws InterruptedException
	{
		exp.put(o1);
		exp.put(o2);
		assertEquals(o1, exp.get("1"));
		assertEquals(o2, exp.get("2"));
		Thread.sleep(500);
		exp.put(new CacheObject("new 1", "x"));
		exp.put(new CacheObject("new 2", "y"));
		Thread.sleep(500);
		exp.removeExpired();
		assertNull(exp.get("1"));
		assertNull(exp.get("2"));
		assertEquals("x", exp.get("new 1").getValue());
		assertEquals("y", exp.get("new 2").getValue());
		Thread.sleep(1000);
		exp.removeExpired();
		assertNull(exp.get("new 1"));
		assertNull(exp.get("new 2"));
	}

	/**
	 * Test method for statistics.
	 */
	public void testStatistic()
	{
		assertEquals(0.0, var.statistic().hitRatio(), 0.0);
		var.put(o1);
		assertEquals(0, var.statistic().hits());
		assertEquals(0, var.statistic().misses());
		assertEquals(0, var.statistic().hitRatio(), 0);
		var.get(o1.getKey());
		assertEquals(1, var.statistic().hits());
		assertEquals(0, var.statistic().misses());
		assertEquals(1, var.statistic().hitRatio(), 0);
		var.get(o1.getKey());
		assertEquals(2, var.statistic().hits());
		assertEquals(0, var.statistic().misses());
		assertEquals(1, var.statistic().hitRatio(), 0);
		var.get("invalid key");
		assertEquals(2, var.statistic().hits());
		assertEquals(1, var.statistic().misses());
		assertEquals(2.0 / 3, var.statistic().hitRatio(), 0.000001);
	}

}

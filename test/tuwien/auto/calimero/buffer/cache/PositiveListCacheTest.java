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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.buffer.LDataObject;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * @author B. Malinowsky
 */
public class PositiveListCacheTest extends TestCase
{
	private PositiveListCache fix, exp;
	private CacheObject o1, o2, o3, o4;
	private LDataObject co;

	/**
	 * @param name name of test case
	 */
	public PositiveListCacheTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		fix = new PositiveListCache(0);
		exp = new PositiveListCache(1);
		o1 = new CacheObject("1", "value 1");
		o2 = new CacheObject("2", "value 2");
		o3 = new CacheObject("3", "value 3");
		o4 = new CacheObject("4", "value 4");
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
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#PositiveListCache
	 * (java.util.Collection, int)}.
	 */
	public void testPositiveListCacheCollectionInt()
	{
		final List<Object> v = new Vector<>();
		v.add(new String("1"));
		v.add(o2.getKey());
		v.add(o3.getKey());
		v.add(o4.getKey());
		v.add(co.getKey());
		final PositiveListCache c = new PositiveListCache(v, 0);
		final Object[] list = c.getPositiveList();
		for (int i = 0; i < list.length; ++i)
			assertTrue(v.indexOf(list[i]) != -1);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#setPositiveList
	 * (java.util.Collection)}.
	 */
	public void testSetPositiveList()
	{
		assertEquals(0, fix.getPositiveList().length);
		final String[] a = { "1", "2", "3" };
		final Collection<String> list = Arrays.asList(a);
		fix.setPositiveList(list);
		final Object[] ret = fix.getPositiveList();
		assertEquals(3, fix.getPositiveList().length);
		for (int i = 0; i < ret.length; ++i)
			assertTrue(list.contains(ret[i]));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#addToPositiveList
	 * (java.lang.Object)}.
	 */
	public void testAddToPositiveList()
	{
		// will be overwritten by setPositiveList()
		fix.addToPositiveList("bogus");

		final List<String> v = new Vector<>();
		v.add("1");
		v.add("2");
		fix.setPositiveList(v);
		Object[] ret = fix.getPositiveList();
		assertEquals(2, fix.getPositiveList().length);
		for (int i = 0; i < ret.length; ++i)
			assertTrue(v.contains(ret[i]));
		fix.addToPositiveList("3");
		v.add("3");
		ret = fix.getPositiveList();
		assertEquals(3, fix.getPositiveList().length);
		for (int i = 0; i < ret.length; ++i)
			assertTrue(v.contains(ret[i]));

		// the old list again
		v.remove("3");
		fix.setPositiveList(v);
		ret = fix.getPositiveList();
		assertEquals(2, fix.getPositiveList().length);
		for (int i = 0; i < ret.length; ++i)
			assertTrue(v.contains(ret[i]));

	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#removeFromPositiveList
	 * (java.lang.Object)}.
	 */
	public void testRemoveFromPositiveList()
	{
		fix.removeFromPositiveList("1");

		final String[] a = { "1", "2", };
		final Collection<String> list = Arrays.asList(a);
		fix.setPositiveList(list);
		Object[] ret = fix.getPositiveList();
		assertEquals(2, fix.getPositiveList().length);

		fix.removeFromPositiveList("2");
		ret = fix.getPositiveList();
		assertEquals(1, fix.getPositiveList().length);
		assertEquals("1", ret[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#get(java.lang.Object)}.
	 */
	public void testGet()
	{
		fix.put(o1);
		fix.put(o2);
		assertNull(fix.get(o1.getKey()));
		assertNull(fix.get(o2.getKey()));
		fix.addToPositiveList(o1.getKey());
		fix.put(o1);
		fix.put(o2);
		assertEquals(o1, fix.get(o1.getKey()));
		assertNull(fix.get(o2.getKey()));

		final String[] a = { "1", "2", };
		final Collection<String> list = Arrays.asList(a);
		fix.setPositiveList(list);
		fix.put(o1);
		fix.put(o2);
		assertEquals(o1, fix.get(o1.getKey()));
		assertEquals(o2, fix.get(o2.getKey()));

		fix.setPositiveList(new Vector<String>());
		assertNull(fix.get(o1.getKey()));
		assertNull(fix.get(o2.getKey()));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#put
	 * (tuwien.auto.calimero.buffer.cache.CacheObject)}.
	 */
	public void testPut()
	{
		fix.put(o1);
		fix.put(o2);
		assertNull(fix.get(o1.getKey()));
		assertNull(fix.get(o2.getKey()));
		fix.addToPositiveList(o1.getKey());
		fix.put(o1);
		fix.put(o2);
		assertEquals(o1, fix.get(o1.getKey()));
		assertNull(fix.get(o2.getKey()));
		final CacheObject equal = new CacheObject("1", "equal object");
		fix.put(equal);
		assertEquals(equal.getValue(), fix.get(o1.getKey()).getValue());
		assertEquals(equal.getValue(), fix.get(equal.getKey()).getValue());

		fix.setPositiveList(new Vector<String>());
		fix.put(equal);
		assertNull(fix.get(equal.getKey()));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#remove(java.lang.Object)}.
	 */
	public void testRemove()
	{
		fix.remove(o1.getKey());

		fix.addToPositiveList(o1.getKey());
		fix.put(o1);
		assertEquals(o1, fix.get(o1.getKey()));
		fix.remove(o1.getKey());
		assertNull(fix.get(o1.getKey()));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.PositiveListCache#removeExpired()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	public void testRemoveExpired() throws InterruptedException
	{
		exp.addToPositiveList(o1.getKey());
		exp.addToPositiveList(o2.getKey());

		exp.put(o1);
		exp.put(o2);
		assertEquals(o1, exp.get("1"));
		assertEquals(o2, exp.get("2"));
		Thread.sleep(500);
		exp.addToPositiveList("new 1");
		exp.addToPositiveList("new 2");
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
		final String[] a = { "1", "2", "3" };
		final Collection<String> list = Arrays.asList(a);
		fix.setPositiveList(list);
		fix.put(o1);
		fix.put(o2);
		fix.put(o3);
		assertEquals(0, fix.statistic().hits());
		assertEquals(0, fix.statistic().misses());
		assertEquals(0, fix.statistic().hitRatio(), 0);
		fix.get(o1.getKey());
		assertEquals(1, fix.statistic().hits());
		assertEquals(0, fix.statistic().misses());
		assertEquals(1, fix.statistic().hitRatio(), 0);
		fix.get(o2.getKey());
		assertEquals(2, fix.statistic().hits());
		assertEquals(0, fix.statistic().misses());
		assertEquals(1, fix.statistic().hitRatio(), 0);
		fix.get("invalid key");
		assertEquals(2, fix.statistic().hits());
		assertEquals(1, fix.statistic().misses());
		assertEquals(2.0 / 3, fix.statistic().hitRatio(), 0.000001);
	}
}

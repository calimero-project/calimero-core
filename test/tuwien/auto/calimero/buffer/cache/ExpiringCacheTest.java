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

import junit.framework.TestCase;

/**
 * @author B. Malinowsky
 */
public class ExpiringCacheTest extends TestCase
{

	private class ExpCacheImpl extends ExpiringCache
	{
		boolean notified;
		boolean remove;
		int count;

		/**
		 * @param timeToExpire
		 */
		public ExpCacheImpl(final int timeToExpire)
		{
			super(timeToExpire);
			sweepInterval = 1;
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.Cache#clear()
		 */
		public void clear()
		{}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.Cache#get(java.lang.Object)
		 */
		public CacheObject get(final Object key)
		{
			return (CacheObject) map.get(key);
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.Cache#put(tuwien.auto.calimero.cache.CacheObject)
		 */
		public void put(final CacheObject obj)
		{
			map.put(obj.getKey(), obj);
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.Cache#remove(java.lang.Object)
		 */
		public void remove(final Object key)
		{
			map.remove(key);
		}

		void myStartSweeper()
		{
			startSweeper();
		}

		void myStopSweeper()
		{
			stopSweeper();
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.ExpiringCache#removeExpired()
		 */
		public void removeExpired()
		{
			remove = true;
			super.removeExpired();
			synchronized (this) {
				notifyAll();
			}
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.ExpiringCache#notifyRemoved
		 * (tuwien.auto.calimero.cache.CacheObject)
		 */
		protected void notifyRemoved(final CacheObject obj)
		{
			notified = true;
			count++;
			synchronized (this) {
				notifyAll();
			}
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.cache.Cache#statistic()
		 */
		public Statistic statistic()
		{
			return null;
		}
	}

	private ExpCacheImpl c;

	/**
	 * @param name name of test case
	 */
	public ExpiringCacheTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		c = new ExpCacheImpl(1);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
		c.myStopSweeper();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.ExpiringCache#ExpiringCache(int)}.
	 */
	public void testExpiringCache()
	{
		c.myStartSweeper();
		try {
			synchronized (c) {
				c.wait(1300);
			}
		}
		catch (final InterruptedException e) {}
		assertTrue(c.remove);

		ExpCacheImpl c2 = new ExpCacheImpl(0);
		try {
			synchronized (c2) {
				c2.wait(1300);
			}
		}
		catch (final InterruptedException e) {
			fail("no remove should be called");
		}
		assertFalse(c2.remove);

		c2 = new ExpCacheImpl(-1);
		try {
			synchronized (c2) {
				c2.wait(1300);
			}
		}
		catch (final InterruptedException e) {
			fail("notifyRemoved shouldnt be called");
		}
		assertFalse(c2.remove);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.ExpiringCache#removeExpired()}.
	 */
	public void testRemoveExpired()
	{
		c.myStartSweeper();
		c.put(new CacheObject("key1", "value"));
		c.put(new CacheObject("key2", "value"));
		c.put(new CacheObject("key3", "value"));
		try {
			synchronized (c) {
				c.wait(500);
			}
		}
		catch (final InterruptedException e) {
			fail("remove shouldnt be called yet");
		}
		assertFalse(c.notified);
		assertFalse(c.remove);
		assertNotNull(c.get("key1"));
		assertNotNull(c.get("key2"));
		assertNotNull(c.get("key3"));
		c.put(new CacheObject("key4", "value"));

		try {
			synchronized (c) {
				c.wait(300);
			}
		}
		catch (final InterruptedException e) {
			fail("remove shouldnt be called yet");
		}
		assertFalse(c.notified);
		assertFalse(c.remove);
		assertNotNull(c.get("key1"));
		assertNotNull(c.get("key2"));
		assertNotNull(c.get("key3"));
		c.remove("key1");
		c.put(new CacheObject("key1", "value"));

		try {
			synchronized (c) {
				c.wait(400);
			}
		}
		catch (final InterruptedException e) {}
		assertTrue(c.notified);
		assertTrue(c.remove);
		assertEquals(2, c.count);
		assertNull(c.get("key"));
		assertNotNull(c.get("key1"));
		assertNotNull(c.get("key4"));
		assertNull(c.get("key2"));
		assertNull(c.get("key3"));
	}

	/**
	 * Test method for
	 * tuwien.auto.calimero.cache.ExpiringCache#notifyRemoved
	 * (tuwien.auto.calimero.cache.CacheObject).
	 */
	public void testNotifyRemoved()
	{
		c.myStartSweeper();
		c.put(new CacheObject("key", "value"));
		try {
			synchronized (c) {
				c.wait(600);
			}
		}
		catch (final InterruptedException e) {
			fail("should not happen");
		}
		assertFalse(c.notified);
		try {
			synchronized (c) {
				c.wait(600);
			}
		}
		catch (final InterruptedException e) {
			fail("should not happen");
		}
		assertTrue(c.notified);
	}

	/**
	 * Test method for tuwien.auto.calimero.cache.ExpiringCache#startSweeper().
	 */
	public void testStartSweeper()
	{
		c.myStartSweeper();
		c.myStopSweeper();
		c.myStartSweeper();
		c.myStartSweeper();
		try {
			synchronized (c) {
				c.wait(1200);
			}
		}
		catch (final InterruptedException e) {
			fail("no remove was called while cache sweeping running");
		}
		assertTrue("no remove was called while cache sweeping running", c.remove);
	}

	/**
	 * Test method for tuwien.auto.calimero.cache.ExpiringCache#stopSweeper().
	 */
	public void testStopSweeper()
	{
		c.myStartSweeper();
		c.myStartSweeper();
		c.myStartSweeper();
		c.myStopSweeper();
		try {
			synchronized (c) {
				c.wait(1200);
			}
		}
		catch (final InterruptedException e) {
			fail("remove was called without cache sweeping");
		}
		assertFalse(c.remove);
		assertFalse(c.notified);
		assertEquals(0, c.count);
	}

}

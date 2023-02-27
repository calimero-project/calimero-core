/*
    Calimero 2 - A library for KNX network access
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiringCacheTest {
	private static class ExpCacheImpl extends ExpiringCache {
		boolean notified;
		boolean remove;
		int count;

		/** @param timeToExpire */
		ExpCacheImpl(final int timeToExpire) {
			super(timeToExpire);
			sweepInterval = 1;
		}

		@Override
		public void clear() {}

		@Override
		public CacheObject get(final Object key) {
			return map.get(key);
		}

		@Override
		public void put(final CacheObject obj) {
			map.put(obj.getKey(), obj);
		}

		@Override
		public void remove(final Object key) {
			map.remove(key);
		}

		void myStartSweeper() {
			startSweeper();
		}

		void myStopSweeper() {
			stopSweeper();
		}

		@Override
		public void removeExpired() {
			remove = true;
			super.removeExpired();
			synchronized (this) {
				notifyAll();
			}
		}

		@Override
		protected void notifyRemoved(final CacheObject obj) {
			notified = true;
			count++;
			synchronized (this) {
				notifyAll();
			}
		}

		@Override
		public Statistic statistic() {
			return null;
		}
	}

	private ExpCacheImpl c;


	@BeforeEach
	void init() {
		c = new ExpCacheImpl(1);
	}

	@AfterEach
	void tearDown() {
		c.myStopSweeper();
	}

	@Test
	void expiringCache() {
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

	@Test
	void removeExpired() {
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

	@Test
	void notifyRemoved() {
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

	@Test
	void startSweeper() {
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
		assertTrue(c.remove, "no remove was called while cache sweeping running");
	}

	@Test
	void stopSweeper() {
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

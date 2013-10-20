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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A {@link Cache} using a LFU replacement policy.
 * <p>
 * The usage value of {@link CacheObject#getUsage()} equals the access count,
 * {@link CacheObject#getCount()}.
 * 
 * @author B. Malinowsky
 */
public class LFUCache extends ExpiringCache
{
	private final SortedMap tree;
	private int maxSize;
	private long hits;
	private long misses;
	
	/**
	 * Creates a new LFU cache.
	 * <p>
	 * Optionally, a maximum cache size and an expiring time can be specified.
	 * 
	 * @param cacheSize maximum number of {@link CacheObject}s in the cache, or
	 *        0 for no maximum
	 * @param timeToExpire time in seconds for cache objects to stay valid,
	 *        or 0 for no expiring
	 */
	public LFUCache(final int cacheSize, final int timeToExpire)
	{
		super(timeToExpire);
		if (cacheSize > 0)
			maxSize = cacheSize;
		tree = new TreeMap(new LFUObjectCompare());
	}

	/**
	 * {@inheritDoc}<br>
	 * If expiring of cache objects is set, and the timestamp of a
	 * {@link CacheObject} is renewed after it has been put into the cache, a
	 * new {@link #put(CacheObject)} is required for that object to apply the
	 * timestamp and keep the cache in a consistent state.
	 */
	public synchronized void put(final CacheObject obj)
	{
		// ensure sweeping is on if we have expiring objects
		startSweeper();
		final Object old = map.remove(obj.getKey());
		if (old != null)
			tree.remove(old);
		else
			ensureSizeLimits();
		obj.resetTimestamp();
		map.put(obj.getKey(), obj);
		tree.put(obj, obj);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#get(java.lang.Object)
	 */
	public synchronized CacheObject get(final Object key)
	{
		final CacheObject o = (CacheObject) map.get(key);
		if (o != null) {
			tree.remove(o);
			updateAccess(o);
			tree.put(o, o);
			++hits;
		}
		else
			++misses;
		return o;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#remove(java.lang.Object)
	 */
	public synchronized void remove(final Object key)
	{
		final Object o = map.remove(key);
		if (o != null)
			tree.remove(o);
	}
	
	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#clear()
	 */
	public synchronized void clear()
	{
		stopSweeper();
		map.clear();
		tree.clear();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#statistic()
	 */
	public synchronized Statistic statistic()
	{
		return new StatisticImpl(hits, misses);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.ExpiringCache#notifyRemoved(
	 * tuwien.auto.calimero.buffer.cache.CacheObject)
	 */
	protected final void notifyRemoved(final CacheObject obj)
	{
		tree.remove(obj);
	}

	private void ensureSizeLimits()
	{
		if (maxSize > 0)
			while (map.size() >= maxSize)
				remove(((CacheObject) tree.firstKey()).getKey());
	}

	private static class LFUObjectCompare implements Comparator
	{
		LFUObjectCompare() {}
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
}

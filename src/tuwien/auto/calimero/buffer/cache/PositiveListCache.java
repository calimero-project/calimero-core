/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * A static {@link Cache} using a positive list with allowed keys for the cache.
 * <p>
 * The positive list contains key objects which are allowed to be cached. On a
 * {@link #put(CacheObject)} operation, {@link CacheObject#getKey()} is checked for a
 * positive match in that list in order to be allowed for caching.
 * <p>
 * This cache does not use a replacement policy (static cache).<br>
 * Nevertheless, a timeout can be given to specify the expiring time of cache values.
 * <p>
 * The usage value of {@link CacheObject#getUsage()} equals the access count,
 * {@link CacheObject#getCount()}.
 *
 * @author B. Malinowsky
 */
public class PositiveListCache extends ExpiringCache
{
	private Set<Object> posList = new HashSet<>();
	private long hits;
	private long misses;

	/**
	 * Creates a new {@link PositiveListCache}.
	 * <p>
	 * Optionally, an expiring time can be specified.
	 *
	 * @param timeToExpire timespan in seconds for cache objects to stay valid,
	 *        or 0 for no expiring
	 */
	public PositiveListCache(final int timeToExpire)
	{
		super(timeToExpire);
	}

	/**
	 * Creates a new {@link PositiveListCache} and inits the positive key list.
	 * <p>
	 * Optionally, an expiring time can be specified.
	 *
	 * @param positiveList a Collection holding the allowed keys for this cache
	 * @param timeToExpire timespan in seconds for cache objects to stay valid,
	 *        or 0 for no expiring
	 */
	public PositiveListCache(final Collection<? extends Object> positiveList, final int timeToExpire)
	{
		this(timeToExpire);
		setPositiveList(positiveList);
	}

	/**
	 * Sets a new positive list for this cache.
	 * <p>
	 * The old list is discarded. All cache objects will be updated immediately
	 * according to the new list.
	 *
	 * @param positiveList a Collection holding the allowed keys for this cache
	 */
	public final synchronized void setPositiveList(final Collection<? extends Object> positiveList)
	{
		if (posList.size() == 0)
			posList.addAll(positiveList);
		else {
			posList = new HashSet<>(positiveList);
			// remove old keys not in the new list anymore
			for (final Iterator<Object> i = map.keySet().iterator(); i.hasNext(); )
				if (!posList.contains(i.next()))
					i.remove();
		}
	}

	/**
	 * Adds a new allowed <code>key</code> to the positive list, if it is not
	 * already present.
	 *
	 * @param key the new key object
	 */
	public final synchronized void addToPositiveList(final Object key)
	{
		posList.add(key);
	}

	/**
	 * Removes the <code>key</code> from the positive list, if it is present.
	 * <p>
	 * The cache objects will be updated immediately according to the removed
	 * key.
	 *
	 * @param key key object to remove
	 */
	public final synchronized void removeFromPositiveList(final Object key)
	{
		if (posList.remove(key))
			remove(key);
	}

	/**
	 * Returns the positive list currently used by this cache.
	 *
	 * @return array of all allowed key objects.
	 */
	public final synchronized Object[] getPositiveList()
	{
		return posList.toArray();
	}

	/**
	 * For a {@link CacheObject} to be put into the cache, its key
	 * {@link CacheObject#getKey()} has to be equal to one in the positive list of this
	 * cache.<br>
	 * If expiring of cache objects is set, and the timestamp of a {@link CacheObject} is
	 * renewed externally after it has been put into the cache, a new
	 * {@link #put(CacheObject)} is required for that object to apply the new timestamp
	 * and keep the cache in a consistent state.
	 */
	@Override
	public synchronized void put(final CacheObject obj)
	{
		if (posList.contains(obj.getKey())) {
			startSweeper();
			obj.resetTimestamp();
			// maintain insertion order, if any
			if (map instanceof LinkedHashMap)
				map.remove(obj.getKey());
			map.put(obj.getKey(), obj);
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#get(java.lang.Object)
	 */
	@Override
	public synchronized CacheObject get(final Object key)
	{
		final CacheObject o = map.get(key);
		if (o != null) {
			updateAccess(o);
			++hits;
		}
		else
			++misses;
		return o;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#remove(java.lang.Object)
	 */
	@Override
	public synchronized void remove(final Object key)
	{
		map.remove(key);
	}

	/**
	 * {@inheritDoc}<br>
	 * This does not affect the positive list.
	 */
	@Override
	public synchronized void clear()
	{
		stopSweeper();
		map.clear();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache#statistic()
	 */
	@Override
	public synchronized Statistic statistic()
	{
		return new StatisticImpl(hits, misses);
	}
}

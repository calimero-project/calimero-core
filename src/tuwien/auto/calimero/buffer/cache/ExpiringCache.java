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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements a cache expiring mechanism for {@link CacheObject}s.
 * <p>
 * A time span is specified for how long a value is considered valid. After that time for
 * expiring, the cache object is removed on the next call of {@link #removeExpired()} by
 * an internal {@link CacheSweeper}. If a cache with expiring cache objects is not used
 * anymore, invoke {@link Cache#clear()} to quit the running cache sweeping mechanism.<br>
 * The timestamp of {@link CacheObject#getTimestamp()} is used to determine if a cache
 * object value has expired.<br>
 * Note that if the timestamp of a {@link CacheObject} changes after it was put into the
 * cache, the object has to be reinserted (see {@link #put(CacheObject)}) to keep the
 * cache in a consistent state. If no expiring is used, this might be omitted.<br>
 * 
 * @author B. Malinowsky
 */
public abstract class ExpiringCache implements Cache
{
	/**
	 * Default sweep interval in seconds used for a {@link CacheSweeper}
	 * ({@value defaultSweepInterval} seconds).
	 * <p>
	 */
	protected static final int defaultSweepInterval = 60;

	/**
	 * Sweep interval in seconds used for the {@link CacheSweeper}.
	 * <p>
	 * It defaults to {@value #defaultSweepInterval} seconds. For a new value to take
	 * effect, it has to be assigned either before the first {@link #startSweeper()} call,
	 * or the cache sweeper has to be restarted.
	 */
	protected int sweepInterval = defaultSweepInterval;

	/**
	 * The map holding the {@link CacheObject}s.
	 * <p>
	 * The map instance itself is not synchronized, synchronization is done using the
	 * cache object (this).
	 */
	protected Map map;
	private CacheSweeper sweeper;
	private final int timeToExpire;

	/**
	 * Creates an {@link ExpiringCache}.
	 * <p>
	 * Note that for the actual begin of cache sweeping {@link #startSweeper()} has to be
	 * invoked.
	 * 
	 * @param timeToExpire time > 0 in seconds for cache entries to stay valid, on time =
	 *        0 no expiring of cache entries will occur
	 */
	public ExpiringCache(final int timeToExpire)
	{
		if (timeToExpire > 0) {
			this.timeToExpire = timeToExpire;
			map = new LinkedHashMap();
		}
		else {
			this.timeToExpire = 0;
			map = new HashMap();
		}
	}

	/**
	 * Removes all {@link CacheObject}s, where<br>
	 * <code>{@link CacheObject#getTimestamp()} + timeToExpire &lt;= now</code>, with
	 * <code>timeToExpire &gt; 0</code> and <code>now</code> is the point of time
	 * {@link #removeExpired()} is invoked. <br>
	 * If no expiring time was specified at creation of cache, no cache object will be
	 * expired.
	 */
	public void removeExpired()
	{
		if (timeToExpire == 0 || !(map instanceof LinkedHashMap))
			return;
		final long now = System.currentTimeMillis();
		final long duration = timeToExpire * 1000;
		CacheObject o = null;
		synchronized (this) {
			for (final Iterator i = map.values().iterator(); i.hasNext();) {
				o = (CacheObject) i.next();
				if (now >= o.getTimestamp() + duration) {
					i.remove();
					notifyRemoved(o);
				}
				else
					break;
			}
		}
	}

	/**
	 * Override this method to get notified when {@link #removeExpired()} removed a
	 * {@link CacheObject} from the {@link #map}.
	 * <p>
	 * 
	 * @param obj removed {@link CacheObject}
	 */
	protected void notifyRemoved(final CacheObject obj)
	{}

	/**
	 * Starts a new {@link CacheSweeper}, if not already running, and if an expiring time
	 * for {@link CacheObject} was specified.<br>
	 * If the methods <code>startSweeper</code> and <code>stopSweeper</code> are invoked
	 * by different threads, they need to be synchronized.
	 */
	protected final void startSweeper()
	{
		if (timeToExpire > 0 && sweeper == null)
			(sweeper = new CacheSweeper(this, sweepInterval)).start();
	}

	/**
	 * Stops the {@link CacheSweeper}, if any.
	 */
	protected final void stopSweeper()
	{
		final CacheSweeper s = sweeper;
		if (s != null) {
			s.stopSweeper();
			sweeper = null;
		}
	}
	
	static void updateAccess(final CacheObject obj)
	{
		obj.incCount();
		obj.setUsage(obj.getCount());
	}
}

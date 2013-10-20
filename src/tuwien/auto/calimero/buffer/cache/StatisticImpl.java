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

import tuwien.auto.calimero.buffer.cache.Cache.Statistic;

/**
 * Provide basic {@link Cache} information as required by {@link Cache.Statistic}.
 * <p>
 * This statistics implementation is immutable.
 * 
 * @author B. Malinowsky
 * @see Cache.Statistic
 */
public class StatisticImpl implements Statistic
{
	private final long hits;
	private final long misses;

	/**
	 * Creates an instance and fills it with data.
	 * <p>
	 * 
	 * @param hits cache hit count
	 * @param misses cache miss count
	 */
	public StatisticImpl(final long hits, final long misses)
	{
		this.hits = hits;
		this.misses = misses;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache.Statistic#hits()
	 */
	public final long hits()
	{
		return hits;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache.Statistic#misses()
	 */
	public final long misses()
	{
		return misses;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache.Statistic#hitRatio()
	 */
	public final double hitRatio()
	{
		final double total = hits + misses;
		return total == 0.0 ? 0.0 : hits / total;
	}
}

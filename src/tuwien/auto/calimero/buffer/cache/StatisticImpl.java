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
	@Override
	public final long hits()
	{
		return hits;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache.Statistic#misses()
	 */
	@Override
	public final long misses()
	{
		return misses;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.buffer.cache.Cache.Statistic#hitRatio()
	 */
	@Override
	public final double hitRatio()
	{
		final double total = hits + misses;
		return total == 0.0 ? 0.0 : hits / total;
	}
}

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

package performance.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author B. Malinowsky
 */
public class PerfTimer
{
	private final List<Long> start = new ArrayList<>();
	private final List<Long> stop = new ArrayList<>();

	/**
	 * Creates a new performance timer.
	 */
	public PerfTimer()
	{}

	/**
	 * Starts a new timer and puts it into the timer list.
	 */
	public void start()
	{
		start.add(new Long(System.currentTimeMillis()));
	}

	/**
	 * Stops the first running timer.
	 */
	public void stop()
	{
		stop.add(new Long(System.currentTimeMillis()));
	}

	/**
	 * Returns all timing durations in milliseconds.
	 *
	 * @return durations array
	 */
	Integer[] getDurations()
	{
		return getDurations(0);
	}

	/**
	 * Returns all timing durations in milliseconds without <code>omitExtremes</code>
	 * number of extremes in the durations value range.
	 *
	 * @param omitExtremes number of extremes to leave out, alternating one maximum and
	 *        one minimum extreme is left out, starting with a maximum
	 * @return durations array
	 */
	Integer[] getDurations(final int omitExtremes)
	{
		final List<Integer> buf = new ArrayList<>();
		final int size = Math.min(start.size(), stop.size());
		for (int i = 0; i < size; ++i)
			buf.add(new Integer((int) (stop.get(i).longValue() - start.get(i).longValue())));
		for (int i = 0; i < omitExtremes; ++i)
			if (i % 2 == 0)
				buf.remove(Collections.max(buf));
			else
				buf.remove(Collections.min(buf));
		return buf.toArray(new Integer[0]);
	}

	float getAverageDuration()
	{
		final int durations = Math.min(start.size(), stop.size());
		if (durations == 0)
			return 0;
		long sum = 0;
		for (int i = 0; i < durations; ++i)
			sum += stop.get(i).longValue() - start.get(i).longValue();
		return (float) sum / durations;
	}
}

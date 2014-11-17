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

import java.io.PrintStream;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @author B. Malinowsky
 */
public class PerfTestCase extends TestCase
{
	/** rounds to perform before timing the test. */
	protected int warmups = 10;
	/** rounds to perform for measuring the test. */
	protected int measure = 10;
	/** output print stream for results. */
	protected PrintStream out = System.out;

	private int normalize = 1;
	private final PerfTimer t = new PerfTimer();

	/**
	 * Creates a new test case for measuring performance.
	 */
	public PerfTestCase()
	{
		super();
	}

	/**
	 * Creates a new test case for measuring performance.
	 * <p>
	 * 
	 * @param name name of test case name of test case
	 */
	public PerfTestCase(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#runBare()
	 */
	@Override
	public void runBare() throws Throwable
	{
		setUp();
		Thread.currentThread().setPriority(10);
		assertEquals(Thread.currentThread().getPriority(), 10);
		try {
			for (int i = 0; i < warmups; ++i)
				runTest();
			for (int i = 0; i < measure; ++i) {
				Thread.sleep(1);
				t.start();
				try {
					runTest();
				}
				finally {
					t.stop();
				}
			}
		}
		finally {
			tearDown();
		}
	}

	/**
	 * Returns the number of rounds to perform before timing the test.
	 * <p>
	 * 
	 * @return number of warm-up rounds
	 */
	public int getWarmupLaps()
	{
		return warmups;
	}

	/**
	 * Returns the number of rounds to perform for measuring the test.
	 * <p>
	 * 
	 * @return number of measure rounds
	 */
	public int getMeasureLaps()
	{
		return measure;
	}

	/**
	 * Returns the normalize value used for the test case result.
	 * <p>
	 * 
	 * @return normalize value
	 */
	public int getNormalize()
	{
		return normalize;
	}

	/**
	 * Sets the normalize value used for calculating the test result.
	 * <p>
	 * It is used if one measure round consists of more than one iteration in the test
	 * case, to prevent extreme short time measurements for example. Then the normalize
	 * value is set to the number of iterations and considered in the result.<br>
	 * By default, normalize is set to 1.
	 * 
	 * @param normalizer new normalize value
	 */
	public void setNormalize(final int normalizer)
	{
		normalize = normalizer;
	}

	/**
	 * Prints out the test results using the test case default output stream.
	 */
	public void printResults()
	{
		final Integer[] d = t.getDurations();
		out.println("Timing results for " + getName());
		out.println("Laps: " + warmups + " warmups, " + measure + " measured (out of "
			+ d.length + "), " + (warmups + measure) + " total");
		out.print("Avg lap: " + t.getAverageDuration() + " ms, normalized: "
			+ (t.getAverageDuration() / normalize) + " ms");
		if (d.length > 3) {
			final Integer[] d2 = t.getDurations(2);
			long sum = 0;
			for (int i = 0; i < d2.length; ++i)
				sum += d2[i].intValue();
			final float avg = (float) sum / d2.length;
			out.print(" (" + avg + " ms / " + (avg / normalize)
				+ " ms without extremes)");
		}
		out.println();
		out.print("Lap times: ");
		out.println(Arrays.asList(d));
	}

	/**
	 * Returns the performance timer.
	 * <p>
	 * 
	 * @return performance timer object
	 */
	protected PerfTimer getTimer()
	{
		return t;
	}
	
	public final void testDummy()
	{
		// prevents jUnit warning "no test found in test case"
	}
}

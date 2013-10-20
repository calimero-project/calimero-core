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

package tuwien.auto.calimero.buffer.cache.performance;

import java.util.ArrayList;
import java.util.List;

import performance.base.PerfTestCase;


/**
 * @author B. Malinowsky
 */
public class QueueTest extends PerfTestCase
{
	private final int iterations = 100000;
	private final int capacity = 30;
	private LongVector primitive;
	private List list;
	private LongVector primitiveFilled;
	private List listFilled;

	private static final class LongVector
	{
		private long[] arr;
		private int size;

		LongVector(final int initCapacity)
		{
			arr = new long[initCapacity];
		}

		void add(final long value)
		{
			if (size == arr.length) {
				final long[] old = arr;
				// yes, someone might have an old buffer size of 1...
				final int newCapacity = (size * 3) / 2 + 1;
				arr = new long[newCapacity];
				System.arraycopy(old, 0, arr, 0, size);
			}
			arr[size++] = value;
		}

		int capacity()
		{
			return arr.length;
		}

		long get(final int index)
		{
			return arr[index];
		}

		void set(final int index, final long value)
		{
			arr[index] = value;
		}

		int size()
		{
			return size;
		}

		void clear()
		{
			while (size > 0)
				arr[--size] = 0;
		}

		long[] toArray()
		{
			final long[] buf = new long[size];
			System.arraycopy(arr, 0, buf, 0, size);
			return buf;
		}
	}

	/**
	 * @param name name of test case
	 */
	public QueueTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		setNormalize(iterations);
		primitive = new LongVector(capacity);
		list = new ArrayList(capacity);

		primitiveFilled = new LongVector(capacity);
		listFilled = new ArrayList(capacity);
		for (int i = 0; i < capacity; ++i) {
			primitiveFilled.add(System.currentTimeMillis());
			listFilled.add(new Long(System.currentTimeMillis()));
		}
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		printResults();
	}

	/**
	 * Test method for LongVector.
	 */
	public void testLongVectorSet()
	{
		for (int i = 0; i < iterations; ++i) {
			final long time = System.currentTimeMillis();
			primitive.set(i % capacity, time);
			primitive.set(i % capacity, time);
			primitive.set(i % capacity, time);
			primitive.set(i % capacity, time);
			primitive.set(i % capacity, time);
		}
	}

	/**
	 * Test method for ArrayList.
	 */
	public void testArrayListSet()
	{
		for (int i = 0; i < capacity; ++i)
			list.add(new Long(System.currentTimeMillis()));

		for (int i = capacity; i < iterations; ++i) {
			final Long time = new Long(System.currentTimeMillis());
			list.set(i % capacity, time);
			list.set(i % capacity, time);
			list.set(i % capacity, time);
			list.set(i % capacity, time);
			list.set(i % capacity, time);
		}
	}

	/**
	 * Test method for LongVector to long[] conversion.
	 */
	public void testLongVectorToArray()
	{
		for (int i = 0; i < iterations; ++i) {
			primitiveFilled.toArray();
		}
	}

	/**
	 * Test method for ArrayList to long[] conversion.
	 */
	public void testArrayListToArray()
	{
		for (int i = 0; i < iterations; ++i) {
			listFilled.toArray(new Long[0]);
		}
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

package tuwien.auto.calimero.buffer.cache.performance;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import performance.base.PerfTestCase;


class QueueTest extends PerfTestCase
{
	private final int iterations = 100000;
	private final int capacity = 30;
	private LongVector primitive;
	private List<Long> list;
	private LongVector primitiveFilled;
	private List<Long> listFilled;

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

//		int capacity()
//		{
//			return arr.length;
//		}
//
//		long get(final int index)
//		{
//			return arr[index];
//		}

		void set(final int index, final long value)
		{
			arr[index] = value;
		}

//		int size()
//		{
//			return size;
//		}
//
//		void clear()
//		{
//			while (size > 0)
//				arr[--size] = 0;
//		}

		long[] toArray()
		{
			final long[] buf = new long[size];
			System.arraycopy(arr, 0, buf, 0, size);
			return buf;
		}
	}


	@BeforeEach
	void init() throws Exception
	{
		setNormalize(iterations);
		primitive = new LongVector(capacity);
		list = new ArrayList<>(capacity);

		primitiveFilled = new LongVector(capacity);
		listFilled = new ArrayList<>(capacity);
		for (int i = 0; i < capacity; ++i) {
			primitiveFilled.add(System.currentTimeMillis());
			listFilled.add(System.currentTimeMillis());
		}
	}

	@AfterEach
	void tearDown() throws Exception
	{
		printResults();
	}

	@Test
	void longVectorSet()
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

	@Test
	void arrayListSet()
	{
		for (int i = 0; i < capacity; ++i)
			list.add(System.currentTimeMillis());

		for (int i = capacity; i < iterations; ++i) {
			final Long time = System.currentTimeMillis();
			list.set(i % capacity, time);
			list.set(i % capacity, time);
			list.set(i % capacity, time);
			list.set(i % capacity, time);
			list.set(i % capacity, time);
		}
	}

	@Test
	void longVectorToArray()
	{
		for (int i = 0; i < iterations; ++i) {
			primitiveFilled.toArray();
		}
	}

	@Test
	void arrayListToArray()
	{
		for (int i = 0; i < iterations; ++i) {
			listFilled.toArray(new Long[0]);
		}
	}
}

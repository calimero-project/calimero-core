/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

package tuwien.auto.calimero.buffer;

import junit.framework.TestCase;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * @author B. Malinowsky
 */
public class LDataObjectQueueTest extends TestCase
{
	private LDataObjectQueue var, fix, ring, one;
	private CEMILData frame1, frame2, frame3, frame4, frame5, diff;
	private QueueListener l;

	private final class QueueListener implements LDataObjectQueue.QueueListener
	{
		LDataObjectQueue q;

		@Override
		public void queueFilled(final LDataObjectQueue queue)
		{
			q = queue;
		}
	}

	/**
	 * @param name name of test case
	 */
	public LDataObjectQueueTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		l = new QueueListener();
		var = new LDataObjectQueue(new GroupAddress("10.4.10"));
		fix = new LDataObjectQueue(new GroupAddress("10.4.10"), false, 4, false, l);
		ring = new LDataObjectQueue(new GroupAddress("10.4.10"), true, 2, true, l);
		one = new LDataObjectQueue(new GroupAddress("10.4.10"), true, 1, false, l);

		frame1 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("10.4.10"), new byte[] { 1, }, Priority.NORMAL);
		frame2 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("10.4.10"), new byte[] { 2, }, Priority.NORMAL);
		frame3 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("10.4.10"), new byte[] { 3, }, Priority.NORMAL);
		frame4 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("10.4.10"), new byte[] { 4, }, Priority.NORMAL);
		frame5 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("10.4.10"), new byte[] { 5, }, Priority.NORMAL);
		diff =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("5.5.5"), new byte[] { 6, }, Priority.NORMAL);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObjectQueue#getFrame()}.
	 */
	public void testGetFrame()
	{
		// var
		assertNull(var.getFrame());
		var.setFrame(frame1);
		assertEquals(frame1, var.getFrame());
		assertEquals(frame1, var.getFrame());
		assertEquals(frame1, var.getFrame());
		var.setFrame(frame2);
		assertEquals(frame1, var.getFrame());
		// fix
		assertNull(fix.getFrame());
		fix.setFrame(frame5);
		assertEquals(frame5, fix.getFrame());

		fix.setFrame(frame4);
		assertEquals(frame5, fix.getFrame());

		fix.setFrame(frame3);
		assertNull(l.q);
		fix.setFrame(frame2);
		assertEquals(fix, l.q);
		assertEquals(frame5, fix.getFrame());
		// this one shoudn't be set now
		l.q = null;
		fix.setFrame(frame1);
		assertEquals(frame5, fix.getFrame());
		assertNull(l.q);

		// ring
		assertNull(ring.getFrame());
		ring.setFrame(frame5);
		assertEquals(frame5, ring.getFrame());
		assertNull(l.q);
		// we consumed one frame, so no notifying
		ring.setFrame(frame4);
		assertNull(l.q);
		assertEquals(frame4, ring.getFrame());

		ring.setFrame(frame3);
		assertNull(l.q);
		ring.setFrame(frame2);
		assertEquals(ring, l.q);

		assertEquals(frame3, ring.getFrame());
		ring.setFrame(frame1);
		assertEquals(frame2, ring.getFrame());
		assertEquals(frame1, ring.getFrame());
		assertNull(ring.getFrame());

		ring.setFrame(frame1);
		ring.setFrame(frame2);
		ring.setFrame(frame3);
		ring.setFrame(frame4);
		ring.setFrame(frame5);
		assertEquals(frame4, ring.getFrame());
		assertEquals(frame5, ring.getFrame());

		// one
		assertNull(one.getFrame());
		one.setFrame(frame2);
		assertEquals(frame2, one.getFrame());
		assertNull(one.getFrame());
		one.setFrame(frame1);
		one.setFrame(frame3);
		assertEquals(frame1, one.getFrame());
		assertNull(one.getFrame());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.LDataObjectQueue#setFrame(CEMILData)}.
	 */
	public void testSet()
	{
		// var
		boolean failed = false;
		try {
			var.setFrame(null);
		}
		catch (final NullPointerException e) {
			failed = true;
		}
		assertTrue(failed);
		assertNull(var.getFrame());
		var.setFrame(frame1);
		assertEquals(frame1, var.getFrame());
		try {
			var.setFrame(diff);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertEquals(frame1, var.getFrame());
		// fix
		fix.setFrame(frame5);
		assertEquals(frame5, fix.getFrame());
		try {
			fix.setFrame(diff);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertEquals(frame5, fix.getFrame());
		fix.setFrame(frame4);
		assertEquals(frame5, fix.getFrame());
		// ring
		ring.setFrame(frame5);
		assertEquals(frame5, ring.getFrame());
		try {
			ring.setFrame(diff);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertNull(ring.getFrame());
		// one
		try {
			one.setFrame(diff);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertNull(one.getFrame());
		one.setFrame(frame2);
		one.setFrame(frame5);
		assertEquals(frame2, one.getFrame());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.LDataObjectQueue#LDataObjectQueue
	 * (tuwien.auto.calimero.GroupAddress)}.
	 */
	public void testCEMICacheObjectQueueGroupAddress()
	{
		var = null;
		try {
			var = new LDataObjectQueue(null);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertNull(var);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.LDataObjectQueue#LDataObjectQueue(GroupAddress,
	 * boolean, int, boolean,
	 * tuwien.auto.calimero.buffer.LDataObjectQueue.QueueListener)}.
	 *
	 * @throws KNXFormatException
	 */
	public void testCEMICacheObjectQueueGroupAddressIntBooleanBoolean()
		throws KNXFormatException
	{
		var = null;
		try {
			var = new LDataObjectQueue(new GroupAddress("1.1.1"), false, -1, false, null);
		}
		catch (final RuntimeException e) {}
		assertNull(var);
		try {
			var = new LDataObjectQueue(new GroupAddress("1.1.1"), false, 0, false, null);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertNull(var);
		try {
			var = new LDataObjectQueue(new GroupAddress("1.1.1"), false, 1, false, null);
		}
		catch (final KNXIllegalArgumentException e) {
			fail("correct ctor, should not fail");
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObjectQueue#getFrames()}.
	 */
	public void testGetFrames()
	{
		// var
		assertEquals(0, var.getFrames().length);
		final CEMI[] buf = new CEMI[] { frame1, frame2, frame3, frame4 };
		var.setFrame(frame1);
		var.setFrame(frame2);
		var.setFrame(frame3);
		var.setFrame(frame4);
		assertEquals(4, var.getFrames().length);
		assertEquals(var.getSize(), var.getFrames().length);
		for (int i = 0; i < var.getFrames().length; ++i)
			assertEquals(buf[i], var.getFrames()[i]);

		// fix
		assertEquals(0, fix.getFrames().length);
		assertEquals(0, fix.getSize());
		final CEMI[] buf2 = new CEMI[] { frame5, frame4, frame3, frame2, frame1 };
		fix.setFrame(frame5);
		fix.setFrame(frame4);
		assertEquals(2, fix.getFrames().length);
		for (int i = 0; i < fix.getFrames().length; ++i)
			assertEquals(buf2[i], fix.getFrames()[i]);
		fix.setFrame(frame3);
		fix.setFrame(frame2);
		assertEquals(4, fix.getFrames().length);
		assertEquals(fix.getSize(), fix.getFrames().length);
		for (int i = 0; i < fix.getFrames().length; ++i)
			assertEquals(buf2[i], fix.getFrames()[i]);
		fix.setFrame(frame1);
		assertEquals(4, fix.getFrames().length);
		assertEquals(fix.getSize(), fix.getFrames().length);
		for (int i = 0; i < fix.getFrames().length; ++i)
			assertEquals(buf2[i], fix.getFrames()[i]);

		// ring
		assertEquals(0, ring.getFrames().length);
		assertEquals(0, ring.getSize());
		ring.setFrame(frame5);
		ring.setFrame(frame4);
		assertEquals(2, ring.getFrames().length);
		assertEquals(0, ring.getSize());
		ring.setFrame(frame5);
		ring.setFrame(frame4);
		final CEMILData[] frames = ring.getFrames();
		assertEquals(0, ring.getSize());
		for (int i = 0; i < 2; ++i)
			assertEquals(buf2[i], frames[i]);

		ring.setFrame(frame3);
		assertEquals(1, ring.getFrames().length);
		ring.setFrame(frame3);
		ring.setFrame(frame3);
		assertEquals(2, ring.getFrames().length);

		ring.setFrame(frame2);
		ring.setFrame(frame1);
		assertEquals(2, ring.getFrames().length);
		assertEquals(0, ring.getFrames().length);

		// one
		assertEquals(0, one.getFrames().length);
		assertEquals(0, one.getSize());
		one.setFrame(frame5);
		one.setFrame(frame4);
		assertEquals(1, one.getFrames().length);
		assertEquals(0, one.getSize());
		one.setFrame(frame5);
		one.setFrame(frame4);
		assertEquals(frame5, one.getFrames()[0]);

		one.setFrame(frame3);
		one.setFrame(frame1);
		one.setFrame(frame2);
		assertEquals(frame3, one.getFrames()[0]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObjectQueue#getSize()}.
	 */
	public void testGetSize()
	{
		assertEquals(0, var.getSize());
		var.setFrame(frame1);
		assertEquals(1, var.getSize());
		assertEquals(0, fix.getSize());
		fix.setFrame(frame1);
		assertEquals(1, fix.getSize());
		assertEquals(0, ring.getSize());
		ring.setFrame(frame1);
		assertEquals(1, ring.getSize());
		assertEquals(0, one.getSize());
		one.setFrame(frame1);
		assertEquals(1, one.getSize());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.LDataObjectQueue#getTimestamps()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	public void testGetTimestamps() throws InterruptedException
	{
		final long[] time = new long[6];
		// var
		long[] empty = var.getTimestamps();
		assertEquals(0, empty.length);

		var.setFrame(frame1);
		time[0] = System.currentTimeMillis();
		Thread.sleep(500);
		var.setFrame(frame1);
		time[1] = System.currentTimeMillis();
		Thread.sleep(500);
		var.setFrame(frame1);
		time[2] = System.currentTimeMillis();
		long[] stamps = var.getTimestamps();
		assertEquals(3, stamps.length);
		// resolution of currentTimeMillis is system dependent
		// but should be <= 50 ms
		for (int i = 0; i < stamps.length; ++i)
			assertTrue(stamps[i] >= time[i] && stamps[i] <= time[i] + 50);
		assertEquals(var.getTimestamp(), stamps[2]);

		// fix
		empty = fix.getTimestamps();
		assertEquals(0, empty.length);

		fix.setFrame(frame2);
		time[0] = System.currentTimeMillis();
		Thread.sleep(200);
		fix.setFrame(frame2);
		time[1] = System.currentTimeMillis();
		Thread.sleep(200);
		fix.setFrame(frame2);
		time[2] = System.currentTimeMillis();
		Thread.sleep(200);
		fix.setFrame(frame2);
		time[3] = System.currentTimeMillis();
		Thread.sleep(200);
		fix.setFrame(frame2);
		time[4] = System.currentTimeMillis();
		Thread.sleep(200);
		fix.setFrame(frame2);
		time[5] = System.currentTimeMillis();
		stamps = fix.getTimestamps();
		assertEquals(4, stamps.length);
		// resolution of currentTimeMillis is system dependent
		// but should be <= 50 ms
		for (int i = 0; i < stamps.length; ++i)
			assertTrue(stamps[i] >= time[i] && stamps[i] <= time[i] + 50);
		assertEquals(fix.getTimestamp(), stamps[3]);

		// ring
		empty = ring.getTimestamps();
		assertEquals(0, empty.length);

		ring.setFrame(frame1);
		time[0] = System.currentTimeMillis();
		Thread.sleep(200);
		ring.setFrame(frame2);
		time[1] = System.currentTimeMillis();
		Thread.sleep(200);
		ring.setFrame(frame3);
		time[2] = System.currentTimeMillis();
		Thread.sleep(200);
		final LDataObjectQueue.QueueItem item = ring.getItem();
		assertEquals(frame2, item.getFrame());
		assertTrue(item.getTimestamp() >= time[1] && item.getTimestamp() <= time[1] + 50);

		ring.setFrame(frame4);
		time[3] = System.currentTimeMillis();
		stamps = ring.getTimestamps();
		assertEquals(2, stamps.length);
		// resolution of currentTimeMillis is system dependent
		// but should be <= 50 ms
		for (int i = 0; i < stamps.length; ++i)
			assertTrue(stamps[i] >= time[i + 2] && stamps[i] <= time[i + 2] + 50);
		assertEquals(ring.getTimestamp(), stamps[1]);

		// one
		empty = one.getTimestamps();
		assertEquals(0, empty.length);

		one.setFrame(frame4);
		time[0] = System.currentTimeMillis();
		Thread.sleep(500);
		one.setFrame(frame4);
		time[1] = System.currentTimeMillis();
		Thread.sleep(500);
		one.setFrame(frame4);
		time[2] = System.currentTimeMillis();
		stamps = one.getTimestamps();
		assertEquals(1, stamps.length);
		assertTrue(stamps[0] >= time[0] && stamps[0] <= time[0] + 50);
		assertEquals(one.getTimestamp(), stamps[0]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObjectQueue#clear()}.
	 *
	 * @throws Exception
	 */
	public void testClear() throws Exception
	{
		var.clear();
		fix.clear();
		ring.clear();
		one.clear();
		testGetSize();
		var.clear();
		fix.clear();
		ring.clear();
		one.clear();
		testSet();
		var.clear();
		fix.clear();
		ring.clear();
		one.clear();
		// we got notified, so reset for testGetFrame()
		l.q = null;
		testGetFrame();
		var.clear();
		fix.clear();
		ring.clear();
		one.clear();
		testGetFrames();
	}

	/**
	 * Test method for size.
	 */
	public final void testSize()
	{
		for (int i = 0; i < 40; ++i)
			var.setFrame(frame1);
		for (int i = 0; i < 60; ++i)
			var.setFrame(frame2);
		assertEquals(100, var.getFrames().length);
		assertEquals(100, var.getSize());
		final CEMILData[] frames = var.getFrames();
		for (int i = 0; i < 40; ++i)
			assertEquals(frame1, frames[i]);
		for (int i = 40; i < 100; ++i)
			assertEquals(frame2, frames[i]);
	}

	/**
	 * Test method for consuming read.
	 */
	public final void testConsuming()
	{
		// test specific behavior with variable size and consuming read
		final LDataObjectQueue con =
			new LDataObjectQueue(new GroupAddress(10, 4, 10), true);
		for (int i = 0; i < 40; ++i)
			con.setFrame(frame1);
		for (int i = 0; i < 39; ++i)
			con.getFrame();
		assertEquals(1, con.getSize());
		for (int i = 0; i < 40; ++i)
			con.setFrame(frame3);
		for (int i = 0; i < 20; ++i)
			con.setFrame(frame5);

		final CEMILData[] frames = con.getFrames();
		assertEquals(frame1, frames[0]);
		for (int i = 1; i < 41; ++i)
			assertEquals(frame3, frames[i]);
		for (int i = 41; i < 61; ++i)
			assertEquals(frame5, frames[i]);
		LDataObjectQueue.QueueItem item = con.getItem();
		assertNull(item.getFrame());
		assertEquals(0, item.getTimestamp());
		con.setFrame(frame1);
		con.setFrame(frame5);
		item = con.getItem();
		assertEquals(frame1, item.getFrame());
		assertFalse(0 == item.getTimestamp());
		item = con.getItem();
		assertEquals(frame5, item.getFrame());
		assertFalse(0 == item.getTimestamp());
	}
}

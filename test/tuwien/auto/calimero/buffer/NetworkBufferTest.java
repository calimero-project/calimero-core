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

import java.util.Date;

import category.RequireKNXNetworkLink;
import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.buffer.LDataObjectQueue.QueueItem;
import tuwien.auto.calimero.datapoint.DatapointMap;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;

/**
 * @author B. Malinowsky
 */
@Category(RequireKNXNetworkLink.class)
public class NetworkBufferTest extends TestCase
{
	private KNXNetworkLink lnk;
	private NetworkBuffer buffer;

	/**
	 * @param name name of test case
	 */
	public NetworkBufferTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		lnk = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, null, Util.getServer(),
			false, TPSettings.TP1);
		Util.setupLogging();
		buffer = NetworkBuffer.createBuffer("test");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		if (lnk != null)
			lnk.close();
		buffer.destroy();
		Util.tearDownLogging();
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#createBuffer(java.lang.String)}.
	 */
	public final void testCreateBuffer()
	{
		final String id = "testInstallation";
		final NetworkBuffer b = NetworkBuffer.createBuffer(id);
		assertEquals(id, b.getInstallationID());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.NetworkBuffer#addConfiguration(
	 * tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
	public final void testCreateConfigurationKNXNetworkLink()
	{
		final NetworkBuffer b = NetworkBuffer.createBuffer(null);
		b.addConfiguration(lnk);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.NetworkBuffer#getConfiguration(
	 * tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
	public final void testGetConfiguration()
	{
		final Configuration c = buffer.addConfiguration(lnk);
		final Configuration c2 = buffer.getConfiguration(c.getBufferedLink());
		assertEquals(c, c2);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.NetworkBuffer#removeConfiguration(
	 * tuwien.auto.calimero.buffer.Configuration)}.
	 */
	public final void testRemoveConfigurationConfiguration()
	{
		final Configuration c = buffer.addConfiguration(lnk);
		final KNXNetworkLink buf = c.getBufferedLink();
		buffer.removeConfiguration(c);
		assertNull(buffer.getConfiguration(buf));
	}

	/**
	 * Test method for state based buffering.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	public final void testStateBasedBuffering() throws InterruptedException, KNXException
	{
		final GroupAddress group1 = new GroupAddress(1, 0, 1);
		final GroupAddress group2 = new GroupAddress(1, 0, 11);
		final Configuration c = buffer.addConfiguration(lnk);
		assertEquals(c.getBufferedLink().getName(), "buffered " + lnk.getName());
		final StateFilter f = new StateFilter();
		c.setFilter(f, f);
		c.activate(true);
		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());

		// buffered write, check of buffer
		pc.write(group2, true);
		// little time for notifier for confirmation
		Thread.sleep(50);
		assertTrue(pc.readBool(group2));
		pc.write(group2, false);
		Thread.sleep(50);
		assertFalse(pc.readBool(group2));

		final boolean b1 = pc.readBool(group1);
		final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
		final boolean b2 = pc2.readBool(group1);
		assertEquals(b1, b2);
		pc.write(group2, true);
		Thread.sleep(50);
		assertTrue(pc.readBool(group2));

		// unbuffered write, check of buffer
		pc2.write(group1, true);
		Thread.sleep(50);
		assertEquals(pc.readBool(group1), pc2.readBool(group1));
		pc2.write(group1, false);
		Thread.sleep(50);
		assertEquals(pc.readBool(group1), pc2.readBool(group1));

		// test with datapoint model and timeout
		final DatapointMap<StateDP> map = new DatapointMap<>();
		final StateDP dp = new StateDP(group1, "group1", 0, "1.001");
		dp.setExpirationTimeout(2);
		map.add(dp);
		c.setDatapointModel(map);
		pc.read(dp);
		Thread.sleep(500);
		pc.read(dp);
		Thread.sleep(2500);
		pc.read(dp);
		pc.read(dp);
	}

	/**
	 * Test method for state based buffering.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	public final void testInvalidationUpdating() throws KNXException,
		InterruptedException
	{
		final GroupAddress group1 = new GroupAddress(1, 0, 1);
		final GroupAddress group2 = new GroupAddress(1, 0, 11);
		final GroupAddress group3 = new GroupAddress(1, 0, 111);
		final Configuration c = buffer.addConfiguration(lnk);
		final DatapointMap<StateDP> map = new DatapointMap<>();
		final StateDP dp = new StateDP(group1, "group1", 0, "1.001");
		dp.add(group2, false);
		dp.add(group3, true);
		final StateDP dp2 = new StateDP(group2, "group2", 0, "1.001");
		final StateDP dp3 = new StateDP(group3, "group3", 0, "1.001");
		map.add(dp);
		map.add(dp2);
		map.add(dp3);
		c.setDatapointModel(map);
		final StateFilter f = new StateFilter();
		c.setFilter(f, f);

		// set datapoint states before activating config
		final ProcessCommunicator init = new ProcessCommunicatorImpl(lnk);
		init.write(dp, "off");
		init.write(dp2, "on");
		init.write(dp3, "on");
		init.detach();

		c.activate(true);

		// dp gets invalidated by group2, updated by group3
		// a write updates and invalidates, read.res only updates

		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());
		final String s1 = pc.read(dp);
		Thread.sleep(50);
		/*final String s2 =*/ pc.read(dp2);
		// read of dp2 should have *not* have invalidated dp
		assertEquals(s1, pc.read(dp));
		final String s3 = pc.read(dp3);
		// allow some time to receive update from dp3
		Thread.sleep(100);
		assertEquals(s3, pc.read(dp));
		final String write = s3.equals("off") ? "on" : "off";
		pc.write(dp3, write);
		// allow some time to receive write updates and update map
		Thread.sleep(100);
		assertEquals(write, pc.read(dp));
		assertEquals(write, pc.read(dp3));
		assertEquals(write, pc.read(dp));

		pc.write(group3, true);
		Thread.sleep(50);
		assertTrue(pc.readBool(group1));
		pc.write(group3, false);
		Thread.sleep(50);
		assertFalse(pc.readBool(group1));
		pc.write(group2, false);
		Thread.sleep(50);
		assertEquals(s1, pc.read(dp));
		pc.write(group2, false);
		Thread.sleep(50);
		assertEquals(s1, pc.read(dp));
	}

	/**
	 * Test method for command based buffering.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	public final void testCommandBasedBuffering() throws InterruptedException,
		KNXException
	{
		final GroupAddress group1 = new GroupAddress(1, 0, 1);
		final GroupAddress group2 = new GroupAddress(1, 0, 11);
		final Configuration c = buffer.addConfiguration(lnk);
		final CommandFilter f = new CommandFilter();
		c.setFilter(f, f);
		c.activate(true);
		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());

		// buffered write, check of buffer
		pc.write(group2, true);
		Thread.sleep(1000);
		pc.write(group2, false);
		// little time for notifier for confirmation
		Thread.sleep(50);
		assertTrue(f.hasNewIndication());
		final QueueItem qi = f.getNextIndication();
		Debug.printLData(qi.getFrame());
		System.out.println(new Date(qi.getTimestamp()));
		assertTrue(f.hasNewIndication());

		final QueueItem qi2 = f.getNextIndication();
		Debug.printLData(qi2.getFrame());
		System.out.println(new Date(qi2.getTimestamp()));
		assertTrue(qi.getTimestamp() < (qi2.getTimestamp() - 700));

		assertFalse(f.hasNewIndication());

		final class ListenerImpl implements LDataObjectQueue.QueueListener
		{
			boolean filled;

			@Override
			public void queueFilled(final LDataObjectQueue queue)
			{
				filled = true;
				assertEquals(10, queue.getFrames().length);
				assertEquals(0, queue.getFrames().length);
			}
		}
		final ListenerImpl listener = new ListenerImpl();
		f.setQueueListener(listener);

		final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
		pc2.readBool(group1);
		Thread.sleep(50);
		assertTrue(f.hasNewIndication());
		pc2.write(group2, true);
		Thread.sleep(50);

		for (int i = 0; i < 9; ++i)
			pc2.write(group2, i % 2 != 0);
		Thread.sleep(50);
		assertTrue(listener.filled);
		assertTrue(f.hasNewIndication());

		try {
			QueueItem qi3 = f.getNextIndication();
			assertEquals(group1, qi3.getFrame().getDestination());
			assertTrue(f.hasNewIndication());
			for (int i = 0; i < 10; ++i) {
				qi3 = f.getNextIndication();
				assertEquals(0, qi3.getTimestamp());
				assertNull(qi3.getFrame());
			}
			assertFalse(f.hasNewIndication());
		}
		catch (final KNXIllegalStateException e) {}
	}

	/**
	 * Test method for query buffer only mode.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	public final void testQueryBufferOnly() throws InterruptedException, KNXException
	{
		final GroupAddress group1 = new GroupAddress(1, 0, 1);
		final GroupAddress group2 = new GroupAddress(1, 0, 11);
		final Configuration c = buffer.addConfiguration(lnk);
		final StateFilter f = new StateFilter();
		c.setFilter(f, f);
		c.setQueryBufferOnly(true);
		c.activate(true);
		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());

		// buffered write, check of buffer
		pc.write(group2, true);
		// little time for notifier for confirmation
		Thread.sleep(50);
		assertTrue(pc.readBool(group2));
		pc.write(group2, false);
		Thread.sleep(50);
		assertFalse(pc.readBool(group2));

		try {
			pc.readBool(group1);
			fail("there should be no " + group1 + " value in the buffer");
		}
		catch (final KNXTimeoutException e) {}
		final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
		/*final boolean b2 =*/ pc2.readBool(group1);
		pc.write(group2, true);
		Thread.sleep(50);
		assertTrue(pc.readBool(group2));

		// unbuffered write, check of buffer
		pc2.write(group1, true);
		Thread.sleep(50);
		assertEquals(pc.readBool(group1), pc2.readBool(group1));
		pc2.write(group1, false);
		Thread.sleep(50);
		assertEquals(pc.readBool(group1), pc2.readBool(group1));

		// test with datapoint model and timeout
		final DatapointMap<StateDP> map = new DatapointMap<>();
		final StateDP dp = new StateDP(group1, "group1", 0, "1.001");
		dp.setExpirationTimeout(2);
		map.add(dp);
		c.setDatapointModel(map);
		pc.read(dp);
		Thread.sleep(500);
		pc.read(dp);
		Thread.sleep(2500);
		try {
			pc.read(dp);
			fail(group1 + " value in buffer should be too old");
		}
		catch (final KNXTimeoutException e) {}
		// check that link didn't query the KNX network
		try {
			pc.read(dp);
			fail(group1 + " value in buffer should be too old");
		}
		catch (final KNXTimeoutException e) {}
	}
}

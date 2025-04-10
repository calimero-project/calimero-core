/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

package io.calimero.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.FrameEvent;
import io.calimero.GroupAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXException;
import io.calimero.KNXTimeoutException;
import io.calimero.Util;
import io.calimero.buffer.LDataObjectQueue.QueueItem;
import io.calimero.cemi.CEMILData;
import io.calimero.datapoint.Datapoint;
import io.calimero.datapoint.DatapointMap;
import io.calimero.datapoint.StateDP;
import io.calimero.knxnetip.Debug;
import io.calimero.link.KNXLinkClosedException;
import io.calimero.link.KNXNetworkLink;
import io.calimero.link.KNXNetworkLinkIP;
import io.calimero.link.NetworkLinkListener;
import io.calimero.link.medium.TPSettings;
import io.calimero.process.ProcessCommunicator;
import io.calimero.process.ProcessCommunicatorImpl;
import tag.KnxnetIP;
import tag.KnxnetIPSequential;


@KnxnetIP
class NetworkBufferTest
{
	private final GroupAddress group1 = new GroupAddress(1, 0, 1);
	private final GroupAddress invalidatingGroup = new GroupAddress(1, 0, 11);
	private final GroupAddress updatingGroup = new GroupAddress(1, 0, 111);
	private StateDP dp;
	private StateDP invalidatingDp;
	private StateDP updatingDp;

	private KNXNetworkLink lnk;
	private NetworkBuffer buffer;

	private final Set<GroupAddress> waitObject = new HashSet<>();
	private static final long WaitForUpdate = 1000;

	private final NetworkLinkListener linkListener = new NetworkLinkListener() {
		@Override
		public void confirmation(final FrameEvent e)
		{
			synchronized (waitObject) {
				final KNXAddress ga = ((CEMILData) e.getFrame()).getDestination();
				if (waitObject.removeIf(ga::equals)) {
					waitObject.notifyAll();
				}
			}
		}
	};

	@BeforeEach
	void init() throws Exception
	{
		lnk = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, new TPSettings());
		buffer = NetworkBuffer.createBuffer("test");
	}

	@AfterEach
	void tearDown() {
		if (lnk != null)
			lnk.close();
		if (buffer != null)
			buffer.close();
	}

	// some helpers

	private ProcessCommunicator initConfig() throws KNXException, InterruptedException {
		final Configuration c = buffer.addConfiguration(lnk);
		final DatapointMap<StateDP> map = new DatapointMap<>();
		dp = new StateDP(group1, "group1", 0, "1.001");
		// dp gets invalidated by invalidatingGroup, updated by updatingGroup
		// a write updates and invalidates, read.res only update
		dp.addInvalidatingAddress(invalidatingGroup);
		dp.addUpdatingAddress(updatingGroup);
		invalidatingDp = new StateDP(invalidatingGroup, "group2", 0, "1.001");
		updatingDp = new StateDP(updatingGroup, "group3", 0, "1.001");
		map.add(dp);
		map.add(invalidatingDp);
		map.add(updatingDp);
		c.setDatapointModel(map);
		final StateFilter f = new StateFilter();
		c.setFilter(f, f);

		// set datapoint states before activating config
		final ProcessCommunicator init = new ProcessCommunicatorImpl(lnk);
		init.write(dp, "off");
		init.write(invalidatingDp, "on");
		init.write(updatingDp, "on");
		init.close();

		c.activate(true);
		c.getBufferedLink().addLinkListener(linkListener);

		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());
		pc.readBool(group1);
		return pc;
	}

	private void write(final ProcessCommunicator pc, final GroupAddress dst, final boolean value)
		throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		try {
			waitObject.add(dst);
			pc.write(dst, value);
			final long start = System.nanoTime();
			synchronized (waitObject) {
				while (waitObject.contains(dst)) {
					if (System.nanoTime() - start > (WaitForUpdate * 1000_000))
						break;
					waitObject.wait(WaitForUpdate);
				}
			}
		}
		finally {
			waitObject.remove(dst);
		}
	}

	private void write(final ProcessCommunicator pc, final Datapoint dp, final String value)
			throws KNXException, InterruptedException
	{
		final GroupAddress dst = dp.getMainAddress();
		try {
			waitObject.add(dst);
			pc.write(dp, value);
			final long start = System.nanoTime();
			synchronized (waitObject) {
				while (waitObject.contains(dst)) {
					if (System.nanoTime() - start > (WaitForUpdate * 1000_000))
						break;
					waitObject.wait(WaitForUpdate);
				}
			}
		}
		finally {
			waitObject.remove(dst);
		}
	}

	private boolean read(final ProcessCommunicator pc, final GroupAddress dst)
			throws KNXException, InterruptedException
	{
		final boolean ret;
		try {
			waitObject.add(dst);
			ret = pc.readBool(dst);
			final long start = System.nanoTime();
			synchronized (waitObject) {
				while (waitObject.contains(dst)) {
					if (System.nanoTime() - start > (WaitForUpdate * 1000_000))
						break;
					waitObject.wait(WaitForUpdate);
				}
			}
		}
		finally {
			waitObject.remove(dst);
		}
		return ret;
	}

	private String read(final ProcessCommunicator pc, final Datapoint dp)
			throws KNXException, InterruptedException
	{
		final GroupAddress dst = dp.getMainAddress();
		final String ret;
		try {
			waitObject.add(dst);
			ret = pc.read(dp);
			final long start = System.nanoTime();
			synchronized (waitObject) {
				while (waitObject.contains(dst)) {
					if (System.nanoTime() - start > (WaitForUpdate * 1000_000))
						break;
					waitObject.wait(WaitForUpdate);
				}
			}
		}
		finally {
			waitObject.remove(dst);
		}
		return ret;
	}

	// tests

	@Test
	void createBuffer()
	{
		final String id = "testInstallation";
		assertNotNull(NetworkBuffer.createBuffer(id));
	}

	@Test
	void createConfigurationKNXNetworkLink()
	{
		final NetworkBuffer b = NetworkBuffer.createBuffer(null);
		b.addConfiguration(lnk);
	}

	@Test
	void getConfiguration()
	{
		final Configuration c = buffer.addConfiguration(lnk);
		final Configuration c2 = buffer.getConfiguration(c.getBufferedLink());
		assertEquals(c, c2);
	}

	@Test
	void removeConfigurationConfiguration()
	{
		final Configuration c = buffer.addConfiguration(lnk);
		final KNXNetworkLink buf = c.getBufferedLink();
		buffer.removeConfiguration(c);
		assertNull(buffer.getConfiguration(buf));
	}

	@Test
	void stateBasedBuffering() throws InterruptedException, KNXException
	{
		final GroupAddress group1 = new GroupAddress(1, 0, 1);
		final GroupAddress group2 = new GroupAddress(1, 0, 11);
		final Configuration c = buffer.addConfiguration(lnk);
		assertEquals(c.getBufferedLink().getName(), "buffered " + lnk.getName());
		final StateFilter f = new StateFilter();
		c.setFilter(f, f);
		c.activate(true);
		c.getBufferedLink().addLinkListener(linkListener);
		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());

		// buffered write, check of buffer
		write(pc, group2, true);
		assertTrue(pc.readBool(group2));
		write(pc, group2, false);
		assertFalse(pc.readBool(group2));

		final boolean b1 = pc.readBool(group1);
		final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
		final boolean b2 = pc2.readBool(group1);
		assertEquals(b1, b2);
		write(pc, group2, true);
		assertTrue(pc.readBool(group2));

		// unbuffered write, check of buffer
		write(pc2, group1, true);
		assertEquals(pc.readBool(group1), pc2.readBool(group1));
		write(pc2, group1, false);
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

	@Test
	void readDoesNotInvalidate() throws KNXException, InterruptedException
	{
		final ProcessCommunicator pc = initConfig();

		final String s1 = pc.read(dp);
		read(pc, invalidatingDp);
		// read of invalidatingDp should have *not* have invalidated dp
		assertEquals(s1, pc.read(dp));
	}

	@Test
	void readDoesUpdate() throws KNXException, InterruptedException
	{
		final ProcessCommunicator pc = initConfig();
		assertFalse(pc.readBool(dp.getMainAddress()));
		final String s3 = read(pc, updatingDp);
		assertEquals(s3, pc.read(dp));
	}

	@Test
	void writeDoesUpdate() throws KNXException, InterruptedException
	{
		final ProcessCommunicator pc = initConfig();

		final String s3 = pc.read(updatingDp);
		final String write = "off".equals(s3) ? "on" : "off";
		write(pc, updatingDp, write);
		assertEquals(write, pc.read(dp));
		assertEquals(write, pc.read(updatingDp));
		assertEquals(write, pc.read(dp));
	}

	@Test
	void alternateWriteDoesUpdate() throws KNXException, InterruptedException
	{
		final ProcessCommunicator pc = initConfig();
		write(pc, updatingGroup, true);
		assertTrue(pc.readBool(group1));
		waitObject.add(updatingGroup);
		write(pc, updatingGroup, false);
		assertFalse(pc.readBool(group1));
	}

	@Test
	void repeatedWriteInvalidate() throws KNXException, InterruptedException
	{
		final ProcessCommunicator pc = initConfig();
		write(pc, invalidatingGroup, false);
		final String s1 = "off";
		assertEquals(s1, pc.read(dp));
		write(pc, invalidatingGroup, false);
		assertEquals(s1, pc.read(dp));
	}

	@Test
	void commandBasedBuffering() throws InterruptedException, KNXException
	{
		final Configuration c = buffer.addConfiguration(lnk);
		final CommandFilter f = new CommandFilter();
		c.setFilter(f, f);
		c.activate(true);
		c.getBufferedLink().addLinkListener(linkListener);
		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());

		// buffered write, check of buffer
		write(pc, invalidatingGroup, true);
		write(pc, invalidatingGroup, false);
		assertTrue(f.hasNewIndication());
		final QueueItem qi = f.getNextIndication();
		Debug.printLData(qi.frame());
		assertTrue(f.hasNewIndication());

		final QueueItem qi2 = f.getNextIndication();
		Debug.printLData(qi2.frame());
		assertTrue(qi.timestamp() <= (qi2.timestamp()), qi.timestamp() + ", " + qi2.timestamp());

		assertFalse(f.hasNewIndication());

		final AtomicBoolean filled = new AtomicBoolean();
		final Consumer<LDataObjectQueue> listener = queue -> {
			filled.set(true);
			assertEquals(10, queue.getFrames().length);
			assertEquals(0, queue.getFrames().length);
		};
		f.setQueueListener(listener);

		final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
		read(pc2, group1);
		assertTrue(f.hasNewIndication());

		write(pc2, invalidatingGroup, true);
		for (int i = 0; i < 9; ++i)
			write(pc2, invalidatingGroup, i % 2 != 0);
		c.activate(false);
		assertTrue(filled.get());
		assertTrue(f.hasNewIndication());

		QueueItem qi3 = f.getNextIndication();
		assertEquals(group1, qi3.frame().getDestination());
		assertTrue(f.hasNewIndication());
		for (int i = 0; i < 10; ++i) {
			qi3 = f.getNextIndication();
			assertNull(qi3.frame());
			assertEquals(0, qi3.timestamp());
		}
		assertFalse(f.hasNewIndication());
	}

	@Test
	@KnxnetIPSequential
	void queryBufferOnly() throws InterruptedException, KNXException
	{
		final Configuration c = buffer.addConfiguration(lnk);
		final StateFilter f = new StateFilter();
		c.setFilter(f, f);
		c.setQueryBufferOnly(true);
		c.activate(true);
		final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());

		// buffered write, check of buffer
		write(pc, invalidatingGroup, true);
		assertTrue(pc.readBool(invalidatingGroup));
		write(pc, invalidatingGroup, false);
		assertFalse(pc.readBool(invalidatingGroup));

		try {
			pc.readBool(group1);
			fail("there should be no " + group1 + " value in the buffer");
		}
		catch (final KNXTimeoutException e) {}
		final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
		/*final boolean b2 =*/ pc2.readBool(group1);
		write(pc, invalidatingGroup, true);
		assertTrue(pc.readBool(invalidatingGroup));

		// unbuffered write, check of buffer
		write(pc2, group1, true);
		assertEquals(pc.readBool(group1), pc2.readBool(group1));
		write(pc2, group1, false);
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

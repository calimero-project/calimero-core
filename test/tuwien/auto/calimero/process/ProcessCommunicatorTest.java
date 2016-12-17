/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

package tuwien.auto.calimero.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorString;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class ProcessCommunicatorTest
{
	private ProcessCommunicator pc;
	private ProcessCommunicator pc2;
	private KNXNetworkLink link;

	private final GroupAddress dpBool;
	private final GroupAddress dpBool2;
	private final GroupAddress dpControl;
	private final GroupAddress dpUnsigned1;
	private final GroupAddress dpString;
	private final GroupAddress dpFloat2;
	private final GroupAddress dpFloat4;

	private final String dpStringValue = "Hello KNX!";

	/**
	 * @throws KNXFormatException
	 */
	public ProcessCommunicatorTest() throws KNXFormatException
	{
		dpBool = new GroupAddress("1/0/1");
		dpBool2 = new GroupAddress("1/0/11");
		dpControl = new GroupAddress("1/0/2");
		dpUnsigned1 = new GroupAddress("1/0/3");
		dpString = new GroupAddress("1/0/5");
		dpFloat2 = new GroupAddress("1/0/6");
		dpFloat4 = new GroupAddress("1/0/7");
	}

	@BeforeEach
	void init() throws Exception
	{
		link = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, TPSettings.TP1);
		pc = new ProcessCommunicatorImpl(link);
		pc2 = new ProcessCommunicatorImpl(link);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		pc.detach();
		pc2.detach();
		link.close();
	}

	/**
	 * Test method for {@link ProcessCommunicator#setResponseTimeout(int)}.
	 */
	@Test
	public final void testSetResponseTimeout()
	{
		pc.setResponseTimeout(2);
		assertEquals(2, pc.getResponseTimeout());
		try {
			pc.setResponseTimeout(0);
			fail("shouldn't work");
		}
		catch (final KNXIllegalArgumentException ok) {}
		assertEquals(2, pc.getResponseTimeout());

		pc.setResponseTimeout(5);
		assertEquals(5, pc.getResponseTimeout());
	}

	/**
	 * Test method for {@link ProcessCommunicator#getResponseTimeout()}.
	 */
	@Test
	public final void testGetResponseTimeout()
	{
		// test for correct standard timeout
		assertEquals(10, pc.getResponseTimeout());
	}

	/**
	 * Test method for {@link ProcessCommunicator#setPriority(tuwien.auto.calimero.Priority)}.
	 */
	@Test
	public final void testSetPriority()
	{
		pc.setPriority(Priority.SYSTEM);
		assertEquals(Priority.SYSTEM, pc.getPriority());

		pc.setPriority(Priority.LOW);
		assertEquals(Priority.LOW, pc.getPriority());
	}

	/**
	 * Test method for {@link ProcessCommunicator#getPriority()}.
	 */
	@Test
	public final void testGetPriority()
	{
		// test for default priority
		assertEquals(Priority.LOW, pc.getPriority());
	}

	/**
	 * Test method for {@link ProcessCommunicator#addProcessListener(ProcessListener)}.
	 */
	@Test
	public final void testAddProcessListener()
	{
		final ProcessListener l = new ProcessListener() {
			@Override
			public void groupReadRequest(final ProcessEvent e)
			{}

			@Override
			public void groupReadResponse(final ProcessEvent e)
			{}

			@Override
			public void groupWrite(final ProcessEvent e)
			{}

			@Override
			public void detached(final DetachEvent e)
			{}
		};
		pc.addProcessListener(l);
		pc.removeProcessListener(l);

		pc.addProcessListener(l);
		pc.addProcessListener(l);
	}

	/**
	 * Test method for {@link ProcessCommunicator#removeProcessListener(ProcessListener)}.
	 */
	@Test
	public final void testRemoveProcessListener()
	{
		final ProcessListener l = new ProcessListener() {
			@Override
			public void groupReadRequest(final ProcessEvent e)
			{}

			@Override
			public void groupReadResponse(final ProcessEvent e)
			{}

			@Override
			public void groupWrite(final ProcessEvent e)
			{}

			@Override
			public void detached(final DetachEvent e)
			{}
		};
		pc.removeProcessListener(l);
		pc.addProcessListener(l);
		pc.removeProcessListener(l);
		pc.removeProcessListener(l);
	}

	/**
	 * Test method for {@link ProcessCommunicator#readBool(tuwien.auto.calimero.GroupAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadBool() throws KNXException, InterruptedException
	{
		// test concurrent read, needs breakpoints in waitForResponse method
		// useful to see behavior when more than one indication is in the queue

		// read from same address
		// if we are testing with a virtual network make sure we have some value set
		pc.write(dpBool, true);
		new Thread("testReadBool Concurrent 1") {
			@Override
			public void run()
			{
				try {
					pc2.readBool(dpBool);
				}
				catch (final KNXException e) {
					e.printStackTrace();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
		pc.readBool(dpBool);

		// read from different address
		// if we are testing with a virtual network make sure we have some value set
		pc.write(dpBool2, false);
		new Thread("testReadBool Concurrent 2") {
			@Override
			public void run()
			{
				try {
					pc2.readBool(dpBool);
				}
				catch (final KNXException e) {
					e.printStackTrace();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
		pc.readBool(dpBool2);

		// read from different address using same process communicator
		new Thread("testReadBool Concurrent 3") {
			@Override
			public void run()
			{
				try {
					pc.readBool(dpBool);
				}
				catch (final KNXException e) {
					e.printStackTrace();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
		pc.readBool(dpBool2);
	}

	/**
	 * Test method for {@link ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteGroupAddressBoolean() throws KNXException, InterruptedException
	{
		// read from same address
		// if we are testing with a virtual network make sure we have some value set
		pc.write(dpBool, true);
		Thread.sleep(100);
		assertTrue(pc.readBool(dpBool));
		pc.write(dpBool, false);
		Thread.sleep(100);
		assertFalse(pc.readBool(dpBool));
	}

	/**
	 * Test method for {@link ProcessCommunicator#readUnsigned(tuwien.auto.calimero.GroupAddress, java.lang.String)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadUnsigned() throws KNXException, InterruptedException
	{
		// read from same address
		pc.readUnsigned(dpUnsigned1, ProcessCommunicationBase.SCALING);
	}

	/**
	 * Test method for {@link ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, int, java.lang.String)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteGroupAddressIntString() throws KNXException, InterruptedException
	{
		final int v = 80;
		pc.write(dpUnsigned1, v, ProcessCommunicationBase.SCALING);
		Thread.sleep(100);
		final int i = pc.readUnsigned(dpUnsigned1, ProcessCommunicationBase.SCALING);
		assertEquals(v, i);
	}

	/**
	 * Test method for {@link ProcessCommunicator#readControl(tuwien.auto.calimero.GroupAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadControl() throws KNXException, InterruptedException
	{
		pc.readControl(dpControl);
	}

	/**
	 * Test method for {@link ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, boolean, int)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testWriteGroupAddressBooleanInt() throws KNXException
	{
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.write(addr, true, 4);
	}

	/**
	 * Test method for {@link ProcessCommunicator#readFloat(tuwien.auto.calimero.GroupAddress, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadFloatBoolean() throws KNXException, InterruptedException
	{
		/*final double f2 =*/ pc.readFloat(dpFloat2, false);
		/*final double f4 =*/ pc.readFloat(dpFloat4, true);
	}

	/**
	 * Test method for {@link ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, float, boolean)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testWriteGroupAddressFloatBoolean() throws KNXException
	{
		final float f = (float) 0.01;
		pc.write(dpFloat2, f, false);
		pc.write(dpFloat4, f, true);
	}

	/**
	 * Test method for {@link ProcessCommunicator#readString(tuwien.auto.calimero.GroupAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadString() throws KNXException, InterruptedException
	{
		final String s = pc.readString(dpString);
		assertTrue(s.length() > 0);
		assertEquals(dpStringValue, s);
	}

	/**
	 * Test method for {@link ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, java.lang.String)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteGroupAddressString() throws KNXException, InterruptedException
	{
		pc.write(dpString, "test");
		pc.write(dpString, "test2");
		Thread.sleep(100);
		assertEquals("test2", pc.readString(dpString));
		pc.write(dpString, dpStringValue);
	}

	/**
	 * Test method for {@link ProcessCommunicator#read(tuwien.auto.calimero.datapoint.Datapoint)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testRead() throws KNXException, InterruptedException
	{
		final Datapoint dp = new StateDP(dpString, "test datapoint");
		dp.setDPT(0, DPTXlatorString.DPT_STRING_8859_1.getID());
		final String res = pc2.read(dp);
		assertTrue(res.length() > 0);
	}

	/**
	 * Test method for {@link ProcessCommunicator#read(Datapoint)}.
	 */
	@Test
	public final void testConcurrentRead()
	{
		final Datapoint dp = new StateDP(dpString, "test datapoint");
		dp.setDPT(0, DPTXlatorString.DPT_STRING_8859_1.getID());
		final List<Thread> threads = new ArrayList<>();
		final AtomicInteger count = new AtomicInteger();
		for (int i = 0; i < 10; i++) {
			final Thread t = new Thread(() -> {
				try {
					count.addAndGet(pc2.read(dp).length() > 0 ? 1 : 0);
				}
				catch (KNXException | InterruptedException e) {}
			});
			threads.add(t);
		}
		threads.forEach(Thread::start);
		threads.forEach((t) -> {
			try {
				t.join();
			}
			catch (final InterruptedException e) {}
		});
		assertEquals(threads.size(), count.get());
	}

	/**
	 * Test method for {@link ProcessCommunicator#read(Datapoint)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testConcurrentRead2() throws KNXException, InterruptedException
	{
		final List<Thread> threads = new ArrayList<>();
		final AtomicInteger count = new AtomicInteger();
		final boolean b = pc2.readBool(dpBool);
		final double d = pc2.readFloat(dpFloat2, false);
		for (int i = 0; i < 20; i++) {
			final int index = i;
			final Thread t = new Thread(() -> {
				try {
					if (index % 2 == 0)
						count.addAndGet(pc2.readBool(dpBool) == b ? 1 : 0);
					else
						count.addAndGet(pc2.readFloat(dpFloat2, false) == d ? 1 : 0);
				}
				catch (KNXException | InterruptedException e) {
					final StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					fail("ProcessCommunicatorTest:testConcurrentRead2() thread " + Thread.currentThread().getName()
							+ ": " + errors.toString());
				}
			}, "concurrent read index " + i);
			threads.add(t);
		}
		threads.forEach(Thread::start);
		threads.forEach((t) -> {
			try {
				t.join();
			}
			catch (final InterruptedException e) {}
		});
		assertEquals(threads.size(), count.get());
	}

	/**
	 * Test method for {@link ProcessCommunicator#read(Datapoint)}.
	 */
	@Test
	public final void testConcurrentReadNonExistingDestination()
	{
		final List<Thread> threads = new ArrayList<>();
		final AtomicInteger count = new AtomicInteger();
		final LocalTime start = LocalTime.now();
		for (int i = 0; i < 5; i++) {
			final Thread t = new Thread(() -> {
				try {
					pc2.readBool(new GroupAddress(7, 7, 7));
					count.incrementAndGet();
				}
				catch (KNXException | InterruptedException e) {}
			});
			threads.add(t);
		}
		threads.forEach(Thread::start);
		threads.forEach((t) -> {
			try {
				t.join();
			}
			catch (final InterruptedException e) {}
		});
		assertEquals(0, count.get());
		final LocalTime now = LocalTime.now();
		assertTrue(now.isAfter(start.plusSeconds(10)));
		assertTrue(now.isBefore(start.plusSeconds(12)));
	}

	/**
	 * Test method for {@link ProcessCommunicator#write(tuwien.auto.calimero.datapoint.Datapoint, java.lang.String)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testWriteDatapointString() throws KNXException
	{
		final Datapoint dp = new StateDP(dpUnsigned1, "test datapoint");
		dp.setDPT(0, DPTXlator8BitUnsigned.DPT_PERCENT_U8.getID());
		pc2.write(dp, "80");
	}

	/**
	 * Test method for {@link ProcessCommunicator#detach()}.
	 */
	@Test
	public final void testDetach()
	{
		final KNXNetworkLink ret = pc.detach();
		assertEquals(link, ret);
	}
}

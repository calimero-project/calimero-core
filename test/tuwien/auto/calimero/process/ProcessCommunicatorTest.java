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

package tuwien.auto.calimero.process;

import junit.framework.TestCase;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogManager;

/**
 * @author B. Malinowsky
 */
public class ProcessCommunicatorTest extends TestCase
{
	private ProcessCommunicator pc;
	private ProcessCommunicator pc2;
	private KNXNetworkLink link;

	/**
	 * @param name
	 */
	public ProcessCommunicatorTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		LogManager.getManager().addWriter(null, Util.getLogWriter());
		link = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, Util.getLocalHost(),
			Util.getServer(), false, TPSettings.TP1);
		pc = new ProcessCommunicatorImpl(link);
		pc2 = new ProcessCommunicatorImpl(link);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		pc.detach();
		pc2.detach();
		link.close();
		LogManager.getManager().removeWriter(null, Util.getLogWriter());
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#setResponseTimeout(int)}.
	 */
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
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#getResponseTimeout()}.
	 */
	public final void testGetResponseTimeout()
	{
		// test for correct standard timeout
		assertEquals(10, pc.getResponseTimeout());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#setPriority(tuwien.auto.calimero.Priority)}
	 * .
	 */
	public final void testSetPriority()
	{
		pc.setPriority(Priority.SYSTEM);
		assertEquals(Priority.SYSTEM, pc.getPriority());

		pc.setPriority(Priority.LOW);
		assertEquals(Priority.LOW, pc.getPriority());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#getPriority()}.
	 */
	public final void testGetPriority()
	{
		// test for default priority
		assertEquals(Priority.LOW, pc.getPriority());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#addProcessListener
	 * (tuwien.auto.calimero.process.ProcessListener)}.
	 */
	public final void testAddProcessListener()
	{
		final ProcessListener l = new ProcessListener()
		{
			public void groupWrite(final ProcessEvent e)
			{}

			public void detached(final DetachEvent e)
			{}
		};
		pc.addProcessListener(l);
		pc.removeProcessListener(l);

		pc.addProcessListener(l);
		pc.addProcessListener(l);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#removeProcessListener
	 * (tuwien.auto.calimero.process.ProcessListener)}.
	 */
	public final void testRemoveProcessListener()
	{
		final ProcessListener l = new ProcessListener()
		{
			public void groupWrite(final ProcessEvent e)
			{}

			public void detached(final DetachEvent e)
			{}
		};
		pc.removeProcessListener(l);
		pc.addProcessListener(l);
		pc.removeProcessListener(l);
		pc.removeProcessListener(l);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#readBool(tuwien.auto.calimero.GroupAddress)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadBool() throws KNXException, InterruptedException
	{
		// test concurrent read, needs breakpoints in waitForResponse method
		// useful to see behavior when more than one indication is in the queue

		// read from same address
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		// if we are testing with a virtual network make sure we have some value set
		pc.write(addr, true);
		new Thread()
		{
			public void run()
			{
				try {
					pc2.readBool(addr);
				}
				catch (final KNXException e) {
					e.printStackTrace();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
		pc.readBool(addr);

		// read from different address
		final GroupAddress addr2 = new GroupAddress(1, 0, 2);
		// if we are testing with a virtual network make sure we have some value set
		pc.write(addr2, false);
		new Thread()
		{
			public void run()
			{
				try {
					pc2.readBool(addr);
				}
				catch (final KNXException e) {
					e.printStackTrace();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
		pc.readBool(addr2);

		// read from different address using same process communicator
		new Thread()
		{
			public void run()
			{
				try {
					pc.readBool(addr);
				}
				catch (final KNXException e) {
					e.printStackTrace();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
		pc.readBool(addr2);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, boolean)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteGroupAddressBoolean() throws KNXException,
		InterruptedException
	{
		// read from same address
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		// if we are testing with a virtual network make sure we have some value set
		pc.write(addr, true);
		Thread.sleep(100);
		assertTrue(pc.readBool(addr));
		pc.write(addr, false);
		Thread.sleep(100);
		assertFalse(pc.readBool(addr));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#readUnsigned(tuwien.auto.calimero.GroupAddress, java.lang.String)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadUnsigned() throws KNXException, InterruptedException
	{
		// read from same address
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.readUnsigned(addr, ProcessCommunicationBase.UNSCALED);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, int, java.lang.String)}
	 * .
	 * 
	 * @throws KNXException
	 */
	public final void testWriteGroupAddressIntString() throws KNXException
	{
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.write(addr, 80, ProcessCommunicationBase.SCALING);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#readControl(tuwien.auto.calimero.GroupAddress)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadControl() throws KNXException, InterruptedException
	{
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.readControl(addr);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, boolean, int)}
	 * .
	 * 
	 * @throws KNXException
	 */
	public final void testWriteGroupAddressBooleanInt() throws KNXException
	{
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.write(addr, true, 4);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#readFloat(tuwien.auto.calimero.GroupAddress)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadFloat() throws KNXException, InterruptedException
	{
		// this test procedure now uses as direct replacement the 2 arg method
		
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.readFloat(addr, false);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, float)}
	 * .
	 * 
	 * @throws KNXException
	 */
	public final void testWriteGroupAddressFloat() throws KNXException
	{
		// this test procedure now uses as direct replacement the 3 arg method
		
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		final float f = (float) 0.01;
		pc.write(addr, f, false);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#readString(tuwien.auto.calimero.GroupAddress)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadString() throws KNXException, InterruptedException
	{
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.readString(addr);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#write(tuwien.auto.calimero.GroupAddress, java.lang.String)}
	 * .
	 * 
	 * @throws KNXException
	 */
	public final void testWriteGroupAddressString() throws KNXException
	{
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.write(addr, "test");
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#read(tuwien.auto.calimero.datapoint.Datapoint)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testRead() throws KNXException, InterruptedException
	{
		final Datapoint dp = new StateDP(new GroupAddress(1, 0, 0), "test datapoint");
		final String res = pc2.read(dp);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.process.ProcessCommunicator#write(tuwien.auto.calimero.datapoint.Datapoint, java.lang.String)}
	 * .
	 * 
	 * @throws KNXException
	 */
	public final void testWriteDatapointString() throws KNXException
	{
		final Datapoint dp = new StateDP(new GroupAddress(1, 0, 0), "test datapoint");
		dp.setDPT(0, DPTXlator8BitUnsigned.DPT_PERCENT_U8.getID());
		pc2.write(dp, "80");
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.process.ProcessCommunicator#detach()}.
	 */
	public final void testDetach()
	{
		final KNXNetworkLink ret = pc.detach();
		assertEquals(link, ret);
	}

}

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

package tuwien.auto.calimero.knxnetip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode;
import tuwien.auto.calimero.log.LogManager;

/**
 * @author B. Malinowsky
 */
public class KNXnetIPDevMgmtTest extends TestCase
{
	private static KNXnetIPConnection.BlockingMode noblock =
		KNXnetIPConnection.NONBLOCKING;
	private static KNXnetIPConnection.BlockingMode ack = KNXnetIPConnection.WAIT_FOR_ACK;
	private static KNXnetIPConnection.BlockingMode con = KNXnetIPConnection.WAIT_FOR_CON;

	private KNXnetIPDevMgmt m;
	private KNXListenerImpl l;

	private CEMIDevMgmt frame;
	private CEMIDevMgmt frame2;

	private final class KNXListenerImpl implements KNXListener
	{
		boolean closed;
		CEMI received;

		public void frameReceived(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(m, e.getSource());
			received = e.getFrame();
			final CEMIDevMgmt f = (CEMIDevMgmt) received;
			Debug.printMData(f);
			if (f.getPID() == 52)
				System.out.println(new IndividualAddress(f.getPayload()));
			if (f.getPID() == 57)
				try {
					System.out.println(InetAddress.getByAddress(f.getPayload()));
				}
				catch (final UnknownHostException e1) {
					e1.printStackTrace();
				}
		}

		public void connectionClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(m, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	/**
	 * @param name name of test case
	 */
	public KNXnetIPDevMgmtTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		l = new KNXListenerImpl();

		LogManager.getManager().addWriter(null, Util.getLogWriter());

		// pid 52 = individual address
		frame = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, 11, 1, 52, 1, 1);
		// pid 57 = current ip address
		frame2 = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, 11, 1, 57, 1, 1);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		if (m != null) {
			m.close();
		}
		LogManager.getManager().removeWriter(null, Util.getLogWriter());
		super.tearDown();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt#send
	 * (tuwien.auto.calimero.cemi.CEMI,
	 * tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode)}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testSend() throws KNXException, InterruptedException
	{
		newMgmt();
		doSend(frame, con, true);
		doSend(frame2, con, true);
	}

	private void doSend(final CEMIDevMgmt f, final BlockingMode mode, final boolean positiveResponse)
		throws KNXTimeoutException, KNXConnectionClosedException
	{
		l.received = null;
		m.send(f, mode);
		assertNotNull(l.received);
		final CEMIDevMgmt fcon = (CEMIDevMgmt) l.received;
		assertEquals(!positiveResponse, fcon.isNegativeResponse());
		l.received = null;
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt#KNXnetIPDevMgmt
	 * (java.net.InetSocketAddress, java.net.InetSocketAddress, boolean)}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testKNXnetIPDevMgmt() throws KNXException, InterruptedException
	{
		newMgmt();
		assertEquals(KNXnetIPConnection.OK, m.getState());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt#close()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testClose() throws KNXException, InterruptedException
	{
		newMgmt();
		m.close();
		assertEquals(KNXnetIPConnection.CLOSED, m.getState());
		try {
			m.send(frame, con);
			fail("we are closed");
		}
		catch (final KNXConnectionClosedException e) {}
		assertEquals(KNXnetIPConnection.CLOSED, m.getState());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt#getRemoteAddress()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testGetRemoteAddress() throws KNXException, InterruptedException
	{
		newMgmt();
		assertEquals(Util.getServer(), m.getRemoteAddress());
		m.close();
		assertTrue(m.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertTrue(m.getRemoteAddress().getPort() == 0);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt#getState()}.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
	public final void testGetState() throws InterruptedException,
		KNXException
	{
		newMgmt();
		assertEquals(KNXnetIPConnection.OK, m.getState());
		System.out.println("Testing heartbeat, will take some minutes");
		// give some seconds space for delay so we're on the save side
		Thread.sleep(4000);
		Thread.sleep(6000);
		assertEquals(KNXnetIPConnection.OK, m.getState());
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, m.getState());
	}

	private void newMgmt() throws KNXException, InterruptedException
	{
		m = new KNXnetIPDevMgmt(Util.getLocalHost(), Util.getServer(), false);
		m.addConnectionListener(l);
	}

}

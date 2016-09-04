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

package tuwien.auto.calimero.knxnetip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tag.Slow;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class KNXnetIPDevMgmtTest
{
//	private static KNXnetIPConnection.BlockingMode noblock = KNXnetIPConnection.BlockingMode.NonBlocking;
//	private static KNXnetIPConnection.BlockingMode ack = KNXnetIPConnection.BlockingMode.WaitForAck;
	private static KNXnetIPConnection.BlockingMode con = KNXnetIPConnection.BlockingMode.WaitForCon;

	private KNXnetIPDevMgmt m;
	private KNXListenerImpl l;

	private CEMIDevMgmt frame;
	private CEMIDevMgmt frame2;

	private final class KNXListenerImpl implements KNXListener
	{
		boolean closed;
		CEMI received;

		@Override
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

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(m, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	@BeforeEach
	void init() throws Exception
	{
		l = new KNXListenerImpl();
		// pid 52 = individual address
		frame = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, 11, 1, 52, 1, 1);
		// pid 57 = current ip address
		frame2 = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, 11, 1, 57, 1, 1);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (m != null) {
			m.close();
		}
	}

	/**
	 * Test method for {@link KNXnetIPDevMgmt#send(CEMI, KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSend() throws KNXException, InterruptedException
	{
		newMgmt();
		doSend(frame, con, true);
		doSend(frame2, con, true);
	}

	private void doSend(final CEMIDevMgmt f, final BlockingMode mode, final boolean positiveResponse)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
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
	 * {@link KNXnetIPDevMgmt#KNXnetIPDevMgmt(java.net.InetSocketAddress, java.net.InetSocketAddress, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testKNXnetIPDevMgmt() throws KNXException, InterruptedException
	{
		newMgmt();
		assertEquals(KNXnetIPConnection.OK, m.getState());
	}

	/**
	 * Test method for {@link KNXnetIPDevMgmt#close()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
	 * Test method for {@link KNXnetIPDevMgmt#getRemoteAddress()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetRemoteAddress() throws KNXException, InterruptedException
	{
		newMgmt();
		assertEquals(Util.getServer(), m.getRemoteAddress());
		m.close();
		assertTrue(m.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertTrue(m.getRemoteAddress().getPort() == 0);
	}

	/**
	 * Test method for {@link KNXnetIPDevMgmt#getState()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	@Test
	@Slow
	public final void testGetState() throws InterruptedException, KNXException
	{
		newMgmt();
		assertEquals(KNXnetIPConnection.OK, m.getState());
		System.out.println();
		System.out.println("Testing heartbeat, will take some minutes !!!");
		System.out.println("...");
		// give some seconds space for delay so we're on the safe side
		Thread.sleep(4000);
		Thread.sleep(60000);
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

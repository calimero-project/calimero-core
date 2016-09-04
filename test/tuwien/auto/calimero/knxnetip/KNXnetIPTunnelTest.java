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
import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.BusMonitorLayer;
import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.LinkLayer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tag.Slow;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class KNXnetIPTunnelTest
{
	private static KNXnetIPConnection.BlockingMode noblock = KNXnetIPConnection.BlockingMode.NonBlocking;
	private static KNXnetIPConnection.BlockingMode ack = KNXnetIPConnection.BlockingMode.WaitForAck;
	private static KNXnetIPConnection.BlockingMode con = KNXnetIPConnection.BlockingMode.WaitForCon;

	private KNXnetIPTunnel t;
	private KNXnetIPTunnel tnat;
	private KNXnetIPTunnel mon;

	private KNXListenerImpl l;
	private KNXListenerImpl lnat;
	private KNXListenerImpl lmon;
	private CEMILData frame;
	private CEMILData frame2;
	// should be a frame with unused destination address
	private CEMILData frameNoDest;

	private final class KNXListenerImpl implements KNXListener
	{
		boolean closed;
		CEMI received;
		List<CEMI> fifoReceived = new Vector<>();

		@Override
		public void frameReceived(final FrameEvent e)
		{
			assertNotNull(e);
			if (this == l)
				assertEquals(t, e.getSource());
			if (this == lnat)
				assertEquals(tnat, e.getSource());
			if (this == lmon)
				assertEquals(mon, e.getSource());
			received = e.getFrame();
			if (e.getFrame() instanceof CEMIBusMon) {
				Debug.printMonData((CEMIBusMon) e.getFrame());
			}
			// Debug.parseLData((CEMILData) received);
			fifoReceived.add(e.getFrame());
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			assertNotNull(e);
			if (this == l)
				assertEquals(t, e.getSource());
			if (this == lnat)
				assertEquals(tnat, e.getSource());
			if (this == lmon)
				assertEquals(mon, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	@BeforeEach
	void init() throws Exception
	{
		l = new KNXListenerImpl();
		lnat = new KNXListenerImpl();
		lmon = new KNXListenerImpl();

		frame = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 1) }, Priority.NORMAL);
		frame2 = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.URGENT);
		frameNoDest = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(10, 7, 10),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.LOW);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (t != null) {
			t.close();
		}
		if (tnat != null) {
			tnat.close();
		}
		if (mon != null)
			mon.close();
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#send(CEMI, tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSend() throws KNXException, InterruptedException
	{
		newTunnel();
		doSend(frame, con, true);
		doSend(frame2, con, true);
		// the test expects a negative confirmation with the received L_Data.con
		// the Calimero server always sends positive .con any frame, even if the group
		// address is non-existing
		final boolean posCon = true;
		doSend(frameNoDest, noblock, posCon);
		doSend(frame, ack, true);
		doSend(frame2, con, true);

		final long start = System.currentTimeMillis();
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		doSend(frameNoDest, con, posCon);
		final long end = System.currentTimeMillis();
		System.out.println("time for 10 send with con: " + (end - start));
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#send(CEMI, tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testFIFOSend() throws KNXException, InterruptedException
	{
		final int sends = 10;
		final List<CEMILData> frames = new Vector<>();
		for (int i = 0; i < sends; i++) {
			frames.add(new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(i + 1), new GroupAddress(2, 2, 2),
					new byte[] { 0, (byte) (0x80 | (i % 2)) }, Priority.LOW));
		}
		class Sender extends Thread
		{
			Sender(final String name)
			{
				super(name);
			}

			@Override
			public void run()
			{
				try {
					final CEMILData f = frames.remove(0);
					synchronized (this) {
						notify();
					}
					t.send(f, con);
					System.out.println(getName() + " returned sending " + f.getSource());
				}
				catch (KNXTimeoutException | KNXConnectionClosedException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		newTunnel();
		final Thread[] threads = new Thread[sends];
		for (int i = 0; i < sends; i++) {
			threads[i] = new Sender("sender " + (i + 1));
		}
		Thread.sleep(50);
		final long start = System.currentTimeMillis();
		for (int i = 0; i < threads.length; i++) {
			synchronized (threads[i]) {
				threads[i].start();
				threads[i].wait();
			}
			Thread.sleep(20);
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
		final long end = System.currentTimeMillis();
		System.out.println("time for " + sends + " send MT with con: " + (end - start));
		assertEquals(sends, l.fifoReceived.size());
		for (int i = 0; i < sends; i++) {
			assertEquals(new IndividualAddress(i + 1), ((CEMILData) l.fifoReceived.get(i)).getSource());
		}
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#send(tuwien.auto.calimero.cemi.CEMI, KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testNATSend() throws KNXException, InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATSend ====\n");
			return;
		}
		newNATTunnel();
		doNATSend(frame, con, true);
		doNATSend(frame2, con, true);
		// XXX why is the confirmation here true?
		doNATSend(frameNoDest, con, true);
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#send(CEMI, KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testMonitorSend() throws KNXException, InterruptedException
	{
		newMonitor();
		try {
			mon.send(frame, ack);
			fail("no send in busmon");
		}
		catch (final KNXIllegalStateException e) {}
		// on open monitor test behavior on new tunnel
		try {
			newTunnel();
			fail("no tunnel on busmonitor");
		}
		catch (final KNXException e) {}
	}

	@Test
	public final void testTunnelWithMonitor() throws KNXException, InterruptedException
	{
		newTunnel();
		try {
			newMonitor();
			fail("no monitor on open tunnel");
		}
		catch (final KNXException e) {}
	}

	@Test
	public final void testReceive() throws KNXException, InterruptedException
	{
		newTunnel();
		System.out.println("Tunnel: waiting for some incoming frames...");
		try {
			Thread.sleep(10 * 1000);
		}
		catch (final InterruptedException e) {}
	}

	@Test
	public final void testReceiveMonitor() throws KNXException, InterruptedException
	{
		newMonitor();
		System.out.println("Monitor: waiting for some incoming frames...");
		try {
			Thread.sleep(10 * 1000);
		}
		catch (final InterruptedException e) {}
	}

	private void doSend(final CEMILData f, final KNXnetIPConnection.BlockingMode m, final boolean positiveConfirmation)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
	{
		l.received = null;
		t.send(f, m);
		if (m == noblock) {
			while (t.getState() == ConnectionBase.ACK_PENDING)
				try {
					Thread.sleep(10);
				}
				catch (final InterruptedException e) {}
		}
		if (m == ack || m == noblock) {
			while (t.getState() == ClientConnection.CEMI_CON_PENDING)
				try {
					Thread.sleep(10);
				}
				catch (final InterruptedException e) {}
		}
		assertNotNull(l.received);
		final CEMILData fcon = (CEMILData) l.received;
		assertEquals(positiveConfirmation, fcon.isPositiveConfirmation());
		l.received = null;
	}

	private void doNATSend(final CEMILData f, final KNXnetIPConnection.BlockingMode m,
		final boolean positiveConfirmation)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
	{
		lnat.received = null;
		tnat.send(f, m);
		assertNotNull(lnat.received);
		final CEMILData fcon = (CEMILData) lnat.received;
		assertEquals(positiveConfirmation, fcon.isPositiveConfirmation());
		lnat.received = null;
	}

	/**
	 * Test method for
	 * {@link KNXnetIPTunnel#KNXnetIPTunnel(KNXnetIPTunnel.TunnelingLayer, InetSocketAddress, InetSocketAddress, boolean)}
	 * .
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testKNXnetIPTunnel() throws KNXException, InterruptedException
	{
		try (final KNXnetIPConnection c = new KNXnetIPTunnel(LinkLayer, null, new InetSocketAddress("127.0.0.1", 4000),
				false)) {
			fail("local socket is null");
		}
		catch (final KNXIllegalArgumentException e) {}

		try (final KNXnetIPConnection c = new KNXnetIPTunnel(LinkLayer, new InetSocketAddress("0.0.0.0", 0),
				new InetSocketAddress("127.0.0.1", 4000), false)) {
			fail("wildcard for local socket not null");
		}
		catch (final KNXIllegalArgumentException e) {}

		newTunnel();
		assertEquals(KNXnetIPConnection.OK, t.getState());
	}

	/**
	 * Test method for
	 * {@link KNXnetIPTunnel#KNXnetIPTunnel(KNXnetIPTunnel.TunnelingLayer, InetSocketAddress, InetSocketAddress, boolean)}
	 * .
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testKNXnetIPMonitor() throws KNXException, InterruptedException
	{
		newMonitor();
		assertEquals(KNXnetIPConnection.OK, mon.getState());
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#close()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testClose() throws KNXException, InterruptedException
	{
		newTunnel();
		t.close();
		assertEquals(KNXnetIPConnection.CLOSED, t.getState());
		try {
			t.send(frame, con);
			fail("we are closed");
		}
		catch (final KNXConnectionClosedException e) {}
		assertEquals(KNXnetIPConnection.CLOSED, t.getState());
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#getRemoteAddress()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetRemoteAddress() throws KNXException, InterruptedException
	{
		newTunnel();
		assertEquals(Util.getServer(), t.getRemoteAddress());
		t.close();
		assertTrue(t.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertTrue(t.getRemoteAddress().getPort() == 0);
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#getState()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	@Slow
	public final void testGetState() throws KNXException, InterruptedException
	{
		newTunnel();
		assertEquals(KNXnetIPConnection.OK, t.getState());
		System.out.println();
		System.out.println("Testing heartbeat, this will take some minutes !!!");
		System.out.println("...");
		// give some seconds space for delay so we're on the safe side
		Thread.sleep(4000);
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, t.getState());
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, t.getState());
	}

	/**
	 * Test method for {@link KNXnetIPTunnel#getState()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	@Slow
	public final void testMonitorGetState() throws KNXException, InterruptedException
	{
		newMonitor();
		assertEquals(KNXnetIPConnection.OK, mon.getState());
		System.out.println();
		System.out.println("Testing heartbeat, this will take some minutes !!!");
		System.out.println("...");
		// give some seconds space for delay so we're on the save side
		Thread.sleep(4000);
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, mon.getState());
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, mon.getState());
	}

	@Test
	void interruptedSend() throws Exception
	{
		init();
		newTunnel();
		for (int i = 0; i < 20; i++) {
			try {
				Thread.currentThread().interrupt();
				t.send(frame, con);
				fail("we are interrupted");
				try {
					Thread.sleep(100);
				}
				catch (final InterruptedException e) {}
			}
			catch (final InterruptedException expected) {}
		}
	}

	private void newTunnel() throws KNXException, InterruptedException
	{
		t = new KNXnetIPTunnel(LinkLayer, Util.getLocalHost(), Util.getServer(), false);
		t.addConnectionListener(l);
	}

	private void newNATTunnel() throws KNXException, InterruptedException
	{
		tnat = new KNXnetIPTunnel(LinkLayer, Util.getLocalHost(), Util.getServer(), true);
		tnat.addConnectionListener(lnat);
	}

	private void newMonitor() throws KNXException, InterruptedException
	{
		mon = new KNXnetIPTunnel(BusMonitorLayer, Util.getLocalHost(), Util.getServer(), false);
		mon.addConnectionListener(lmon);
	}
}

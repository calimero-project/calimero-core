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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.servicetype.RoutingBusy;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class KNXnetIPRouterTest
{
	private static KNXnetIPConnection.BlockingMode noblock = KNXnetIPConnection.BlockingMode.NonBlocking;

	private KNXnetIPRouting r;
	private RouterListenerImpl l;

	private CEMILData frame;
	private CEMILData frame2;
	// should be a frame with unused destination address
	private CEMILData frameNoDest;

	private final AtomicInteger routingBusy = new AtomicInteger();

	private final class RouterListenerImpl implements RoutingListener
	{
		volatile boolean closed;
		volatile CEMI received;
		List<LostMessageEvent> lost = new Vector<>();

		@Override
		public void frameReceived(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(r, e.getSource());
			received = e.getFrame();
			Debug.printLData((CEMILData) received);
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(r, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}

		@Override
		public void lostMessage(final LostMessageEvent e)
		{
			assertNotNull(e);
			lost.add(e);
		}

		@Override
		public void routingBusy(final RoutingBusyEvent e)
		{
			routingBusy.incrementAndGet();
		}
	}

	@BeforeEach
	void init() throws Exception
	{
		l = new RouterListenerImpl();
		frame = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 1) }, Priority.NORMAL);
		frame2 = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.URGENT);
		frameNoDest = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(10, 7, 10),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.URGENT);
		routingBusy.set(0);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (r != null) {
			r.close();
		}
	}

	/**
	 * Test method for {@link KNXnetIPRouting#send(tuwien.auto.calimero.cemi.CEMI, KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testSend() throws KNXException
	{
		newRouter();
		doSend(frame, noblock);
		doSend(frame2, noblock);
		doSend(frameNoDest, noblock);
	}

	/**
	 * Test method for {@link KNXnetIPRouting#send(tuwien.auto.calimero.cemi.CEMI, KNXnetIPConnection.BlockingMode)}.
	 *
	 * @throws KNXException
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	@Test
	public final void testSend2() throws KNXException, SocketException, UnknownHostException
	{
		r = new KNXnetIPRouting(NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()),
				InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST));
		r.addConnectionListener(l);
		doSend(frame, noblock);
		doSend(frame2, noblock);
		doSend(frameNoDest, noblock);
	}

	private void doSend(final CEMILData f, final KNXnetIPConnection.BlockingMode m) throws KNXConnectionClosedException
	{
		r.send(f, m);
		try {
			Thread.sleep(500);
		}
		catch (final InterruptedException e) {}
	}

	/**
	 * Test method for {@link KNXnetIPRouting#KNXnetIPRouting(java.net.NetworkInterface, java.net.InetAddress)}.
	 *
	 * @throws KNXException
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	@Test
	public final void testKNXnetIPRouter() throws SocketException, UnknownHostException, KNXException
	{
		newRouter();
		assertEquals(KNXnetIPConnection.OK, r.getState());
		r.close();
		r = new KNXnetIPRouting(NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()),
				InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST));
		r.close();
		try {
			r = new KNXnetIPRouting(null, InetAddress.getByName("224.0.23.11"));
			fail("invalid routing multicast");
		}
		catch (final KNXIllegalArgumentException e) {}
		r = new KNXnetIPRouting(null, InetAddress.getByName("224.0.23.13"));
	}

	/**
	 * @throws KNXException
	 */
	@Test
	public final void testReceive() throws KNXException
	{
		newRouter();
		System.out.println("waiting for some incoming frames...");
		try {
			Thread.sleep(10 * 1000);
		}
		catch (final InterruptedException e) {}
	}

	/**
	 * Test method for {@link KNXnetIPRouting#setHopCount(int)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testSetHopCount() throws KNXException
	{
		newRouter();
		final int hobbes = r.getHopCount();
		r.setHopCount(hobbes + 1);
		assertEquals(hobbes + 1, r.getHopCount());
		try {
			r.setHopCount(-1);
			fail("negative hopcount");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertEquals(hobbes + 1, r.getHopCount());
		try {
			r.setHopCount(256);
			fail("hopcount too big");
		}
		catch (final KNXIllegalArgumentException e) {}
		assertEquals(hobbes + 1, r.getHopCount());
		r.setHopCount(255);
		assertEquals(255, r.getHopCount());
		r.setHopCount(hobbes);
		assertEquals(hobbes, r.getHopCount());
		r.close();
		r.setHopCount(20);
	}

	/**
	 * Test method for {@link KNXnetIPRouting#close()}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testClose() throws KNXException
	{
		newRouter();
		r.close();
		assertEquals(KNXnetIPConnection.CLOSED, r.getState());
		try {
			r.send(frame, noblock);
			fail("we are closed");
		}
		catch (final KNXConnectionClosedException e) {}
		assertEquals(KNXnetIPConnection.CLOSED, r.getState());
	}

	/**
	 * Test method for {@link KNXnetIPRouting#getRemoteAddress()}.
	 *
	 * @throws KNXException
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	@Test
	public final void testGetRemoteAddress() throws KNXException, SocketException, UnknownHostException
	{
		newRouter();
		assertEquals(new InetSocketAddress(KNXnetIPRouting.DEFAULT_MULTICAST, KNXnetIPConnection.DEFAULT_PORT),
				r.getRemoteAddress());
		r.close();
		assertTrue(r.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertTrue(r.getRemoteAddress().getPort() == 0);

		r = new KNXnetIPRouting(NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()),
				InetAddress.getByName("224.0.23.33"));
		assertEquals(new InetSocketAddress("224.0.23.33", KNXnetIPConnection.DEFAULT_PORT), r.getRemoteAddress());
		r.close();
		assertTrue(r.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertTrue(r.getRemoteAddress().getPort() == 0);
	}

	@Test
	public void testLostMessageIndication() throws KNXException
	{
		newRouter();
		int sent = 0;
		while (sent < 1000 && l.lost.isEmpty()) {
			r.send(frame, noblock);
			++sent;
			try {
				Thread.sleep(10);
			}
			catch (final InterruptedException e) {}
		}
		// let receiver some time for firing all incoming events
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException e1) {}
		for (final Iterator<LostMessageEvent> i = l.lost.iterator(); i.hasNext();) {
			final LostMessageEvent e = i.next();
			System.out.println("dev.state:" + e.getDeviceState() + ", lost msgs:" + e.getLostMessages());
		}
	}

	private static final Duration timeout = Duration.ofMillis(1000);

	@Test
	void sendRoutingBusy() throws KNXException
	{
		newRouter();
		r.send(new RoutingBusy(0, 100, 0));
		Assertions.assertTimeout(timeout, () -> {
			while (routingBusy.get() == 0)
				Thread.sleep(50);
		}, "no routing busy notification");
		assertEquals(1, routingBusy.get());
	}

	@Test
	void incrementRoutingBusyCounter() throws KNXException, InterruptedException
	{
		newRouter();
		final int messages = 5;
		for (int i = 0; i < messages; i++) {
			r.send(new RoutingBusy(1, 20, 0));
			Thread.sleep(12);
		}
		Assertions.assertTimeout(timeout, () -> {
			while (routingBusy.get() != messages)
				Thread.sleep(50);
		}, "wrong number of routing busy notification");
		assertEquals(messages, routingBusy.get());
	}

	@Test
	void fastSendManyRoutingBusy() throws KNXException
	{
		newRouter();
		final int messages = 40;
		for (int i = 0; i < messages; i++) {
			r.send(new RoutingBusy(1, 20, 0));
		}
		Assertions.assertTimeout(timeout, () -> {
			while (routingBusy.get() != messages)
				Thread.sleep(50);
		}, "wrong number of routing busy notification");
		assertEquals(messages, routingBusy.get());
	}

	private void newRouter() throws KNXException
	{
		r = new KNXnetIPRouting(null, null);
		r.addConnectionListener(l);
	}
}

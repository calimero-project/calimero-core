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

package tuwien.auto.calimero.mgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class TransportLayerImplTest
{
	private KNXNetworkLink nl;
	private TransportLayer tl;
	private TLListener ltl;
	private Destination dco;
	private Destination dcl;
	private final Priority p = Priority.NORMAL;

	// device descriptor read
	private final int devDescRead = 0x300;
	private final byte[] tsduDescRead = new byte[] { (byte) (devDescRead >> 8), (byte) (devDescRead | 0) };

	private final class TLListener implements TransportListener
	{
		final List<CEMI> broad = new Vector<>();
		final List<CEMI> conn = new Vector<>();
		final List<CEMI> ind = new Vector<>();
		final List<CEMI> group = new Vector<>();
		final List<Destination> dis = new Vector<>();
		volatile boolean closed;
		volatile boolean detached;

		TLListener()
		{}

		@Override
		public void broadcast(final FrameEvent e)
		{
			assertNotNull(e, "broadcast frame event");
			broad.add(e.getFrame());
			final CEMILData f = (CEMILData) e.getFrame();
			assertEquals(new GroupAddress(0), f.getDestination());
			Debug.printLData("broadcast: ", f);
		}

		@Override
		public void dataConnected(final FrameEvent e)
		{
			assertNotNull(e, "data-connected frame event");
			conn.add(e.getFrame());
			final CEMILData f = (CEMILData) e.getFrame();
			Debug.printLData("data connected: ", f);
		}

		@Override
		public void dataIndividual(final FrameEvent e)
		{
			assertNotNull(e, "data-individual frame event");
			ind.add(e.getFrame());
			final CEMILData f = (CEMILData) e.getFrame();
			assertTrue(f.getDestination() instanceof IndividualAddress);
			Debug.printLData("data individual: ", f);
		}

		@Override
		public void disconnected(final Destination d)
		{
			assertNotNull(d, "disconnected");
			dis.add(d);
			System.out.println("disconnected: " + d);
		}

		@Override
		public void group(final FrameEvent e)
		{
			assertNotNull(e, "group frame event");
			group.add(e.getFrame());
			final CEMILData f = (CEMILData) e.getFrame();
			assertTrue(f.getDestination() instanceof GroupAddress);
			Debug.printLData("group data: ", f);
		}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			assertNotNull(e, "link closed event");
			assertEquals(nl, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}

		@Override
		public void detached(final DetachEvent e)
		{
			detached = true;
		}
	};

	@BeforeEach
	void init() throws Exception
	{
		nl = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, TPSettings.TP1);
		tl = new TransportLayerImpl(nl);
		ltl = new TLListener();
		tl.addTransportListener(ltl);
		dco = tl.createDestination(Util.getKnxDeviceCO(), true);
		dcl = tl.createDestination(Util.getKnxDevice(), false);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (tl != null) {
			tl.detach();
		}
		nl.close();
	}

	@Test
	public final void testTransportLayerImpl()
	{
		nl.close();
		try {
			new TransportLayerImpl(nl);
			fail("link closed");
		}
		catch (final KNXLinkClosedException e) {}
	}

	/**
	 * Test method for {@link TransportLayerImpl#addTransportListener(TransportListener)}.
	 */
	@Test
	public final void testAddEventListener()
	{
		tl.addTransportListener(new TLListener());
		tl.addTransportListener(ltl);
	}

	/**
	 * Test method for {@link TransportLayerImpl#broadcast(boolean, Priority, byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testBroadcast() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		final int indAddrRead = 0x0100;
		final byte[] tsdu = new byte[] { (byte) (indAddrRead >> 8), (byte) indAddrRead };
		tl.broadcast(false, Priority.SYSTEM, tsdu);
		Thread.sleep(500);
		assertFalse(ltl.broad.isEmpty(), "no broadcast received");
	}

	/**
	 * Test method for {@link TransportLayerImpl#connect(Destination)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testConnect() throws KNXTimeoutException, KNXLinkClosedException
	{
		tl.connect(dco);
		tl.connect(dco);

		tl.connect(dcl);

		final TransportLayer tl2 = new TransportLayerImpl(nl);
		final Destination d2 = tl2.createDestination(new IndividualAddress(3, 1, 1), true);
		try {
			tl.connect(d2);
			fail("not owning destination");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link TransportLayerImpl#createDestination(tuwien.auto.calimero.IndividualAddress, boolean)}.
	 */
	@Test
	public final void testCreateDestinationIndividualAddressBoolean()
	{
		assertTrue(dco.isConnectionOriented());
		assertEquals(Util.getKnxDeviceCO(), dco.getAddress());
		dco.destroy();
		final Destination d = tl.createDestination(Util.getRouterAddress(), true);
		assertNotSame(dco, d);
		assertEquals(Util.getRouterAddress(), d.getAddress());
	}

	/**
	 * Test method for
	 * {@link TransportLayerImpl#createDestination(tuwien.auto.calimero.IndividualAddress, boolean, boolean, boolean)}.
	 */
	@Test
	public final void testCreateDestinationIndividualAddressBooleanBooleanBoolean()
	{
		Destination d;
		try {
			d = tl.createDestination(Util.getKnxDeviceCO(), true, false, true);
			fail("already created");
		}
		catch (final KNXIllegalArgumentException e) {}

		d = tl.createDestination(new IndividualAddress(3, 2, 1), true, false, true);
		assertTrue(d.isConnectionOriented());
		assertEquals(new IndividualAddress(3, 2, 1), d.getAddress());
		assertFalse(d.isKeepAlive());
		assertTrue(d.isVerifyMode());
	}

	/**
	 * Test method for {@link TransportLayerImpl#detach()}.
	 *
	 * @throws KNXLinkClosedException
	 */
	@Test
	public final void testDetach() throws KNXLinkClosedException
	{
		assertFalse(ltl.detached);
		tl.detach();
		assertTrue(ltl.detached);
		ltl.detached = false;
		tl.detach();
		assertFalse(ltl.detached);

		final TransportLayer tl2 = new TransportLayerImpl(nl);
		final TLListener l2 = new TLListener();
		tl2.addTransportListener(l2);
		assertFalse(l2.detached);
		tl2.detach();
		assertTrue(l2.detached);
		l2.detached = false;
		tl2.detach();
		assertFalse(l2.detached);
	}

	/**
	 * Test method for {@link TransportLayerImpl#detach()}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testDetach2() throws KNXTimeoutException, KNXLinkClosedException
	{
		tl.detach();
		tl.detach();
		try {
			tl.sendData(new IndividualAddress(4, 4, 4), p, new byte[] { 0, 0 });
			fail("we are detached");
		}
		catch (final KNXIllegalStateException e) {}
		try {
			tl.connect(dco);
			fail("we are detached");
		}
		catch (final KNXIllegalStateException e) {}
		try {
			tl.createDestination(new IndividualAddress(5, 5, 5), false, false, false);
			fail("we are detached");
		}
		catch (final KNXIllegalStateException e) {}
	}

	/**
	 * Test method for {@link TransportLayerImpl#detach()}.
	 */
	@Test
	public final void testDetach3()
	{
		nl.close();
		tl.detach();
	}

	/**
	 * Test method for {@link TransportLayerImpl#disconnect(Destination)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testDisconnect() throws KNXLinkClosedException, KNXTimeoutException
	{
		tl.disconnect(dco);
		tl.disconnect(dco);
		tl.disconnect(dcl);
		assertFalse(ltl.dis.contains(dcl));

		assertFalse(ltl.dis.contains(dco));
		tl.connect(dco);
		assertFalse(ltl.dis.contains(dco));
		tl.disconnect(dco);
		assertTrue(ltl.dis.contains(dco));
		ltl.dis.clear();
		tl.disconnect(dco);
		assertFalse(ltl.dis.contains(dco));
		tl.disconnect(dcl);

		final TransportLayer tl2 = new TransportLayerImpl(nl);
		final Destination d2 = tl2.createDestination(new IndividualAddress(3, 1, 1), true);
		tl2.connect(d2);
		try {
			tl.disconnect(d2);
			fail("not the owner of destination");
		}
		catch (final KNXIllegalArgumentException e) {}
		d2.destroy();
	}

	/**
	 * Test method for {@link TransportLayerImpl#getName()}.
	 */
	@Test
	public final void testGetName()
	{
		String n = tl.getName();
		assertTrue(n.indexOf(nl.getName()) > -1);
		assertTrue(n.indexOf("TL") > -1);
		tl.detach();
		n = tl.getName();
		assertNotNull(n);
		assertTrue(n.indexOf("TL") > -1);
	}

	/**
	 * Test method for {@link TransportLayerImpl#removeTransportListener(TransportListener)}.
	 */
	@Test
	public final void testRemoveEventListener()
	{
		tl.removeTransportListener(ltl);
		tl.removeTransportListener(ltl);
		// should do nothing
		tl.removeTransportListener(new TLListener());
	}

	/**
	 * Test method for {@link TransportLayerImpl#sendData(Destination, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 * @throws KNXDisconnectException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSendDataDestinationPriorityByteArray()
		throws KNXDisconnectException, KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		try {
			tl.sendData(dco, p, tsduDescRead);
			fail("disconnected");
		}
		catch (final KNXDisconnectException e) {}

		tl.connect(dco);
		tl.sendData(dco, p, tsduDescRead);
		Thread.sleep(500);
		assertEquals(1, ltl.conn.size());
		// second time
		tl.sendData(dco, p, tsduDescRead);
		Thread.sleep(500);
		assertEquals(2, ltl.conn.size());
		tl.disconnect(dco);

		try {
			tl.sendData(dco, p, tsduDescRead);
			fail("disconnected");
		}
		catch (final KNXDisconnectException e) {}
		assertEquals(2, ltl.conn.size());

		final TransportLayer tl2 = new TransportLayerImpl(nl);
		final Destination d2 = tl2.createDestination(new IndividualAddress(3, 1, 1), true);
		try {
			tl.sendData(d2, p, tsduDescRead);
			fail("not owning destination");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link TransportLayerImpl#sendData(Destination, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws KNXTimeoutException
	 * @throws KNXLinkClosedException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public void testSendDataNonExistingAddress()
		throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		// not existing device address
		final Destination dunknown = tl.createDestination(Util.getNonExistingKnxDevice(), true);
		tl.connect(dunknown);
		try {
			tl.sendData(dunknown, p, tsduDescRead);
			tl.disconnect(dunknown);
			fail("destination should been disconnected");
		}
		catch (final KNXDisconnectException e) {}
		Thread.sleep(100);
		assertTrue(ltl.conn.size() == 0);
	}

	@Test
	public void testSendDataDetach() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		// do a detach while waiting for L4 ack
		tl.connect(dco);
		try {
			final Thread detacher = new Thread() {
				@Override
				public void run()
				{
					synchronized (this) {
						this.notify();
					}
					try {
						sleep(0);
					}
					catch (final InterruptedException e) {}
					tl.detach();
				}
			};
			synchronized (detacher) {
				detacher.start();
				detacher.wait();
			}
			try {
				tl.sendData(dco, p, tsduDescRead);
				fail("we got detached");
			}
			catch (final KNXIllegalStateException expected) {}
		}
		catch (final KNXDisconnectException e) {}
		// for TL listener to process remaining indications
		Thread.sleep(500);
	}

	/**
	 * @throws KNXTimeoutException
	 * @throws KNXLinkClosedException
	 * @throws KNXDisconnectException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public void testSendDataLinkClose()
		throws KNXTimeoutException, KNXLinkClosedException, KNXDisconnectException, InterruptedException
	{
		// do a link closed while waiting for L4 response
		tl.connect(dco);
		tl.sendData(dco, p, tsduDescRead);
		Thread.sleep(10);
		nl.close();
	}

	/**
	 * Test method for
	 * {@link TransportLayerImpl#sendData(tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSendDataKNXAddressPriorityByteArray()
		throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		final int propRead = 0x03D5;
		// read pid max_apdu_length
		final byte[] tsdu = new byte[] { (byte) (propRead >> 8), (byte) propRead, 0, 56, 0x10, 1, };
		tl.sendData(Util.getKnxDevice(), p, tsdu);
		Thread.sleep(500);
		assertFalse(ltl.ind.isEmpty());
	}
}

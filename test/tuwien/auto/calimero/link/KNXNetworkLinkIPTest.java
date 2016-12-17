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

package tuwien.auto.calimero.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.knxnetip.KNXnetIPRouting;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class KNXNetworkLinkIPTest
{
	private KNXNetworkLink tnl;
	private KNXNetworkLink rtr;
	private NLListenerImpl ltnl, lrtr;
	private CEMILData frame;
	private CEMILData frame2;
	private CEMILData frameInd;

	private final class NLListenerImpl implements NetworkLinkListener
	{
		volatile CEMILData ind;
		volatile CEMILData con;
		volatile boolean closed;

		@Override
		public void indication(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(this == ltnl ? tnl : rtr, e.getSource());
			final CEMILData f = (CEMILData) e.getFrame();
			ind = f;
			assertEquals(CEMILData.MC_LDATA_IND, ind.getMessageCode());
			System.out.println((this == ltnl ? "tunnel " : "router ") + "indication");
			Debug.printLData(ind);
		}

		@Override
		public void confirmation(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(this == ltnl ? tnl : rtr, e.getSource());
			final CEMILData f = (CEMILData) e.getFrame();
			con = f;
			assertEquals(CEMILData.MC_LDATA_CON, f.getMessageCode());
			assertTrue(f.isPositiveConfirmation());
			System.out.println((this == ltnl ? "tunnel " : "router ") + "confirmation");
			Debug.printLData(f);
		}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(this == ltnl ? tnl : rtr, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	@BeforeEach
	void init() throws Exception
	{
		tnl = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false,
				TPSettings.TP1);
		rtr = new KNXNetworkLinkIP(KNXNetworkLinkIP.ROUTING, Util.getLocalHost(),
				new InetSocketAddress(InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST), 0), false,
				TPSettings.TP1);
		ltnl = new NLListenerImpl();
		lrtr = new NLListenerImpl();
		tnl.addLinkListener(ltnl);
		rtr.addLinkListener(lrtr);

		frame = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 1) }, Priority.LOW);
		frame2 = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.URGENT);
		frameInd = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.NORMAL);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (tnl != null)
			tnl.close();
		if (rtr != null)
			rtr.close();
	}

	/**
	 * Test method for
	 * {@link KNXNetworkLinkIP#newTunnelingLink(InetSocketAddress, InetSocketAddress, boolean, KNXMediumSettings)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testKNXNetworkLinkIPConstructor() throws KNXException, InterruptedException
	{
		tnl.close();
		try (KNXNetworkLink l = new KNXNetworkLinkIP(100, new InetSocketAddress(0), Util.getServer(), false,
				TPSettings.TP1)) {
			fail("illegal arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try (KNXNetworkLink l = KNXNetworkLinkIP.newTunnelingLink(new InetSocketAddress(0), Util.getServer(), false,
				TPSettings.TP1)) {
			fail("wildcard no supported");
		}
		catch (final KNXIllegalArgumentException e) {}
		// use default local host
		final KNXNetworkLink lnk = KNXNetworkLinkIP.newTunnelingLink(null, Util.getServer(), false, TPSettings.TP1);
		lnk.close();
	}

	@Test
	void tunnelingLinkFactoryMethod() throws KNXException, InterruptedException
	{
		// TODO null argument requesting default local endpoint might fail based on system settings
		try (KNXNetworkLink link = KNXNetworkLinkIP.newTunnelingLink(null, Util.getServer(), false, TPSettings.TP1)) {}

		try (KNXNetworkLink link = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false,
				TPSettings.TP1)) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#newRoutingLink(NetworkInterface, InetAddress, KNXMediumSettings)}.
	 *
	 * @throws KNXException
	 * @throws UnknownHostException
	 */
	@Test
	void newRoutingLink() throws UnknownHostException, KNXException
	{
		try (KNXNetworkLinkIP link = KNXNetworkLinkIP.newRoutingLink((NetworkInterface) null,
				InetAddress.getByName("224.0.23.14"), TPSettings.TP1)) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#addLinkListener(NetworkLinkListener)}.
	 */
	@Test
	public final void testAddLinkListener()
	{
		tnl.addLinkListener(ltnl);
		tnl.addLinkListener(ltnl);
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#setKNXMedium(KNXMediumSettings)}.
	 */
	@Test
	public final void testSetKNXMedium()
	{
		try {
			tnl.setKNXMedium(new PLSettings());
			fail("different medium");
		}
		catch (final KNXIllegalArgumentException e) {}
		final class TPSettingsSubClass extends TPSettings
		{
			TPSettingsSubClass()
			{
				super();
			}
		}
		// replace basetype with subtype
		tnl.setKNXMedium(new TPSettingsSubClass());
		// replace subtype with its supertype
		tnl.setKNXMedium(new TPSettings());

		tnl.setKNXMedium(new TPSettings(new IndividualAddress(200)));
		assertEquals(200, tnl.getKNXMedium().getDeviceAddress().getRawAddress());
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#getKNXMedium()}.
	 */
	@Test
	public final void testGetKNXMedium()
	{
		assertTrue(tnl.getKNXMedium() instanceof TPSettings);
		assertEquals(0, tnl.getKNXMedium().getDeviceAddress().getRawAddress());
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#close()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testClose() throws InterruptedException, KNXTimeoutException
	{
		assertTrue(tnl.isOpen());
		tnl.close();
		// time for link event notifier
		Thread.sleep(50);
		assertTrue(ltnl.closed);
		assertFalse(tnl.isOpen());
		tnl.close();
		try {
			tnl.send(frame, false);
			fail("we are closed");
		}
		catch (final KNXLinkClosedException e) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#getHopCount()}.
	 */
	@Test
	public final void testGetHopCount()
	{
		assertEquals(6, rtr.getHopCount());
		rtr.setHopCount(7);
		assertEquals(7, rtr.getHopCount());
		try {
			rtr.setHopCount(-1);
			fail("negative hop count");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			rtr.setHopCount(8);
			fail("hop count too big");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#removeLinkListener(NetworkLinkListener)}.
	 */
	@Test
	public final void testRemoveLinkListener()
	{
		tnl.removeLinkListener(ltnl);
		tnl.removeLinkListener(ltnl);
		// should do nothing
		tnl.removeLinkListener(lrtr);
	}

	/**
	 * Test method for
	 * {@link KNXNetworkLinkIP#sendRequest(tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 * @throws UnknownHostException
	 */
	@Test
	public final void testSendRequestKNXAddressPriorityByteArray()
		throws InterruptedException, UnknownHostException, KNXException
	{
		doSend(true, new byte[] { 0, (byte) (0x80 | 1) });
		doSend(true, new byte[] { 0, (byte) (0x80 | 0) });
		doSend(false, new byte[] { 0, (byte) (0x80 | 1) });
		doSend(false, new byte[] { 0, (byte) (0x80 | 0) });

		// send an extended PL frame
		final KNXNetworkLink plrtr = new KNXNetworkLinkIP(KNXNetworkLinkIP.ROUTING, Util.getLocalHost(),
				new InetSocketAddress(InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST), 0), false,
				new PLSettings());
		plrtr.sendRequest(new GroupAddress(0, 0, 1), Priority.LOW,
				new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) (0x80 | 0) });
		plrtr.close();
	}

	private void doSend(final boolean tunnel, final byte[] nsdu)
		throws KNXLinkClosedException, InterruptedException, KNXTimeoutException
	{
		@SuppressWarnings("resource")
		final KNXNetworkLink lnk = tunnel ? tnl : rtr;
		final NLListenerImpl l = tunnel ? ltnl : lrtr;
		l.con = null;
		lnk.sendRequest(new GroupAddress(0, 0, 1), Priority.LOW, nsdu);
		Thread.sleep(100);
		if (tunnel) {
			assertNotNull(l.con);
		}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#send(CEMILData, boolean)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testSendRequestCEMILData() throws KNXLinkClosedException, KNXTimeoutException
	{
		ltnl.con = null;
		tnl.send(frame2, false);
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException e) {}
		assertNotNull(ltnl.con);

		rtr.send(frameInd, false);
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException e) {}
	}

	/**
	 * Test method for
	 * {@link KNXNetworkLinkIP#sendRequestWait(tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testSendRequestWaitKNXAddressPriorityByteArray()
		throws KNXTimeoutException, KNXLinkClosedException
	{
		doSendWait(true, new byte[] { 0, (byte) (0x80 | 1) });
		doSendWait(true, new byte[] { 0, (byte) (0x80 | 0) });
		doSendWait(false, new byte[] { 0, (byte) (0x80 | 1) });
		doSendWait(false, new byte[] { 0, (byte) (0x80 | 0) });
	}

	private void doSendWait(final boolean tunnel, final byte[] nsdu) throws KNXLinkClosedException, KNXTimeoutException
	{
		@SuppressWarnings("resource")
		final KNXNetworkLink lnk = tunnel ? tnl : rtr;
		final NLListenerImpl l = tunnel ? ltnl : lrtr;
		l.con = null;
		lnk.sendRequestWait(new GroupAddress(0, 0, 1), Priority.LOW, nsdu);
		// even in router mode, we still get tunnel ind., so always wait
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException e) {}
		if (tunnel) {
			assertNotNull(l.con);
		}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#send(tuwien.auto.calimero.cemi.CEMILData, boolean)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testSendRequestWaitCEMILData() throws KNXTimeoutException, KNXLinkClosedException
	{
		ltnl.con = null;
		try {
			tnl.send(frameInd, true);
			fail("should get timeout because of wrong frame");
		}
		catch (final KNXTimeoutException e) {}
		assertNull(ltnl.con);
		tnl.send(frame, true);
		try {
			Thread.sleep(50);
		}
		catch (final InterruptedException e1) {}
		assertNotNull(ltnl.con);
		rtr.send(frameInd, true);
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException e) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkIP#getName()}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testGetName() throws KNXException
	{
		String n = tnl.getName();
		assertTrue(n.indexOf(Util.getServer().getAddress().getHostAddress()) > -1);
		tnl.close();
		n = tnl.getName();
		assertNotNull(n);
		assertTrue(n.indexOf(Util.getServer().getAddress().getHostAddress()) > -1);

		n = rtr.getName();
		assertTrue(n.indexOf(KNXnetIPRouting.DEFAULT_MULTICAST) > -1);
//		assertTrue(n.indexOf("link") > -1);
		rtr.close();
		n = rtr.getName();
		assertNotNull(n);
		assertTrue(n.indexOf(KNXnetIPRouting.DEFAULT_MULTICAST) > -1);
	}
}

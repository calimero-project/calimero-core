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

package tuwien.auto.calimero.mgmt;

import org.junit.experimental.categories.Category;

import category.RequireKNXNetworkLink;
import junit.framework.TestCase;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.mgmt.Destination.State;

/**
 * @author B. Malinowsky
 */
@Category(RequireKNXNetworkLink.class)
public class DestinationTest extends TestCase
{
	private KNXNetworkLink lnk;
	private TransportLayer tl;
	private Destination dst;
	private TLListener tll;

	private final class TLListener implements TransportListener
	{
		volatile int disconnected;

		TLListener()
		{}

		@Override
		public void broadcast(final FrameEvent e)
		{}

		@Override
		public void dataConnected(final FrameEvent e)
		{}

		@Override
		public void dataIndividual(final FrameEvent e)
		{}

		@Override
		public void disconnected(final Destination d)
		{
			++disconnected;
		}

		@Override
		public void group(final FrameEvent e)
		{}

		@Override
		public void linkClosed(final CloseEvent e)
		{}

		@Override
		public void detached(final DetachEvent e)
		{}
	};

	/**
	 * @param name name for test case
	 */
	public DestinationTest(final String name)
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
		Util.setupLogging();

		lnk = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, null, Util.getServer(), false,
				TPSettings.TP1);
		tl = new TransportLayerImpl(lnk);
		dst = tl.createDestination(new IndividualAddress("2.2.2"), true, false, false);
		tll = new TLListener();
		tl.addTransportListener(tll);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		if (lnk != null)
			lnk.close();

		Util.tearDownLogging();
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.Destination#Destination
	 * (tuwien.auto.calimero.mgmt.Destination.AggregatorProxy, tuwien.auto.calimero.IndividualAddress, boolean)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testDestinationAggregatorProxyIndividualAddressBoolean()
		throws KNXFormatException
	{
		final Destination d =
			new Destination(new Destination.AggregatorProxy(tl), new IndividualAddress(
				"2.2.2"), true);
		assertFalse(d.isKeepAlive());
		assertFalse(d.isVerifyMode());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.Destination#destroy()}.
	 */
	public final void testDestroy()
	{
		dst.destroy();
		assertEquals(State.Destroyed, dst.getState());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.Destination#getAddress()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAddress() throws KNXFormatException
	{
		assertEquals(new IndividualAddress("2.2.2"), dst.getAddress());
		dst.destroy();
		assertEquals(new IndividualAddress("2.2.2"), dst.getAddress());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.Destination#getState()}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 * @throws InterruptedException
	 */
	public final void testGetState() throws KNXLinkClosedException, KNXTimeoutException,
		InterruptedException
	{
		assertEquals(Destination.State.Disconnected, dst.getState());
		assertEquals(0, tll.disconnected);
		tl.connect(dst);
		assertEquals(0, tll.disconnected);
		tl.disconnect(dst);
		assertEquals(Destination.State.Disconnected, dst.getState());
		assertEquals(1, tll.disconnected);
		tl.connect(dst);
		try {
			tl.sendData(dst, Priority.LOW, new byte[] { 0 });
			fail("we should've been disconnected");
		}
		catch (final KNXDisconnectException e) {
			assertEquals(dst, e.getDestination());
		}
		assertEquals(Destination.State.Disconnected, dst.getState());
		assertEquals(2, tll.disconnected);

		tl.connect(dst);
		assertEquals(Destination.State.OpenIdle, dst.getState());
		Thread.sleep(6100);
		assertEquals(Destination.State.Disconnected, dst.getState());
		assertEquals(3, tll.disconnected);

		dst.destroy();
		assertEquals(3, tll.disconnected);
		assertEquals(Destination.State.Destroyed, dst.getState());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.Destination#isConnectionOriented()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testIsConnectionOriented() throws KNXFormatException
	{
		assertTrue(dst.isConnectionOriented());
		final Destination d =
			new Destination(new Destination.AggregatorProxy(tl), new IndividualAddress(
				"2.2.2"), false);
		assertFalse(d.isConnectionOriented());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.Destination#isKeepAlive()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testIsKeepAlive() throws KNXFormatException
	{
		assertFalse(dst.isKeepAlive());
		final Destination d =
			new Destination(new Destination.AggregatorProxy(tl), new IndividualAddress(
				"2.2.2"), true, true, false);
		assertTrue(d.isKeepAlive());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.Destination#isVerifyMode()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testIsVerifyMode() throws KNXFormatException
	{
		assertFalse(dst.isVerifyMode());
		final Destination d =
			new Destination(new Destination.AggregatorProxy(tl), new IndividualAddress(
				"2.2.2"), true, true, true);
		assertTrue(d.isVerifyMode());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.Destination#toString()}.
	 */
	public final void testToString()
	{
		System.out.println(dst.toString());
		dst.destroy();
		System.out.println(dst.toString());
	}
}

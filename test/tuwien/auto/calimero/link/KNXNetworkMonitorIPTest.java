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

package tuwien.auto.calimero.link;

import java.net.InetSocketAddress;

import junit.framework.TestCase;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RawFrame;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogManager;

/**
 * @author B. Malinowsky
 */
public class KNXNetworkMonitorIPTest extends TestCase
{
	private KNXNetworkMonitor mon;
	private MonListener lmon;

	final class MonListener implements LinkListener
	{
		volatile CEMIBusMon ind;
		volatile boolean closed;
		volatile RawFrame raw;

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.link.event.LinkListener#indication
		 * (tuwien.auto.calimero.FrameEvent)
		 */
		public void indication(final FrameEvent e)
		{
			assertNotNull(e);
			assertTrue(e instanceof MonitorFrameEvent);
			assertEquals(mon, e.getSource());
			ind = (CEMIBusMon) e.getFrame();
			raw = ((MonitorFrameEvent) e).getRawFrame();
			assertEquals(CEMIBusMon.MC_BUSMON_IND, ind.getMessageCode());
			System.out.println("indication");
			Debug.printMonData(ind);
			if (raw != null)
				Debug.printTP1Frame(lmon.raw);
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.link.event.LinkListener#linkClosed
		 * (tuwien.auto.calimero.CloseEvent)
		 */
		public void linkClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(mon, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	/**
	 * @param name name of test case
	 */
	public KNXNetworkMonitorIPTest(final String name)
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

		mon = new KNXNetworkMonitorIP(Util.getLocalHost(), Util.getServer(), false,
			TPSettings.TP1);
		lmon = new MonListener();
		mon.addMonitorListener(lmon);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		if (mon != null)
			mon.close();

		LogManager.getManager().removeWriter(null, Util.getLogWriter());
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.link.KNXNetworkMonitorIP#KNXNetworkMonitorIP
	 * (java.net.InetSocketAddress, java.net.InetSocketAddress, boolean,
	 * tuwien.auto.calimero.link.medium.KNXMediumSettings)}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testKNXNetworkMonitorIP() throws KNXException, InterruptedException
	{
		if (mon != null)
			mon.close();
		try {
			new KNXNetworkMonitorIP(new InetSocketAddress(0), Util.getServer(), false,
				TPSettings.TP1);
			fail("wildcard no supported");
		}
		catch (final KNXIllegalArgumentException e) {}
		mon = new KNXNetworkMonitorIP(null, Util.getServer(), false, TPSettings.TP1);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.link.KNXNetworkMonitorIP#setKNXMedium
	 * (tuwien.auto.calimero.link.medium.KNXMediumSettings)}.
	 */
	public final void testSetKNXMedium()
	{
		try {
			mon.setKNXMedium(new PLSettings(true));
			fail("different medium");
		}
		catch (final KNXIllegalArgumentException e) {}
		final class TPSettingsSubClass extends TPSettings
		{
			TPSettingsSubClass()
			{
				super(false);
			}
		}
		// replace basetype with subtype
		mon.setKNXMedium(new TPSettingsSubClass());
		// replace subtype with its supertype
		mon.setKNXMedium(new TPSettings(true));

		mon.setKNXMedium(new TPSettings(new IndividualAddress(200), true));
		assertEquals(200, mon.getKNXMedium().getDeviceAddress().getRawAddress());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.link.KNXNetworkMonitorIP#close()}.
	 * 
	 * @throws InterruptedException
	 */
	public final void testClose() throws InterruptedException
	{
		assertTrue(mon.isOpen());
		mon.close();
		// time for link event notifier
		Thread.sleep(50);
		assertTrue(lmon.closed);
		assertFalse(mon.isOpen());
		mon.close();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.link.KNXNetworkMonitorIP#setDecodeRawFrames (boolean)}.
	 * 
	 * @throws InterruptedException
	 */
	public final void testSetDecodeRawFrames() throws InterruptedException
	{
		mon.setDecodeRawFrames(true);
		lmon.raw = null;
		System.out.println("monitor: waiting for incoming frames..");
		Thread.sleep(10 * 1000);
		assertNotNull(lmon.raw);
		mon.setDecodeRawFrames(false);
		lmon.raw = null;
		Thread.sleep(10 * 1000);
		assertNull(lmon.raw);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.link.KNXNetworkMonitorIP#getName ()}.
	 * 
	 * @throws KNXException
	 */
	public final void testGetName() throws KNXException
	{
		String n = mon.getName();
		assertTrue(n.indexOf(Util.getServer().getAddress().getHostAddress()) > -1);
		assertTrue(n.indexOf("monitor") > -1);
		mon.close();
		n = mon.getName();
		assertNotNull(n);
		assertTrue(n.indexOf("monitor") > -1);
	}
}

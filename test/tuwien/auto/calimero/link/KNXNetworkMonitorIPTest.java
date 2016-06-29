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

import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RawFrame;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class KNXNetworkMonitorIPTest
{
	private KNXNetworkMonitor mon;
	private MonListener lmon;

	final class MonListener implements LinkListener
	{
		volatile CEMIBusMon ind;
		volatile boolean closed;
		volatile RawFrame raw;

		@Override
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

		@Override
		public void linkClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(mon, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	@BeforeEach
	void init() throws Exception
	{
		mon = new KNXNetworkMonitorIP(Util.getLocalHost(), Util.getServer(), false, TPSettings.TP1);
		lmon = new MonListener();
		mon.addMonitorListener(lmon);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (mon != null)
			mon.close();
	}

	/**
	 * Test method for
	 * {@link KNXNetworkMonitorIP#KNXNetworkMonitorIP(InetSocketAddress, InetSocketAddress, boolean, KNXMediumSettings)}
	 * .
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testKNXNetworkMonitorIP() throws KNXException, InterruptedException
	{
		if (mon != null)
			mon.close();
		try (final KNXNetworkMonitor m = new KNXNetworkMonitorIP(new InetSocketAddress(0), Util.getServer(), false,
				TPSettings.TP1)) {
			fail("wildcard no supported");
		}
		catch (final KNXIllegalArgumentException e) {}
		mon = new KNXNetworkMonitorIP(null, Util.getServer(), false, TPSettings.TP1);
	}

	/**
	 * Test method for {@link KNXNetworkMonitorIP#setKNXMedium(KNXMediumSettings)}.
	 */
	@Test
	public final void testSetKNXMedium()
	{
		try {
			mon.setKNXMedium(new PLSettings());
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
		mon.setKNXMedium(new TPSettingsSubClass());
		// replace subtype with its supertype
		mon.setKNXMedium(new TPSettings());

		mon.setKNXMedium(new TPSettings(new IndividualAddress(200)));
		assertEquals(200, mon.getKNXMedium().getDeviceAddress().getRawAddress());
	}

	/**
	 * Test method for {@link KNXNetworkMonitorIP#close()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
	 * Test method for {@link KNXNetworkMonitorIP#setDecodeRawFrames(boolean)}.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSetDecodeRawFrames() throws InterruptedException
	{
		mon.setDecodeRawFrames(true);
		lmon.raw = null;
		System.out.println("monitor: waiting for incoming frames..");
		Thread.sleep(10 * 1000);
//		assertNotNull(lmon.raw);
		mon.setDecodeRawFrames(false);
		lmon.raw = null;
		Thread.sleep(10 * 1000);
		assertNull(lmon.raw);
	}

	/**
	 * Test method for {@link KNXNetworkMonitorIP#getName()}.
	 *
	 * @throws KNXException
	 */
	@Test
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

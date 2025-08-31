/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.CloseEvent;
import io.calimero.FrameEvent;
import io.calimero.KNXException;
import io.calimero.Util;
import io.calimero.cemi.CEMIBusMon;
import io.calimero.knxnetip.Debug;
import io.calimero.link.medium.RawFrame;
import io.calimero.link.medium.TPSettings;
import tag.FT12;

@FT12
class KNXNetworkMonitorFT12Test
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
			assertInstanceOf(MonitorFrameEvent.class, e);
			assertEquals(mon, e.getSource());
			ind = (CEMIBusMon) e.getFrame();
			raw = ((MonitorFrameEvent) e).getRawFrame();
			assertEquals(CEMIBusMon.MC_BUSMON_IND, ind.getMessageCode());
			System.out.println("indication");
			Debug.printMonData(ind);
			if (raw != null)
				Debug.printTP1Frame(lmon.raw);
			// System.out.println(e.getFrame().toString());
			// System.out.println(((MonitorFrameEvent) e).getRawFrame());
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
	void setUp() throws Exception
	{
		// prevents access problems with a just previously closed port
		Thread.sleep(50);
		mon = new KNXNetworkMonitorFT12(Util.getSerialPortID(), new TPSettings());
		lmon = new MonListener();
		mon.addMonitorListener(lmon);
	}

	@AfterEach
	void tearDown() {
		if (mon != null)
			mon.close();
	}

	@Test
	void networkMonitorFT12StringKNXMediumSettings() throws KNXException, InterruptedException
	{
		mon.close();
		mon = new KNXNetworkMonitorFT12(Util.getSerialPortID(), new TPSettings());
	}

	@Test
	void networkMonitorFT12EmptyPortId() throws InterruptedException
	{
		mon.close();
		try {
			mon = new KNXNetworkMonitorFT12("", new TPSettings());
			fail("should fail");
		}
		catch (final KNXException e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	void setDecodeRawFrames() throws InterruptedException
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

	@Test
	void getName()
	{
		String n = mon.getName();
		final String port = Util.getSerialPortID();
		assertTrue(n.contains(port), port);
		assertTrue(n.contains("monitor"));
		mon.close();
		n = mon.getName();
		assertNotNull(n);
		assertTrue(n.contains("monitor"));
	}

	@Test
	void close() throws InterruptedException
	{
		System.out.println(mon.toString());
		assertTrue(mon.isOpen());
		mon.close();
		// time for link event notifier
		Thread.sleep(50);
		assertTrue(lmon.closed);
		assertFalse(mon.isOpen());
		mon.close();
		System.out.println(mon.toString());
	}
}

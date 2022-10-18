/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.BusMonitorLayer;
import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.LinkLayer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import tag.KnxnetIP;
import tag.KnxnetIPSequential;
import tag.Slow;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.ReturnCode;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode;
import tuwien.auto.calimero.knxnetip.servicetype.TunnelingFeature;
import tuwien.auto.calimero.knxnetip.servicetype.TunnelingFeature.InterfaceFeature;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;


@KnxnetIP
class KNXnetIPTunnelTest
{
	private static KNXnetIPConnection.BlockingMode noblock = KNXnetIPConnection.BlockingMode.NonBlocking;
	private static KNXnetIPConnection.BlockingMode ack = KNXnetIPConnection.BlockingMode.WaitForAck;
	private static KNXnetIPConnection.BlockingMode con = KNXnetIPConnection.BlockingMode.WaitForCon;

	private TcpConnection connection;

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

	private final class KNXListenerImpl implements TunnelingListener
	{
		boolean closed;
		CEMI received;
		List<CEMI> fifoReceived = new Vector<>();

		BlockingQueue<TunnelingFeature> featureResponse = new ArrayBlockingQueue<>(1);
		BlockingQueue<CEMILData> con = new ArrayBlockingQueue<>(100);

		@Override
		public void frameReceived(final FrameEvent e)
		{
			try {
				assertNotNull(e);
				if (this == l)
					assertEquals(t, e.getSource());
				if (this == lnat)
					assertEquals(tnat, e.getSource());
				if (this == lmon)
					assertEquals(mon, e.getSource());
				received = e.getFrame();
				if (received.getMessageCode() == CEMILData.MC_LDATA_CON) {
					con.add((CEMILData) received);
				}
				if (e.getFrame() instanceof CEMIBusMon) {
					Debug.printMonData((CEMIBusMon) e.getFrame());
				}
				fifoReceived.add(e.getFrame());
			}
			catch (final RuntimeException rte) {
				fail("exception in frame received", rte);
			}
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

		@Override
		public void featureResponse(final TunnelingFeature feature) {
			featureResponse.add(feature);
		}

		@Override
		public void featureInfo(final TunnelingFeature feature) {}
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

		if (connection != null)
			connection.close();
	}

	@Test
	void send() throws KNXException, InterruptedException
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
	}

	@Test
	void fifoSend() throws KNXException, InterruptedException
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
				}
				catch (KNXTimeoutException | KNXConnectionClosedException | InterruptedException e) {
					fail("send fifo message in " + Thread.currentThread().getName());
				}
			}
		}

		newTunnel();
		final Thread[] threads = new Thread[sends];
		for (int i = 0; i < sends; i++) {
			threads[i] = new Sender("sender " + (i + 1));
		}
		for (int i = 0; i < threads.length; i++) {
			synchronized (threads[i]) {
				threads[i].start();
				threads[i].wait();
			}
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
		final int size = l.fifoReceived.size();
		assertTrue(sends >= size, "sends = " + sends + ", received = " + size);

		final List<IndividualAddress> fifoAddresses = new ArrayList<>();
		synchronized (l.fifoReceived) {
			for (final CEMI cemi : l.fifoReceived) {
				if (cemi instanceof CEMILData) {
					final CEMILData ldata = (CEMILData) cemi;
					fifoAddresses.add(ldata.getSource());
				}
			}
		}
		for (int i = 0; i < sends; i++) {
			final IndividualAddress addr = new IndividualAddress(i + 1);
			assertTrue(fifoAddresses.contains(addr), "no .con for " + addr);
		}
	}

	@Test
	void natSend() throws KNXException, InterruptedException
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

	@Test
	@KnxnetIPSequential
	void monitorSend() throws KNXException, InterruptedException
	{
		newMonitor();
		try {
			mon.send(frame, ack);
			fail("no send in busmon");
		}
		catch (final IllegalStateException e) {}
		// on open monitor test behavior on new tunnel
		try {
			newTunnel();
			fail("no tunnel on busmonitor");
		}
		catch (final KNXException e) {}
	}

	@Test
	@KnxnetIPSequential
	void tunnelWithMonitor() throws KNXException, InterruptedException
	{
		newTunnel();
		try {
			newMonitor();
			fail("no monitor on open tunnel");
		}
		catch (final KNXException e) {}
	}

	@Test
	@Slow
	void receive() throws KNXException, InterruptedException
	{
		newTunnel();
		Util.out("Tunnel: waiting for some incoming frames...");
		try {
			Thread.sleep(10 * 1000);
		}
		catch (final InterruptedException e) {}
	}

	@Test
	@KnxnetIPSequential
	void receiveMonitor() throws KNXException, InterruptedException
	{
		newMonitor();
		Util.out("Monitor: waiting for some incoming frames...");
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
		final CEMILData con = l.con.poll(1, TimeUnit.SECONDS);
		assertNotNull(con);
		assertEquals(positiveConfirmation, con.isPositiveConfirmation());
		l.received = null;
	}

	private void doNATSend(final CEMILData f, final KNXnetIPConnection.BlockingMode m,
		final boolean positiveConfirmation)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
	{
		lnat.received = null;
		tnat.send(f, m);
		final CEMILData con = lnat.con.poll(1, TimeUnit.SECONDS);
		assertNotNull(con);
		assertEquals(positiveConfirmation, con.isPositiveConfirmation());
		lnat.received = null;
	}

	@Test
	void knxnetIPTunnel() throws KNXException, InterruptedException
	{
		try (KNXnetIPConnection c = new KNXnetIPTunnel(LinkLayer, null, new InetSocketAddress("127.0.0.1", 4000),
				false)) {
			fail("local socket is null");
		}
		catch (final KNXIllegalArgumentException e) {}

		try (KNXnetIPConnection c = new KNXnetIPTunnel(LinkLayer, new InetSocketAddress("0.0.0.0", 0),
				new InetSocketAddress("127.0.0.1", 4000), false)) {}
		catch (final KNXTimeoutException expected) {
			// can happen if server does not respond on loopback
		}

		newTunnel();
		assertEquals(KNXnetIPConnection.OK, t.getState());
	}

	@Test
	@KnxnetIPSequential
	void knxnetIPMonitor() throws KNXException, InterruptedException
	{
		newMonitor();
		assertEquals(KNXnetIPConnection.OK, mon.getState());
	}

	@Test
	void close() throws KNXException, InterruptedException
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

	@Test
	void getRemoteAddress() throws KNXException, InterruptedException
	{
		newTunnel();
		assertEquals(Util.getServer(), t.getRemoteAddress());
		t.close();
		assertTrue(t.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertTrue(t.getRemoteAddress().getPort() == 0);
	}

	@Test
	@Slow
	void getState() throws KNXException, InterruptedException
	{
		newTunnel();
		assertEquals(KNXnetIPConnection.OK, t.getState());
		System.out.println("Testing heartbeat, this will take some minutes... ! ");
		// give some seconds space for delay so we're on the safe side
		Thread.sleep(4000);
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, t.getState());
		Thread.sleep(60000);
		assertEquals(KNXnetIPConnection.OK, t.getState());
	}

	@Test
	@Slow
	@KnxnetIPSequential
	void monitorGetState() throws KNXException, InterruptedException
	{
		newMonitor();
		assertEquals(KNXnetIPConnection.OK, mon.getState());
		System.out.println("Testing heartbeat, this will take some minutes...!");
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

	@Test
	void requestAvailableTunnelingAddress() throws KNXException, InterruptedException {
		final IndividualAddress requestAddress = new IndividualAddress(1, 1, 15);
		t = new KNXnetIPTunnel(LinkLayer, Util.localEndpoint(), Util.getServer(), false, requestAddress);
	}

	@Test
	void requestNonAvailableTunnelingAddress() {
		final IndividualAddress requestAddress = new IndividualAddress(1, 1, 200);
		assertThrows(KNXException.class,
				() -> new KNXnetIPTunnel(LinkLayer, Util.localEndpoint(), Util.getServer(), false, requestAddress));
	}

	@Test
	void requestUsedTunnelingAddress() throws KNXException, InterruptedException {
		final IndividualAddress requestAddress = new IndividualAddress(1, 1, 25);
		try (final KNXnetIPTunnel snatch = new KNXnetIPTunnel(LinkLayer, Util.localEndpoint(), Util.getServer(), false, requestAddress)) {
			assertThrows(KNXException.class, () -> new KNXnetIPTunnel(LinkLayer, Util.localEndpoint(), Util.getServer(), false, requestAddress));
		}
	}

	@ParameterizedTest
	@EnumSource(InterfaceFeature.class)
	void tunnelingFeatureGet(final InterfaceFeature feature) throws InterruptedException, KNXException {
		newTunnel();
		t.send(feature);
		final TunnelingFeature response = l.featureResponse.poll(3, TimeUnit.SECONDS);
		assertNotNull(response);
		assertEquals(feature, response.featureId());
	}

	@ParameterizedTest
	@EnumSource(InterfaceFeature.class)
	void tunnelingFeatureSet(final InterfaceFeature feature) throws InterruptedException, KNXException {
		newTunnel();
		final List<InterfaceFeature> twoBytes = List.of(InterfaceFeature.SupportedEmiTypes, InterfaceFeature.DeviceDescriptorType0,
				InterfaceFeature.Manufacturer, InterfaceFeature.IndividualAddress, InterfaceFeature.MaxApduLength);
		final int length = twoBytes.contains(feature) ? 2 : 1;

		t.send(feature, new byte[length]);
		final TunnelingFeature response = l.featureResponse.poll(3, TimeUnit.SECONDS);
		assertNotNull(response);
		assertEquals(feature, response.featureId());
		if (feature != InterfaceFeature.IndividualAddress && feature != InterfaceFeature.EnableFeatureInfoService)
			assertEquals(ReturnCode.AccessReadOnly, response.status());
	}

	@Test
	void tcpConnection() throws KNXException, InterruptedException {
		newTcpTunnel();
		assertEquals(KNXnetIPConnection.OK, t.getState());
		assertEquals(Util.getServer(), t.getRemoteAddress());
	}

	@Test
	void tcpSend() throws KNXException, InterruptedException {
		newTcpTunnel();
		doSend(frame, BlockingMode.WaitForCon, true);
		doSend(frame, BlockingMode.WaitForCon, true);
		doSend(frame, BlockingMode.WaitForCon, true);
	}

	private void newTunnel() throws KNXException, InterruptedException
	{
		t = new KNXnetIPTunnel(LinkLayer, Util.localEndpoint(), Util.getServer(), false);
		t.addConnectionListener(l);
	}

	private void newNATTunnel() throws KNXException, InterruptedException
	{
		tnat = new KNXnetIPTunnel(LinkLayer, Util.localEndpoint(), Util.getServer(), true);
		tnat.addConnectionListener(lnat);
	}

	private void newTcpTunnel() throws KNXException, InterruptedException {
		connection = TcpConnection.newTcpConnection(Util.localEndpoint(), Util.getServer());
		t = new KNXnetIPTunnel(LinkLayer, connection, KNXMediumSettings.BackboneRouter);
		System.out.println(t);
		t.addConnectionListener(l);
	}

	private void newMonitor() throws KNXException, InterruptedException
	{
		mon = new KNXnetIPTunnel(BusMonitorLayer, Util.localEndpoint(), Util.getServer(), false);
		mon.addConnectionListener(lmon);
	}
}

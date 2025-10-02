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

package io.calimero.knxnetip;

import static io.calimero.knxnetip.KNXnetIPConnection.BlockingMode.NonBlocking;
import static java.nio.ByteBuffer.allocate;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.CloseEvent;
import io.calimero.DataUnitBuilder;
import io.calimero.FrameEvent;
import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.Priority;
import io.calimero.Util;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMILData;
import io.calimero.cemi.CEMILDataEx;
import io.calimero.knxnetip.KNXnetIPConnection.BlockingMode;
import io.calimero.knxnetip.servicetype.KNXnetIPHeader;
import io.calimero.knxnetip.servicetype.RoutingBusy;
import io.calimero.knxnetip.servicetype.RoutingSystemBroadcast;
import io.calimero.mgmt.PropertyAccess.PID;
import tag.KnxnetIP;
import tag.KnxnetIPSequential;
import tag.Slow;


@KnxnetIP
class KNXnetIPRouterTest
{
	private KNXnetIPRouting r;
	private RouterListenerImpl l;

	private CEMILData frame;
	private CEMILData frame2;
	// should be a frame with unused destination address
	private CEMILData frameNoDest;

	private final AtomicInteger routingBusy = new AtomicInteger();
	private final AtomicInteger rateLimit = new AtomicInteger();

	private final class RouterListenerImpl implements RoutingListener
	{
		volatile boolean closed;
		final BlockingQueue<CEMI> received = new ArrayBlockingQueue<>(100);
		final List<LostMessageEvent> lost = new Vector<>();

		@Override
		public void frameReceived(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(r, e.getSource());
			received.add(e.getFrame());
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

		@Override
		public void rateLimit(final RateLimitEvent e) {
			rateLimit.incrementAndGet();
		}
	}

	@BeforeEach
	void init() throws Exception
	{
		l = new RouterListenerImpl();
		frame = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 1) }, Priority.NORMAL);
		frame2 = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80) }, Priority.URGENT);
		frameNoDest = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress(10, 7, 10),
				new byte[] { 0, (byte) (0x80) }, Priority.URGENT);
		routingBusy.set(0);

		r = new KNXnetIPRouting(null, KNXnetIPRouting.DefaultMulticast);
		r.addConnectionListener(l);
	}

	@AfterEach
	void tearDown() {
		if (r != null) {
			r.close();
		}
	}

	@Test
	void send() throws KNXException
	{
		doSend(frame, NonBlocking);
		doSend(frame2, NonBlocking);
		doSend(frameNoDest, NonBlocking);
	}

	@Test
	void send2() throws KNXException, SocketException, UnknownHostException
	{
		r.close();
		r = new KNXnetIPRouting(Util.localInterface(), InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST));
		l = new RouterListenerImpl();
		r.addConnectionListener(l);
		doSend(frame, NonBlocking);
		doSend(frame2, NonBlocking);
		doSend(frameNoDest, NonBlocking);
	}

	private void doSend(final CEMILData f, final KNXnetIPConnection.BlockingMode m) throws KNXConnectionClosedException
	{
		r.send(f, m);
		try {
			Thread.sleep(500);
		}
		catch (final InterruptedException e) {}
	}

	@Test
	void knxnetIPRouter() throws SocketException, UnknownHostException, KNXException
	{
		assertEquals(KNXnetIPConnection.OK, r.getState());
		r.close();
		r = new KNXnetIPRouting(Util.localInterface(), InetAddress.getByName(KNXnetIPRouting.DEFAULT_MULTICAST));
		r.close();
		try {
			r = new KNXnetIPRouting(null, InetAddress.getByName("224.0.23.11"));
			fail("invalid routing multicast");
		}
		catch (final KNXIllegalArgumentException e) {}
		r = new KNXnetIPRouting(null, InetAddress.getByName("224.0.23.13"));
	}

	@Test
	@Slow
	void receive()
	{
		Util.out("waiting for some incoming frames...");
		try {
			Thread.sleep(10 * 1000);
		}
		catch (final InterruptedException e) {}
	}

	@Test
	void setHopCount()
	{
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

	@Test
	void close()
	{
		r.close();
		assertEquals(KNXnetIPConnection.CLOSED, r.getState());
		try {
			r.send(frame, NonBlocking);
			fail("we are closed");
		}
		catch (final KNXConnectionClosedException e) {}
		assertEquals(KNXnetIPConnection.CLOSED, r.getState());
	}

	@Test
	void getRemoteAddress() throws KNXException, SocketException, UnknownHostException
	{
		assertEquals(new InetSocketAddress(KNXnetIPRouting.DEFAULT_MULTICAST, KNXnetIPConnection.DEFAULT_PORT),
				r.getRemoteAddress());
		r.close();
		assertTrue(r.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertEquals(0, r.getRemoteAddress().getPort());

		r = new KNXnetIPRouting(Util.localInterface(), InetAddress.getByName("224.0.23.33"));
		assertEquals(new InetSocketAddress("224.0.23.33", KNXnetIPConnection.DEFAULT_PORT), r.getRemoteAddress());
		r.close();
		assertTrue(r.getRemoteAddress().getAddress().isAnyLocalAddress());
		assertEquals(0, r.getRemoteAddress().getPort());
	}

	@Test
	@KnxnetIPSequential
	void lostMessageIndication() throws KNXException
	{
		int sent = 0;
		while (sent < 1000 && l.lost.isEmpty()) {
			r.send(frame, NonBlocking);
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
		for (final LostMessageEvent e : l.lost) {
			System.out.println("dev.state:" + e.getDeviceState() + ", lost msgs:" + e.getLostMessages());
		}
	}

	private static final Duration timeout = Duration.ofMillis(1000);

	@Test
	void sendRoutingBusy() throws KNXException
	{
		r.send(new RoutingBusy(0, Duration.ofMillis(100), 0));
		Assertions.assertTimeout(timeout, () -> {
			while (routingBusy.get() == 0)
				Thread.sleep(50);
		}, "no routing busy notification");
		assertEquals(1, routingBusy.get());
	}

	@Test
	void incrementRoutingBusyCounter() throws KNXException, InterruptedException
	{
		final int messages = 5;
		for (int i = 0; i < messages; i++) {
			r.send(new RoutingBusy(1, Duration.ofMillis(20), 0));
			Thread.sleep(12);
		}
		Assertions.assertTimeout(timeout, () -> {
			while (routingBusy.get() < messages)
				Thread.sleep(50);
		}, "wrong number of routing busy notification");
		assertEquals(messages, routingBusy.get());
	}

	@Test
	@KnxnetIPSequential
	void fastSendManyRoutingBusy() throws KNXException
	{
		final int messages = 40;
		for (int i = 0; i < messages; i++) {
			r.send(new RoutingBusy(1, Duration.ofMillis(20), 0));
		}
		Assertions.assertTimeout(timeout, () -> {
			while (routingBusy.get() < messages)
				Thread.sleep(50);
		}, "wrong number of routing busy notification");
		assertEquals(messages, routingBusy.get());
	}

	@Test
	@SuppressWarnings("resource")
	void initWithSystemBroadcastSocket() {
		assertDoesNotThrow(() ->  new KNXnetIPRouting(null, InetAddress.getByName("224.0.23.13")));
	}

	@Test
	void systemBroadcastWithDefaultSocket() throws KNXException, IOException, InterruptedException {
		sysBroadcast(Discoverer.SYSTEM_SETUP_MULTICAST, Discoverer.SYSTEM_SETUP_MULTICAST);
	}

	@Test
	void systemBroadcastWithSysBcastSocket() throws KNXException, IOException, InterruptedException {
		sysBroadcast(InetAddress.getByName("224.0.23.13"), InetAddress.getByName("224.0.23.14"));
	}

	private void sysBroadcast(final InetAddress senderGroup, final InetAddress receiverGroup)
			throws KNXException, IOException, InterruptedException {

		final CEMILDataEx sysBcast = newSystemBroadcastFrame(systemNetworkParamResponse());

		// receiver
		r.close();
		r = new KNXnetIPRouting(null, receiverGroup);
		l = new RouterListenerImpl();
		r.addConnectionListener(l);

		try (KNXnetIPRouting sender = new KNXnetIPRouting(null, senderGroup);
				MulticastSocket verify = new MulticastSocket(KNXnetIPConnection.DEFAULT_PORT)) {

			verify.joinGroup(new InetSocketAddress(Discoverer.SYSTEM_SETUP_MULTICAST, 0), null);
			verify.setSoTimeout(1000);

			sender.send(sysBcast, BlockingMode.NonBlocking);

			for (int i = 0; i < 5; i++) {
				final byte[] buf = new byte[512];
				final DatagramPacket p = new DatagramPacket(buf, buf.length);
				verify.receive(p);
				final KNXnetIPHeader header = new KNXnetIPHeader(buf, 0);
				try {
					final var ind = new RoutingSystemBroadcast(buf, header.getStructLength(),
							header.getTotalLength() - header.getStructLength());
					assertSystemBroadcast(sysBcast, (CEMILData) ind.cemi());
					break;
				}
				catch (final KNXFormatException ignore) {}
			}
		}
		assertSystemBroadcast(sysBcast, (CEMILData) l.received.poll(2, TimeUnit.SECONDS));
	}

	private CEMILDataEx newSystemBroadcastFrame(final byte[] tpdu) {
		final IndividualAddress src = new IndividualAddress(1, 1, 200);
		final KNXAddress dst = GroupAddress.Broadcast;
		return new CEMILDataEx(CEMILData.MC_LDATA_IND, src, dst, tpdu, Priority.SYSTEM, false, false, false, 6);
	}

	private static byte[] systemNetworkParamResponse() {
		final int objectType = 0;
		final int pid = PID.SERIAL_NUMBER;
		final int operand = 1;
		final byte[] value = { 1, 2, 3, 4, 5, 6 };
		final byte[] asdu = allocate(5 + value.length).putShort((short) objectType).putShort((short) (pid << 4))
				.put((byte) operand).put(value).array();

		final int SystemNetworkParamResponse = 0b0111001001;
		final byte[] tsdu = DataUnitBuilder.createAPDU(SystemNetworkParamResponse, asdu);
		return tsdu;
	}

	private static void assertSystemBroadcast(final CEMILData expected, final CEMILData actual) {
		assertEquals(CEMILData.MC_LDATA_IND, actual.getMessageCode());
		assertEquals(expected.getSource(), actual.getSource());
		assertEquals(expected.getDestination(), actual.getDestination());
		assertArrayEquals(expected.getPayload(), actual.getPayload());
	}

	@Test
	void maxDatagramsPerSecond() throws InterruptedException, KNXException {
		sendMaxDatagramsPerSecond();
		Thread.sleep(1000);
		l.received.clear();
		sendMaxDatagramsPerSecond();
	}

	private void sendMaxDatagramsPerSecond() throws KNXConnectionClosedException, InterruptedException {
		Thread.sleep(1000);
		final long end = System.nanoTime() + 1_000_000_000;
		int sent = 0;
		while (System.nanoTime() < end) {
			r.send(frame, BlockingMode.NonBlocking);
			++sent;
			assertTrue(l.received.size() <= KNXnetIPRouting.MaxDatagramsPerSecond + 1);
		}
		assertTrue(sent <= KNXnetIPRouting.MaxDatagramsPerSecond + 1, "sent should be <= 51, but sent = " + sent);
	}

	@Test
	void rateLimitNotification() throws KNXConnectionClosedException, InterruptedException {
		sendMaxDatagramsPerSecond();
		assertTrue(rateLimit.get() > 0);
	}

	private static final int A_FunctionPropertyCommand = 0b1011000111;
	private static final int A_FunctionPropertyRead = 0b1011001000;
	private static final int pidIpSbcControl = 120;

	@Test
	void maxSysBcastsPerSecond() throws KNXConnectionClosedException, InterruptedException {
		final int routerObjectIndex = 9;

		var tsdu = DataUnitBuilder.createAPDU(A_FunctionPropertyCommand, (byte) routerObjectIndex, (byte) pidIpSbcControl,
				(byte) 0, (byte) 0, (byte) 1);
		final IndividualAddress src = new IndividualAddress(1, 1, 200);
		final var server = Util.getRouterAddress();

		final CEMILData enableSysBcast = new CEMILData(CEMILData.MC_LDATA_IND, src, server, tsdu, Priority.SYSTEM, false, 6);
		r.send(enableSysBcast, BlockingMode.NonBlocking);

		tsdu = DataUnitBuilder.createAPDU(A_FunctionPropertyRead, (byte) routerObjectIndex, (byte) pidIpSbcControl,
				(byte) 0, (byte) 0);
		final CEMILData checkSysBcast = new CEMILData(CEMILData.MC_LDATA_IND, src, server, tsdu, Priority.SYSTEM, false, 6);
		r.send(checkSysBcast, BlockingMode.NonBlocking);

		final CEMILDataEx sysBcast = newSystemBroadcastFrame(systemNetworkParamResponse());
		for (int i = 0; i < 300; i++) {
			r.send(sysBcast, BlockingMode.NonBlocking);
		}
		Thread.sleep(1000);
	}
}

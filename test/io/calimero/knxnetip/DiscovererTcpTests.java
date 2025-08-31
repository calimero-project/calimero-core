/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2021, 2025 B. Malinowsky

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

import static io.calimero.knxnetip.util.Srp.withDeviceDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.FrameEvent;
import io.calimero.KNXException;
import io.calimero.KNXTimeoutException;
import io.calimero.Util;
import io.calimero.cemi.CEMIDevMgmt;
import io.calimero.knxnetip.KNXnetIPConnection.BlockingMode;
import io.calimero.knxnetip.util.DIB;
import io.calimero.knxnetip.util.Srp;
import tag.KnxnetIP;
import tag.KnxnetIPSequential;

@KnxnetIP
class DiscovererTcpTests {

	private TcpConnection conn;
	private DiscovererTcp discoverer;

	@BeforeEach
	void setUp() {
		conn = TcpConnection.newTcpConnection(Util.localEndpoint(), Util.getServer());
		discoverer = Discoverer.tcp(conn);
	}

	@AfterEach
	void tearDown() {
		conn.close();
	}

	@Test
	void searchUsingNewTcp() throws InterruptedException, ExecutionException {
		try (var connection = TcpConnection.newTcpConnection(Util.getLocalHost(), Util.getServer())) {
			final var future = Discoverer.tcp(connection).search();
			final var result = future.get();
			assertFalse(result.isEmpty());
		}
	}

	@Test
	void searchUsingSecureSession() throws InterruptedException, ExecutionException {
		final var pwdHash = SecureConnection.hashUserPassword("user1".toCharArray());
		try (var session = conn.newSecureSession(1, pwdHash, new byte[16])) {

			final var future = Discoverer.secure(session).search();
			final var result = future.get();
			assertFalse(result.isEmpty());
		}
	}

	@Test
	void description() throws KNXException, InterruptedException {
		final var result = discoverer.description();
		assertEquals(Util.getServer(), result.remoteEndpoint());
	}

	@Test
	void descriptionUsingSecureSession() throws KNXException, InterruptedException {
		final var pwdHash = SecureConnection.hashUserPassword("user1".toCharArray());
		try (var session = conn.newSecureSession(1, pwdHash, new byte[16])) {
			final var discoverer = Discoverer.secure(session);
			final var result = discoverer.description();
			assertEquals(Util.getServer(), result.remoteEndpoint());
		}
	}

	@Test
	void tcpEndpointSearch() throws InterruptedException, ExecutionException {
		final InetSocketAddress server = Util.getServer();
		try (var c = TcpConnection.newTcpConnection(new InetSocketAddress(0), server)) {
			final var result = Discoverer.tcp(c).search(
					withDeviceDescription(DIB.DEVICE_INFO, DIB.SUPP_SVC_FAMILIES, DIB.AdditionalDeviceInfo,
							DIB.SecureServiceFamilies, DIB.TunnelingInfo));
			final var response = result.get().getFirst();
			assertEquals(server, response.remoteEndpoint());
		}
	}

	@Test
	@KnxnetIPSequential
	void searchInProgrammingMode() throws KNXException, InterruptedException, ExecutionException {
		final InetSocketAddress server = Util.getServer();
		final int pidProgMode = 54;
		try (var c = TcpConnection.newTcpConnection(new InetSocketAddress(0), server); var mgmt = new KNXnetIPDevMgmt(c)) {

			final var progModeOn = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, 0, 1, pidProgMode, 1, 1, new byte[] { 1 });
			mgmt.send(progModeOn, BlockingMode.WaitForCon);

			try {
				final var result = Discoverer.tcp(c).search(Srp.withProgrammingMode());
				final var response = result.get().getFirst();
				assertEquals(server, response.remoteEndpoint());
			}
			finally {
				final var progModeOff = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, 0, 1, pidProgMode, 1, 1,
						new byte[] { 0 });
				mgmt.send(progModeOff, BlockingMode.WaitForCon);
			}
		}
	}

	@Test
	void searchDeviceNotInProgrammingMode() {
		final var future = discoverer.search(Srp.withProgrammingMode());
		assertThrows(ExecutionException.class, future::get);
	}

	@Test
	void searchWithMacAddress() throws KNXException, InterruptedException, ExecutionException {
		final InetSocketAddress server = Util.getServer();
		try (var c = TcpConnection.newTcpConnection(new InetSocketAddress(0), server); var mgmt = new KNXnetIPDevMgmt(c)) {
			final byte[] macAddress = new byte[6];
			mgmt.addConnectionListener((final FrameEvent e) -> {
					final byte[] data = e.getFrame().getPayload();
					System.arraycopy(data, 0, macAddress, 0, 6);
				});
			final int pidMacAddress = 64;
			final var cemi = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, 11, 1, pidMacAddress, 1, 1);
			mgmt.send(cemi, BlockingMode.WaitForCon);

			final var result = Discoverer.tcp(c).search(Srp.withMacAddress(macAddress));
			final var response = result.get().getFirst();
			assertEquals(server, response.remoteEndpoint());
		}
	}

	@Test
	void timeout() {
		/*final var d = (DiscovererTcp) */ discoverer.timeout(Duration.ofMillis(1));
		// local knx server might be faster than 1 ms
//		assertThrows(KNXTimeoutException.class, () -> d.description());
	}

	@Test
	void timeoutTooSmall() throws KNXException, InterruptedException {
		// implementation cuts off at millis, so check that smaller timeout still succeeds
		try {
			discoverer.timeout(Duration.ofNanos(1)).description();
		}
		catch (final KNXTimeoutException ok) {}
	}
}

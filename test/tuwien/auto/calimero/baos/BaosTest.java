/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2023 B. Malinowsky

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

package tuwien.auto.calimero.baos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.baos.BaosService.Property;
import tuwien.auto.calimero.baos.ip.BaosIp;
import tuwien.auto.calimero.baos.ip.BaosLinkIp;
import tuwien.auto.calimero.knxnetip.Discoverer;
import tuwien.auto.calimero.knxnetip.Discoverer.Result;
import tuwien.auto.calimero.knxnetip.TcpConnection;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.link.LinkEvent;
import tuwien.auto.calimero.link.NetworkLinkListener;

class BaosTest {

	private static List<Result<SearchResponse>> list;

	@BeforeAll
	static void init() throws InterruptedException, ExecutionException {
		final var discoverer = new Discoverer(0, true);
		list = new ArrayList<>(discoverer.timeout(Duration.ofSeconds(2)).search().get());
		list.removeIf(r -> !r.response().getDevice().getName()
				.contains("calimero-core knx test-server"));
		if (list.isEmpty())
			fail("no KNX IP server found");
	}

	@Test
	@Disabled
	void supportsBaos() {
		final Result<SearchResponse> result = list.get(0);
		assertTrue(BaosIp.supportsBaos(result));
	}

	@Test
	void doesNotSupportsBaos() {
		final Result<SearchResponse> result = list.get(0);
		assertFalse(BaosIp.supportsBaos(result));
	}

	@Test
	void baosIpConnection() throws KNXException, InterruptedException, SocketException {
		final var serverEp = Util.localInterface().inetAddresses().filter(Inet4Address.class::isInstance).findFirst().get();
		final var objectServer = new InetSocketAddress(serverEp, 12004);

		try (var server = new BaosServer()) {
			CompletableFuture.runAsync(server);
			Thread.sleep(500);
			try (var link = BaosLinkIp.newTcpLink(TcpConnection.newTcpConnection(new InetSocketAddress(0), objectServer))) {
				final var rcv = new LinkedBlockingQueue<>();
				link.addLinkListener(new NetworkLinkListener() {
					@LinkEvent
					void objectServerEvent(final BaosService svc) {
						rcv.offer(svc);
					}
				});

				link.send(BaosService.getServerItem(Property.TimeSinceReset, 1));
				assertNotNull(rcv.poll(3, TimeUnit.SECONDS));
			}
		}
	}
}

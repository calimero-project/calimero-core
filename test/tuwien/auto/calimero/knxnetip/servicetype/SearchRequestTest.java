/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip.servicetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.KNXFormatException;

class SearchRequestTest {

	private final InetSocketAddress responseAddr = new InetSocketAddress("192.168.10.10", 3671);
	private final List<Integer> dibs = List.of(1, 2, 8, 6, 7);

	@Test
	void parseRequestWithoutDibs() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr));
		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		assertEquals(KNXnetIPHeader.SEARCH_REQ, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, packet, h.getStructLength());
		assertTrue(req.requestedDibs().isEmpty(), "no DIBs are requested");
	}

	@Test
	void parseRequestWithDibs() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr, dibs));
		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		assertEquals(KNXnetIPHeader.SearchRequest, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, packet, h.getStructLength());
		assertEquals(dibs, req.requestedDibs(), "DIB type codes mismatch");
	}

	@Test
	void parseRequestWithEmptyDibs() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr, List.of()));
		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		assertEquals(KNXnetIPHeader.SearchRequest, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, packet, h.getStructLength());
		assertEquals(List.of(), req.requestedDibs(), "DIB type codes mismatch");
	}

	@Test
	void newRequestWithoutDibs() {
		final SearchRequest req = new SearchRequest(responseAddr);
		assertTrue(req.requestedDibs().isEmpty(), "no DIBs are requested");
	}

	@Test
	void newRequestWithDibs() {
		final SearchRequest req = new SearchRequest(responseAddr, dibs);
		assertEquals(dibs, req.requestedDibs(), "DIB type codes mismatch");

		final List<Integer> shortList = List.of(1, 2);
		final SearchRequest shortReq = new SearchRequest(responseAddr, shortList);
		assertEquals(shortList, shortReq.requestedDibs(), "DIB type codes mismatch");
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2021 B. Malinowsky

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

package io.calimero.knxnetip.servicetype;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.knxnetip.util.ServiceFamiliesDIB.ServiceFamily;
import io.calimero.knxnetip.util.Srp;

class SearchRequestTest {

	private final InetSocketAddress responseAddr = new InetSocketAddress("192.168.10.10", 3671);
	final byte[] dibBytes = {0x01, 0x02, 0x08, 0x06, 0x07, 0x00};
	private final Srp dib = new Srp(Srp.Type.RequestDibs, false, (byte) 0x01, (byte) 0x02, (byte) 0x08,
		(byte) 0x06, (byte) 0x07);

	@Test
	void parseRequestWithoutSrps() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr));
		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		assertEquals(KNXnetIPHeader.SEARCH_REQ, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, packet, h.getStructLength());
		assertTrue(req.searchParameters().isEmpty(), "no SRPs are requested");
	}

	@Test
	void parseRequestWithSrps() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr, dib));
		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		assertEquals(KNXnetIPHeader.SearchRequest, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, packet, h.getStructLength());
		assertArrayEquals(dibBytes, req.searchParameters().get(0).data(), "SRP mismatch");
	}

	@Test
	void parseRequestWithSrpAtNonZeroOffset() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr, dib));

		final int offset = 10;
		final byte[] data = ByteBuffer.allocate(50).put(new byte[offset]).put(packet).array();
		final KNXnetIPHeader h = new KNXnetIPHeader(data, offset);
		assertEquals(KNXnetIPHeader.SearchRequest, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, data, offset + h.getStructLength());
		assertArrayEquals(dibBytes, req.searchParameters().get(0).data(), "SRP mismatch");
	}

	@Test
	void parseRequestWithEmptySrps() throws KNXFormatException {
		final byte[] packet = PacketHelper.toPacket(new SearchRequest(responseAddr, new Srp[0]));
		final KNXnetIPHeader h = new KNXnetIPHeader(packet, 0);
		assertEquals(KNXnetIPHeader.SearchRequest, h.getServiceType());

		final SearchRequest req = SearchRequest.from(h, packet, h.getStructLength());
		assertEquals(List.of(), req.searchParameters(), "SRP count mismatch");
	}

	@Test
	void newRequestWithoutSrps() {
		final SearchRequest req = new SearchRequest(responseAddr);
		assertTrue(req.searchParameters().isEmpty(), "no SRPs are requested");
	}

	@Test
	void newRequestWithSrps() {
		final SearchRequest req = new SearchRequest(responseAddr, dib);
		assertArrayEquals(dibBytes, req.searchParameters().get(0).data(), "SRP mismatch");

		final byte[] dib = {0x01, 0x02};
		final SearchRequest shortReq = new SearchRequest(responseAddr, new Srp(Srp.Type.RequestDibs, false, dib));
		assertArrayEquals(dib, shortReq.searchParameters().get(0).data(), "SRP mismatch");
	}

	@Test
	void newRequestWithMultiSrp() {
		final byte[] pmBytes = {0x02, (byte) 0x81};
		final Srp pm = new Srp(Srp.Type.SelectByProgrammingMode, true);

		final byte[] pm2Bytes = {0x02, 0x01};
		final Srp pm2 = new Srp(Srp.Type.SelectByProgrammingMode, false);

		final byte[] macBytes = {0x08, (byte) 0x82, (byte) 0xbc, (byte) 0xae, (byte) 0xc5, (byte) 0x66, (byte) 0x90,
			(byte) 0xf9};
		final Srp mac = new Srp(Srp.Type.SelectByMacAddress, true, (byte) 0xbc, (byte) 0xae, (byte) 0xc5,
			(byte) 0x66, (byte) 0x90, (byte) 0xf9);

		final byte[] sbsBytes = {0x04, (byte) 0x83, 0x09, 0x01};
		final Srp sbs = new Srp(Srp.Type.SelectByService, true, (byte) ServiceFamily.Security.id(), (byte) 0x01);

		final SearchRequest req = new SearchRequest(responseAddr, pm, pm2, mac, sbs, dib);

		final List<Srp> srps = req.searchParameters();
		assertEquals(5, srps.size(), "SRP count mismatch, expected 5");

		assertArrayEquals(pmBytes, srps.get(0).toByteArray(), "SRP mismatch");
		assertArrayEquals(pm2Bytes, srps.get(1).toByteArray(), "SRP mismatch");
		assertArrayEquals(macBytes, srps.get(2).toByteArray(), "SRP mismatch");
		assertArrayEquals(sbsBytes, srps.get(3).toByteArray(), "SRP mismatch");
		assertArrayEquals(dibBytes, srps.get(4).data(), "SRP mismatch");
	}
}

/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2019, 2026 B. Malinowsky

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

package io.calimero.knxnetip.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.calimero.KNXIllegalArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;

class HPAITest {
	private InetAddress iaddr;

	@BeforeEach
	void setUp() throws UnknownHostException {
		iaddr = InetAddress.getByName("192.168.10.1");
	}

	@Test
	void validTcpHpai() throws KNXFormatException {
		HPAI.from(HPAI.Tcp.toByteArray(), 0);
	}

	@Test
	void tcpHpaiWithNonZeroPort() throws UnknownHostException {
		final var data = HPAI.Tcp.toByteArray();
		data[6] = 1;
		assertThrows(KNXFormatException.class, () -> HPAI.from(data, 0));
		data[6] = 0;
		data[7] = 1;
		assertThrows(KNXFormatException.class, () -> HPAI.from(data, 0));
	}

	@Test
	void tcpHpaiWithInvalidIpAddress() throws UnknownHostException {
		final var data = HPAI.Tcp.toByteArray();
		System.arraycopy(iaddr.getAddress(), 0, data, 2, iaddr.getAddress().length);
		assertThrows(KNXFormatException.class, () -> HPAI.from(data, 0));
	}

	@Test
	void tcpIsRouteBackHpai() {
		assertTrue(HPAI.Tcp.isRouteBack());
	}

	@Test
	void natIsRouteBackHpai() throws UnknownHostException {
		assertTrue(HPAI.Nat.isRouteBack());

		final var anyLocal = InetAddress.getByAddress(new byte[4]);
		assertTrue(new HPAI(HPAI.IPV4_UDP, new InetSocketAddress(anyLocal, 0)).isRouteBack());
	}

	@Test
	void rejectNatWithNonZeroIpAndZeroPort() {
		assertThrows(KNXIllegalArgumentException.class, () -> new HPAI(HPAI.IPV4_UDP, iaddr, 0));
	}

	@Test
	void rejectNatWithZeroIpAndNonZeroPort() {
		assertThrows(KNXIllegalArgumentException.class,
				() -> new HPAI(HPAI.IPV4_UDP, InetAddress.getByAddress(new byte[4]), 0xffff));
	}

	@Test
	void parsingNatAllowsNonZeroIpAndZeroPort() throws KNXFormatException {
		final byte[] hpai = HPAI.Nat.toByteArray();
		System.arraycopy(iaddr.getAddress(), 0, hpai, 2, iaddr.getAddress().length);
		assertTrue(HPAI.from(hpai, 0).nat());
	}

	@Test
	void parsingNatAllowsZeroIpAndNonZeroPort() throws KNXFormatException {
		final byte[] hpai = HPAI.Nat.toByteArray();
		hpai[7] = 1;
		assertTrue(HPAI.from(hpai, 0).nat());
	}

	@Test
	void nonRouteBackHpai() {
		assertFalse(new HPAI(HPAI.IPV4_UDP, iaddr, 1234).isRouteBack());
	}
}

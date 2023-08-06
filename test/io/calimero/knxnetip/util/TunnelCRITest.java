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

package io.calimero.knxnetip.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.calimero.knxnetip.KNXnetIPTunnel.TUNNEL_CONNECTION;

import org.junit.jupiter.api.Test;

import io.calimero.IndividualAddress;
import io.calimero.KNXFormatException;
import io.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;

class TunnelCRITest {
	private final TunnelingLayer linklayer = TunnelingLayer.LinkLayer;
	private final IndividualAddress ia = new IndividualAddress(1, 2, 3);

	@Test
	void defaultCri() {
		final TunnelCRI cri = new TunnelCRI(linklayer);
		assertEquals(4, cri.getStructLength());
		assertArrayEquals(cri.toByteArray(), new byte[] { 4, TUNNEL_CONNECTION, (byte) linklayer.getCode(), 0 });

		assertFalse(cri.tunnelingAddress().isPresent());
	}

	@Test
	void extendedCri() {
		final TunnelCRI cri = new TunnelCRI(linklayer, ia);
		assertEquals(6, cri.getStructLength());
		final byte[] ba = ia.toByteArray();
		assertArrayEquals(cri.toByteArray(), new byte[] { 6, TUNNEL_CONNECTION, (byte) linklayer.getCode(), 0, ba[0], ba[1] });

		assertEquals(ia, cri.tunnelingAddress().get());
	}

	@Test
	void parseExtendedCri() throws KNXFormatException {
		final byte[] data = new byte[] { (byte) 0xff, (byte) 0xff, 6, TUNNEL_CONNECTION, (byte) linklayer.getCode(), 0, 2, 5, (byte) 0xff };
		final TunnelCRI cri = new TunnelCRI(data, 2);
		assertEquals(6, cri.getStructLength());
		assertEquals(new IndividualAddress(0, 2, 5), cri.tunnelingAddress().get());
	}

	@Test
	void criWithInvalidLength() {
		final byte[] data = new byte[] { 3, TUNNEL_CONNECTION, (byte) linklayer.getCode(), 0, 2, 5, (byte) 0xff };

		assertThrows(KNXFormatException.class, () -> new TunnelCRI(data, 2));
		data[0] = 5;
		assertThrows(KNXFormatException.class, () -> new TunnelCRI(data, 2));
		data[0] = 7;
		assertThrows(KNXFormatException.class, () -> new TunnelCRI(data, 2));
	}
}

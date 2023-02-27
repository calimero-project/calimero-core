/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.calimero.IndividualAddress;
import io.calimero.KNXFormatException;
import io.calimero.knxnetip.util.TunnelingDib.SlotStatus;

class TunnelingDibTest {
	private final EnumSet<SlotStatus> status = EnumSet.allOf(SlotStatus.class);
	private final Map<IndividualAddress, EnumSet<SlotStatus>> slots = Map.of(new IndividualAddress(5), status);

	@Test
	void status() {
		assertEquals(1, SlotStatus.Free.value());
		assertEquals(2, SlotStatus.Authorized.value());
		assertEquals(4, SlotStatus.Usable.value());
	}

	@Test
	void statusOfValue() {
		assertEquals(SlotStatus.Free, SlotStatus.of(1));
		assertEquals(SlotStatus.Authorized, SlotStatus.of(2));
		assertEquals(SlotStatus.Usable, SlotStatus.of(4));
	}

	@Test
	void deepCopy() {
		var dib = new TunnelingDib(slots);
		EnumSet<SlotStatus> copy = dib.slots().values().iterator().next();
		assertNotSame(status, copy);
		assertEquals(status, copy);
	}

	@Test
	void immutableSlots() {
		var dib = new TunnelingDib(slots);
		EnumSet<SlotStatus> copy = dib.slots().values().iterator().next();

		copy.removeAll(EnumSet.allOf(SlotStatus.class));
		assertTrue(copy.isEmpty());
		assertNotEquals(copy, dib.slots().values().iterator().next());
	}

	@Test
	void emptyStatus() {
		EnumSet<SlotStatus> clear = EnumSet.noneOf(SlotStatus.class);
		var dib = new TunnelingDib(Map.of(new IndividualAddress(1, 1, 4), clear));
		assertEquals(clear, dib.slots().values().iterator().next());
	}

	@Test
	void tunnelingSlots() {
		var three = Map.of(
				new IndividualAddress(1, 1, 1), EnumSet.of(SlotStatus.Free),
				new IndividualAddress(1, 1, 2), EnumSet.of(SlotStatus.Authorized),
				new IndividualAddress(1, 1, 3), EnumSet.of(SlotStatus.Free));
		var dib = new TunnelingDib(three);
		assertEquals(three, dib.slots());
	}

	@Test
	void fromData() throws KNXFormatException {
		var dib = new TunnelingDib(slots);

		byte[] data = dib.toByteArray();
		assertEquals(dib, new TunnelingDib(data, 0, data.length));
	}

	@Test
	void maxApduLength() {
		assertEquals(44, new TunnelingDib(44, slots).maxApduLength());
	}
}

/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2025, 2025 B. Malinowsky

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

package io.calimero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SerialNumberTest {
	@Test
	void of() {
		final long l = 0xffff_eeeeeeeeL;
		final var sn = SerialNumber.of(l);
		assertEquals(l, sn.number());
	}

	@Test
	void ofNumberTooBig() {
		final long tooBig = 0x01ffff_eeeeeeeeL;
		final long l = 0xffff_eeeeeeeeL;
		final var sn = SerialNumber.of(tooBig);
		assertEquals(l, sn.number());
	}

	@Test
	void ofNegative() {
		Assertions.assertThrows(KNXIllegalArgumentException.class, () -> SerialNumber.of(-1));
		Assertions.assertThrows(KNXIllegalArgumentException.class, () -> SerialNumber.of(Long.MIN_VALUE));
	}

	@Test
	void fromWithInvalidLength() {
		Assertions.assertThrows(KNXIllegalArgumentException.class,
				() -> SerialNumber.from(new byte[] { 0x10, 0x10, 0x10, 0x10, 0x10 }));
	}

	@Test
	void testEquals() {
		assertTrue(SerialNumber.of(100).equals(SerialNumber.of(100)));
		assertTrue(SerialNumber.Zero.equals(SerialNumber.of(0)));
	}

	@Test
	void testNotEquals() {
		assertFalse(SerialNumber.of(1).equals(SerialNumber.of(2)));
		assertFalse(SerialNumber.Zero.equals(SerialNumber.of(100)));
	}

	@Test
	void testToString() {
		assertEquals("0000:00000000", SerialNumber.Zero.toString());
		assertEquals("1020:30405060", SerialNumber.of(0x102030405060L).toString());
	}
}

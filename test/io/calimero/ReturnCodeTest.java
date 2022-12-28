/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ReturnCodeTest {

	@Test
	void positiveReturnCode() {
		ReturnCode.of(0);
		ReturnCode.of(1);
	}

	@Test
	void negativeReturnCode() {
		for (int i = 0xf1; i < 0xff; i++)
			ReturnCode.of(i);
	}

	@Test
	void unknownReturnCode() {
		for (int i = 0; i < 0xff; i++)
			if (!ReturnCode.values.containsKey(i))
				assertTrue(ReturnCode.of(i).description().startsWith("unknown"));
	}

	@ParameterizedTest
	@MethodSource("values")
	void friendlyCodeName(final ReturnCode c) {
		// checks non-empty and formatting
		if (c != ReturnCode.Success && c != ReturnCode.Error)
			assertTrue(c.friendly().contains(" ") || c.friendly().startsWith("0x"), c.friendly());
		assertTrue(c.friendly().length() > 0);
	}

	@ParameterizedTest
	@MethodSource("values")
	void description(final ReturnCode c) {
		assertTrue(c.description().length() > 0);
		assertTrue(c.description().contains(" "));
		if (c.friendly().startsWith("0x"))
			assertEquals("unknown return code", c.description());
	}

	static ReturnCode[] values() {
		return ReturnCode.values.values().toArray(new ReturnCode[0]);
	}
}

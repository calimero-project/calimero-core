/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.dptxlator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.calimero.KNXException;
import io.calimero.dptxlator.PropertyTypes.DPTID;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTypesTest {
	@Test
	void getAllPropertyTypes() {
		final Map<Integer, DPTID> m = PropertyTypes.getAllPropertyTypes();
		m.put(1000, new DPTID(1000, "1000.001"));
		m.remove(1000);
		for (final Integer type : m.keySet()) {
			assertTrue(type > 0);
		}
		for (final DPTID type : m.values()) {
			assertTrue(type.getMainNumber() > 0);
			assertTrue(type.getDPT().length() >= 5);
		}
	}

	@Test
	void createTranslatorInt() throws KNXException {
		final DPTXlator t = PropertyTypes.createTranslator(PropertyTypes.PDT_BINARY_INFORMATION);
		assertTrue(t instanceof DPTXlatorBoolean);
		try {
			PropertyTypes.createTranslator(PropertyTypes.PDT_SHORT_CHAR_BLOCK);
			fail("we really have such a translator?");
		}
		catch (final KNXException e) {
		}
		try {
			PropertyTypes.createTranslator(-1);
			fail("invalid PDT");
		}
		catch (final KNXException e) {
		}
	}

	@Test
	void hasTranslator() {
		assertTrue(PropertyTypes.hasTranslator(PropertyTypes.PDT_BINARY_INFORMATION));
		assertFalse(PropertyTypes.hasTranslator(1000));
		assertFalse(PropertyTypes.hasTranslator(PropertyTypes.PDT_CONTROL));
	}

	@Test
	void getValues() throws KNXException {
		final String[] values = PropertyTypes.getValues(PropertyTypes.PDT_UNSIGNED_INT,
				new byte[] { 0, 1, 0, 2, 3, 0 });
		assertEquals(3, values.length);
		assertEquals(1, Integer.parseInt(values[0]));
		assertEquals(2, Integer.parseInt(values[1]));
		assertEquals(0x300, Integer.parseInt(values[2]));
	}

	@Test
	void createTranslatorIntByteArray() throws KNXException {
		final DPTXlator t = PropertyTypes.createTranslator(PropertyTypes.PDT_BINARY_INFORMATION,
				new byte[] { 1, 0, 1 });
		assertEquals(3, t.getItems());
		assertArrayEquals(new byte[]{1, 0, 1}, t.getData());
	}
}

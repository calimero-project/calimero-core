/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.dptxlator.TranslatorTypes.MainType;


@Isolated("clears map obtained by getAllMainTypes")
class TranslatorTypesTest
{
	private final MainType[] types =
		TranslatorTypes.getAllMainTypes().values().toArray(new MainType[0]);


	@Test
	void getMainType() throws KNXException
	{
		for (int i = 0; i < 100; ++i) {
			if (TranslatorTypes.getMainType(i) == null
				&& TranslatorTypes.getAllMainTypes().containsKey(i))
				fail("not found but in type list");
		}

		for (MainType type : types) {
			final MainType t = TranslatorTypes.getMainType(type.mainNumber());
			assertEquals(t.mainNumber(), type.mainNumber());
			t.createTranslator(t.getSubTypes().values().iterator().next().getID());
		}
	}

	@Test
	void getAllMainTypes() throws KNXException
	{
		assertTrue(TranslatorTypes.getAllMainTypes().size() > 7);
		final Map<Integer, MainType> m = TranslatorTypes.getAllMainTypes();
		final Map<Integer, MainType> copy = new HashMap<>(m);
		m.clear();
		assertTrue(TranslatorTypes.getAllMainTypes().isEmpty());
		DPTXlator t = null;
		try {
			t = TranslatorTypes.createTranslator(TranslatorTypes.TYPE_BOOLEAN,
				DPTXlatorBoolean.DPT_BOOL.getID());
			fail("map is empty - should fail");
		}
		catch (final KNXException e) {}
		assertNull(t);

		m.putAll(copy);
		assertFalse(TranslatorTypes.getAllMainTypes().isEmpty());
		try {
			t = TranslatorTypes.createTranslator(TranslatorTypes.TYPE_BOOLEAN,
				DPTXlatorBoolean.DPT_BOOL.getID());
		}
		catch (final KNXException e) {
			fail("map is filled - should not fail");
		}
		assertNotNull(t);

		//newMainTypeFail(2000, Object.class);
		newMainTypeFail(2000, DPTXlator.class);
		final MainType mt = new MainType(2000, DPTXlatorBoolean.class, "DPTXlatorBoolean.class");
		TranslatorTypes.getAllMainTypes().put(2000, mt);
		assertEquals(TranslatorTypes.getMainType(2000).createTranslator(
			DPTXlatorBoolean.DPT_ENABLE).getClass(), DPTXlatorBoolean.class);
	}

	@Test
	void ofBitSize()
	{
		List<MainType> ofBitSize = TranslatorTypes.ofBitSize(-1);
		assertEquals(0, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(0);
		assertEquals(0, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(1);
		assertEquals(1, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(2);
		assertEquals(1, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(4);
		assertEquals(1, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(32);
		assertEquals(4, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(64);
		assertEquals(3, ofBitSize.size());
		ofBitSize = TranslatorTypes.ofBitSize(123);
		assertEquals(0, ofBitSize.size());
	}

	private void newMainTypeFail(final int mainNo, final Class<? extends DPTXlator> cl)
	{
		try {
			new MainType(mainNo, cl, "faulty main type");
			fail("main type illegal arg - should fail");
		}
		catch (final KNXIllegalArgumentException e) {}

	}

	@Test
	void createTranslator() throws KNXException
	{
		// with main number
		for (MainType mainType : types) {
			final int main = mainType.mainNumber();
			final String dptID = TranslatorTypes.getMainType(main).getSubTypes()
					.values().iterator().next().getID();
			TranslatorTypes.createTranslator(main, dptID);
		}

		// without main number
		for (MainType type : types) {
			final int main = type.mainNumber();
			final String dptID = TranslatorTypes.getMainType(main).getSubTypes()
					.values().iterator().next().getID();
			TranslatorTypes.createTranslator(0, dptID);
		}

		try {
			TranslatorTypes.createTranslator(0, "123");
			fail("not supported dptID");
		}
		catch (final Exception e) { }
		try {
			TranslatorTypes.createTranslator(0, ".12");
			fail("not supported dptID");
		}
		catch (final Exception e) { }

		TranslatorTypes.createTranslator(DPTXlatorBoolean.DPT_ACK);
		TranslatorTypes.createTranslator(DPTXlator2ByteFloat.DPT_HUMIDITY);
		try {
			TranslatorTypes.createTranslator(new DPT("1000.1000", "", "-1", "1"));
			fail("non-existent DPT");
		}
		catch (final KNXException e) {}
	}

	@Test
	void createTranslatorWithData() throws KNXIllegalArgumentException, KNXException
	{
		DPTXlator t = TranslatorTypes.createTranslator("9.001");
		assertEquals(0.0, t.getNumericValue());

		t = TranslatorTypes.createTranslator("1.001");
		assertEquals("off", t.getValue());

		t = TranslatorTypes.createTranslator("1.001", (byte) 1);
		assertEquals(1.0, t.getNumericValue());
		assertEquals("on", t.getValue());

		t = TranslatorTypes.createTranslator("9.001", (byte) 0xc, (byte) 0xe2);
		assertEquals(25.0, t.getNumericValue());

		t = TranslatorTypes.createTranslator("9.001", (byte) 0xc, (byte) 0xe2, (byte) 0xc, (byte) 0xf2);
		t.setAppendUnit(false);
		assertEquals(2, t.getItems());
		assertTrue(Arrays.deepEquals(new String[] { "25.0", "25.32" }, t.getAllValues()));
	}

	@Test
	void createTranslatorWithMainSub() throws KNXException
	{
		final DPTXlator t = TranslatorTypes.createTranslator(9, 1);
		assertEquals(DPTXlator2ByteFloat.DPT_TEMPERATURE, t.getType());
		assertTrue(t.getValue().endsWith("C"));

		DPTXlator t2 = TranslatorTypes.createTranslator(9, 0);
		assertTrue(t2.getType().getID().startsWith("9"));

		final byte[] data = new byte[] { (byte) 0xc, (byte) 0xe2 };
		t2 = TranslatorTypes.createTranslator(9, 0, data);
		assertTrue(t2.getType().getID().startsWith("9"));
		assertArrayEquals(data, t2.getData());

		try {
			TranslatorTypes.createTranslator(9, -1);
		}
		catch (final KNXIllegalArgumentException expected) {}
		try {
			TranslatorTypes.createTranslator(0, 1);
		}
		catch (final KNXException expected) {}
	}
}

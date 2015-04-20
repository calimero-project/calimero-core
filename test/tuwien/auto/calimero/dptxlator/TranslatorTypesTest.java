/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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

package tuwien.auto.calimero.dptxlator;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.dptxlator.TranslatorTypes.MainType;

/**
 * @author B. Malinowsky
 */
public class TranslatorTypesTest extends TestCase
{
	private final MainType[] types =
		TranslatorTypes.getAllMainTypes().values().toArray(new MainType[0]);

	/**
	 * @param name name for test case
	 */
	public TranslatorTypesTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.TranslatorTypes#getMainType(int)}.
	 *
	 * @throws KNXException
	 */
	public final void testGetMainType() throws KNXException
	{
		for (int i = 0; i < 100; ++i) {
			if (TranslatorTypes.getMainType(i) == null
				&& TranslatorTypes.getAllMainTypes().containsKey(new Integer(i)))
				fail("not found but in type list");
		}

		for (int i = 0; i < types.length; ++i) {
			final MainType t = TranslatorTypes.getMainType(types[i].getMainNumber());
			assertEquals(t.getMainNumber(), types[i].getMainNumber());
			t.createTranslator(t.getSubTypes().values().iterator().next()
				.getID());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.TranslatorTypes#getAllMainTypes()}.
	 *
	 * @throws KNXException
	 */
	public final void testGetAllMainTypes() throws KNXException
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
		TranslatorTypes.getAllMainTypes().put(new Integer(2000), mt);
		assertEquals(TranslatorTypes.getMainType(2000).createTranslator(
			DPTXlatorBoolean.DPT_ENABLE).getClass(), DPTXlatorBoolean.class);
	}

	private void newMainTypeFail(final int mainNo, final Class<? extends DPTXlator> cl)
	{
		try {
			new MainType(mainNo, cl, "faulty main type");
			fail("main type illegal arg - should fail");
		}
		catch (final KNXIllegalArgumentException e) {}

	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.TranslatorTypes#createTranslator(int, java.lang.String)}.
	 *
	 * @throws KNXException
	 */
	public final void testCreateTranslator() throws KNXException
	{
		// with main number
		for (int i = 0; i < types.length; i++) {
			final int main = types[i].getMainNumber();
			final String dptID = TranslatorTypes.getMainType(main).getSubTypes()
				.values().iterator().next().getID();
			TranslatorTypes.createTranslator(main, dptID);
		}

		// without main number
		for (int i = 0; i < types.length; i++) {
			final int main = types[i].getMainNumber();
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
			fail("not existant DPT");
		}
		catch (final KNXException e) {}
	}
}

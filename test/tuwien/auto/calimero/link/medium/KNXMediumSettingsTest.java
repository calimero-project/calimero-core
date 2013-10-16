/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2013 B. Malinowsky

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
*/

package tuwien.auto.calimero.link.medium;

import junit.framework.TestCase;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * @author B. Malinowsky
 */
public class KNXMediumSettingsTest extends TestCase
{
	/**
	 * @param name
	 */
	public KNXMediumSettingsTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.link.medium.KNXMediumSettings
	 * #create(int, tuwien.auto.calimero.IndividualAddress)}.
	 */
	public void testCreate()
	{
		testCreate(KNXMediumSettings.MEDIUM_TP0, TPSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_TP1, TPSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_PL110, PLSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_PL132, PLSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_RF, RFSettings.class);
		
		try {
			testCreate(0, RFSettings.class);
			fail("invalid medium type");
		}
		catch (final KNXIllegalArgumentException e) {
			// we're fine
		}
		try {
			testCreate(43, RFSettings.class);
			fail("invalid medium type");
		}
		catch (final KNXIllegalArgumentException e) {
			// we're fine
		}
	}

	private void testCreate(final int medium, final Class type)
	{
		final KNXMediumSettings settings = KNXMediumSettings.create(medium, null);
		assertEquals(settings.getClass(), type);
		assertEquals(medium, settings.getMedium());
	}
	
	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.link.medium.KNXMediumSettings#getMedium(java.lang.String)}.
	 */
	public void testGetMediumString()
	{
		assertEquals(KNXMediumSettings.MEDIUM_TP0, KNXMediumSettings.getMedium("tp0"));
		assertEquals(KNXMediumSettings.MEDIUM_TP1, KNXMediumSettings.getMedium("TP1"));
		assertEquals(KNXMediumSettings.MEDIUM_PL132, KNXMediumSettings.getMedium("p132"));
		assertEquals(KNXMediumSettings.MEDIUM_PL110, KNXMediumSettings.getMedium("p110"));
		assertEquals(KNXMediumSettings.MEDIUM_PL132, KNXMediumSettings.getMedium("pL132"));
		assertEquals(KNXMediumSettings.MEDIUM_PL110, KNXMediumSettings.getMedium("PL110"));
		assertEquals(KNXMediumSettings.MEDIUM_RF, KNXMediumSettings.getMedium("Rf"));
		try {
			KNXMediumSettings.getMedium("Re");
			fail("unknown medium type name");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			KNXMediumSettings.getMedium("");
			fail("no medium type name");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			KNXMediumSettings.getMedium(null);
			fail("null medium type name");
		}
		catch (final KNXIllegalArgumentException e) {}
	}
}

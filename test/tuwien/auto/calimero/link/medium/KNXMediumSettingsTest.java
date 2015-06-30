/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2013, 2015 B. Malinowsky

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

package tuwien.auto.calimero.link.medium;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXIllegalArgumentException;

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
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
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
		testCreate(KNXMediumSettings.MEDIUM_TP1, TPSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_PL110, PLSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_RF, RFSettings.class);
		testCreate(KNXMediumSettings.MEDIUM_KNXIP, KnxIPSettings.class);

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

	private void testCreate(final int medium, final Class<? extends KNXMediumSettings> type)
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
		assertEquals(KNXMediumSettings.MEDIUM_TP1, KNXMediumSettings.getMedium("TP1"));
		assertEquals(KNXMediumSettings.MEDIUM_PL110, KNXMediumSettings.getMedium("p110"));
		assertEquals(KNXMediumSettings.MEDIUM_PL110, KNXMediumSettings.getMedium("PL110"));
		assertEquals(KNXMediumSettings.MEDIUM_RF, KNXMediumSettings.getMedium("Rf"));
		assertEquals(KNXMediumSettings.MEDIUM_KNXIP, KNXMediumSettings.getMedium("knxip"));
		assertEquals(KNXMediumSettings.MEDIUM_KNXIP, KNXMediumSettings.getMedium("knx ip"));
		try {
			KNXMediumSettings.getMedium("Re");
			fail("unknown medium type name");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			KNXMediumSettings.getMedium("KNX  IP");
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

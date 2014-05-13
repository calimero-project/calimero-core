/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2014 B. Malinowsky

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

package tuwien.auto.calimero;

import junit.framework.TestCase;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * @author B. Malinowsky
 */
public class IndividualAddressTest extends TestCase
{

	/**
	 * @param name
	 */
	public IndividualAddressTest(final String name)
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
	 * Test method for
	 * {@link tuwien.auto.calimero.IndividualAddress#IndividualAddress(java.lang.String)}.
	 * 
	 * @throws KNXFormatException
	 */
	public void testIndividualAddressString() throws KNXFormatException
	{
		new IndividualAddress("1.2.3");
		final IndividualAddress a = new IndividualAddress("4611");
		assertEquals(4611, a.getRawAddress());
		final IndividualAddress a2 = new IndividualAddress("0");
		assertEquals(0, a2.getRawAddress());
		try {
			new IndividualAddress("-100"); // out of range
		}
		catch (final KNXFormatException e) {}
		try {
			new IndividualAddress("4.5"); // wrong format
		}
		catch (final KNXFormatException e) {}
		try {
			new IndividualAddress("4611>"); // not a number
		}
		catch (final KNXFormatException e) {}
		try {
			new IndividualAddress("4.6.1.1"); // wrong format
		}
		catch (final KNXFormatException e) {}
	}
}

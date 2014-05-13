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
public class KNXAddressTest extends TestCase
{

	/**
	 * @param name
	 */
	public KNXAddressTest(final String name)
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
	 * Test method for {@link tuwien.auto.calimero.KNXAddress#create(java.lang.String)}.
	 * 
	 * @throws KNXFormatException
	 */
	public void testCreateString() throws KNXFormatException
	{
		KNXAddress.create("1/2/3");
		KNXAddress.create("3.6.9");
		try {
			KNXAddress.create("4611");
			fail();
		}
		catch (final KNXFormatException e) {
			// fine, we can't create from raw addresses in base class
		}
	}
}

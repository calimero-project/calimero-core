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

package tuwien.auto.calimero.buffer;

import junit.framework.TestCase;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * @author B. Malinowsky
 */
public class LDataObjectTest extends TestCase
{
	CEMILData frame;

	/**
	 * @param name name of test case
	 */
	public LDataObjectTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		frame =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1.1.1"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
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
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObject#LDataObject
	 * (tuwien.auto.calimero.cemi.CEMILData)}.
	 */
	public void testLDataObjectCEMILData()
	{
		final LDataObject o = new LDataObject(frame);
		assertEquals(frame, o.getFrame());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObject#getFrame()}.
	 * 
	 * @throws KNXFormatException
	 */
	public void testGetFrame() throws KNXFormatException
	{
		final LDataObject o = new LDataObject(frame);
		assertEquals(frame, o.getFrame());
		final CEMILData frame2 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1.1.1"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
		o.setFrame(frame2);
		assertEquals(frame2, o.getFrame());
	}

	/**
	 * Test method for checking created LDataObject key.
	 */
	public void testLDataObjectKey()
	{
		final LDataObject o = new LDataObject(frame);
		assertEquals(o.getKey(), frame.getDestination());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.LDataObject#setFrame(CEMILData)}.
	 * 
	 * @throws KNXFormatException
	 */
	public void testSet() throws KNXFormatException
	{
		final LDataObject o = new LDataObject(frame);
		final CEMILData frame2 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1.1.2"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
		boolean failed = false;
		try {
			o.setFrame(frame2);
		}
		catch (final KNXIllegalArgumentException e) {
			failed = true;
		}
		assertTrue(failed);

		final CEMILData frame3 =
			new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1.1.1"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
		failed = false;
		try {
			o.setFrame(frame3);
		}
		catch (final KNXIllegalArgumentException e) {
			failed = true;
		}
		assertFalse(failed);
	}

}

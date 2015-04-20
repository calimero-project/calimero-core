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

package tuwien.auto.calimero.cemi;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILDataEx.AddInfo;

/**
 * @author B. Malinowsky
 */
public class CEMILDataExTest extends TestCase
{
	private CEMILDataEx f;
	private final IndividualAddress src = new IndividualAddress(1, 2, 3);
	private final GroupAddress dst = new GroupAddress(2, 4, 4);
	private final byte[] tpdu = new byte[] { 0, (byte) 129 };
	private final byte[] plinfo = new byte[] { 0x10, 0x20 };
	private final byte[] extts = new byte[] { (byte) 0x80, 0x2, 0x3, 0x4 };

	/**
	 * @param name name of test case
	 */
	public CEMILDataExTest(final String name)
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
		f = new CEMILDataEx(CEMILData.MC_LDATA_REQ, src, dst, tpdu, Priority.LOW);
		f.addAdditionalInfo(CEMILDataEx.ADDINFO_PLMEDIUM, plinfo);
		f.addAdditionalInfo(CEMILDataEx.ADDINFO_TIMESTAMP_EXT, extts);
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
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#getStructLength()}.
	 */
	public final void testGetStructLength()
	{
		assertEquals(11 + 2 + 2 + 2 + 4, f.getStructLength());
		f.removeAdditionalInfo(CEMILDataEx.ADDINFO_PLMEDIUM);
		f.removeAdditionalInfo(CEMILDataEx.ADDINFO_TIMESTAMP_EXT);
		assertEquals(11, f.getStructLength());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#toString()}.
	 */
	public final void testToString()
	{
		System.out.println(f.toString());
		System.out.println(new CEMILDataEx(CEMILData.MC_LDATA_REQ, src, dst, tpdu,
			Priority.LOW).toString());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#setHopCount(int)}.
	 */
	public final void testSetHopCount()
	{
		assertEquals(6, f.getHopCount());
		f.setHopCount(2);
		assertEquals(2, f.getHopCount());
		f.setHopCount(7);
		assertEquals(7, f.getHopCount());
		try {
			f.setHopCount(8);
			fail("out of range");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#setPriority
	 * (tuwien.auto.calimero.Priority)}.
	 */
	public final void testSetPriority()
	{
		assertEquals(Priority.LOW, f.getPriority());
		f.setPriority(Priority.SYSTEM);
		assertEquals(Priority.SYSTEM, f.getPriority());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#setBroadcast(boolean)}.
	 */
	public final void testSetBroadcast()
	{
		assertTrue(f.isDomainBroadcast());
		f.setBroadcast(false);
		assertFalse(f.isDomainBroadcast());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.cemi.CEMILDataEx#addAdditionalInfo(int, byte[])}.
	 */
	public final void testAddAdditionalInfo()
	{
		try {
			f.addAdditionalInfo(CEMILDataEx.ADDINFO_PLMEDIUM, new byte[] { 1 });
			fail("wrong length");
		}
		catch (final KNXIllegalArgumentException e) {}
		final byte[] getPL = f.getAdditionalInfo(CEMILDataEx.ADDINFO_PLMEDIUM);
		assertTrue(Arrays.equals(plinfo, getPL));
		f.addAdditionalInfo(CEMILDataEx.ADDINFO_TIMESTAMP_EXT, new byte[] { 4, 4, 4, 4 });
		final byte[] getTS = f.getAdditionalInfo(CEMILDataEx.ADDINFO_TIMESTAMP_EXT);
		assertTrue(Arrays.equals(new byte[] { 4, 4, 4, 4 }, getTS));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#getAdditionalInfo()}.
	 */
	public final void testGetAdditionalInfo()
	{
		final List<AddInfo> l = f.getAdditionalInfo();
		assertEquals(2, l.size());
		assertEquals(CEMILDataEx.ADDINFO_PLMEDIUM, l.get(0)
			.getType());
		assertEquals(2, l.get(0).getInfo().length);
		assertEquals(CEMILDataEx.ADDINFO_TIMESTAMP_EXT, l.get(1)
			.getType());
		assertEquals(4, l.get(1).getInfo().length);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.cemi.CEMILDataEx#getAdditionalInfo(int)}.
	 */
	public final void testGetAdditionalInfoInt()
	{
		assertNull(f.getAdditionalInfo(CEMILDataEx.ADDINFO_RFMEDIUM));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#isExtendedFrame()}.
	 */
	public final void testIsExtendedFrame()
	{
		assertFalse(f.isExtendedFrame());
		final CEMILDataEx f2 =
			new CEMILDataEx(CEMILData.MC_LDATA_REQ, src, dst, new byte[] { 0, 1, 2, 3, 4,
				5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, Priority.LOW);
		assertTrue(f2.isExtendedFrame());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.cemi.CEMILDataEx#removeAdditionalInfo(int)}.
	 */
	public final void testRemoveAdditionalInfo()
	{
		f.removeAdditionalInfo(CEMILDataEx.ADDINFO_TIMESTAMP_EXT);
		assertNull(f.getAdditionalInfo(CEMILDataEx.ADDINFO_TIMESTAMP_EXT));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.cemi.CEMILDataEx#clone()}.
	 */
	public final void testClone()
	{
		final CEMILDataEx f2 = (CEMILDataEx) f.clone();
		final List<AddInfo> l = f.getAdditionalInfo();
		final List<AddInfo> l2 = f2.getAdditionalInfo();
		for (int i = 0; i < l.size(); ++i)
			assertNotSame(l.get(i), l2.get(i));
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2011, 2014 B. Malinowsky

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

package tuwien.auto.calimero.mgmt;

import junit.framework.TestCase;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogManager;

/**
 * @author B. Malinowsky
 */
public class ManagementProceduresImplTest extends TestCase
{
	private ManagementProcedures mp;
	private KNXNetworkLink link;
	private final IndividualAddress device = Util.getKnxDeviceCO();
	private IndividualAddress nonexist;

	/**
	 * @param name
	 */
	public ManagementProceduresImplTest(final String name)
	{
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		LogManager.getManager().addWriter("", Util.getLogWriter());
		link = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, Util.getLocalHost(),
			Util.getServer(), false, TPSettings.TP1);
		mp = new ManagementProceduresImpl(link);
		nonexist = Util.getNonExistingKnxDevice();
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		link.close();
		LogManager.getManager().removeWriter("", Util.getLogWriter());
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#ManagementProceduresImpl
	 * (tuwien.auto.calimero.link.KNXNetworkLink)}.
	 *
	 * @throws KNXLinkClosedException
	 */
	public final void testManagementProceduresImpl() throws KNXLinkClosedException
	{
		final ManagementProcedures test = new ManagementProceduresImpl(link);
		test.detach();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#readAddress()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadAddress() throws KNXException, InterruptedException
	{
		IndividualAddress[] read = mp.readAddress();
		assertEquals(read.length, 0);
		mp.setProgrammingMode(device, true);
		read = mp.readAddress();
		assertEquals(read.length, 1);
		mp.setProgrammingMode(device, false);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#writeAddress(IndividualAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteAddress() throws KNXException, InterruptedException
	{
		final IndividualAddress nonexist2 = Util.getNonExistingKnxDevice();
		boolean write = mp.writeAddress(nonexist2);
		assertFalse(write);

		mp.setProgrammingMode(device, true);
		write = mp.writeAddress(nonexist2);
		assertTrue(write);
		// undo address change
		mp.setProgrammingMode(nonexist2, true);
		mp.writeAddress(device);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#resetAddress()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testResetAddress() throws KNXException, InterruptedException
	{
		// TODO enable test again
		// reset of test device works fine, but we have to undo it to not fail in subsequent tests
//		mp.resetAddress();
//		Thread.sleep(3000);
//		mp.resetAddress();
//		final IndividualAddress def = new IndividualAddress(0xffff);
//		mp.setProgrammingMode(def, true);
//		mp.writeAddress(device);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#isAddressOccupied(IndividualAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testIsAddressOccupied() throws KNXException, InterruptedException
	{
		assertTrue(mp.isAddressOccupied(device));
		assertFalse(mp.isAddressOccupied(nonexist));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#readAddress(byte[])}.
	 * @throws InterruptedException
	 * @throws KNXException
	 *
	 */
	public final void testReadAddressByte() throws KNXException, InterruptedException
	{
		final byte[] serialNo = new byte[] {0x10, 0x10, 0x10, 0x10, 0x10, 0x10 };
		mp.readAddress(serialNo);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#writeAddress(byte[], IndividualAddress)}.
	 * @throws InterruptedException
	 * @throws KNXException
	 *
	 */
	public final void testWriteAddressByteArrayIndividualAddress() throws KNXException,
		InterruptedException
	{
		final byte[] serialNo = new byte[] { 0x10, 0x10, 0x10, 0x10, 0x10, 0x10 };
		mp.writeAddress(serialNo, new IndividualAddress(1, 1, 10));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#scanNetworkRouters()}.
	 *
	 * @throws InterruptedException
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	public final void testScanNetworkRouters() throws KNXTimeoutException, KNXLinkClosedException,
		InterruptedException
	{
		System.out.println("start scanNetworkRouters, takes a while ...");
		final IndividualAddress[] list = mp.scanNetworkRouters();
		final int i = list.length;
		System.out.println("scanNetworkRouters found " + i + " devices");
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#scanNetworkDevices(int, int)}.
	 *
	 * @throws InterruptedException
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	public final void testScanNetworkDevices() throws KNXTimeoutException, KNXLinkClosedException,
		InterruptedException
	{
		try {
			mp.scanNetworkDevices(-1, 0);
			fail("argument");
		}
		catch (final RuntimeException ok) {}

		try {
			mp.scanNetworkDevices(16, 0);
			fail("argument");
		}
		catch (final RuntimeException ok) {}

		try {
			mp.scanNetworkDevices(0, -1);
			fail("argument");
		}
		catch (final RuntimeException ok) {}

		try {
			mp.scanNetworkDevices(0, 16);
			fail("argument");
		}
		catch (final RuntimeException ok) {}

		System.out.println("start scanNetworkDevices on 1.1.x, takes a while ...");
		final IndividualAddress[] list = mp.scanNetworkDevices(1, 1);
		assertTrue(list.length > 0);
		System.out.println("start scanNetworkDevices on 0.0.x, takes a while ...");
		final IndividualAddress[] list2 = mp.scanNetworkDevices(0, 0);
		assertEquals(0, list2.length);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#setProgrammingMode(
	 * IndividualAddress, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testSetProgrammingMode() throws KNXException, InterruptedException
	{
		mp.setProgrammingMode(device, true);
		mp.setProgrammingMode(device, false);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#writeMemory(
	 * IndividualAddress, long, byte[], boolean, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteMemoryShort() throws KNXException, InterruptedException
	{
		final byte[] data = new byte[8];

		mp.writeMemory(device, 0x10, data, false, false);

		mp.writeMemory(device, 0x10, data, true, false);

		mp.writeMemory(device, 0x10, data, false, true);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#writeMemory(
	 * IndividualAddress, long, byte[], boolean, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteMemoryLong() throws KNXException, InterruptedException
	{
		final byte[] data = new byte[32];

		mp.writeMemory(device, 0x10, data, false, false);

		mp.writeMemory(device, 0x10, data, true, false);

		mp.writeMemory(device, 0x10, data, false, true);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#readMemory(
	 * tuwien.auto.calimero.IndividualAddress, long, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadMemoryShort() throws KNXException, InterruptedException
	{
		final byte[] data = mp.readMemory(device, 0x10, 8);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementProceduresImpl#readMemory(
	 * IndividualAddress, long, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadMemoryLong() throws KNXException, InterruptedException
	{
		final byte[] data = mp.readMemory(device, 0x20, 8);
	}
}

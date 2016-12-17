/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2011, 2016 B. Malinowsky

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

package tuwien.auto.calimero.mgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tag.Slow;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class ManagementProceduresImplTest
{
	private ManagementProcedures mp;
	private KNXNetworkLink link;
	private final IndividualAddress device = Util.getKnxDeviceCO();
	private final IndividualAddress deviceInProgMode = Util.getKnxDevice();
	private IndividualAddress nonexist;

	@BeforeEach
	void init() throws Exception
	{
		link = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, TPSettings.TP1);
		mp = new ManagementProceduresImpl(link);
		nonexist = Util.getNonExistingKnxDevice();
	}

	@AfterEach
	void tearDown() throws Exception
	{
		link.close();
	}

	/**
	 * Test method for
	 * {@link ManagementProceduresImpl#ManagementProceduresImpl(tuwien.auto.calimero.link.KNXNetworkLink)}.
	 *
	 * @throws KNXLinkClosedException
	 */
	@Test
	public final void testManagementProceduresImpl() throws KNXLinkClosedException
	{
		try (final ManagementProcedures test = new ManagementProceduresImpl(link)) {};
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#readAddress()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadAddress() throws KNXException, InterruptedException
	{
		IndividualAddress[] read = mp.readAddress();
		assertEquals(1, read.length);
		mp.setProgrammingMode(Util.getKnxDevice(), false);
		read = mp.readAddress();
		assertEquals(0, read.length);
		mp.setProgrammingMode(Util.getKnxDevice(), true);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#writeAddress(IndividualAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteAddress() throws KNXException, InterruptedException
	{
		final IndividualAddress programDevice = Util.getKnxDevice();
		final IndividualAddress nonexist2 = Util.getNonExistingKnxDevice();
		boolean write = mp.writeAddress(nonexist2);
		assertTrue(write);

		mp.setProgrammingMode(nonexist2, false);
		write = mp.writeAddress(programDevice);
		assertFalse(write);

		// undo address change
		mp.setProgrammingMode(nonexist2, true);
		write = mp.writeAddress(programDevice);
		assertTrue(write);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#resetAddress()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testResetAddress() throws KNXException, InterruptedException
	{
		mp.resetAddress();
		Thread.sleep(1000);
		mp.resetAddress();
		final IndividualAddress def = new IndividualAddress(0xffff);
		mp.setProgrammingMode(def, true);
		mp.writeAddress(deviceInProgMode);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#isAddressOccupied(IndividualAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testIsAddressOccupied() throws KNXException, InterruptedException
	{
		assertTrue(mp.isAddressOccupied(device));
		assertFalse(mp.isAddressOccupied(nonexist));
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#readAddress(byte[])}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	@Test
	public final void testReadAddressByte() throws KNXException, InterruptedException
	{
		final byte[] serialNo = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
		mp.readAddress(serialNo);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#writeAddress(byte[], IndividualAddress)}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	@Test
	public final void testWriteAddressByteArrayIndividualAddress() throws KNXException, InterruptedException
	{
		final byte[] serialNo = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
		mp.writeAddress(serialNo, new IndividualAddress(1, 1, 5));
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#scanNetworkRouters()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	@Slow
	public final void testScanNetworkRouters() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		System.out.println("start scanNetworkRouters, takes a while ...");
		final IndividualAddress[] list = mp.scanNetworkRouters();
		final int i = list.length;
		System.out.println("scanNetworkRouters found " + i + " devices");
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#scanNetworkDevices(int, int)}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	@Slow
	public final void testScanNetworkDevices() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
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
	 * Test method for {@link ManagementProceduresImpl#setProgrammingMode(IndividualAddress, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSetProgrammingMode() throws KNXException, InterruptedException
	{
		mp.setProgrammingMode(device, true);
		mp.setProgrammingMode(device, false);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#writeMemory(IndividualAddress, long, byte[], boolean, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteMemoryShort() throws KNXException, InterruptedException
	{
		final byte[] data = new byte[8];

		mp.writeMemory(device, 0x10, data, false, false);

		mp.writeMemory(device, 0x10, data, true, false);

		mp.writeMemory(device, 0x10, data, false, true);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#writeMemory(IndividualAddress, long, byte[], boolean, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteMemoryLong() throws KNXException, InterruptedException
	{
		final byte[] data = new byte[32];

		mp.writeMemory(device, 0x10, data, false, false);

		mp.writeMemory(device, 0x10, data, true, false);

		mp.writeMemory(device, 0x10, data, false, true);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#readMemory(tuwien.auto.calimero.IndividualAddress, long, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadMemoryShort() throws KNXException, InterruptedException
	{
		/*final byte[] data =*/ mp.readMemory(device, 0x10, 8);
	}

	/**
	 * Test method for {@link ManagementProceduresImpl#readMemory(IndividualAddress, long, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadMemoryLong() throws KNXException, InterruptedException
	{
		/*final byte[] data =*/ mp.readMemory(device, 0x20, 17);
	}
}

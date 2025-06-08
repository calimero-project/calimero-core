/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2011, 2025 B. Malinowsky

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
import org.junit.jupiter.api.parallel.Isolated;

import tag.KnxnetIP;
import tag.Slow;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;


@KnxnetIP
@Isolated
class ManagementProceduresImplTest
{
	private ManagementProcedures mp;
	private KNXNetworkLink link;
	private final IndividualAddress device = Util.getKnxDeviceCO();
	private final IndividualAddress deviceInProgMode = Util.getKnxDevice();
	private IndividualAddress nonexist;

	@BeforeEach
	void init() throws Exception
	{
		link = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, new TPSettings());
		mp = new ManagementProceduresImpl(link);
		nonexist = Util.getNonExistingKnxDevice();
	}

	@AfterEach
	void tearDown() {
		if (link != null)
			link.close();
	}

	@Test
	void managementProceduresImpl() throws KNXLinkClosedException
	{
		try (ManagementProcedures test = new ManagementProceduresImpl(link)) {}
	}

	@Test
	void readAddress() throws KNXException, InterruptedException
	{
		IndividualAddress[] read = mp.readAddress();
		assertEquals(1, read.length);
		mp.setProgrammingMode(Util.getKnxDevice(), false);
		read = mp.readAddress();
		assertEquals(0, read.length);
		mp.setProgrammingMode(Util.getKnxDevice(), true);
	}

	@Test
	@Slow
	void writeAddress() throws KNXException, InterruptedException
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

	@Test
	void resetAddress() throws KNXException, InterruptedException
	{
		mp.resetAddress();
		Thread.sleep(1000);
		mp.resetAddress();
		final IndividualAddress def = new IndividualAddress(0xffff);
		mp.setProgrammingMode(def, true);
		mp.writeAddress(deviceInProgMode);
	}

	@Test
	void isAddressOccupied() throws KNXException, InterruptedException
	{
		assertTrue(mp.isAddressOccupied(device));
		assertFalse(mp.isAddressOccupied(nonexist));
	}

	@Test
	void readAddressByte() throws KNXException, InterruptedException
	{
		final byte[] serialNo = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
		mp.readAddress(serialNo);
	}

	@Test
	void writeAddressByteArrayIndividualAddress() throws KNXException, InterruptedException
	{
		final byte[] serialNo = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
		mp.writeAddress(serialNo, new IndividualAddress(1, 1, 5));
	}

	@Test
	@Slow
	void scanNetworkRouters() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		System.out.println("start scanNetworkRouters, takes a while ...");
		final IndividualAddress[] list = mp.scanNetworkRouters();
		final int i = list.length;
		System.out.println("scanNetworkRouters found " + i + " devices");
	}

	@Test
	@Slow
	void scanNetworkDevices() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
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

	@Test
	void scanExistingGroupAddresses() throws KNXException, InterruptedException {
		final ManagementProceduresImpl impl = (ManagementProceduresImpl) mp;
		// test 1/0/1 and 1/0/2
		assertFalse(impl.scanGroupAddresses(new GroupAddress(1, 0, 1), 2).isEmpty(), "addresses exist");
	}

	@Test
	void scanFreeGroupAddresses() throws KNXException, InterruptedException {
		final ManagementProceduresImpl impl = (ManagementProceduresImpl) mp;
		// test 3/0/1 and 3/0/2
		assertTrue(impl.scanGroupAddresses(new GroupAddress(3, 0, 1), 2).isEmpty());
	}

	@Test
	void setProgrammingMode() throws KNXException, InterruptedException
	{
		mp.setProgrammingMode(device, true);
		mp.setProgrammingMode(device, false);
	}

	@Test
	void writeMemoryShort() throws KNXException, InterruptedException
	{
		final byte[] data = new byte[8];

		mp.writeMemory(device, 0x10, data, false, false);

		mp.writeMemory(device, 0x10, data, true, false);

		mp.writeMemory(device, 0x10, data, false, true);
	}

	@Test
	void writeMemoryLong() throws KNXException, InterruptedException
	{
		final byte[] data = new byte[2 * 63 + 10];

		mp.writeMemory(device, 0x1000, data, false, false);

		mp.writeMemory(device, 0x1000, data, true, false);

		mp.writeMemory(device, 0x1000, data, false, true);
	}

	@Test
	void writeMemoryExt() throws KNXException, InterruptedException {
		final byte[] data = new byte[2 * 149 + 10];
		final int startAddress = 0x10000;

		mp.writeMemory(Util.getRouterAddress(), startAddress, data, false, false);
		mp.writeMemory(Util.getRouterAddress(), startAddress, data, true, false);
		mp.writeMemory(Util.getRouterAddress(), startAddress, data, false, true);
	}

	@Test
	void readMemoryShort() throws KNXException, InterruptedException
	{
		/*final byte[] data =*/ mp.readMemory(device, 0x10, 8);
	}

	@Test
	void readMemoryLong() throws KNXException, InterruptedException
	{
		/*final byte[] data =*/ mp.readMemory(device, 0x20, 2 * 63 + 10);
	}

	@Test
	void readMemoryExt() throws KNXException, InterruptedException {
		final int startAddress = 0x10000;
		mp.readMemory(Util.getRouterAddress(), startAddress, 2 * 249 + 10);
	}
}

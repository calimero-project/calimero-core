/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogManager;

/**
 * @author B. Malinowsky
 */
public class ManagementClientImplTest extends TestCase
{
	private KNXNetworkLink lnk;
	private ManagementClient mc;
	private Destination dco, dco2;
	private Destination dcl;

	/**
	 * @param name name of test case
	 */
	public ManagementClientImplTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		LogManager.getManager().addWriter(null, Util.getLogWriter());

		lnk = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, null, Util.getServer(), false,
				TPSettings.TP1);
		// LogManager.getManager().removeWriter(lnk.getName(), Util.getLogWriter());
		mc = new ManagementClientImpl(lnk);
		// dco = mc.createDestination(new IndividualAddress(3, 0, 0), true);
		dco2 = mc.createDestination(Util.getKnxDeviceCO(), true);
		dco = dco2;
		dcl = mc.createDestination(Util.getKnxDevice(), false);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		if (mc != null)
			mc.detach();
		if (lnk != null)
			lnk.close();

		LogManager.getManager().removeWriter(null, Util.getLogWriter());
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#ManagementClientImpl(
	 * tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
	public final void testManagementClientImpl()
	{
		mc.detach();
		lnk.close();
		try {
			mc = new ManagementClientImpl(lnk);
			fail("link closed");
		}
		catch (final KNXLinkClosedException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#authorize
	 * (tuwien.auto.calimero.mgmt.Destination, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testAuthorize() throws KNXException, InterruptedException
	{
		try {
			mc.authorize(dco, new byte[] { 0x10, 0x10, 0x10 });
			fail("invalid key length");
		}
		catch (final KNXIllegalArgumentException e) {}

		final byte[] invalidKey = new byte[] { 0x10, 0x10, 0x10, 0x10 };
		final byte[] validKey = new byte[] { 0x10, 0x20, 0x30, 0x40 };
		final byte[] defaultKey = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
		try {
			int level = mc.authorize(dco, invalidKey);
			assertEquals(15, level);

			// 2 is the associated access level on the KNX test device for this valid key
			level = mc.authorize(dco, validKey);
			assertEquals(2, level);

			level = mc.authorize(dco, defaultKey);
			// 14 is selected on the KNX test device as max. unauthorized access level
			assertEquals(14, level);
		}
		catch (final KNXTimeoutException e) {
			// authorize not supported on every device, ignore for now..
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#authorize
	 * (tuwien.auto.calimero.mgmt.Destination, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testAuthorizeCL() throws KNXException, InterruptedException
	{
		try {
			final int level = mc.authorize(dcl, new byte[] { 0x10, 0x10, 0x10, 0x10 });
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readADC
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadADC() throws KNXException, InterruptedException
	{
		try {
			mc.readADC(dco, -1, 1);
			fail("invalid channel");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readADC(dco, 64, 1);
			fail("invalid channel");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readADC(dco, 1, -1);
			fail("invalid repeat");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readADC(dco, -1, 256);
			fail("invalid repeat");
		}
		catch (final KNXIllegalArgumentException e) {}
		final Destination adcDst = dco;
		final int adc = mc.readADC(adcDst, 1, 1);
		assertTrue(adc > 0);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readADC
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadADCCL() throws KNXException, InterruptedException
	{
		try {
			final int adc = mc.readADC(dcl, 1, 1);
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readAddress(boolean)}.
	 *
	 * @throws InterruptedException
	 * @throws KNXException
	 */
	public final void testReadAddressBoolean() throws InterruptedException, KNXException
	{
		System.out.println("put device into prog mode for read address...");
		Thread.sleep(5000);
		IndividualAddress[] ias = mc.readAddress(true);
		assertTrue(ias.length <= 1);
		System.out.println(ias[0]);

		final long start = System.currentTimeMillis();
		ias = mc.readAddress(false);
		assertTrue(System.currentTimeMillis() - start >= mc.getResponseTimeout() * 1000);
		System.out.println(ias[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readAddress(byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadAddressByteArray() throws KNXException, InterruptedException
	{
		try {
			mc.readAddress(new byte[] { 0x10, 0x10, 0x10, 0x10, 0x10 });
			fail("invalid SN length");
		}
		catch (final KNXIllegalArgumentException e) {}
		// XXX correct serial number for test device
		final byte[] sno = new byte[] { 0x00, 0x01, 0x00, 0x11, (byte) 0xcb, 0x08 };
		final IndividualAddress addr = Util.getKnxDeviceCO();

		final IndividualAddress ia = mc.readAddress(sno);
		assertEquals(addr, ia);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadMemory() throws KNXException, InterruptedException
	{
		try {
			mc.readMemory(dco, -1, 2);
			fail("invalid mem address");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readMemory(dco, 0x10000, 2);
			fail("invalid mem address");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readMemory(dco, 0x100, -1);
			fail("invalid mem range");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readMemory(dco, 0x100, 64);
			fail("invalid mem range");
		}
		catch (final KNXIllegalArgumentException e) {}

		final byte[] mem = mc.readMemory(dco2, 0x105, 2);
		Util.out("read mem from 0x105", mem);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadMemoryCL() throws KNXException, InterruptedException
	{
		try {
			final byte[] mem = mc.readMemory(dcl, 0x105, 2);
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadPropertyDestinationIntIntIntInt() throws KNXException,
		InterruptedException
	{
		try {
			mc.readProperty(dco, -1, 2, 1, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, 256, 2, 1, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, 1, -1, 1, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, 1, 256, 1, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, 0, 2, -1, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, -1, 2, 0x1000, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, 1, 2, 1, -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readProperty(dco, 1, 2, 1, 16);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}

		final byte[] prop = mc.readProperty(dco2, 0, 11, 1, 1);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadPropertyDestinationIntIntIntIntCL() throws KNXException,
		InterruptedException
	{
		dco2.destroy();
		final Destination connless = mc.createDestination(Util.getKnxDevice(), false);
		final byte[] prop = mc.readProperty(connless, 0, 11, 1, 1);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readPropertyDesc
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadPropertyDescDestinationIntIntInt() throws KNXException,
		InterruptedException
	{
		try {
			mc.readPropertyDesc(dco, -1, 2, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readPropertyDesc(dco, 256, 2, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readPropertyDesc(dco, 1, -1, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readPropertyDesc(dco, 1, 256, 1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readPropertyDesc(dco, 0, 2, -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readPropertyDesc(dco, -1, 2, 256);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}

		byte[] desc = mc.readPropertyDesc(dco2, 0, 51, 5);
		desc = mc.readPropertyDesc(dco2, 0, 0, 1);

		final byte[] cmp = mc.readPropertyDesc(dco2, 0, 1, 5);
		desc = mc.readPropertyDesc(dco2, 0, 0, 0);
		//assertTrue(Arrays.equals(desc, cmp));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeAddress
	 * (tuwien.auto.calimero.IndividualAddress)}.
	 *
	 * @throws InterruptedException
	 * @throws KNXException
	 */
	public final void testWriteAddressIndividualAddress() throws InterruptedException,
		KNXException
	{
		System.out.println("put device into prog mode for write address...");
		Thread.sleep(5000);
		final IndividualAddress[] orig = mc.readAddress(true);

		assertEquals(1, orig.length);
		final IndividualAddress write = Util.getNonExistingKnxDevice();
		mc.writeAddress(write);
		Thread.sleep(50);
		final IndividualAddress[] ias = mc.readAddress(true);
		assertEquals(1, ias.length);
		assertEquals(write, ias[0]);

		// write back original address
		mc.writeAddress(orig[0]);
		Thread.sleep(50);
		// test for original address
		assertEquals(orig[0], mc.readAddress(true)[0]);
		System.out.println("turn prog mode off...");
		Thread.sleep(5000);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeAddress
	 * (byte[], tuwien.auto.calimero.IndividualAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteAddressByteArrayIndividualAddress() throws KNXException,
		InterruptedException
	{
		final byte[] sno = new byte[] { 0x00, 0x01, 0x00, 0x11, (byte) 0xcb, 0x08 };
		final IndividualAddress write = mc.readAddress(sno);
		mc.writeAddress(sno, write);
		final IndividualAddress read = mc.readAddress(sno);
		assertEquals(write, read);

		final IndividualAddress write2 = Util.getNonExistingKnxDevice();
		mc.writeAddress(sno, write2);
		final IndividualAddress read2 = mc.readAddress(sno);
		assertEquals(write2, read2);
		// set to old address
		mc.writeAddress(sno, write);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeKey
	 * (tuwien.auto.calimero.mgmt.Destination, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteKey() throws KNXException, InterruptedException
	{
		try {
			mc.writeKey(dco, -1, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.writeKey(dco, 16, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.writeKey(dco, 1, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}

		mc.writeKey(dco, 1, new byte[] { 0x01, 0x02, 0x03, 0x04 });
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWriteMemory() throws KNXException, InterruptedException
	{
		final byte[] mem = mc.readMemory(dco, 0x105, 2);
		mc.writeMemory(dco, 0x105, mem);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#createDestination
	 * (tuwien.auto.calimero.IndividualAddress, boolean, boolean, boolean)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testCreateDestination() throws KNXFormatException
	{
		mc.createDestination(new IndividualAddress("1.1.1"), false);
		mc.createDestination(new IndividualAddress("2.2.2"), true, false, true);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#restart
	 * (tuwien.auto.calimero.mgmt.Destination)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	public final void testRestart() throws KNXTimeoutException, KNXLinkClosedException
	{
		// TODO why is the meaning of this destroy here?
		//dcl.destroy();
		mc.restart(dcl);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testWritePropertyDestinationIntIntIntIntByteArray()
		throws KNXException, InterruptedException
	{
		final byte[] read = mc.readProperty(dco2, 0, 51, 1, 1);
		mc.writeProperty(dco2, 0, 51, 1, 1, new byte[] { 7 });
		final byte[] read2 = mc.readProperty(dco2, 0, 51, 1, 1);
		assertTrue(Arrays.equals(new byte[] { 7 }, read2));
		mc.writeProperty(dco2, 0, 51, 1, 1, read);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#detach()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testClose() throws KNXException, InterruptedException
	{
		mc.detach();
		assertFalse(mc.isOpen());
		try {
			mc.readAddress(true);
			fail("we are closed");
		}
		catch (final KNXIllegalStateException e) {}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#getResponseTimeout()}.
	 */
	public final void testGetResponseTimeout()
	{
		assertEquals(5, mc.getResponseTimeout());
		mc.setResponseTimeout(10);
		assertEquals(10, mc.getResponseTimeout());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#getPriority()}.
	 */
	public final void testGetPriority()
	{
		assertEquals(Priority.LOW, mc.getPriority());
		mc.setPriority(Priority.URGENT);
		assertEquals(Priority.URGENT, mc.getPriority());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readDomainAddress (byte[],
	 * tuwien.auto.calimero.IndividualAddress, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadDomainAddressIntIndividualAddressInt() throws KNXException,
		InterruptedException
	{
	try {
			mc.readDomainAddress(new byte[] { 1, }, Util.getRouterAddress(), -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readDomainAddress(new byte[] { 1, 2 }, Util.getRouterAddress(), -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readDomainAddress(new byte[] { 1, 2, }, Util.getRouterAddress(), 256);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}

		final List doas =
			mc.readDomainAddress(new byte[] { 1, 2, }, Util.getRouterAddress(), 100);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readDomainAddress(boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadDomainAddressBoolean() throws KNXException,
		InterruptedException
	{
		final List domain = mc.readDomainAddress(true);
		assertTrue(domain.size() <= 1);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeDomainAddress(byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	public final void testWriteDomainAddress() throws KNXTimeoutException,
		KNXLinkClosedException
	{
		try {
			mc.writeDomainAddress(new byte[] { 1, 2, 3 });
			fail("wrong length of domain address");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.writeDomainAddress(new byte[] { 1, });
			fail("wrong length of domain address");
		}
		catch (final KNXIllegalArgumentException e) {}
		mc.writeDomainAddress(new byte[] { 1, 2, });
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readDeviceDesc
	 * (tuwien.auto.calimero.mgmt.Destination, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testReadDeviceDesc() throws KNXException, InterruptedException
	{
		try {
			mc.readDeviceDesc(dco, -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readDeviceDesc(dco, 64);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}

		final byte[] desc = mc.readDeviceDesc(dco2, 0);
		Util.out(dco2.getAddress().toString() + " has desc.type 0", desc);
		// desc = mc.readDeviceDesc(dco2, 2);
		// Debug.out(dco2.getAddress().toString() + " has desc.type 2 = ", desc);
	}
}

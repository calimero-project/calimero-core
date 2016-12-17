/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class ManagementClientImplTest
{
	private KNXNetworkLink lnk;
	private ManagementClient mc;
	private Destination dco, dco2;
	private Destination dcl;

	@BeforeEach
	void init() throws Exception
	{
		lnk = KNXNetworkLinkIP.newTunnelingLink(null, Util.getServer(), false, TPSettings.TP1);
		mc = new ManagementClientImpl(lnk);
		dco2 = mc.createDestination(Util.getKnxDeviceCO(), true);
		dco = dco2;
		dcl = mc.createDestination(Util.getKnxDevice(), false);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (mc != null)
			mc.detach();
		if (lnk != null)
			lnk.close();
	}

	/**
	 * Test method for {@link ManagementClientImpl#ManagementClientImpl(tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
	@Test
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
	 * Test method for {@link ManagementClientImpl#authorize(Destination, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
		int level = mc.authorize(dco, invalidKey);
		assertEquals(15, level);

		// 2 is the associated access level on the KNX test device for this valid key
		level = mc.authorize(dco, validKey);
		assertEquals(2, level);

		level = mc.authorize(dco, defaultKey);
		// 14 is selected on the KNX test device as max. unauthorized access level
		assertEquals(14, level);
	}

	/**
	 * Test method for {@link ManagementClientImpl#authorize(Destination, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testAuthorizeCL() throws KNXException, InterruptedException
	{
		try {
			/*final int level =*/ mc.authorize(dcl, new byte[] { 0x10, 0x10, 0x10, 0x10 });
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	/**
	 * Test method for {@link ManagementClientImpl#readADC(Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
	 * Test method for {@link ManagementClientImpl#readADC(Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadADCCL() throws KNXException, InterruptedException
	{
		try {
			/*final int adc =*/ mc.readADC(dcl, 1, 1);
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	/**
	 * Test method for {@link ManagementClientImpl#readAddress(boolean)}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	@Test
	public final void testReadAddressBoolean() throws InterruptedException, KNXException
	{
		IndividualAddress[] ias = mc.readAddress(true);
		assertTrue(ias.length <= 1);
		System.out.println(ias[0]);

		final long start = System.currentTimeMillis();
		ias = mc.readAddress(false);
		assertTrue(System.currentTimeMillis() - start >= mc.getResponseTimeout() * 1000);
		System.out.println(ias[0]);
	}

	/**
	 * Test method for {@link ManagementClientImpl#readAddress(byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadAddressByteArray() throws KNXException, InterruptedException
	{
		try {
			mc.readAddress(new byte[] { 0x10, 0x10, 0x10, 0x10, 0x10 });
			fail("invalid SN length");
		}
		catch (final KNXIllegalArgumentException e) {}

		final byte[] sno = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6 };
		final IndividualAddress addr = Util.getKnxDeviceCO();

		final IndividualAddress ia = mc.readAddress(sno);
		assertEquals(addr, ia);
	}

	/**
	 * Test method for {@link ManagementClientImpl#readMemory(Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
	 * Test method for {@link ManagementClientImpl#readMemory(Destination, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadMemoryCL() throws KNXException, InterruptedException
	{
		try {
			/*final byte[] mem =*/ mc.readMemory(dcl, 0x105, 2);
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	/**
	 * Test method for {@link ManagementClientImpl#readProperty(Destination, int, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadPropertyDestinationIntIntIntInt() throws KNXException, InterruptedException
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

		/*final byte[] prop =*/ mc.readProperty(dco2, 0, 11, 1, 1);
	}

	/**
	 * Test method for {@link ManagementClientImpl#readProperty(Destination, int, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadPropertyDestinationIntIntIntIntCL() throws KNXException, InterruptedException
	{
		dcl.destroy();
		final Destination connless = mc.createDestination(Util.getKnxDevice(), false);
		/*final byte[] prop =*/ mc.readProperty(connless, 0, 14, 1, 1);
	}

	/**
	 * Test method for {@link ManagementClientImpl#readPropertyDesc(Destination, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadPropertyDescDestinationIntIntInt() throws KNXException, InterruptedException
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

		/*byte[] desc =*/ mc.readPropertyDesc(dco2, 0, 51, 5);
		/*desc =*/ mc.readPropertyDesc(dco2, 0, 0, 1);

		/*final byte[] cmp =*/ mc.readPropertyDesc(dco2, 0, 1, 5);
		/*desc =*/ mc.readPropertyDesc(dco2, 0, 0, 0);
		//assertTrue(Arrays.equals(desc, cmp));
	}

	/**
	 * Test method for {@link ManagementClientImpl#writeAddress(tuwien.auto.calimero.IndividualAddress)}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	@Test
	public final void testWriteAddressIndividualAddress() throws InterruptedException, KNXException
	{
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
	}

	/**
	 * Test method for {@link ManagementClientImpl#writeAddress(byte[], tuwien.auto.calimero.IndividualAddress)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteAddressByteArrayIndividualAddress() throws KNXException, InterruptedException
	{
		final byte[] sno = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
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
	 * Test method for {@link ManagementClientImpl#writeKey(Destination, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
	 * Test method for {@link ManagementClientImpl#writeMemory(Destination, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWriteMemory() throws KNXException, InterruptedException
	{
		final byte[] mem = mc.readMemory(dco, 0x105, 2);
		mc.writeMemory(dco, 0x105, mem);
	}

	/**
	 * Test method for
	 * {@link ManagementClientImpl#createDestination(tuwien.auto.calimero.IndividualAddress, boolean, boolean, boolean)}
	 * .
	 *
	 * @throws KNXFormatException
	 */
	@Test
	public final void testCreateDestination() throws KNXFormatException
	{
		mc.createDestination(new IndividualAddress("1.1.1"), false);
		mc.createDestination(new IndividualAddress("2.2.2"), true, false, true);
	}

	/**
	 * Test method for {@link ManagementClientImpl#restart(Destination)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testRestart() throws KNXTimeoutException, KNXLinkClosedException
	{
		mc.restart(dcl);
	}

	/**
	 * Test method for {@link ManagementClientImpl#writeProperty(Destination, int, int, int, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testWritePropertyDestinationIntIntIntIntByteArray() throws KNXException, InterruptedException
	{
		final byte[] read = mc.readProperty(dco2, 0, 51, 1, 1);
		mc.writeProperty(dco2, 0, 51, 1, 1, new byte[] { 7 });
		final byte[] read2 = mc.readProperty(dco2, 0, 51, 1, 1);
		assertTrue(Arrays.equals(new byte[] { 7 }, read2));
		mc.writeProperty(dco2, 0, 51, 1, 1, read);
	}

	/**
	 * Test method for {@link ManagementClientImpl#detach()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
	 * Test method for {@link ManagementClientImpl#getResponseTimeout()}.
	 */
	@Test
	public final void testGetResponseTimeout()
	{
		assertEquals(5, mc.getResponseTimeout());
		mc.setResponseTimeout(10);
		assertEquals(10, mc.getResponseTimeout());
	}

	/**
	 * Test method for {@link ManagementClientImpl#getPriority()}.
	 */
	@Test
	public final void testGetPriority()
	{
		assertEquals(Priority.LOW, mc.getPriority());
		mc.setPriority(Priority.URGENT);
		assertEquals(Priority.URGENT, mc.getPriority());
	}

	/**
	 * Test method for
	 * {@link ManagementClientImpl#readDomainAddress(byte[], tuwien.auto.calimero.IndividualAddress, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadDomainAddressIntIndividualAddressInt() throws KNXException, InterruptedException
	{
		try {
			mc.readDomainAddress(new byte[] { 1, }, Util.getKnxDeviceCO(), -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readDomainAddress(new byte[] { 1, 2 }, Util.getKnxDeviceCO(), -1);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readDomainAddress(new byte[] { 1, 2, }, Util.getKnxDeviceCO(), 256);
			fail("invalid arg");
		}
		catch (final KNXIllegalArgumentException e) {}

		final byte[] domain = new byte[] { 0, (byte) 0x6c };
		/*List<byte[]> doas =*/ mc.readDomainAddress(domain, Util.getKnxDeviceCO(), 10);
		final IndividualAddress start = new IndividualAddress(Util.getKnxDeviceCO().getRawAddress() - 3);
		/*doas =*/ mc.readDomainAddress(domain, start, 5);
	}

	/**
	 * Test method for {@link ManagementClientImpl#readDomainAddress(boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testReadDomainAddressBoolean() throws KNXException, InterruptedException
	{
		final List<byte[]> domain = mc.readDomainAddress(true);
		assertTrue(domain.size() <= 1);
	}

	/**
	 * Test method for {@link ManagementClientImpl#writeDomainAddress(byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testWriteDomainAddress() throws KNXTimeoutException, KNXLinkClosedException
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

	private static final int NetworkParamObjectType = 0;
	private static final int NetworkParamPid = 59;

	@Test
	public final void testWriteNetworkParameter() throws KNXLinkClosedException, KNXTimeoutException
	{
		mc.writeNetworkParameter(dcl.getAddress(), NetworkParamObjectType, NetworkParamPid, new byte[] { 0 });
	}

	@Test
	public final void testWriteNetworkParameterBroadcast() throws KNXLinkClosedException, KNXTimeoutException
	{
		mc.writeNetworkParameter(null, NetworkParamObjectType, NetworkParamPid, new byte[] { 0 });
	}

	@Test
	public final void testReadNetworkParameterBroadcast() throws KNXException, InterruptedException
	{
		mc.readNetworkParameter(null, NetworkParamObjectType, NetworkParamPid, new byte[] {});
	}

	@Test
	public final void testReadNetworkParameter() throws KNXException, InterruptedException
	{
		mc.readNetworkParameter(dcl.getAddress(), NetworkParamObjectType, NetworkParamPid, new byte[] { 0 });
	}

	@Test
	public final void testReadUnsupportedNetworkParameter() throws InterruptedException, KNXException
	{
		try {
			mc.readNetworkParameter(dcl.getAddress(), 99, 254, new byte[] {});
			fail("unsupported object type");
		}
		catch (final KNXInvalidResponseException expected) {}
		try {
			mc.readNetworkParameter(dcl.getAddress(), 0, 254, new byte[] {});
			fail("unsupported pid");
		}
		catch (final KNXInvalidResponseException expected) {}
	}

	@Test
	public final void testReadUnsupportedNetworkParameterBroadcast() throws InterruptedException, KNXException
	{
		try {
			mc.readNetworkParameter(null, 99, 254, new byte[] {});
			fail("unsupported object type, should be timeout");
		}
		catch (final KNXTimeoutException expected) {}
		try {
			mc.readNetworkParameter(null, 0, 254, new byte[] {});
			fail("unsupported pid, should be timeout");
		}
		catch (final KNXTimeoutException expected) {}
	}

	/**
	 * Test method for {@link ManagementClientImpl#readDeviceDesc(Destination, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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

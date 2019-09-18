/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2019 B. Malinowsky

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tag.KnxnetIP;
import tag.KnxnetIPSequential;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.mgmt.PropertyAccess.PID;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
class ManagementClientImplTest
{
	private KNXNetworkLink lnk;
	private ManagementClient mc;
	private Destination dco, dco2;
	private Destination dcl;

	@BeforeEach
	void init() throws Exception
	{
		lnk = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, TPSettings.TP1);
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

	@Test
	void testManagementClientImpl()
	{
		mc.detach();
		lnk.close();
		try {
			mc = new ManagementClientImpl(lnk);
			fail("link closed");
		}
		catch (final KNXLinkClosedException e) {}
	}

	@Test
	void testAuthorize() throws KNXException, InterruptedException
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
		assertTrue(15 == level || 3 == level);

		// 2 is the associated access level on the KNX test device for this valid key
		level = mc.authorize(dco, validKey);
		assertEquals(2, level);

		level = mc.authorize(dco, defaultKey);
		// 3/15 is selected on the KNX test device as max. unauthorized access level
		assertTrue(15 == level || 3 == level);
	}

	@Test
	void testAuthorizeCL() throws KNXException, InterruptedException
	{
		try {
			/*final int level =*/ mc.authorize(dcl, new byte[] { 0x10, 0x10, 0x10, 0x10 });
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	@Test
	void testReadADC() throws KNXException, InterruptedException
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

	@Test
	void testReadADCCL() throws KNXException, InterruptedException
	{
		try {
			/*final int adc =*/ mc.readADC(dcl, 1, 1);
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	@Test
	@KnxnetIPSequential
	void testReadAddressBoolean() throws InterruptedException, KNXException
	{
		IndividualAddress[] ias = mc.readAddress(true);
		assertTrue(ias.length <= 1);

		final long start = System.currentTimeMillis();
		ias = mc.readAddress(false);
		assertTrue(System.currentTimeMillis() - start >= mc.getResponseTimeout() * 1000);
	}

	@Test
	void testReadAddressByteArray() throws KNXException, InterruptedException
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

	@Test
	void testReadMemory() throws KNXException, InterruptedException
	{
		try {
			mc.readMemory(dco, -1, 2);
			fail("invalid mem address");
		}
		catch (final KNXIllegalArgumentException e) {}
//		try {
//			mc.readMemory(dco, 0x10000, 2);
//			fail("invalid mem address");
//		}
//		catch (final KNXIllegalArgumentException e) {}
		try {
			mc.readMemory(dco, 0x100, -1);
			fail("invalid mem range");
		}
		catch (final KNXIllegalArgumentException e) {}
//		try {
//			mc.readMemory(dco, 0x100, 64);
//			fail("invalid mem range");
//		}
//		catch (final KNXIllegalArgumentException e) {}

		final byte[] mem = mc.readMemory(dco2, 0x105, 2);
		Util.out("read mem from 0x105", mem);
	}

	@Test
	void testReadMemoryCL() throws KNXException, InterruptedException
	{
		try {
			/*final byte[] mem =*/ mc.readMemory(dcl, 0x105, 2);
			fail("connection less");
		}
		catch (final KNXDisconnectException e) {}
	}

	@Test
	void testReadPropertyDestinationIntIntIntInt() throws KNXException, InterruptedException
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

	@Test
	void testReadPropertyDestinationIntIntIntIntCL() throws KNXException, InterruptedException
	{
		dcl.destroy();
		final Destination connless = mc.createDestination(Util.getKnxDevice(), false);
		/*final byte[] prop =*/ mc.readProperty(connless, 0, 14, 1, 1);
	}

	@Test
	void testReadPropertyDescDestinationIntIntInt() throws KNXException, InterruptedException
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

	@Test
	void testWriteAddressIndividualAddress() throws InterruptedException, KNXException
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

	@Test
	void testWriteAddressByteArrayIndividualAddress() throws KNXException, InterruptedException
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

	@Test
	void testWriteKey() throws KNXException, InterruptedException
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

	@Test
	void testWriteMemory() throws KNXException, InterruptedException
	{
		final byte[] mem = mc.readMemory(dco, 0x105, 2);
		mc.writeMemory(dco, 0x105, mem);
	}

	@Test
	void testCreateDestination() throws KNXFormatException
	{
		mc.createDestination(new IndividualAddress("1.1.1"), false);
		mc.createDestination(new IndividualAddress("2.2.2"), true, false, true);
	}

	@Test
	void testRestart() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		mc.restart(dcl);
	}

	@Test
	void testWritePropertyDestinationIntIntIntIntByteArray() throws KNXException, InterruptedException
	{
		final int pidProgramVersion = 13;

		byte[] read = new byte[1];
		try {
			read = mc.readProperty(dco2, 0, pidProgramVersion, 1, 1);
		}
		catch (final KNXException ignore) {}
		mc.writeProperty(dco2, 0, pidProgramVersion, 1, 1, new byte[] { 7 });
		final byte[] read2 = mc.readProperty(dco2, 0, pidProgramVersion, 1, 1);
		assertTrue(Arrays.equals(new byte[] { 7 }, read2));
		mc.writeProperty(dco2, 0, pidProgramVersion, 1, 1, read);
	}

	@Test
	void testClose() throws KNXException, InterruptedException
	{
		mc.detach();
		assertFalse(mc.isOpen());
		try {
			mc.readAddress(true);
			fail("we are closed");
		}
		catch (final IllegalStateException e) {}
	}

	@Test
	void testGetResponseTimeout()
	{
		assertEquals(5, mc.getResponseTimeout());
		mc.setResponseTimeout(10);
		assertEquals(10, mc.getResponseTimeout());
	}

	@Test
	void testGetPriority()
	{
		assertEquals(Priority.LOW, mc.getPriority());
		mc.setPriority(Priority.URGENT);
		assertEquals(Priority.URGENT, mc.getPriority());
	}

	@Test
	void testReadDomainAddressIntIndividualAddressInt() throws KNXException, InterruptedException
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

	@Test
	void testReadDomainAddressBoolean() throws KNXException, InterruptedException
	{
		final List<byte[]> domain = mc.readDomainAddress(true);
		assertTrue(domain.size() <= 1);
	}

	@Test
	void testWriteDomainAddress() throws KNXTimeoutException, KNXLinkClosedException
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
	void testWriteNetworkParameter() throws KNXLinkClosedException, KNXTimeoutException
	{
		mc.writeNetworkParameter(dcl.getAddress(), NetworkParamObjectType, NetworkParamPid, new byte[] { 0 });
	}

	@Test
	void testWriteNetworkParameterBroadcast() throws KNXLinkClosedException, KNXTimeoutException
	{
		mc.writeNetworkParameter(null, NetworkParamObjectType, NetworkParamPid, new byte[] { 0 });
	}

	@Test
	void testReadNetworkParameterBroadcast() throws KNXException, InterruptedException
	{
		mc.readNetworkParameter(null, NetworkParamObjectType, NetworkParamPid, (byte) 0);
	}

	@Test
	void testReadNetworkParameter() throws KNXException, InterruptedException
	{
		mc.readNetworkParameter(dcl.getAddress(), NetworkParamObjectType, NetworkParamPid, (byte) 0);
	}

	@Test
	void testReadUnsupportedNetworkParameter() throws InterruptedException, KNXException
	{
		final byte testInfo = 1;
		var responses = mc.readNetworkParameter(dcl.getAddress(), 99, 254, testInfo);
		assertTrue(responses.isEmpty());
		responses = mc.readNetworkParameter(dcl.getAddress(), 0, 254, testInfo);
		assertTrue(responses.isEmpty());
	}

	@Test
	void testReadUnsupportedNetworkParameterBroadcast() throws InterruptedException, KNXException
	{
		final byte testInfo = 1;
		var responses = mc.readNetworkParameter(null, 99, 254, testInfo);
		assertTrue(responses.isEmpty());
		responses = mc.readNetworkParameter(null, 0, 254, testInfo);
		assertTrue(responses.isEmpty());
	}

	@Test
	void readSystemNetworkParameterInProgrammingMode() throws KNXException, InterruptedException {
		final List<byte[]> l = mc.readSystemNetworkParameter(0, PID.SERIAL_NUMBER, 1);
		assertFalse(l.isEmpty());
		l.forEach(sn -> assertEquals(6, sn.length));
	}

	@Test
	void readSystemNetworkParameterStartup() throws KNXException, InterruptedException {
		final byte maxWaitSeconds = 2;
		List<byte[]> l = mc.readSystemNetworkParameter(0, PID.SERIAL_NUMBER, 3, maxWaitSeconds);
		assertTrue(!l.isEmpty(), "devices should respond once");
		assertEquals(2, l.size(), "2 test devices, each should respond once");
		l.forEach(sn -> assertEquals(6, sn.length));

		l = mc.readSystemNetworkParameter(0, PID.SERIAL_NUMBER, 3, maxWaitSeconds);
		assertTrue(l.isEmpty(), "devices should not respond twice");
	}

	@Test
	void testReadDeviceDesc() throws KNXException, InterruptedException
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

	@Test
	void writeMemoryExtended() throws KNXException, InterruptedException {
		final byte[] mem = new byte[1];
		mc.writeMemory(dco, 0x10000, mem);
	}

	@Test
	void writeMemoryExtendedOutOfRange() {
		final byte[] mem = new byte[1];
		assertThrows(KNXIllegalArgumentException.class, () -> mc.writeMemory(dco, 0x1000000, mem));
	}

	@Test
	void writeMemoryExtendedMaxData() {
		final byte[] mem = new byte[250];
		assertThrows(KNXIllegalArgumentException.class, () -> mc.writeMemory(dco, 0x10000, mem));
	}

	@Test
	void writeMemoryExtendedTooMuchData() {
		final byte[] mem = new byte[251];
		assertThrows(KNXIllegalArgumentException.class, () -> mc.writeMemory(dco, 0x10000, mem));
	}

	@Test
	void readMemoryExtended() throws KNXException, InterruptedException {
		mc.readMemory(dco, 0x10000, 1);
	}

	@ParameterizedTest
	@CsvSource({ "A, 0x9479", "123456789, 0xe5cc" })
	void crc16Ccitt(final String msg, final String crc) {
		final byte[] test = msg.getBytes(StandardCharsets.US_ASCII);
		final int value = ManagementClientImpl.crc16Ccitt(test);
		assertEquals((int) Integer.decode(crc), value);
	}
}

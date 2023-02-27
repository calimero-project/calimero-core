/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.mgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import io.calimero.CloseEvent;
import io.calimero.IndividualAddress;
import io.calimero.KNXException;
import io.calimero.KNXTimeoutException;
import io.calimero.Util;
import io.calimero.dptxlator.DPTXlator2ByteUnsigned;
import io.calimero.knxnetip.Debug;
import io.calimero.link.KNXNetworkLink;
import io.calimero.link.KNXNetworkLinkIP;
import io.calimero.link.medium.TPSettings;
import io.calimero.mgmt.PropertyAccess.PID;
import io.calimero.mgmt.PropertyClient.XmlPropertyDefinitions;
import io.calimero.xml.XmlInputFactory;


@KnxnetIP
class PropertyClientTest
{
	private KNXNetworkLink lnk;
	private PropertyClient rem;
	private PropertyClient local;
	private PropertyAdapter remAdpt;
	private PropertyAdapter localAdpt;

	private IndividualAddress remote;

	private final CountDownLatch closed = new CountDownLatch(1);
	private final Consumer<CloseEvent> adapterClosed = this::adapterClosed;

	private void adapterClosed(final CloseEvent e) {
		assertTrue(localAdpt == e.getSource() || remAdpt == e.getSource());
		if (closed.getCount() == 0)
			fail("already closed");
		closed.countDown();
	}

	@BeforeEach
	void init() throws Exception
	{
		remote = Util.getKnxDeviceCO();
		try {
			lnk = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, new TPSettings());
			remAdpt = new RemotePropertyServiceAdapter(lnk, remote, event -> {}, true);
			rem = new PropertyClient(remAdpt);
			localAdpt = LocalDeviceManagementIp.newAdapter(new InetSocketAddress(0), Util.getServer(), false,
					true, e -> {});
			local = new PropertyClient(localAdpt);

			final String resource = "/properties.xml";
			try (var is = PropertyClient.class.getResourceAsStream(resource);
				 var r = XmlInputFactory.newInstance().createXMLStreamReader(is)) {
				final var definitions = new XmlPropertyDefinitions().load(r);
				rem.addDefinitions(definitions);
				local.addDefinitions(definitions);
			}
		}
		catch (final RuntimeException e) {
			tearDown();
			throw e;
		}
	}

	@AfterEach
	void tearDown() {
		if (rem != null)
			rem.close();
		if (local != null)
			local.close();
		if (lnk != null)
			lnk.close();
	}

	@Test
	void propertyClient() throws KNXException, InterruptedException
	{
		rem.close();
		remAdpt = null;
		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, event -> {}, true);
		remAdpt.close();
		remAdpt = null;
		try {
			final byte[] key = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
			remAdpt = new RemotePropertyServiceAdapter(lnk, remote, event -> {}, key);
		}
		catch (final KNXTimeoutException e) {
			// authorize doesn't work on all devices, so ignore a response timeout
		}
		// check link is not closed
		rem.close();
		remAdpt = null;
		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, event -> {}, true);
		remAdpt.close();
		assertTrue(lnk.isOpen());

		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, adapterClosed, true);
		lnk.close();
		final boolean c = closed.await(1, TimeUnit.SECONDS);
		assertTrue(c);
	}

	@Test
	void getObjectTypeName()
	{
		for (int i = 0; i < 20; ++i) {
			final String s = PropertyClient.getObjectTypeName(i);
			if (i > 13 && i != 17 && i != 19)
				assertEquals("", s);
			else
				assertNotNull(s);
		}
	}

	@Test
	void close() throws KNXException, InterruptedException
	{
		rem.close();
		try {
			remAdpt.setProperty(0, 11, 1, 1, new byte[] { 0 });
			fail("closed");
		}
		catch (final IllegalStateException e) {}

		local.close();
		try {
			localAdpt.setProperty(0, 11, 1, 1, new byte[] { 0 });
			fail("closed");
		}
		catch (final IllegalStateException e) {}
	}

	@Test
	void localGetDescription() throws KNXException, InterruptedException
	{
		printDesc(local.getDescription(0, PID.SERIAL_NUMBER));
	}

	@Test
	void remoteGetDescription() throws KNXException, InterruptedException
	{
		printDesc(rem.getDescription(0, PID.SERIAL_NUMBER));
	}

	@Test
	void getDescriptionByIndex() throws KNXException, InterruptedException
	{
		final Description d, d2;
		printDesc(d = rem.getDescriptionByIndex(0, 1));
		printDesc(d2 = local.getDescriptionByIndex(0, 1));

		assertEquals(d.getObjectType(), d2.getObjectType());
		assertEquals(d.getObjectIndex(), d2.getObjectIndex());
		assertEquals(d.getPropIndex(), d2.getPropIndex());

		// we use two different devices for d and d2, the following asserts might not hold
		assertEquals(-1, d2.getPDT());
		assertEquals(d.getCurrentElements(), d2.getCurrentElements());
		assertEquals(0, d2.getReadLevel());
		assertEquals(0, d2.getWriteLevel());
	}

	@Test
	void getPropertyIntInt() throws KNXException, InterruptedException
	{
		String s = rem.getProperty(0, 56);
		assertNotNull(s);
		assertTrue(s.length() > 0);
		s = local.getProperty(0, 56);
		assertNotNull(s);
		assertTrue(s.length() > 0);
	}

	@Test
	void getPropertyIntIntIntInt() throws KNXException, InterruptedException
	{
		Util.out("OT 0 PID 56", rem.getProperty(0, 56, 1, 1));
		Util.out("OT 0 PID 56", local.getProperty(0, 56, 1, 1));
	}

	@Test
	void getPropertyTranslated() throws KNXException, InterruptedException
	{
		final DPTXlator2ByteUnsigned t = (DPTXlator2ByteUnsigned) rem.getPropertyTranslated(0, PID.MAX_APDULENGTH, 1, 1);
		assertEquals(15, t.getValueUnsigned());
		final DPTXlator2ByteUnsigned t2 = (DPTXlator2ByteUnsigned) local.getPropertyTranslated(0, PID.MAX_APDULENGTH, 1, 1);
		assertTrue(15 <= t2.getValueUnsigned());
		assertTrue(254 >= t2.getValueUnsigned());
	}

	@Test
	void scanPropertiesBooleanConsumer() throws KNXException, InterruptedException
	{
		final AtomicBoolean i = new AtomicBoolean();
		final AtomicBoolean k = new AtomicBoolean();
		rem.scanProperties(true, (d) -> i.set(true));
		assertTrue(i.get());
		local.scanProperties(true, (d) -> k.set(true));
		assertTrue(k.get());
	}

	@Test
	void scanPropertiesIntegerBooleanConsumer() throws KNXException, InterruptedException
	{
		final AtomicBoolean i = new AtomicBoolean();
		final AtomicBoolean k = new AtomicBoolean();
		rem.scanProperties(0, true, (d) -> i.set(true));
		assertTrue(i.get());
		local.scanProperties(0, true, (d) -> k.set(true));
		assertTrue(k.get());
	}

	@Test
	void setPropertyIntIntIntIntByteArray() throws KNXException, InterruptedException
	{
		final int pidProgramVersion = 13;
		byte[] data = new byte[5];
		try {
			data = rem.getProperty(0, pidProgramVersion, 1, 1);
		}
		catch (final KNXException ignore) {}
		--data[0];
		rem.setProperty(0, pidProgramVersion, 1, 1, data);
		final byte[] data2 = rem.getProperty(0, pidProgramVersion, 1, 1);
		assertEquals(data[0], data2[0]);
		++data[0];
		rem.setProperty(0, pidProgramVersion, 1, 1, data);
	}

	@Test
	void setPropertyIntIntIntString() throws KNXException, InterruptedException
	{
		final int knxParamsIndex = 8;
		final int pidProjectId = PID.PROJECT_INSTALLATION_ID;
		final String s = rem.getProperty(knxParamsIndex, pidProjectId);
		rem.setProperty(knxParamsIndex, pidProjectId, 1, "3");
		final String s2 = rem.getProperty(knxParamsIndex, pidProjectId);
		assertTrue(s2.startsWith("3"));
		rem.setProperty(knxParamsIndex, pidProjectId, 1, s);
	}

	private void printDesc(final Description d)
	{
		final StringBuilder buf = new StringBuilder();
		buf.append("OT=" + d.getObjectType());
		buf.append(", OI=" + d.getObjectIndex());
		buf.append(", PID=" + d.getPID());
		buf.append(", P index=" + d.getPropIndex());
		buf.append(", PDT=" + d.getPDT());
		buf.append(", curr elems=" + d.getCurrentElements());
		buf.append(", max elems=" + d.getMaxElements());
		buf.append(", r-lvl=" + d.getReadLevel());
		buf.append(", w-lvl=" + d.getWriteLevel());
		buf.append(", writeenable=" + d.isWriteEnabled());
		Debug.out(buf);
	}
}

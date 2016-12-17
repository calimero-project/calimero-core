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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.mgmt.PropertyAccess.PID;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class PropertyClientTest
{
	private static final String PIDResource = Util.getPath() + "properties.xml";

	private KNXNetworkLink lnk;
	private PropertyClient rem;
	private PropertyClient local;
	private PropertyAdapter remAdpt;
	private PropertyAdapter localAdpt;

	private IndividualAddress remote;

	private PropertyListenerImpl ll;

	private class PropertyListenerImpl implements PropertyAdapterListener
	{
		volatile boolean closed;

		@Override
		public void adapterClosed(final CloseEvent e)
		{
			assertTrue(localAdpt == e.getSource() || remAdpt == e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}

	}

	@BeforeEach
	void init() throws Exception
	{
		remote = Util.getKnxDeviceCO();
		try {
			lnk = KNXNetworkLinkIP.newTunnelingLink(null, Util.getServer(), false, TPSettings.TP1);
			remAdpt = new RemotePropertyServiceAdapter(lnk, remote, null, true);
			rem = new PropertyClient(remAdpt);
			ll = new PropertyListenerImpl();
			localAdpt = new LocalDeviceMgmtAdapter(null, Util.getServer(), false, ll, true);
			local = new PropertyClient(localAdpt);

			rem.addDefinitions(new PropertyClient.XmlPropertyDefinitions().load(PIDResource));
			local.addDefinitions(new PropertyClient.XmlPropertyDefinitions().load(PIDResource));
		}
		catch (final RuntimeException e) {
			tearDown();
			throw e;
		}
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (rem != null)
			rem.close();
		if (local != null)
			local.close();
		if (lnk != null)
			lnk.close();
	}

	/**
	 * Test method for property adapter.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testPropertyClient() throws KNXException, InterruptedException
	{
		rem.close();
		remAdpt = null;
		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, null, true);
		remAdpt.close();
		remAdpt = null;
		try {
			final byte[] key = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
			remAdpt = new RemotePropertyServiceAdapter(lnk, remote, null, key);
		}
		catch (final KNXTimeoutException e) {
			// authorize doesn't work on all devices, so ignore a response timeout
		}
		// check link is not closed
		rem.close();
		remAdpt = null;
		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, null, true);
		remAdpt.close();
		assertTrue(lnk.isOpen());

		final PropertyListenerImpl l = new PropertyListenerImpl();
		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, l, true);
		lnk.close();
		assertTrue(l.closed);
	}

	/**
	 * Test method for {@link PropertyClient#getObjectTypeName(int)}.
	 */
	@Test
	public final void testGetObjectTypeName()
	{
		for (int i = 0; i < 20; ++i) {
			final String s = PropertyClient.getObjectTypeName(i);
			if (i > 13)
				assertEquals("", s);
			else
				assertNotNull(s);
			System.out.println(i + " = " + s);
		}
	}

	/**
	 * Test method for {@link PropertyClient#close()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testClose() throws KNXException, InterruptedException
	{
		rem.close();
		try {
			remAdpt.setProperty(0, 11, 1, 1, new byte[] { 0 });
			fail("closed");
		}
		catch (final KNXIllegalStateException e) {}

		local.close();
		try {
			localAdpt.setProperty(0, 11, 1, 1, new byte[] { 0 });
			fail("closed");
		}
		catch (final KNXIllegalStateException e) {}
	}

	/**
	 * Test method for {@link PropertyClient#getDescription(int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testLocalGetDescription() throws KNXException, InterruptedException
	{
		printDesc(local.getDescription(0, PID.SERIAL_NUMBER));
	}

	/**
	 * Test method for {@link PropertyClient#getDescription(int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testRemoteGetDescription() throws KNXException, InterruptedException
	{
		printDesc(rem.getDescription(0, PID.SERIAL_NUMBER));
	}

	/**
	 * Test method for {@link PropertyClient#getDescriptionByIndex(int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetDescriptionByIndex() throws KNXException, InterruptedException
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

	/**
	 * Test method for {@link PropertyClient#getProperty(int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetPropertyIntInt() throws KNXException, InterruptedException
	{
		String s = rem.getProperty(0, 56);
		assertNotNull(s);
		assertTrue(s.length() > 0);
		System.out.println("OT 0, PID 56: " + s);
		s = local.getProperty(0, 56);
		assertNotNull(s);
		assertTrue(s.length() > 0);
		System.out.println("OT 0, PID 56: " + s);
	}

	/**
	 * Test method for {@link PropertyClient#getProperty(int, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetPropertyIntIntIntInt() throws KNXException, InterruptedException
	{
		Util.out("OT 0 PID 56", rem.getProperty(0, 56, 1, 1));
		Util.out("OT 0 PID 56", local.getProperty(0, 56, 1, 1));
	}

	/**
	 * Test method for {@link PropertyClient#getPropertyTranslated(int, int, int, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetPropertyTranslated() throws KNXException, InterruptedException
	{
		final DPTXlator2ByteUnsigned t = (DPTXlator2ByteUnsigned) rem.getPropertyTranslated(0, 56, 1, 1);
		assertEquals(15, t.getValueUnsigned());
		final DPTXlator2ByteUnsigned t2 = (DPTXlator2ByteUnsigned) local.getPropertyTranslated(0, 56, 1, 1);
		assertTrue(15 == t2.getValueUnsigned() || 254 == t2.getValueUnsigned());
	}

	/**
	 * Test method for {@link PropertyClient#scanProperties(boolean, java.util.function.Consumer)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testScanPropertiesBooleanConsumer() throws KNXException, InterruptedException
	{
		final AtomicBoolean i = new AtomicBoolean();
		final AtomicBoolean k = new AtomicBoolean();
		rem.scanProperties(true, (d) -> i.set(true));
		assertTrue(i.get());
		local.scanProperties(true, (d) -> k.set(true));
		assertTrue(k.get());
	}

	/**
	 * Test method for {@link PropertyClient#scanProperties(int, boolean, java.util.function.Consumer)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testScanPropertiesIntegerBooleanConsumer() throws KNXException, InterruptedException
	{
		final AtomicBoolean i = new AtomicBoolean();
		final AtomicBoolean k = new AtomicBoolean();
		rem.scanProperties(0, true, (d) -> i.set(true));
		assertTrue(i.get());
		local.scanProperties(0, true, (d) -> k.set(true));
		assertTrue(k.get());
	}

	/**
	 * Test method for {@link PropertyClient#setProperty(int, int, int, int, byte[])}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSetPropertyIntIntIntIntByteArray() throws KNXException, InterruptedException
	{
		// set routing count to
		final byte[] cnt = rem.getProperty(0, 51, 1, 1);
		--cnt[0];
		rem.setProperty(0, 51, 1, 1, cnt);
		final byte[] cnt2 = rem.getProperty(0, 51, 1, 1);
		assertEquals(cnt[0], cnt2[0]);
		++cnt[0];
		rem.setProperty(0, 51, 1, 1, cnt);
	}

	/**
	 * Test method for {@link PropertyClient#setProperty(int, int, int, java.lang.String)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSetPropertyIntIntIntString() throws KNXException, InterruptedException
	{
		// set routing count to
		final String s = rem.getProperty(0, 51);
		final int cnt = Integer.parseInt(new String(new char[] { s.charAt(0) }));
		rem.setProperty(0, 51, 1, "3");
		final String s2 = rem.getProperty(0, 51);
		assertTrue(s2.startsWith("3"));
		rem.setProperty(0, 51, 1, Integer.toString(cnt));
	}

	private void printDesc(final Description d)
	{
		final StringBuffer buf = new StringBuffer();
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
		System.out.println(buf);
	}
}

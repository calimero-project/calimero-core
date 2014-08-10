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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.mgmt.PropertyAccess.PID;

/**
 * @author B. Malinowsky
 */
public class PropertyClientTest extends TestCase
{
	private static final String PIDResource = Util.getPath() + "properties.xml";
	private static final String PIDResourceSave = Util.getPath() + "propertiesSaved.xml";

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

		public void adapterClosed(final CloseEvent e)
		{
			assertTrue(localAdpt == e.getSource() || remAdpt == e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}

	}

	/**
	 * @param name name for test case
	 */
	public PropertyClientTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		remote = Util.getKnxDeviceCO();
		try {
			LogManager.getManager().addWriter(null, Util.getLogWriter());

			lnk = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNELING, null, Util.getServer(),
				false, TPSettings.TP1);
			remAdpt = new RemotePropertyServiceAdapter(lnk, remote, null, true);
			rem = new PropertyClient(remAdpt);
			ll = new PropertyListenerImpl();
			localAdpt = new LocalDeviceMgmtAdapter(null, Util.getServer(), false, ll, true);
			local = new PropertyClient(localAdpt);

			rem.addDefinitions(PropertyClient.loadDefinitions(PIDResource, null));
			local.addDefinitions(PropertyClient.loadDefinitions(PIDResource, null));
		}
		catch (final KNXException e) {
			tearDown();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		if (rem != null)
			rem.close();
		if (local != null)
			local.close();
		if (lnk != null)
			lnk.close();

		Util.getLogWriter().setLogLevel(LogLevel.ALL);
		LogManager.getManager().removeWriter(null, Util.getLogWriter());
		super.tearDown();
	}

	/**
	 * Test method for property adapter.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testPropertyClient() throws KNXException, InterruptedException
	{
		rem.close();
		remAdpt = null;
		remAdpt = new RemotePropertyServiceAdapter(lnk, remote, null, true);
		remAdpt.close();
		remAdpt = null;
		try {
			final byte[] key = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff };
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
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#getObjectTypeName(int)}.
	 */
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
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#loadDefinitions(String,
	 * tuwien.auto.calimero.mgmt.PropertyClient.ResourceHandler)}.
	 *
	 * @throws KNXException
	 */
	public final void testLoadDefinitions() throws KNXException
	{
		PropertyClient.loadDefinitions(PIDResource, null);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#saveDefinitions(String,
	 * Collection, tuwien.auto.calimero.mgmt.PropertyClient.ResourceHandler)}.
	 *
	 * @throws KNXException
	 */
	public final void testSaveDefinitions() throws KNXException
	{
		PropertyClient.saveDefinitions(PIDResourceSave, new ArrayList(), null);

		final Collection c = PropertyClient.loadDefinitions(PIDResource, null);
		PropertyClient.saveDefinitions(PIDResourceSave, c, null);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#close()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException
	 */
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
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#getDescription(int, int)}.
	 *
	 * @throws KNXException
	 */
	public final void testLocalGetDescription() throws KNXException
	{
		printDesc(local.getDescription(0, PID.SERIAL_NUMBER));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#getDescription(int, int)}.
	 *
	 * @throws KNXException
	 */
	public final void testRemoteGetDescription() throws KNXException
	{
		printDesc(rem.getDescription(0, PID.SERIAL_NUMBER));
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.PropertyClient#getDescriptionByIndex(int, int)}.
	 *
	 * @throws KNXException
	 */
	public final void testGetDescriptionByIndex() throws KNXException
	{
		Description d, d2;
		printDesc(d = rem.getDescriptionByIndex(0, 1));
		printDesc(d2 = local.getDescriptionByIndex(0, 1));

		assertEquals(d.getObjectType(), d2.getObjectType());
		assertEquals(d.getObjectIndex(), d2.getObjectIndex());
		//assertEquals(d.getPID(), d2.getPID());
		assertEquals(d.getPropIndex(), d2.getPropIndex());

		// we use two different devices for d and d2, the following asserts might not hold
		assertEquals(-1, d2.getPDT());
		assertEquals(d.getCurrentElements(), d2.getCurrentElements());
		//assertEquals(0, d2.getMaxElements());
		assertEquals(0, d2.getReadLevel());
		assertEquals(0, d2.getWriteLevel());
		//assertEquals(d.isWriteEnabled(), d2.isWriteEnabled());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#getProperty(int, int)}.
	 *
	 * @throws KNXException
	 */
	public final void testGetPropertyIntInt() throws KNXException
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
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.PropertyClient#getProperty(int, int, int, int)}.
	 *
	 * @throws KNXException
	 */
	public final void testGetPropertyIntIntIntInt() throws KNXException
	{
		Util.out("OT 0 PID 56", rem.getProperty(0, 56, 1, 1));
		Util.out("OT 0 PID 56", local.getProperty(0, 56, 1, 1));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.mgmt.PropertyClient#getPropertyTranslated
	 * (int, int, int, int)}.
	 *
	 * @throws KNXException
	 */
	public final void testGetPropertyTranslated() throws KNXException
	{
		final DPTXlator2ByteUnsigned t = (DPTXlator2ByteUnsigned) rem.getPropertyTranslated(0, 56,
				1, 1);
		assertEquals(15, t.getValueUnsigned());
		final DPTXlator2ByteUnsigned t2 = (DPTXlator2ByteUnsigned) local.getPropertyTranslated(0,
				56, 1, 1);
		assertEquals(15, t2.getValueUnsigned());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.PropertyClient#scanProperties(boolean)}.
	 *
	 * @throws KNXException
	 */
	public final void testScanPropertiesBoolean() throws KNXException
	{
		List l = rem.scanProperties(true);
		assertTrue(l.size() > 0);
		for (final Iterator i = l.iterator(); i.hasNext();) {
			final Description d = (Description) i.next();
			printDesc(d);
		}
		l = local.scanProperties(true);
		assertTrue(l.size() > 0);
		for (final Iterator i = l.iterator(); i.hasNext();) {
			final Description d = (Description) i.next();
			printDesc(d);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.PropertyClient#scanProperties(int, boolean)}.
	 *
	 * @throws KNXException
	 */
	public final void testScanPropertiesIntBoolean() throws KNXException
	{
		List l = rem.scanProperties(0, true);
		assertTrue(l.size() > 0);
		for (final Iterator i = l.iterator(); i.hasNext();) {
			final Description d = (Description) i.next();
			printDesc(d);
		}
		l = local.scanProperties(0, true);
		assertTrue(l.size() > 0);
		for (final Iterator i = l.iterator(); i.hasNext();) {
			final Description d = (Description) i.next();
			printDesc(d);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.PropertyClient#setProperty(int, int, int, int, byte[])}.
	 *
	 * @throws KNXException
	 */
	public final void testSetPropertyIntIntIntIntByteArray() throws KNXException
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
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.PropertyClient#setProperty(int, int, int, java.lang.String)}.
	 *
	 * @throws KNXException
	 */
	public final void testSetPropertyIntIntIntString() throws KNXException
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

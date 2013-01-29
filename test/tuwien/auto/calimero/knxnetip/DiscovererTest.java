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
*/

package tuwien.auto.calimero.knxnetip;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogStreamWriter;
import tuwien.auto.calimero.log.LogWriter;

/**
 * @author B. Malinowsky
 */
public class DiscovererTest extends TestCase
{
	private Discoverer ddef;
	private Discoverer dnat;
	private Discoverer dmcast;
	// search/description timeout in seconds
	private final int timeout = 3;

	private final LogWriter w = new LogStreamWriter(LogLevel.ALL, System.out, true, false);

	/**
	 * @param name name for test case
	 */
	public DiscovererTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		ddef = new Discoverer(0, false);
		dnat = new Discoverer(0, true);
		dmcast = new Discoverer(null, 0, false, true);
		LogManager.getManager().addWriter(null, w);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		if (ddef != null)
			ddef.stopSearch();
		if (dnat != null)
			dnat.stopSearch();
		if (dmcast != null)
			dmcast.stopSearch();
		super.tearDown();
		LogManager.getManager().removeWriter(null, w);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#Discoverer(int, boolean)}.
	 * 
	 * @throws KNXException
	 */
	public final void testDiscoverer() throws KNXException
	{
		try {
			ddef = new Discoverer(-1, false);
			fail("negative port number");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			ddef = new Discoverer(0x10000, false);
			fail("port number too big");
		}
		catch (final RuntimeException e) {}
		ddef = new Discoverer(0, false);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#clearSearchResponses()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testClearSearchResponses() throws KNXException,
		InterruptedException
	{
		ddef.startSearch(timeout, true);
		assertTrue(ddef.getSearchResponses().length > 0);
		ddef.clearSearchResponses();
		assertTrue(ddef.getSearchResponses().length == 0);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#getDescription (java.net.InetSocketAddress, int)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testGetDescription() throws KNXException, InterruptedException
	{
		doGetDesc(ddef);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#getDescription (java.net.InetSocketAddress, int)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testNATGetDescription() throws KNXException, InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATGetDescription ====\n");
			return;
		}
		try {
			doGetDesc(dnat);
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	private void doGetDesc(final Discoverer d) throws KNXException, InterruptedException
	{
		d.startSearch(timeout, true);
		final SearchResponse[] search = d.getSearchResponses();
		assertTrue(search.length > 0);
		for (int i = 0; i < search.length; ++i) {
			final DescriptionResponse r = d.getDescription(new InetSocketAddress(
				search[i].getControlEndpoint().getAddress(), search[i]
					.getControlEndpoint().getPort()), timeout);
			assertNotNull(r);
			System.out.println("doGetDesc for " + search[i].getControlEndpoint() + " = "
				+ r.getDevice().getName());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#getSearchResponses()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testGetSearchResponses() throws KNXException, InterruptedException
	{
		doGetSearchRes(ddef);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#getSearchResponses()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testNATGetSearchResponses() throws KNXException,
		InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATGetSearchResponses ====\n");
			return;
		}
		try {
			doGetSearchRes(dnat);
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#getSearchResponses()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testMcastGetSearchResponses() throws KNXException,
		InterruptedException
	{
		doGetSearchRes(dmcast);
	}

	private void doGetSearchRes(final Discoverer d) throws KNXException, InterruptedException
	{
		d.startSearch(timeout, true);
		final SearchResponse[] search = d.getSearchResponses();
		assertTrue(search.length > 0);
		for (int i = 0; i < search.length; ++i) {
			assertNotNull(search[i]);
			System.out.println("doGetSearchRes " + i + " = "
				+ search[i].getControlEndpoint() + ", " + search[i].getServiceFamilies());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#startSearch (int, java.net.NetworkInterface, int, boolean)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws SocketException
	 * @throws InterruptedException
	 */
	public final void testStartSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		doStartSearchIF(ddef);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#startSearch (int, java.net.NetworkInterface, int, boolean)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws SocketException
	 * @throws InterruptedException
	 */
	public final void testNATStartSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip "
				+ "testNATStartSearchIntNetworkInterfaceIntBoolean ====\n");
			return;
		}
		try {
			doStartSearchIF(dnat);
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#startSearch (int, java.net.NetworkInterface, int, boolean)}
	 * .
	 * 
	 * @throws KNXException
	 * @throws SocketException
	 * @throws InterruptedException
	 */
	public final void testMcastStartSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		doStartSearchIF(dmcast);
	}

	private void doStartSearchIF(final Discoverer d) throws SocketException, KNXException,
		InterruptedException
	{
		d.startSearch(40000,
			NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()), timeout,
			true);
		final SearchResponse[] search = d.getSearchResponses();
		assertTrue(search.length > 0);
		for (int i = 0; i < search.length; ++i) {
			assertNotNull(search[i]);
			System.out.println("doStartSearchIF " + i + " = "
				+ search[i].getControlEndpoint() + ", " + search[i].getServiceFamilies());
		}

		// start 2 searches concurrently
		final int responses = d.getSearchResponses().length;
		d.clearSearchResponses();
		d.startSearch(30000,
			NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()), timeout,
			false);
		d.startSearch(30001,
			NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()), timeout,
			false);
		while (d.isSearching())
			Thread.sleep(200);
		assertEquals(2 * responses, d.getSearchResponses().length);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#startSearch(int, boolean)}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testStartSearchIntBoolean() throws KNXException,
		InterruptedException
	{
		doStartSeach(ddef);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#startSearch(int, boolean)}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testNATStartSearchIntBoolean() throws KNXException,
		InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATStartSearchIntBoolean ====\n");
			return;
		}
		try {
			doStartSeach(dnat);
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.knxnetip.Discoverer#startSearch(int, boolean)}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testMcastStartSearchIntBoolean() throws KNXException,
		InterruptedException
	{
		doStartSeach(dmcast);
	}

	/**
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	private void doStartSeach(final Discoverer d) throws KNXException, InterruptedException
	{
		try {
			d.startSearch(-1, true);
			fail("negative timeout");
		}
		catch (final KNXIllegalArgumentException e) {}
		d.startSearch(timeout, false);
		while (d.isSearching())
			Thread.sleep(100);
		assertTrue(d.getSearchResponses().length > 0);
		assertFalse(d.isSearching());
		final int responses = d.getSearchResponses().length;
		d.clearSearchResponses();

		// do two searches same time
		d.startSearch(timeout, false);
		d.startSearch(timeout, false);
		while (d.isSearching())
			Thread.sleep(100);
		assertEquals(2 * responses, d.getSearchResponses().length);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.knxnetip.Discoverer#stopSearch()}.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
	public final void testStopSearch() throws InterruptedException, KNXException
	{
		ddef.startSearch(timeout, false);
		ddef.startSearch(timeout, false);
		ddef.startSearch(timeout, false);
		Thread.sleep(10);
		final int responses = ddef.getSearchResponses().length;
		ddef.stopSearch();
		assertFalse(ddef.isSearching());
		Thread.sleep(timeout);
		assertFalse(ddef.isSearching());
		assertEquals(responses, ddef.getSearchResponses().length);

		final class Stopper extends Thread
		{
			volatile int res;

			public void run()
			{
				try {
					sleep(500);
				}
				catch (final InterruptedException e) {}
				res = ddef.getSearchResponses().length;
				ddef.stopSearch();
			}
		}
		final Stopper stopper = new Stopper();
		ddef.clearSearchResponses();
		stopper.start();
		// run blocking, so we're sure stopper stops search
		ddef.startSearch(0, true);
		stopper.join();
		assertEquals(stopper.res, ddef.getSearchResponses().length);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.knxnetip.Discoverer#isSearching()}.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public final void testIsSearching() throws KNXException, InterruptedException
	{
		ddef.startSearch(timeout, false);
		assertTrue(ddef.isSearching());
		while (ddef.isSearching())
			Thread.sleep(200);
		ddef.startSearch(timeout, true);
		assertFalse(ddef.isSearching());
	}
	
	public final void testStartSearchInterrupt() throws KNXException
	{
		final Thread t = Thread.currentThread();
		try {
			final class Stopper extends Thread
			{
				public void run()
				{
					try {
						sleep(1500);
					}
					catch (final InterruptedException e) {}
					t.interrupt();
				}
			}
			final Stopper stopper = new Stopper();
			stopper.start();
			ddef.startSearch(5, true);
		}
		catch (final InterruptedException e) {
			assertFalse(ddef.isSearching());
			return;
		}
		fail("not interrupted");
	}
}

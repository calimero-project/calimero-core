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

package tuwien.auto.calimero.knxnetip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.framework.AssertionFailedError;
import tag.KnxnetIP;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.knxnetip.Discoverer.Result;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;

/**
 * @author B. Malinowsky
 */
@KnxnetIP
public class DiscovererTest
{
	private Discoverer ddef;
	private Discoverer dnat;
	private Discoverer dmcast;
	// search/description timeout in seconds
	private final int timeout = 3;

	@BeforeEach
	void init() throws Exception
	{
		ddef = new Discoverer(0, false);
		dnat = new Discoverer(0, true);
		dmcast = new Discoverer(null, 0, false, true);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (ddef != null)
			ddef.stopSearch();
		if (dnat != null)
			dnat.stopSearch();
		if (dmcast != null)
			dmcast.stopSearch();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.knxnetip.Discoverer#Discoverer(int, boolean)}.
	 *
	 * @throws KNXException
	 */
	@Test
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
	 * Test method for {@link Discoverer#clearSearchResponses()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testClearSearchResponses() throws KNXException, InterruptedException
	{
		ddef.startSearch(timeout, true);
		assertTrue(ddef.getSearchResponses().size() > 0);
		ddef.clearSearchResponses();
		assertTrue(ddef.getSearchResponses().size() == 0);
	}

	/**
	 * Test method for {@link Discoverer#getDescription(java.net.InetSocketAddress, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetDescription() throws KNXException, InterruptedException
	{
		doGetDesc(ddef);
	}

	/**
	 * Test method for {@link Discoverer#getDescription(java.net.InetSocketAddress, int)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
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
		final List<Result<SearchResponse>> search = d.getSearchResponses();
		assertTrue(search.size() > 0);
		for (final Iterator<Result<SearchResponse>> i = search.iterator(); i.hasNext();) {
			final Result<SearchResponse> result = i.next();
			final SearchResponse response = result.getResponse();
			final Result<DescriptionResponse> r = d.getDescription(new InetSocketAddress(
					response.getControlEndpoint().getAddress(), response.getControlEndpoint().getPort()), timeout);
			assertNotNull(r);
//			System.out.println(
//					"doGetDesc for " + response.getControlEndpoint() + " = " + r.getResponse().getDevice().getName());
		}
	}

	/**
	 * Test method for {@link Discoverer#getSearchResponses()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetSearchResponses() throws KNXException, InterruptedException
	{
		doGetSearchRes(ddef);
	}

	/**
	 * Test method for {@link Discoverer#getSearchResponses()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testNATGetSearchResponses() throws KNXException, InterruptedException
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
	 * Test method for {@link Discoverer#getSearchResponses()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testMcastGetSearchResponses() throws KNXException, InterruptedException
	{
		doGetSearchRes(dmcast);
	}

	private void doGetSearchRes(final Discoverer d) throws KNXException, InterruptedException
	{
		d.startSearch(timeout, true);
		final List<Result<SearchResponse>> search = d.getSearchResponses();
		assertTrue(search.size() > 0);
		for (final Iterator<Result<SearchResponse>> i = search.iterator(); i.hasNext();) {
			final Result<SearchResponse> result = i.next();
			final SearchResponse response = result.getResponse();
			assertNotNull(response);
//			System.out.println("doGetSearchRes " + i + " = " + response.getControlEndpoint() + ", "
//					+ response.getServiceFamilies());
		}
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, java.net.NetworkInterface, int, boolean)}.
	 *
	 * @throws KNXException
	 * @throws SocketException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testStartSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		doStartSearchIF(ddef, false);
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, java.net.NetworkInterface, int, boolean)}.
	 *
	 * @throws SocketException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testNATStartSearchIntNetworkInterfaceIntBoolean() throws SocketException, InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATStartSearchIntNetworkInterfaceIntBoolean ====\n");
			return;
		}
		try {
			doStartSearchIF(dnat, false);
		}
		catch (final KNXException e) {
			// don't fail, we might use an IPv6 socket, and on some OS IPv6 sockets can't join IPv4 multicast groups
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, java.net.NetworkInterface, int, boolean)}.
	 *
	 * @throws KNXException
	 * @throws SocketException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testMcastStartSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		doStartSearchIF(dmcast, true);
	}

	private void doStartSearchIF(final Discoverer d, final boolean usesMulticast)
		throws SocketException, KNXException, InterruptedException
	{
		d.startSearch(40000, NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()), timeout, true);
		final List<Result<SearchResponse>> search = d.getSearchResponses();
		assertTrue(search.size() > 0);
		for (final Iterator<Result<SearchResponse>> i = search.iterator(); i.hasNext();) {
			final Result<SearchResponse> result = i.next();
			final SearchResponse response = result.getResponse();
			assertNotNull(response);
//			System.out.println("doStartSearchIF " + i + " = " + response.getControlEndpoint() + ", "
//					+ response.getServiceFamilies());
		}

		// start 2 searches concurrently
		final int responses = d.getSearchResponses().size();
		d.clearSearchResponses();
		d.startSearch(30000, NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()), timeout, false);
		d.startSearch(30001, NetworkInterface.getByInetAddress(Util.getLocalHost().getAddress()), timeout, false);
		while (d.isSearching())
			Thread.sleep(200);
		final int expected = responses;
		final int actual = d.getSearchResponses().size();
		assertEquals("expected = " + expected + ", actual = " + actual, expected, actual);
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testStartSearchIntBoolean() throws KNXException, InterruptedException
	{
		doStartSearch(ddef, false);
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testNATStartSearchIntBoolean() throws KNXException, InterruptedException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATStartSearchIntBoolean ====\n");
			return;
		}
		try {
			doStartSearch(dnat, false);
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testMcastStartSearchIntBoolean() throws KNXException, InterruptedException
	{
		doStartSearch(dmcast, true);
	}

	/**
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	private void doStartSearch(final Discoverer d, final boolean usesMulticast) throws KNXException, InterruptedException
	{
		try {
			d.startSearch(-1, true);
			fail("negative timeout");
		}
		catch (final KNXIllegalArgumentException e) {}
		d.startSearch(timeout, false);
		while (d.isSearching())
			Thread.sleep(100);
		assertTrue(d.getSearchResponses().size() > 0);
		assertFalse(d.isSearching());
		final int responses = d.getSearchResponses().size();
		d.clearSearchResponses();

		// do two searches same time
		d.startSearch(timeout, false);
		d.startSearch(timeout, false);
		while (d.isSearching())
			Thread.sleep(100);

		// multicasts are not only received on sending IF
		// the number of responses can vary based on network setup
		final int expected = responses;
		final int actual = d.getSearchResponses().size();
		assertEquals("expected = " + expected + ", actual = " + actual, expected, actual);
	}

	/**
	 * Test method for {@link Discoverer#stopSearch()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXException
	 */
	@Test
	public final void testStopSearch() throws InterruptedException, KNXException
	{
		ddef.startSearch(timeout, false);
		ddef.startSearch(timeout, false);
		ddef.startSearch(timeout, false);
		Thread.sleep(10);
		ddef.stopSearch();
		final int responses = ddef.getSearchResponses().size();
		assertFalse(ddef.isSearching());
		Thread.sleep(timeout);
		assertFalse(ddef.isSearching());
		assertEquals(responses, ddef.getSearchResponses().size());

		final class Stopper extends Thread
		{
			volatile int res;

			@Override
			public void run()
			{
				try {
					sleep(500);
				}
				catch (final InterruptedException e) {}
				res = ddef.getSearchResponses().size();
				ddef.stopSearch();
			}
		}
		final Stopper stopper = new Stopper();
		ddef.clearSearchResponses();
		stopper.start();
		// run blocking, so we're sure stopper stops search
		ddef.startSearch(0, true);
		stopper.join();
		assertEquals(stopper.res, ddef.getSearchResponses().size());
	}

	/**
	 * Test method for {@link Discoverer#isSearching()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testIsSearching() throws KNXException, InterruptedException
	{
		ddef.startSearch(timeout, false);
		assertTrue(ddef.isSearching());
		while (ddef.isSearching())
			Thread.sleep(200);
		ddef.startSearch(timeout, true);
		assertFalse(ddef.isSearching());
	}

	/**
	 * Test method for {@link Discoverer#startSearch(int, boolean)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testStartSearchInterrupt() throws KNXException
	{
		final Thread t = Thread.currentThread();
		try {
			final class Stopper extends Thread
			{
				@Override
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

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

package io.calimero.knxnetip;

import static io.calimero.knxnetip.util.Srp.withService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.opentest4j.AssertionFailedError;

import io.calimero.KNXException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.Util;
import io.calimero.internal.Executor;
import io.calimero.knxnetip.Discoverer.Result;
import io.calimero.knxnetip.servicetype.DescriptionResponse;
import io.calimero.knxnetip.servicetype.SearchResponse;
import io.calimero.knxnetip.util.ServiceFamiliesDIB.ServiceFamily;
import tag.KnxnetIP;
import tag.Slow;


@KnxnetIP
@Execution(ExecutionMode.SAME_THREAD)
class DiscovererTest
{
	private Discoverer ddef;
	private Discoverer dnat;
	private Discoverer dmcast;
	// search/description timeout in seconds
	private final int timeout = 3;

	@BeforeEach
	void init() {
		ddef = new Discoverer(0, false);
		dnat = new Discoverer(0, true);
		dmcast = new Discoverer(null, 0, false, true);
	}

	@AfterEach
	void tearDown() {
		if (ddef != null)
			ddef.stopSearch();
		if (dnat != null)
			dnat.stopSearch();
		if (dmcast != null)
			dmcast.stopSearch();
	}

	@Test
	void discoverer()
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

	@Test
	void clearSearchResponses() throws InterruptedException
	{
		ddef.startSearch(timeout, true);
		assertFalse(ddef.getSearchResponses().isEmpty());
		ddef.clearSearchResponses();
		assertEquals(0, ddef.getSearchResponses().size());
	}

	@Test
	void getDescription() throws KNXException
	{
		doGetDesc(ddef);
	}

	@Test
	void natGetDescription() throws KNXException
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

	private void doGetDesc(final Discoverer d) throws KNXException {
		final InetSocketAddress server = Util.getServer();
		final Result<DescriptionResponse> r = d.getDescription(server, timeout);
		assertNotNull(r);
		assertNotNull(r.localEndpoint());
		assertNotEquals(0, r.localEndpoint().getPort());
	}

	@Test
	void getSearchResponses() throws InterruptedException
	{
		doGetSearchRes(ddef);
	}

	@Test
	void natGetSearchResponses() throws InterruptedException
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

	@Test
	void mcastGetSearchResponses() throws InterruptedException
	{
		doGetSearchRes(dmcast);
	}

	private void doGetSearchRes(final Discoverer d) throws InterruptedException
	{
		d.startSearch(timeout, true);
		final List<Result<SearchResponse>> search = d.getSearchResponses();
		assertFalse(search.isEmpty());
		for (final Result<SearchResponse> result : search) {
			final SearchResponse response = result.response();
			assertNotNull(response);
			assertNotNull(result.localEndpoint().getAddress());
			assertNotEquals(0, result.localEndpoint().getPort());
		}
	}

	@Test
	void startSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		doStartSearchIF(ddef, false);
	}

	@Test
	void natStartSearchIntNetworkInterfaceIntBoolean() throws SocketException, InterruptedException
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

	@Test
	void mcastStartSearchIntNetworkInterfaceIntBoolean()
		throws SocketException, KNXException, InterruptedException
	{
		doStartSearchIF(dmcast, true);
	}

	private void doStartSearchIF(final Discoverer d, final boolean usesMulticast)
		throws SocketException, KNXException, InterruptedException
	{
		d.startSearch(40000, Util.localInterface(), timeout, true);
		final List<Result<SearchResponse>> search = d.getSearchResponses();
		assertFalse(search.isEmpty(), "search results > 0");
		for (final Result<SearchResponse> result : search) {
			final SearchResponse response = result.response();
			assertNotNull(response);
		}

		// start 2 searches concurrently
		final int responses = d.getSearchResponses().size();
		d.clearSearchResponses();
		d.startSearch(30000, Util.localInterface(), timeout, false);
		d.startSearch(30001, Util.localInterface(), timeout, false);
		while (d.isSearching())
			Thread.sleep(200);
		final int expected = 2 * responses;
		final int actual = d.getSearchResponses().size();
		if (d == dmcast)
			assertTrue(actual >= expected, "multicast responses should be >= expected ones");
		else
			assertEquals(expected, actual);
	}

	@Test
	void startSearchIntBoolean() throws InterruptedException, ExecutionException
	{
		doSearch(ddef, false);
	}

	@Test
	void natStartSearchIntBoolean() throws InterruptedException, ExecutionException
	{
		if (!Util.TEST_NAT) {
			System.out.println("\n==== skip testNATStartSearchIntBoolean ====\n");
			return;
		}
		try {
			doSearch(dnat, false);
		}
		catch (final AssertionFailedError e) {
			fail("Probably no NAT support on router, " + e.getMessage());
		}
	}

	@Test
	void mcastStartSearchIntBoolean() throws InterruptedException, ExecutionException
	{
		doSearch(dmcast, true);
	}

	@Test
	void invalidTimeout() {
		assertThrows(KNXIllegalArgumentException.class, () -> ddef.timeout(Duration.ofNanos(-1)));
		assertThrows(KNXIllegalArgumentException.class, () -> ddef.timeout(Duration.ZERO));
	}

	private void doSearch(final Discoverer d, final boolean usesMulticast)
			throws InterruptedException, ExecutionException {
		try {
			d.startSearch(-1, true);
			fail("negative timeout");
		}
		catch (final KNXIllegalArgumentException e) {}
		final var result = d.timeout(Duration.ofSeconds(timeout)).search().get();
		assertFalse(result.isEmpty());
		assertFalse(d.isSearching());
		final int responses = result.size();

		// do two searches same time
		final var search1 = d.timeout(Duration.ofSeconds(timeout)).search();
		final var search2 = d.timeout(Duration.ofSeconds(timeout)).search();
		CompletableFuture.allOf(search1, search2);

		assertEquals(responses, search1.get().size());
		assertEquals(responses, search2.get().size());
	}

	@Test
	void stopSearch() throws InterruptedException, ExecutionException
	{
		ddef.startSearch(timeout, false);
		ddef.startSearch(timeout, false);
		ddef.startSearch(timeout, false);
		Thread.sleep(100);
		ddef.stopSearch();
		final int responses = ddef.getSearchResponses().size();
		Thread.sleep(500);
		assertFalse(ddef.isSearching(), "is searching");
		Thread.sleep(timeout * 1000);
		assertFalse(ddef.isSearching());
		assertEquals(responses, ddef.getSearchResponses().size());

		final Callable<Integer> stopper = () -> {
			final int res = ddef.getSearchResponses().size();
			ddef.stopSearch();
			return res;
		};
		ddef.clearSearchResponses();
		final var future = Executor.scheduledExecutor().schedule(stopper, 500, TimeUnit.MILLISECONDS);
		// run blocking, so we're sure stopper stops search
		ddef.startSearch(0, true);
		final int stopperRes = future.get();
		assertEquals(stopperRes, ddef.getSearchResponses().size());
	}

	@Test
	void isSearching() throws InterruptedException
	{
		ddef.startSearch(timeout, false);
		assertTrue(ddef.isSearching());
		while (ddef.isSearching())
			Thread.sleep(200);
		ddef.startSearch(timeout, true);
		assertFalse(ddef.isSearching());
	}

	@Test
	void startSearchInterrupt()
	{
		final Thread t = Thread.currentThread();
		try {
			Executor.scheduledExecutor().schedule(t::interrupt, 1500, TimeUnit.MILLISECONDS);
			ddef.startSearch(5, true);
		}
		catch (final InterruptedException e) {
			assertFalse(ddef.isSearching());
			return;
		}
		fail("not interrupted");
	}

	@Test
	void searchAsync() throws InterruptedException, ExecutionException, TimeoutException {
		final Duration timeout = Duration.ofSeconds(this.timeout);
		final CompletableFuture<List<Result<SearchResponse>>> search = ddef.timeout(timeout).search();
		final List<Result<SearchResponse>> result = search.get(timeout.toMillis() + 200, TimeUnit.MILLISECONDS);
		assertFalse(result.isEmpty());
	}

	@Test
	void searchAsyncTimeout() {
		final Duration timeout = Duration.ofSeconds(10);
		final CompletableFuture<List<Result<SearchResponse>>> search = ddef.timeout(timeout).search();
		assertThrows(TimeoutException.class, () -> search.get(1000, TimeUnit.MILLISECONDS));
		assertFalse(search.isCompletedExceptionally());
		assertFalse(search.isDone());
		assertTrue(ddef.isSearching());
	}

	@Test
	void searchAsyncCancel() throws InterruptedException {
		searchAsyncCancel(ddef);
	}

	@Test
	void searchAsyncNatCancel() throws InterruptedException {
		searchAsyncCancel(dnat);
	}

	@Test
	void searchAsyncMcastCancel() throws InterruptedException {
		searchAsyncCancel(dmcast);
	}

	private static void searchAsyncCancel(final Discoverer d) throws InterruptedException {
		allReceiverThreadsIdle();

		final Duration timeout = Duration.ofSeconds(10);
		final CompletableFuture<List<Result<SearchResponse>>> search = d.timeout(timeout).search();
		Thread.sleep(1000);
		search.cancel(false);
		assertThrows(CancellationException.class, search::get);
		assertTrue(search.isCompletedExceptionally());
		assertTrue(search.isDone());
		while (d.isSearching())
			Thread.sleep(50);
		allReceiverThreadsIdle();
	}

	// checks that all used receiver threads are idle
	private static void allReceiverThreadsIdle() throws InterruptedException {
		Thread.sleep(1500);
		final Thread[] threads = new Thread[Thread.activeCount() + 5];
		assertTrue(Thread.enumerate(threads) < threads.length);
		final List<Thread> l = Arrays.asList(threads);
		assertTrue(l.stream().filter(Objects::nonNull).map(Thread::getName).filter(s -> s.startsWith("Discoverer"))
				.allMatch("Discoverer (idle)"::equals), "receiver thread(s) not idle");
	}

	@Test
	void searchControlEndpoint() throws KNXException, InterruptedException, ExecutionException {
		final InetSocketAddress server = Util.getServer();
		final CompletableFuture<Result<SearchResponse>> search = ddef.search(server,
				withService(ServiceFamily.Core, 2));
		assertEquals(server, search.get().remoteEndpoint());
	}

	@Test
	@Slow
	void searchNonExistingControlEndpoint() throws KNXException {
		final InetSocketAddress server = new InetSocketAddress(Util.getServer().getAddress(), 5000);
		final CompletableFuture<Result<SearchResponse>> search = ddef.search(server,
				withService(ServiceFamily.Core, 2));
		assertThrows(ExecutionException.class, search::get);
	}
}

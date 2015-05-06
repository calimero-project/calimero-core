/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.microedition.io.AccessPoint;
import javax.microedition.io.ConnectionOption;
import javax.microedition.io.NetworkInterface;
import javax.microedition.io.UDPDatagramConnection;
import javax.microedition.io.UDPMulticastConnection;

import org.slf4j.Logger;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.internal.Connection;
import tuwien.auto.calimero.internal.EndpointAddress;
import tuwien.auto.calimero.internal.JavaME;
import tuwien.auto.calimero.internal.UdpSocketLooper;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionRequest;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.SearchRequest;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.log.LogService;

/**
 * Does KNXnet/IP discovery and retrieval of self description from other devices.
 * <p>
 * Both searches for server discovery and description requests can be run in blocking mode or
 * asynchronous in the background.<br>
 * This discoverer supports networks with routers doing network address translation.<br>
 * Requests for self description are sent using the UDP transport protocol.<br>
 * Due to protocol limitations, only IPv4 addresses are supported when network address translation
 * is <b>not</b> used. With NAT enabled, IPv6 addresses can be used as well.
 * <p>
 * A note on (not) using network address translation (NAT):<br>
 * If discovery or description attempts fail indicating a timeout limit, it might be possible that
 * NAT is used on routers while traversing the network, so the solution would be to enable the use
 * of NAT.<br>
 * On the other hand, if NAT is used but not supported by the answering device, no response is
 * received and a timeout will occur nevertheless.
 * <p>
 * For discovery searches, an additional option for choosing IP multicast over IP unicast is
 * available in {@link Discoverer#Discoverer(InetAddress, int, boolean, boolean)}. This allows to
 * select, whether search responses by answering servers are sent via multicast and received by the
 * local multicast socket, or if responses are sent via unicast using the local host and port as
 * address for the reply. Using multicast response is of advantage in case KNXnet/IP servers are
 * located in different subnetworks.
 * <p>
 * Cancellation policy on thread interrupts: any running searches are canceled, see
 * {@link #stopSearch()}, blocking invocations of startSearch (i.e., parameter
 * <code>wait = true</code>) will return (before the specified timeout occurred).
 *
 * @author B. Malinowsky
 */
public class Discoverer
{
	/**
	 * Name of the log service used by a discoverer.
	 */
	public static final String LOG_SERVICE = "Discoverer";

	/**
	 * Multicast IP address used for discovery, multicast group is {@value} .
	 */
	public static final String SEARCH_MULTICAST = "224.0.23.12";

	/**
	 * Port number used for discovery, port is {@value} .
	 */
	public static final int SEARCH_PORT = KNXnetIPConnection.DEFAULT_PORT;

	private static final Logger logger = LogService.getLogger(LOG_SERVICE);
	static final EndpointAddress SYSTEM_SETUP_MULTICAST = EndpointAddress.of(SEARCH_MULTICAST,
			SEARCH_PORT);

	// local host/port
	private final EndpointAddress host;
	private final int port;
	// is our discovery/description aware of network address translation
	private final boolean nat;
	// is our search set up for multicast responses
	// for implications on forwarding multicast through NAT inside/outside interfaces,
	// see also RFC 5135
	private final boolean mcast;

	private final List<ReceiverLoop> receivers = JavaME.synchronizedList();
	private final List<Result<SearchResponse>> responses = JavaME.synchronizedList();

	/**
	 * Discoverer result, either containing a {@link SearchResponse} or {@link DescriptionResponse}.
	 */
	public static final class Result<T>
	{
		private final T response;
		private final NetworkInterface ni;
		private final EndpointAddress addr;

		Result(final T r, final NetworkInterface outgoing, final EndpointAddress bind)
		{
			response = r;
			ni = outgoing;
			addr = bind;
		}

		/**
		 * @return the received discoverer response
		 */
		public T getResponse()
		{
			return response;
		}

		/**
		 * @return the local network interface used for the discovery or description request
		 */
		public NetworkInterface getNetworkInterface()
		{
			return ni;
		}

		/**
		 * @return the local IP address used for the discovery or description request
		 */
		public EndpointAddress getAddress()
		{
			return addr;
		}

		@Override
		public boolean equals(final Object obj)
		{
			if (this == obj)
				return true;
			if (!(obj instanceof Result<?>))
				return false;
			final Result<?> other = (Result<?>) obj;
			return getNetworkInterface().equals(other.getNetworkInterface())
					&& getAddress().equals(other.getAddress())
					&& getResponse().equals(other.getResponse());
		}

		@Override
		public int hashCode()
		{
			final int prime = 17;
			return prime * (prime * getNetworkInterface().hashCode() + getAddress().hashCode())
					+ getResponse().hashCode();
		}
	}

	/**
	 * Creates a new Discoverer.
	 * <p>
	 * Network address translation:<br>
	 * If subsequent discovery or description attempts fail indicating a timeout limit, it might be
	 * possible that network address translation (NAT) is used on routers while traversing the
	 * network (besides the other reason that timeouts are too short). This would effectively stop
	 * any communication done in the standard way, due to the way the HPAI structure is used by
	 * default.<br>
	 * Setting the parameter for indicating use of NAT to <code>true</code> takes account of such
	 * routers, leading to the desired behavior.
	 *
	 * @param localPort the port number used to bind a socket, a valid port is in the range of 1 to
	 *        65535, or use 0 to pick an arbitrary unused (ephemeral) port. Note that a specified
	 *        valid port does not ensure a successful bind in subsequent discoverer operations due
	 *        to operating system dependencies.
	 * @param natAware <code>true</code> to use a NAT (network address translation) aware
	 *        discovery/description mechanism, <code>false</code> to use the default way
	 * @throws KNXException on error getting usable local host
	 */
	public Discoverer(final int localPort, final boolean natAware) throws KNXException
	{
		this(null, localPort, natAware, false);
	}

	/**
	 * Creates a new Discoverer and allows to specify a local host.
	 * <p>
	 * See {@link Discoverer#Discoverer(int, boolean)} for additional description.<br>
	 * The <code>localHost</code> is used to specify a particular local host address, used as
	 * response destination address when doing discovery / description. By default, the local host
	 * is used as obtained by {@link InetAddress#localHost()}. The returned address is quite system
	 * dependent and might not always be useful in some situations. So it can be overruled
	 * specifying a local host address using this constructor.
	 *
	 * @param localHost local host address used for discovery/description responses, or
	 *        <code>null</code> for the default local host
	 * @param localPort the port number used to bind a socket, a valid port is in the range of 1 to
	 *        65535, or use 0 to pick an arbitrary unused (ephemeral) port. Note that a specified
	 *        valid port does not ensure a successful bind in subsequent discoverer operations due
	 *        to operating system dependencies.
	 * @param natAware <code>true</code> to use a NAT (network address translation) aware
	 *        discovery/description mechanism, <code>false</code> to use the default way
	 * @param mcastResponse set <code>true</code> to use multicasting for search responses in
	 *        KNXnet/IP discovery, <code>false</code> to use unicast for search responses to this
	 *        local host and port address
	 * @throws KNXException if local host can't be used
	 */
	public Discoverer(final EndpointAddress localHost, final int localPort, final boolean natAware,
		final boolean mcastResponse) throws KNXException
	{
		if (localPort < 0 || localPort > 0xFFFF)
			throw new KNXIllegalArgumentException("port out of range [0..0xFFFF]");
		if (localHost == null)
			try {
				host = EndpointAddress.localHost();
			}
			catch (final UnknownHostException e) {
				logger.error("can not get local host", e);
				throw new KNXException("can not get local host");
			}
		else
			host = localHost;
		checkHost();
		port = localPort;
		nat = natAware;
		mcast = mcastResponse;
	}

	/**
	 * Starts a new search for KNXnet/IP discovery, the network interface can be specified.
	 * <p>
	 * The search will continue for <code>timeout</code> seconds, or infinite if timeout value is
	 * zero. During this time, search responses will get collected asynchronous in the background by
	 * this {@link Discoverer}.<br>
	 * With <code>wait</code> you can force this method into blocking mode to wait until the search
	 * finished, otherwise the method returns with the search running in the background.<br>
	 * A search is finished if either the <code>timeout</code> was reached or the background
	 * receiver stopped.<br>
	 *
	 * @param ni the {@link NetworkInterface} used for sending outgoing multicast messages, or
	 *        <code>null</code> to use the default multicast interface
	 * @param timeout time window in seconds during which search response messages will get
	 *        collected, timeout &ge; 0. If timeout is zero, no timeout is set, the search has to be
	 *        stopped with {@link #stopSearch()}.
	 * @param wait <code>true</code> to block until end of search before return
	 * @throws KNXException on network I/O error
	 * @throws InterruptedException if search was interrupted in blocking mode before the specified
	 *         timeout was reached; the search is stopped before passing this exception back to the
	 *         caller
	 * @see MulticastSocket
	 * @see NetworkInterface
	 */
	public void startSearch(final NetworkInterface ni, final int timeout, final boolean wait)
		throws KNXException, InterruptedException
	{
		startSearch(port, ni, timeout, wait);
	}

	/**
	 * Starts a new search for KNXnet/IP discovery, the <code>localPort</code> and network interface
	 * can be specified.
	 * <p>
	 * See documentation for method {@link #startSearch(NetworkInterface, int, boolean)}.<br>
	 * To distinguish between search responses if a user started two or more searches running
	 * concurrently, this method allows to specify a dedicated <code>localPort</code>, not using the
	 * port set with {@link #Discoverer(int, boolean)}.
	 *
	 * @param localPort the port used to bind the socket, a valid port is 0 to 65535, if
	 *        <code>localPort</code> is zero an arbitrary unused (ephemeral) port is picked
	 * @param ni the {@link NetworkInterface} used for sending outgoing multicast messages, or
	 *        <code>null</code> to use the default multicast interface
	 * @param timeout time window in seconds during which search response messages will get
	 *        collected, <code>timeout &ge; 0</code>. If timeout is zero, no timeout is set, the
	 *        search has to be stopped with {@link #stopSearch()}.
	 * @param wait <code>true</code> to block until end of search before return
	 * @throws KNXException on network I/O error
	 * @throws InterruptedException if search was interrupted in blocking mode before the specified
	 *         timeout was reached; the search is stopped before passing this exception back to the
	 *         caller
	 * @see MulticastSocket
	 * @see NetworkInterface
	 */
	public void startSearch(final int localPort, final NetworkInterface ni, final int timeout,
		final boolean wait) throws KNXException, InterruptedException
	{
		if (timeout < 0)
			throw new KNXIllegalArgumentException("timeout has to be >= 0");
		if (localPort < 0 || localPort > 65535)
			throw new KNXIllegalArgumentException("port out of range [0..0xFFFF]");
		final ReceiverLoop r = search(host, localPort, ni, timeout);
		if (wait) {
			try {
				join(r);
			}
			finally {
				r.quit();
			}
		}
	}

	/**
	 * Starts a new search for KNXnet/IP discovery on all found network interfaces.
	 * <p>
	 * The search will continue for <code>timeout</code> seconds, or infinite if timeout value is
	 * zero. During this time, search responses will get collected asynchronous in the background by
	 * this Discoverer.<br>
	 * With <code>wait</code> you can force this method into blocking mode to wait until the search
	 * finished, otherwise the method returns with the search running in the background.<br>
	 * A search has finished if either the <code>timeout</code> was reached, all background
	 * receivers stopped (all responses received) or {@link #stopSearch()} was invoked.
	 *
	 * @param timeout time window in seconds during which search response messages will get
	 *        collected, timeout &ge; 0. If timeout is 0, no timeout is set, the search has to be
	 *        stopped with <code>stopSearch</code>.
	 * @param wait <code>true</code> to block until end of search before return
	 * @throws KNXException on network I/O error
	 * @throws InterruptedException if search was interrupted in blocking mode before the specified
	 *         timeout was reached; the search is stopped before passing this exception back to the
	 *         caller
	 */
	public void startSearch(final int timeout, final boolean wait) throws KNXException,
		InterruptedException
	{
		if (timeout < 0)
			throw new KNXIllegalArgumentException("timeout has to be >= 0");
		// TODO Java8ME better loop over APs directly and use the dedicated IF from there
		final Enumeration<NetworkInterface> eni = Collections.enumeration(EndpointAddress
				.getAllNetworkInterfaces());
		if (!eni.hasMoreElements()) {
			logger.error("no network interfaces found");
			throw new KNXException("no network interfaces found");
		}
		final List<ReceiverLoop> rcv = new ArrayList<>();
		// loopback flag, so we start at most one local search
		boolean lo = false;
		while (eni.hasMoreElements()) {
			final NetworkInterface ni = eni.nextElement();
			logger.trace("try network interface {}", ni.getName());
			final AccessPoint[] aps = ni.getConnectedAccessPoints();
			if (aps.length == 0)
				continue;
			// find one IP address we can use for our search on this interface
			final String[] addrs = aps[0].getPropertyValues("ipaddr");
			for (final String addr : addrs) {
				final EndpointAddress a = EndpointAddress.of(addr);
				logger.trace("  with address {}", a);
				// without NAT, we only try IPv4 addresses
				if (!nat && a.getAddress().length != 4)
					logger.info("skip " + a + ", not an IPv4 address");
				else
					try {
						final boolean skipLinkLocal = false;
						if (!(lo && a.isLoopback())) {
							if (!(a.isLinkLocal() && skipLinkLocal))
								rcv.add(search(a, port, ni, timeout));
						}
						if (a.isLoopback()) {
//							System.out.println("loopback " + a);
							lo = true;
						}
						else if (a.isLinkLocal()) {
//							System.out.println("link local " + a);
						}
						// XXX Java8ME don't need site-local
//						else if (a.isSiteLocalAddress()) {
//							System.out.println("site local " + a);
						//break;
//						}
//						else
//							break;
					}
					catch (final KNXException e) {
						// we ignore exceptions here, but print an error for user information
						logger.error(e.getMessage());
					}
			}
		}
		if (rcv.isEmpty())
			throw new KNXException("search could not be started on any network interface");

		// XXX Java8ME better solution?
		final Runnable kill = new Runnable()
		{
			@Override
			public void run()
			{
				final int ms = timeout * 1000;
				logger.debug("kill thread is up and waiting for {} ms", ms);
				try {
					Thread.sleep(ms);
				} catch (final InterruptedException e) {}
				logger.debug("search timed out, kill all receivers");
				for (final ReceiverLoop l : rcv)
					l.quit();
			}
		};
		new Thread(kill).start();

		if (wait)
			try {
				for (final Iterator<ReceiverLoop> i = rcv.iterator(); i.hasNext();)
					join(i.next());
			}
			finally {
				stopSearch();
			}
	}

	/**
	 * Stops every search currently performed by this discoverer.
	 * <p>
	 * Already collected search responses from a search will not be removed.
	 */
	public final void stopSearch()
	{
		final ReceiverLoop[] loopers = receivers.toArray(new ReceiverLoop[receivers.size()]);
		for (int i = 0; i < loopers.length; i++) {
			final ReceiverLoop loop = loopers[i];
			loop.quit();
		}
		receivers.removeAll(Arrays.asList(loopers));
	}

	/**
	 * Returns <code>true</code> if a search is currently running.
	 * <p>
	 *
	 * @return a <code>boolean</code> showing the search state
	 */
	public final boolean isSearching()
	{
		return receivers.size() != 0;
	}

	/**
	 * Returns all collected search responses received by searches so far.
	 * <p>
	 * As long as searches are running, new responses might be added to the list of responses.
	 *
	 * @return list of results with {@link SearchResponse}s
	 * @see #stopSearch()
	 */
	public final List<Result<SearchResponse>> getSearchResponses()
	{
		return JavaME.unmodifiableList(responses);
	}

	/**
	 * Removes all search responses collected so far.
	 */
	public final void clearSearchResponses()
	{
		responses.clear();
	}

	/**
	 * Sends a description request to <code>server</code> and waits at most <code>timeout</code>
	 * seconds for the answer message to arrive.
	 *
	 * @param server the socket address of the server the description is requested from
	 * @param timeout time window in seconds to wait for answer message, 0 &lt; timeout &lt; (
	 *        {@link Integer#MAX_VALUE} / 1000)
	 * @return the result containing the description response message
	 * @throws KNXException on network I/O error
	 * @throws KNXTimeoutException if the timeout was reached before the description response
	 *         arrived
	 * @throws KNXInvalidResponseException if a received message from <code>server</code> does not
	 *         match the expected response
	 */
	public Result<DescriptionResponse> getDescription(final EndpointAddress server,
		final int timeout) throws KNXException
	{
		if (timeout <= 0 || timeout >= Integer.MAX_VALUE / 1000)
			throw new KNXIllegalArgumentException("timeout out of range");
		final int ms = timeout * 1000;
		try (final UDPDatagramConnection s = createSocket(true,
				EndpointAddress.newUdp(host.getHost(), port), null, server, false, ms)) {
			final byte[] buf = PacketHelper.toPacket(new DescriptionRequest(nat ? null
					: new HPAI(EndpointAddress.newUdp(s.getLocalAddress(), s.getLocalPort()), s
							.getLocalPort())));
			s.send(s.newDatagram(buf, buf.length, server.toAddress()));
			final ReceiverLoop looper = new ReceiverLoop(s, 256, ms, server);
			looper.loop();
			if (looper.thrown != null)
				throw looper.thrown;
			if (looper.res != null)
				return new Result<>(looper.res, host.getInterface(), host);
		}
		catch (final IOException e) {
			final String msg = "network failure on getting description";
			logger.error(msg, e);
			throw new KNXException(msg);
		}
		final String msg = "timeout, no description response received";
		logger.warn(msg);
		throw new KNXTimeoutException(msg);
	}

	/**
	 * Starts a search sending a search request message.
	 *
	 * @param localAddr local address to send search request from
	 * @param localPort local port to send search request from
	 * @param ni {@link NetworkInterface} used to send outgoing multicast, or <code>null</code> to
	 *        use the default multicast interface
	 * @param timeout timeout in seconds, timeout &ge; 0, 0 for an infinite time window
	 * @return the receiver thread for the search started
	 * @throws KNXException
	 */
	private ReceiverLoop search(final EndpointAddress localAddr, final int localPort,
		final NetworkInterface ni, final int timeout) throws KNXException
	{
		final UDPDatagramConnection s = createSocket(false, localAddr, ni, SYSTEM_SETUP_MULTICAST,
				mcast, timeout * 1000);
		// create a new socket address with the address from a, since the socket might
		// return the wildcard address for loopback or default host address; but port
		// is necessarily queried from socket since in a it might be 0 (for ephemeral port)
		final String nifName = ni != null ? ni.getName() + " " : "";
		try {
			logger.info("search on " + nifName
					+ EndpointAddress.newUdp(s.getLocalAddress(), s.getLocalPort()));

			// send out beyond local network
			if (mcast)
				((UDPMulticastConnection) s).setTimeToLive(64);

			// leave loopback behavior on while sending datagrams, because:
			// - some sockets don't support this option at all
			// - IP_MULTICAST_LOOP option applies to the datagram receive path on Windows,
			// but to the send path on unix platforms.
			// I love those platform dependencies... ;)
			//try {
			//	s.setLoopbackMode(true);
			//}
			//catch (SocketException ignore) {}

			// IP multicast responses MUST be forwarded by NAT without
			// modifications to IP/port, hence, we can safely state them in our HPAI
			final EndpointAddress res = mcast ? EndpointAddress.newUdp(SEARCH_MULTICAST,
					s.getLocalPort()) : nat ? null : EndpointAddress.newUdp(localAddr.getHost(),
					s.getLocalPort());
			final byte[] buf = PacketHelper.toPacket(new SearchRequest(res));
			s.send(s.newDatagram(buf, buf.length));
			synchronized (receivers) {
				final ReceiverLoop l = startReceiver(s, localAddr, timeout, nifName + localAddr);
				receivers.add(l);
				return l;
			}
		}
		catch (final IOException e) {
			// XXX Java8ME if leave is called on close, I can put this into try/w/res
			if (mcast)
				try {
					((UDPMulticastConnection) s).leave(SEARCH_MULTICAST);
				}
				catch (final IOException ignore) {}
			closeNoThrow(s);
			throw new KNXException("search request to " + SYSTEM_SETUP_MULTICAST + " failed on "
					+ localAddr + ":" + localPort + ", " + e.getMessage());
		}
	}

	// ni can be null to use default interface
	// timeout in ms
	private UDPDatagramConnection createSocket(final boolean unicast,
		final EndpointAddress bindAddr, final NetworkInterface ni, final EndpointAddress remote,
		final boolean mcastResponse, final int timeout) throws KNXException
	{
		UDPDatagramConnection s = null;
		// XXX Java8ME timeout param is a total, which does not really help: a socket will
		// always block timeout in the worst-case on send/receive, but we want to have a responsive
		// socket, try some ms and see how it works out
		final ConnectionOption<Integer> to = new ConnectionOption<>("Timeout", 500);
		try {
			if (unicast)
				return (UDPMulticastConnection) Connection.open(bindAddr, remote, to);

			final AccessPoint[] ap = EndpointAddress.getByNetworkInterface(ni);
			final AccessPoint local = ap.length > 0 ? ap[0] : null;
			final EndpointAddress ep = mcastResponse ? remote
					: EndpointAddress.newUdp(remote.getHost(), remote.getPort());
			s = (UDPDatagramConnection) Connection.open(local, ep, to);
		}
		catch (final IOException e) {
			final String msg = "failed to create socket on " + bindAddr;
			logger.warn(msg, e);
			throw new KNXException(msg + ", " + e.getMessage());
		}
		if (mcastResponse) {
			try {
				((UDPMulticastConnection) s).join(SYSTEM_SETUP_MULTICAST.toAddress());
			}
			catch (final IOException e) {
				closeNoThrow(s);
				final String msg = ni.getName() + ": joining group " + SYSTEM_SETUP_MULTICAST
						+ " failed";
				throw new KNXException(msg + ", " + e.getMessage());
			}
		}
		return s;
	}

	private void join(final ReceiverLoop l) throws InterruptedException
	{
		while (l.t.isAlive())
			l.t.join();
	}

	private void checkHost() throws KNXException
	{
		if (nat || host.getAddress().length == 4)
			return;
		final KNXException e = new KNXException(host + " is not an IPv4 address");
		logger.error("NAT not used, only IPv4 address support", e);
		throw e;
	}

	private void closeNoThrow(final UDPDatagramConnection c)
	{
		try {
			c.close();
		}
		catch (final IOException e) {
			logger.error("close", e);
		}
	}

	private ReceiverLoop startReceiver(final UDPDatagramConnection socket,
		final EndpointAddress addrOnNetIf, final int timeout, final String name) throws IOException
	{
		final ReceiverLoop looper = new ReceiverLoop(socket, addrOnNetIf, 256, timeout * 1000, name
				+ ":" + socket.getLocalPort());
		looper.t = new Thread(looper, "Discoverer " + name);
		// XXX Java8ME
//		looper.t.setDaemon(true);
		looper.t.start();
		return looper;
	}

	private final class ReceiverLoop extends UdpSocketLooper implements Runnable
	{
		private final boolean search;
		private final EndpointAddress server;
		private NetworkInterface nif;

		// used for search looper
		private Thread t;
		// we want this address to return it in a search result even if the socket was not bound
		private final EndpointAddress addrOnNetif;

		// used for description looper
		private DescriptionResponse res;
		private KNXInvalidResponseException thrown;
		private final String id;

		// use for search looper
		// timeout in milliseconds
		ReceiverLoop(final UDPDatagramConnection socket, final EndpointAddress addrOnNetIf,
			final int receiveBufferSize, final int timeout, final String name)
		{
			super(socket, true, receiveBufferSize, timeout);
			try {
				nif = Connection.getNetworkInterface(socket);
			}
			catch (final IOException e) {
				e.printStackTrace();
			}
			this.addrOnNetif = addrOnNetIf;
			search = true;
			server = null;
			id = name;
		}

		// use for description looper
		// timeout in milliseconds
		ReceiverLoop(final UDPDatagramConnection socket, final int receiveBufferSize,
			final int timeout, final EndpointAddress queriedServer) throws IOException
		{
			super(socket, true, receiveBufferSize, timeout);
			addrOnNetif = null;
			search = false;
			server = queriedServer;
			id = socket.getLocalAddress();
		}

		@Override
		public void run()
		{
			logger.trace("started on " + id);
			try {
				loop();
			}
			catch (final IOException e) {
				logger.error("while waiting for response", e);
			}
			logger.trace("stopped on " + id);
		}

		@Override
		public void quit()
		{
			if (search) {
				try {
					if (s instanceof UDPMulticastConnection)
						((UDPMulticastConnection) s).leave(SEARCH_MULTICAST);
				}
				catch (final IOException ignore) {}
				receivers.remove(this);
			}
			super.quit();
		}

		@Override
		public void onReceive(final EndpointAddress sender, final byte[] data, final int offset,
			final int length)
		{
			System.out.println("on receive from " + sender);
			try {
				final KNXnetIPHeader h = new KNXnetIPHeader(data, offset);
				if (h.getTotalLength() > length)
					logger.warn("ignore received packet from " + sender
							+ ", frame length does not match");
				else if (search && h.getServiceType() == KNXnetIPHeader.SEARCH_RES) {
					// if our search is still running, add response
					synchronized (receivers) {
						if (receivers.contains(this))
							responses.add(new Result<>(new SearchResponse(data, offset
									+ h.getStructLength()), nif, addrOnNetif));
					}
				}
				else if (!search && h.getServiceType() == KNXnetIPHeader.DESCRIPTION_RES) {
					if (sender.equals(server)) {
						try {
							res = new DescriptionResponse(data, offset + h.getStructLength());
						}
						catch (final KNXFormatException e) {
							logger.error("invalid description response", e);
							thrown = new KNXInvalidResponseException(e.getMessage());
						}
						finally {
							quit();
						}
					}
				}
			}
			catch (final KNXFormatException e) {
				final String item = e.getItem() != null ? " (" + e.getItem() + ")" : "";
				logger.info("ignore received packet from " + sender + ", " + e.getMessage() + item);
			}
		}
	}
}

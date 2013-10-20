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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXInvalidResponseException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.internal.UdpSocketLooper;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionRequest;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.SearchRequest;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Does KNXnet/IP discovery and retrieval of self description from other devices.
 * <p>
 * Both searches for server discovery and description requests can be run in blocking mode
 * or asynchronous in the background.<br>
 * This discoverer supports networks with routers doing network address translation.<br>
 * Requests for self description are sent using the UDP transport protocol.<br>
 * Due to protocol limitations, only IPv4 addresses are supported when network address
 * translation is <b>not</b> used. With NAT enabled, IPv6 addresses can be used as well.
 * <p>
 * A note on (not) using network address translation (NAT):<br>
 * If discovery or description attempts fail indicating a timeout limit, it might be
 * possible that NAT is used on routers while traversing the network, so the solution
 * would be to enable the use of NAT.<br>
 * On the other hand, if NAT is used but not supported by the answering device, no
 * response is received and a timeout will occur nevertheless.
 * <p>
 * For discovery searches, an additional option for choosing IP multicast over IP unicast
 * is available in {@link Discoverer#Discoverer(InetAddress, int, boolean, boolean)}. This
 * allows to select, whether search responses by answering servers are sent via multicast
 * and received by the local multicast socket, or if responses are sent via unicast using
 * the local host and port as address for the reply. Using multicast response is of
 * advantage in case KNXnet/IP servers are located in different subnetworks.
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
	 * <p>
	 */
	public static final String LOG_SERVICE = "Discoverer";

	/**
	 * Multicast IP address used for discovery, multicast group is {@value}.
	 */
	public static final String SEARCH_MULTICAST = "224.0.23.12";

	/**
	 * Port number used for discovery, port is {@value}.
	 */
	public static final int SEARCH_PORT = KNXnetIPConnection.DEFAULT_PORT;

	static final InetAddress SYSTEM_SETUP_MULTICAST;

	private static final LogService logger = LogManager.getManager().getLogService(LOG_SERVICE);

	// local host/port
	private final InetAddress host;
	private final int port;
	// is our discovery/description aware of network address translation
	private final boolean nat;
	// is our search set up for multicast responses
	// for implications on forwarding multicast through NAT inside/outside interfaces,
	// see also RFC 5135
	private final boolean mcast;

	private final List receivers = Collections.synchronizedList(new ArrayList());
	private final List responses = Collections.synchronizedList(new ArrayList());

	static {
		InetAddress a = null;
		try {
			a = InetAddress.getByName(SEARCH_MULTICAST);
		}
		catch (final UnknownHostException e) {
			logger.fatal("on resolving system setup multicast " + SEARCH_MULTICAST, e);
		}
		SYSTEM_SETUP_MULTICAST = a;
	}

	/**
	 * Creates a new Discoverer.
	 * <p>
	 * Network address translation:<br>
	 * If subsequent discovery or description attempts fail indicating a timeout limit, it
	 * might be possible that network address translation (NAT) is used on routers while
	 * traversing the network (besides the other reason that timeouts are too short). This
	 * would effectively stop any communication done in the standard way, due to the way
	 * the HPAI structure is used by default.<br>
	 * Setting the parameter for indicating use of NAT to <code>true</code> takes account
	 * of such routers, leading to the desired behavior.
	 * 
	 * @param localPort the port number used to bind a socket, a valid port is in the
	 *        range of 1 to 65535, or use 0 to pick an arbitrary unused (ephemeral) port.
	 *        Note that a specified valid port does not ensure a successful bind in
	 *        subsequent discoverer operations due to operating system dependencies.
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
	 * The <code>localHost</code> is used to specify a particular local host address, used
	 * as response destination address when doing discovery / description. By default, the
	 * local host is used as obtained by {@link InetAddress#getLocalHost()}. The returned
	 * address is quite system dependent and might not always be useful in some
	 * situations. So it can be overruled specifying a local host address using this
	 * constructor.
	 * 
	 * @param localHost local host address used for discovery/description responses, or
	 *        <code>null</code> for the default local host
	 * @param localPort the port number used to bind a socket, a valid port is in the
	 *        range of 1 to 65535, or use 0 to pick an arbitrary unused (ephemeral) port.
	 *        Note that a specified valid port does not ensure a successful bind in
	 *        subsequent discoverer operations due to operating system dependencies.
	 * @param natAware <code>true</code> to use a NAT (network address translation) aware
	 *        discovery/description mechanism, <code>false</code> to use the default way
	 * @param mcastResponse set <code>true</code> to use multicasting for search responses
	 *        in KNXnet/IP discovery, <code>false</code> to use unicast for search
	 *        responses to this local host and port address
	 * @throws KNXException if local host can't be used
	 */
	public Discoverer(final InetAddress localHost, final int localPort, final boolean natAware,
		final boolean mcastResponse) throws KNXException
	{
		if (localPort < 0 || localPort > 0xFFFF)
			throw new KNXIllegalArgumentException("port out of range [0..0xFFFF]");
		if (localHost == null)
			try {
				host = InetAddress.getLocalHost();
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
	 * Starts a new search for KNXnet/IP discovery, the network interface can be
	 * specified.
	 * <p>
	 * The search will continue for <code>timeout</code> seconds, or infinite if timeout
	 * value is zero. During this time, search responses will get collected asynchronous
	 * in the background by this {@link Discoverer}.<br>
	 * With <code>wait</code> you can force this method into blocking mode to wait until
	 * the search finished, otherwise the method returns with the search running in the
	 * background.<br>
	 * A search is finished if either the <code>timeout</code> was reached or the
	 * background receiver stopped.<br>
	 * 
	 * @param ni the {@link NetworkInterface} used for sending outgoing multicast
	 *        messages, or <code>null</code> to use the default multicast interface
	 * @param timeout time window in seconds during which search response messages will
	 *        get collected, timeout >= 0. If timeout is zero, no timeout is set, the
	 *        search has to be stopped with {@link #stopSearch()}.
	 * @param wait <code>true</code> to block until end of search before return
	 * @throws KNXException on network I/O error
	 * @throws InterruptedException if search was interrupted in blocking mode before the
	 *         specified timeout was reached; the search is stopped before passing this
	 *         exception back to the caller
	 * @see MulticastSocket
	 * @see NetworkInterface
	 */
	public void startSearch(final NetworkInterface ni, final int timeout, final boolean wait)
		throws KNXException, InterruptedException
	{
		startSearch(port, ni, timeout, wait);
	}

	/**
	 * Starts a new search for KNXnet/IP discovery, the <code>localPort</code> and network
	 * interface can be specified.
	 * <p>
	 * See documentation for method {@link #startSearch(NetworkInterface, int, boolean)}.<br>
	 * To distinguish between search responses if a user started two or more searches
	 * running concurrently, this method allows to specify a dedicated
	 * <code>localPort</code>, not using the port set with
	 * {@link #Discoverer(int, boolean)}.
	 * 
	 * @param localPort the port used to bind the socket, a valid port is 0 to 65535, if
	 *        <code>localPort</code> is zero an arbitrary unused (ephemeral) port is
	 *        picked
	 * @param ni the {@link NetworkInterface} used for sending outgoing multicast
	 *        messages, or <code>null</code> to use the default multicast interface
	 * @param timeout time window in seconds during which search response messages will
	 *        get collected, <code>timeout >= 0</code>. If timeout is zero, no timeout is
	 *        set, the search has to be stopped with {@link #stopSearch()}.
	 * @param wait <code>true</code> to block until end of search before return
	 * @throws KNXException on network I/O error
	 * @throws InterruptedException if search was interrupted in blocking mode before the
	 *         specified timeout was reached; the search is stopped before passing this
	 *         exception back to the caller
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
	 * The search will continue for <code>timeout</code> seconds, or infinite if timeout
	 * value is zero. During this time, search responses will get collected asynchronous
	 * in the background by this Discoverer.<br>
	 * With <code>wait</code> you can force this method into blocking mode to wait until
	 * the search finished, otherwise the method returns with the search running in the
	 * background.<br>
	 * A search has finished if either the <code>timeout</code> was reached, all
	 * background receivers stopped (all responses received) or {@link #stopSearch()} was
	 * invoked.
	 * 
	 * @param timeout time window in seconds during which search response messages will
	 *        get collected, timeout >= 0. If timeout is 0, no timeout is set, the search
	 *        has to be stopped with <code>stopSearch</code>.
	 * @param wait <code>true</code> to block until end of search before return
	 * @throws KNXException on network I/O error
	 * @throws InterruptedException if search was interrupted in blocking mode before the
	 *         specified timeout was reached; the search is stopped before passing this
	 *         exception back to the caller
	 */
	public void startSearch(final int timeout, final boolean wait) throws KNXException,
		InterruptedException
	{
		if (timeout < 0)
			throw new KNXIllegalArgumentException("timeout has to be >= 0");
		final Enumeration eni;
		try {
			eni = NetworkInterface.getNetworkInterfaces();
		}
		catch (final SocketException e) {
			logger.error("failed to get network interfaces", e);
			throw new KNXException("network interface error: " + e.getMessage());
		}
		if (eni == null) {
			logger.error("no network interfaces found");
			throw new KNXException("no network interfaces found");
		}
		final List rcv = new ArrayList();
		// loopback flag, so we start at most one local search
		boolean lo = false;
		while (eni.hasMoreElements()) {
			final NetworkInterface ni = (NetworkInterface) eni.nextElement();
			// find one IP address we can use for our search on this interface
			for (final Enumeration ea = ni.getInetAddresses(); ea.hasMoreElements();) {
				final InetAddress a = (InetAddress) ea.nextElement();
				// without NAT, we only try IPv4 addresses
				if (!nat && a.getAddress().length != 4)
					logger.info("skipped " + a + ", not an IPv4 address");
				else
					try {
						if (!(lo && a.isLoopbackAddress()))
							rcv.add(search(a, port, ni, timeout));
						if (a.isLoopbackAddress())
							lo = true;
						else
							break;
					}
					catch (final KNXException ignore) {}
			}
		}
		if (rcv.size() == 0)
			throw new KNXException("search could not be started on any network interface");
		if (wait)
			try {
				for (final Iterator i = rcv.iterator(); i.hasNext();)
					join((ReceiverLoop) i.next());
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
		final ReceiverLoop[] loopers = (ReceiverLoop[]) receivers
				.toArray(new ReceiverLoop[receivers.size()]);
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
	 * As long as searches are running, new responses might be added to the list of
	 * responses.
	 * 
	 * @return array of {@link SearchResponse}s
	 * @see #stopSearch()
	 */
	public final SearchResponse[] getSearchResponses()
	{
		return (SearchResponse[]) responses.toArray(new SearchResponse[responses.size()]);
	}

	/**
	 * Removes all search responses collected so far.
	 * <p>
	 */
	public final void clearSearchResponses()
	{
		responses.clear();
	}

	/**
	 * Sends a description request to <code>server</code> and waits at most
	 * <code>timeout</code> seconds for the answer message to arrive.
	 * <p>
	 * 
	 * @param server the socket address of the server the description is requested from
	 * @param timeout time window in seconds to wait for answer message, 0 &lt; timeout
	 *        &lt; ({@link Integer#MAX_VALUE} / 1000)
	 * @return the description response message
	 * @throws KNXException on network I/O error
	 * @throws KNXTimeoutException if the timeout was reached before the description
	 *         response arrived
	 * @throws KNXInvalidResponseException if a received message from <code>server</code>
	 *         does not match the expected response
	 */
	public DescriptionResponse getDescription(final InetSocketAddress server, final int timeout)
		throws KNXException
	{
		if (timeout <= 0 || timeout >= Integer.MAX_VALUE / 1000)
			throw new KNXIllegalArgumentException("timeout out of range");
		final DatagramSocket s = createSocket(true, host, port, null, false);
		try {
			final byte[] buf = PacketHelper.toPacket(new DescriptionRequest(nat ? null
					: (InetSocketAddress) s.getLocalSocketAddress()));
			s.send(new DatagramPacket(buf, buf.length, server));
			final ReceiverLoop looper = new ReceiverLoop(s, 256, timeout * 1000,
				server);
			looper.loop();
			if (looper.thrown != null)
				throw looper.thrown;
			if (looper.res != null)
				return looper.res;
		}
		catch (final IOException e) {
			final String msg = "network failure on getting description";
			logger.error(msg, e);
			throw new KNXException(msg);
		}
		finally {
			s.close();
		}
		final String msg = "timeout, no description response received";
		logger.warn(msg);
		throw new KNXTimeoutException(msg);
	}

	/**
	 * Starts a search sending a search request message.
	 * <p>
	 * 
	 * @param localAddr local address to send search request from
	 * @param localPort local port to send search request from
	 * @param ni {@link NetworkInterface} used to send outgoing multicast, or
	 *        <code>null</code> to use the default multicast interface
	 * @param timeout timeout in seconds, timeout >= 0, 0 for an infinite time window
	 * @return the receiver thread for the search started
	 * @throws KNXException
	 */
	private ReceiverLoop search(final InetAddress localAddr, final int localPort,
		final NetworkInterface ni, final int timeout) throws KNXException
	{
		final MulticastSocket s = createSocket(false, localAddr, localPort, ni, mcast);
		// create a new socket address with the address from a, since the socket might
		// return the wildcard address for loopback or default host address; but port
		// is necessarily queried from socket since in a it might be 0 (for ephemeral port)
		final String nifName = ni != null ? ni.getName() + " " : "";
		logger.info("search on " + nifName + new InetSocketAddress(localAddr, s.getLocalPort()));
		try {
			// send out beyond local network
			s.setTimeToLive(64);

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
			final InetSocketAddress res = mcast ? new InetSocketAddress(SYSTEM_SETUP_MULTICAST,
					s.getLocalPort()) : nat ? null : new InetSocketAddress(localAddr,
					s.getLocalPort());
			final byte[] buf = PacketHelper.toPacket(new SearchRequest(res));
			s.send(new DatagramPacket(buf, buf.length, SYSTEM_SETUP_MULTICAST, SEARCH_PORT));
			synchronized (receivers) {
				final ReceiverLoop l = startReceiver(s, timeout,
						nifName + localAddr.getHostAddress());
				receivers.add(l);
				return l;
			}
		}
		catch (final IOException e) {
			if (mcast)
				try {
					s.leaveGroup(new InetSocketAddress(SYSTEM_SETUP_MULTICAST, 0), null);
				}
				catch (final IOException ignore) {}
			s.close();
			logger.warn("failure sending search request on " + localAddr + ":" + localPort, e);
			throw new KNXException("search request failed, " + e.getMessage());
		}
	}

	// ni can be null to use default interface
	private MulticastSocket createSocket(final boolean unicast, final InetAddress bindAddr,
		final int bindPort, final NetworkInterface ni, final boolean mcastResponse)
		throws KNXException
	{
		MulticastSocket s = null;
		try {
			if (unicast)
				return new MulticastSocket(new InetSocketAddress(bindAddr, bindPort));
			s = new MulticastSocket(mcastResponse ? SEARCH_PORT : bindPort);
			if (ni != null)
				s.setNetworkInterface(ni);
		}
		catch (final IOException e) {
			final String msg = "failed to create socket on " + bindAddr + ":" + bindPort;
			logger.warn(msg, e);
			throw new KNXException(msg + ", " + e.getMessage());
		}
		if (mcastResponse) {
			try {
				if (ni != null)
					s.joinGroup(new InetSocketAddress(SYSTEM_SETUP_MULTICAST, 0), ni);
				else
					s.joinGroup(SYSTEM_SETUP_MULTICAST);
			}
			catch (final IOException e) {
				s.close();
				final String msg = "joining group " + SYSTEM_SETUP_MULTICAST + " failed";
				logger.error(msg, e);
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
		final KNXException e = new KNXException(host.getHostAddress() + " is not an IPv4 address");
		logger.error("NAT not used, only IPv4 address support", e);
		throw e;
	}

	private ReceiverLoop startReceiver(final MulticastSocket socket, final int timeout,
		final String name)
	{
		final ReceiverLoop looper = new ReceiverLoop(socket, 256, timeout * 1000, name + ":"
				+ socket.getLocalPort());
		looper.t = new Thread(looper, "Discoverer " + name);
		looper.t.setDaemon(true);
		looper.t.start();
		return looper;
	}

	private final class ReceiverLoop extends UdpSocketLooper implements Runnable
	{
		private final boolean search;
		private final InetSocketAddress server;
		// used for search looper
		private Thread t;
		// used for description looper
		private DescriptionResponse res;
		private KNXInvalidResponseException thrown;
		private final String id;
		
		// use for search looper
		// timeout in milliseconds
		ReceiverLoop(final DatagramSocket socket, final int receiveBufferSize,
			final int timeout, final String name)
		{
			super(socket, true, receiveBufferSize, 0, timeout);
			search = true;
			server = null;
			id = name;
		}

		// use for description looper
		// timeout in milliseconds
		ReceiverLoop(final DatagramSocket socket, final int receiveBufferSize, final int timeout,
			final InetSocketAddress queriedServer)
		{
			super(socket, true, receiveBufferSize, 0, timeout);
			search = false;
			server = queriedServer;
			id = "" + socket.getLocalSocketAddress();
		}

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

		public void quit()
		{
			if (search) {
				try {
					((MulticastSocket) s).leaveGroup(new InetSocketAddress(SYSTEM_SETUP_MULTICAST,
							0), null);
				}
				catch (final IOException ignore) {}
				receivers.remove(this);
			}
			super.quit();
		}

		public void onReceive(final InetSocketAddress source, final byte[] data, final int offset,
			final int length)
		{
			try {
				final KNXnetIPHeader h = new KNXnetIPHeader(data, offset);
				if (h.getTotalLength() > length)
					logger.warn("ignore received packet from " + source
						+ ", frame length does not match");
				else if (search && h.getServiceType() == KNXnetIPHeader.SEARCH_RES) {
					// if our search is still running, add response
					synchronized (receivers) {
						if (receivers.contains(this))
							responses.add(new SearchResponse(data, offset + h.getStructLength()));
					}
				}
				else if (!search && h.getServiceType() == KNXnetIPHeader.DESCRIPTION_RES) {
					if (source.equals(server)) {
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
				logger.info("ignore received packet from " + source + ", " + e.getMessage() + item);
			}
		}
	}
}

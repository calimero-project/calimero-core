/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2018 B. Malinowsky

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

import static tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode.NonBlocking;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.RoutingBusy;
import tuwien.auto.calimero.knxnetip.servicetype.RoutingIndication;
import tuwien.auto.calimero.knxnetip.servicetype.RoutingLostMessage;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.log.LogService.LogLevel;

/**
 * KNXnet/IP connection using the KNXnet/IP routing protocol.
 * <p>
 * A KNXnet/IP router is a fast replacement for line/backbone couplers and connected
 * main/backbone lines, using Ethernet cabling for example.<br>
 * The router use point to multipoint communication (multicast). By default, routers are
 * joined to the {@link KNXnetIPRouting#DEFAULT_MULTICAST} multicast group. On more KNX
 * installations in one IP network, different multicast addresses have to be assigned.<br>
 * All IP datagrams use UDP port number 3671, and only datagrams on this port are
 * observed.<br>
 * The routing protocol is an unconfirmed service.
 * <p>
 * Optionally, a listener of type {@link RoutingListener} can be supplied to
 * {@link #addConnectionListener(KNXListener)} instead of a default {@link KNXListener},
 * to receive {@link RoutingListener#lostMessage(LostMessageEvent)} notifications.
 * <p>
 * Multicast considerations:<br>
 * The multicast loopback behavior defines whether multicast datagrams are looped back to
 * the local socket, see {@link MulticastSocket#setLoopbackMode(boolean)}. By default, the
 * loopback mode of the multicast socket used for sending multicast datagrams is enabled.
 * This behavior can be changed by using a KNXnetIPRouting sub type and initializing it by
 * calling {@link #init(NetworkInterface, boolean, boolean)}.
 * <p>
 * A multicast datagram sent with an initial hop count greater 1 may be delivered to the
 * sending host on a different interface (than the sending one), if the host is a member
 * of the multicast group on that interface. The loopback mode setting of the sender's
 * socket has no effect on this behavior.
 *
 * @author B. Malinowsky
 */
public class KNXnetIPRouting extends ConnectionBase
{
	/**
	 * Multicast address assigned by default to KNXnet/IP routers, address {@value
	 * #DEFAULT_MULTICAST}.
	 * <p>
	 * This is the standard system setup multicast address used in KNXnet/IP.
	 */
	public static final String DEFAULT_MULTICAST = Discoverer.SEARCH_MULTICAST;

	// newer Gira servers have a "reliable communication" option, which uses the
	// following unsupported service type; not part of the knx spec
	private static final int GiraUnsupportedSvcType = 0x538;
	// we will only warn about it once, to avoid spamming the log
	private boolean loggedGiraUnsupportedSvcType;

	private final InetAddress multicast;

	private volatile boolean loopbackEnabled;
	// This list is used for multicast packets that are looped back in loopback mode. If loopback
	// mode is enabled, sent packets are buffered, and subsequently discarded when received again
	// shortly after (and also removed from this buffer again).
	// This list holds cEMI frames, but basically only the byte array representations are required.
	private final List<CEMILData> loopbackFrames = new ArrayList<>();
	private static final int maxLoopbackQueueSize = 20;


	/**
	 * Creates a new KNXnet/IP routing service.
	 * <p>
	 * In general, routers are assigned a multicast address by adding an offset to the
	 * system setup multicast address ({@value #DEFAULT_MULTICAST}) for each KNX
	 * installation, by default this offset is 0 (i.e., only one used installation).
	 *
	 * @param netIf specifies the local network interface used to join the multicast group
	 *        and send outgoing multicast data, use <code>null</code> to use the default
	 *        interface; useful for multi-homed hosts
	 * @param mcGroup address of the multicast group this router is joined to, or
	 *        <code>null</code> to use the default multicast ({@value #DEFAULT_MULTICAST}
	 *        ); value of <code>mcGroup &ge; </code>{@value #DEFAULT_MULTICAST}
	 * @throws KNXException on socket error, or if joining to group failed
	 */
	public KNXnetIPRouting(final NetworkInterface netIf, final InetAddress mcGroup) throws KNXException
	{
		this(mcGroup);
		init(netIf, true, true);
	}

	/**
	 * Use this constructor in case initialization is called separately at a later point
	 * (using {@link #init(NetworkInterface, boolean, boolean)}).
	 * <p>
	 *
	 * @param mcGroup see {@link #KNXnetIPRouting(NetworkInterface, InetAddress)}
	 */
	protected KNXnetIPRouting(final InetAddress mcGroup)
	{
		super(KNXnetIPHeader.ROUTING_IND, 0, 1, 0);
		if (mcGroup == null)
			multicast = Discoverer.SYSTEM_SETUP_MULTICAST;
		else if (!isValidRoutingMulticast(mcGroup))
			throw new KNXIllegalArgumentException("non-valid routing multicast " + mcGroup);
		else
			multicast = mcGroup;
	}

	/**
	 * Sends a cEMI frame to the joined multicast group.
	 * <p>
	 *
	 * @param frame cEMI message to send
	 * @param mode arbitrary value, does not influence behavior, since routing is always a
	 *        unconfirmed, nonblocking service
	 */
	@Override
	public void send(final CEMI frame, final BlockingMode mode) throws KNXConnectionClosedException
	{
		try {
			if (loopbackEnabled) {
				synchronized (loopbackFrames) {
					loopbackFrames.add((CEMILData) frame);
				}
				logger.trace("add to multicast loopback frame buffer: {}", frame);
			}
			super.send(frame, NonBlocking);
			// we always succeed...
			setState(OK);
		}
		catch (final KNXTimeoutException ignore) {}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public final void send(final RoutingBusy busy) throws KNXConnectionClosedException
	{
		send(PacketHelper.toPacket(busy));
	}

	@Override
	public String getName()
	{
		return "KNXnet/IP Routing " + super.getName();
	}

	/**
	 * Sets the default hop count (TTL) used in the IP header of encapsulated cEMI
	 * messages.
	 * <p>
	 * This value is used to limit the multicast geographically, although this is just a
	 * rough estimation. The hop count value is forwarded to the underlying multicast
	 * socket used for communication.
	 *
	 * @param hopCount hop count value, 0 &lt;= value &lt;= 255
	 */
	public final void setHopCount(final int hopCount)
	{
		if (hopCount < 0 || hopCount > 255)
			throw new KNXIllegalArgumentException("hop count out of range");
		try {
			((MulticastSocket) socket).setTimeToLive(hopCount);
		}
		catch (final IOException e) {
			logger.error("failed to set hop count", e);
		}
	}

	/**
	 * Returns the default hop count (TTL) used in the IP header of encapsulated cEMI
	 * messages.
	 * <p>
	 * The hop count value is queried from the used multicast socket.
	 *
	 * @return hop count in the range 0 to 255
	 */
	public final int getHopCount()
	{
		try {
			return ((MulticastSocket) socket).getTimeToLive();
		}
		catch (final IOException e) {
			logger.error("failed to get hop count", e);
		}
		return 1;
	}

	/**
	 * Returns whether this KNXnet/IP routing instance has local loopback of KNXnet/IP
	 * routing multicast datagrams enabled or not.
	 *
	 * @return <code>true</code> if loopback is used, <code>false</code> otherwise
	 */
	public final boolean usesMulticastLoopback()
	{
		try {
			return !((MulticastSocket) socket).getLoopbackMode();
		}
		catch (final SocketException e) {
			// if we can't access loopback mode, we assume that we also couldn't set it
			// during initialization; therefore, return socket defaults
			return true;
		}
	}

	/**
	 * Checks whether the supplied IP address is a valid KNX routing multicast address.
	 * <p>
	 *
	 * @param address the IP address to check
	 * @return <code>true</code> if address qualifies as KNX multicast, <code>false</code>
	 *         otherwise
	 */
	public static boolean isValidRoutingMulticast(final InetAddress address)
	{
		return address != null && address.isMulticastAddress()
				&& toLong(address) >= toLong(Discoverer.SYSTEM_SETUP_MULTICAST);
	}

	/**
	 * Initialization routine to set the multicast network interface, loopback behavior
	 * and receiver loop.
	 * <p>
	 * Call this method only once during initialization.
	 *
	 * @param netIf see {@link #KNXnetIPRouting(NetworkInterface, InetAddress)}
	 * @param useMulticastLoopback <code>true</code> to loopback multicast packets to the
	 *        local socket, <code>false</code> otherwise; this parameter is only
	 *        interpreted as a hint by the operating system
	 * @param startReceiver <code>true</code> to start a threaded receiver loop which
	 *        dispatches to
	 *        {@link #handleServiceType(KNXnetIPHeader, byte[], int, InetAddress, int)},
	 *        <code>false</code> if received socket datagrams are handled by other means
	 * @throws KNXException on failed creation or initialization of multicast socket
	 */
	protected void init(final NetworkInterface netIf, final boolean useMulticastLoopback,
		final boolean startReceiver) throws KNXException
	{
		ctrlEndpt = new InetSocketAddress(multicast, DEFAULT_PORT);
		dataEndpt = ctrlEndpt;
		logger = LogService.getLogger("calimero.knxnetip." + getName());

		MulticastSocket s = null;
		try {
			s = new MulticastSocket(DEFAULT_PORT);
			if (netIf != null) {
				s.setNetworkInterface(netIf);
				// port number is not used in join group
				s.joinGroup(new InetSocketAddress(multicast, 0), netIf);
				logger.info("using network interface {}", netIf.getName());
			}
			else {
				try {
					// On OSX, joinGroup(multicast) is likely to fail if no network interface was
					// set. With the following being the first join call, it will not fail, even
					// if no network interface was set
					s.joinGroup(new InetSocketAddress(multicast, 0), null);
				}
				catch (final IOException e) {
					// Try the simple call as backup, as it works without problem on Linux/Windows
					s.joinGroup(multicast);
				}
			}
			// send out beyond local network
			s.setTimeToLive(16);
		}
		catch (final IOException e) {
			if (s != null)
				s.close();
			throw new KNXException(
					"initializing multicast socket (group " + multicast.getHostAddress() + "): " + e.getMessage(), e);
		}
		try {
			if (!useMulticastLoopback)
				s.setLoopbackMode(true);
			loopbackEnabled = !s.getLoopbackMode();
			logger.info("multicast loopback mode " + (loopbackEnabled ? "enabled" : "disabled"));
		}
		catch (final SocketException e) {
			logger.warn("failed to access multicast loopback mode, " + e.getMessage());
		}

		socket = s;
		if (startReceiver)
			startReceiver();
		setState(OK);
	}

	@Override
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data,
		final int offset, final InetAddress src, final int port)
		throws KNXFormatException, IOException
	{
		final int svc = h.getServiceType();
		if (h.getVersion() != KNXNETIP_VERSION_10)
			close(CloseEvent.INTERNAL, "protocol version changed", LogLevel.ERROR, null);
		else if (svc == KNXnetIPHeader.ROUTING_IND) {
			final RoutingIndication ind = new RoutingIndication(data, offset, h.getTotalLength()
					- h.getStructLength());
			final CEMI frame = ind.getCEMI();
			if (discardLoopbackFrame(frame))
				return true;
			fireFrameReceived(frame);
		}
		else if (svc == KNXnetIPHeader.ROUTING_LOST_MSG) {
			final RoutingLostMessage lost = new RoutingLostMessage(data, offset);
			fireLostMessage(new InetSocketAddress(src, port), lost);
		}
		else if (svc == KNXnetIPHeader.ROUTING_BUSY) {
			final RoutingBusy busy = new RoutingBusy(data, offset);
			fireRoutingBusy(new InetSocketAddress(src, port), busy);
		}
		else if (svc == GiraUnsupportedSvcType) {
			if (!loggedGiraUnsupportedSvcType)
				logger.warn("received unsupported Gira-specific service type 0x538, will be silently ignored: {}",
						DataUnitBuilder.toHex(data, " "));
			loggedGiraUnsupportedSvcType = true;
		}
		else if (svc != KNXnetIPHeader.SEARCH_REQ && svc != KNXnetIPHeader.SEARCH_RES
				&& svc != KNXnetIPHeader.SearchRequest && svc != KNXnetIPHeader.SearchResponse)

			// silently ignore multicast packets from searches,
			// to avoid logged warnings about unknown frames
			return super.handleServiceType(h, data, offset, src, port);
		return true;
	}

	@Override
	protected void close(final int initiator, final String reason, final LogLevel level,
		final Throwable t)
	{
		synchronized (this) {
			if (closing > 0)
				return;
			closing = 1;
		}

		LogService.log(logger, level, "close connection - " + reason, t);
		try {
			((MulticastSocket) socket).leaveGroup(multicast);
		}
		catch (final IOException e) {
			logger.debug("problem leaving multicast group {} ({})", multicast.getHostAddress(), e.toString());
		}
		finally {
			stopReceiver();
			socket.close();
			cleanup(initiator, reason, level, t);
		}
	}

	protected void send(final byte[] packet) throws KNXConnectionClosedException
	{
		final int state = getState();
		if (state == CLOSED) {
			logger.warn("send invoked on closed connection - aborted");
			throw new KNXConnectionClosedException("connection closed");
		}
		if (state < 0) {
			logger.error("send invoked in error state " + state + " - aborted");
			throw new IllegalStateException("in error state, send aborted");
		}
		try {
			final DatagramPacket p = new DatagramPacket(packet, packet.length, dataEndpt.getAddress(),
					dataEndpt.getPort());
			socket.send(p);
			setState(OK);
		}
		catch (final InterruptedIOException e) {
			close(CloseEvent.USER_REQUEST, "interrupted", LogLevel.WARN, e);
			Thread.currentThread().interrupt();
			throw new KNXConnectionClosedException("interrupted connection got closed");
		}
		catch (final IOException e) {
			close(CloseEvent.INTERNAL, "communication failure", LogLevel.ERROR, e);
			throw new KNXConnectionClosedException("connection closed");
		}
	}

	private void fireLostMessage(final InetSocketAddress sender, final RoutingLostMessage lost)
	{
		final LostMessageEvent e = new LostMessageEvent(this, sender, lost.getDeviceState(), lost.getLostMessages());
		listeners.fire(l -> {
			if (l instanceof RoutingListener)
				((RoutingListener) l).lostMessage(e);
		});
	}

	private void fireRoutingBusy(final InetSocketAddress sender, final RoutingBusy busy)
	{
		final RoutingBusyEvent e = new RoutingBusyEvent(this, sender, busy);
		listeners.fire(l -> {
			if (l instanceof RoutingListener)
				((RoutingListener) l).routingBusy(e);
		});
	}

	private boolean discardLoopbackFrame(final CEMI frame)
	{
		if (!loopbackEnabled)
			return false;
		final byte[] a = frame.toByteArray();
		synchronized (loopbackFrames) {
			for (final Iterator<CEMILData> i = loopbackFrames.iterator(); i.hasNext();) {
				if (Arrays.equals(a, i.next().toByteArray())) {
					i.remove();
					logger.trace("discard multicast loopback cEMI frame: {}", frame);
					return true;
				}
				// remove oldest entry if exceeding max. loopback queue size
				if (loopbackFrames.size() > maxLoopbackQueueSize)
					i.remove();
			}
		}
		return false;
	}

	private static long toLong(final InetAddress addr)
	{
		// we assume 4 byte Internet address for multicast
		final byte[] buf = addr.getAddress();
		long ret = buf[3] & 0xffL;
		ret |= (buf[2] << 8) & 0xff00L;
		ret |= (buf[1] << 16) & 0xff0000L;
		ret |= (buf[0] << 24) & 0xff000000L;
		return ret;
	}
}

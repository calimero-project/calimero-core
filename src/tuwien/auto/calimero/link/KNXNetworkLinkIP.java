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

package tuwien.auto.calimero.link;

import static tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode.WaitForAck;
import static tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode.WaitForCon;
import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.LinkLayer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.KNXConnectionClosedException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt;
import tuwien.auto.calimero.knxnetip.KNXnetIPRouting;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

/**
 * Implementation of the KNX network link based on the KNXnet/IP protocol, using a {@link KNXnetIPConnection}.
 * <p>
 * Once a link has been closed, it is not available for further link communication, i.e. it can't be reopened.
 * <p>
 * If KNXnet/IP routing is used as base protocol, the send methods with wait for confirmation behave equally like
 * without wait specified, since routing is an unconfirmed protocol. This implies that no confirmation frames are
 * generated, thus {@link NetworkLinkListener#confirmation(FrameEvent)} is not used.
 * <p>
 * IP address considerations:<br>
 * On more IP addresses assigned to the local host (on possibly several local network interfaces), the default chosen
 * local host address can differ from the expected. In this situation, the local endpoint has to be specified manually
 * during instantiation.<br>
 * Network Address Translation (NAT) aware communication can only be used, if the KNXnet/IP server of the remote
 * endpoint supports it. Otherwise, connection timeouts will occur. With NAT enabled, KNXnet/IP accepts IPv6 addresses.
 * By default, the KNXnet/IP protocol only works with IPv4 addresses.
 *
 * @author B. Malinowsky
 */
public class KNXNetworkLinkIP extends AbstractLink<KNXnetIPConnection>
{
	/**
	 * KNXnet/IP system setup multicast address, KNXnet/IP routers are by default members of that multicast group.
	 */
	public static final InetAddress DefaultMulticast;
	static {
		try {
			DefaultMulticast = InetAddress.getByName("224.0.23.12");
		}
		catch (final UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Service mode for link layer tunneling.
	 */
	protected static final int TUNNELING = 1;

	/**
	 * Service mode for routing.
	 */
	protected static final int ROUTING = 2;

	private final int mode;

	private KNXnetIPDevMgmt mgmt;

	/**
	 * Creates a new network link using KNXnet/IP tunneling (internally using a {@link KNXnetIPConnection}) to a remote
	 * KNXnet/IP server endpoint.
	 *
	 * @param localEP the local control endpoint of the link to use, supply the wildcard address to use a local IP on
	 *        the same subnet as <code>remoteEP</code> and an ephemeral port number
	 * @param remoteEP the remote endpoint of the link to communicate with; this is the KNXnet/IP server control
	 *        endpoint
	 * @param useNAT <code>true</code> to use network address translation (NAT) in tunneling service mode,
	 *        <code>false</code> to use the default (non aware) mode
	 * @param settings medium settings defining device and KNX medium specifics for communication
	 * @return the network link in open state
	 * @throws KNXException on failure establishing link using the KNXnet/IP connection
	 * @throws InterruptedException on interrupted thread while establishing link
	 */
	public static KNXNetworkLinkIP newTunnelingLink(final InetSocketAddress localEP, final InetSocketAddress remoteEP,
		final boolean useNAT, final KNXMediumSettings settings) throws KNXException, InterruptedException
	{
		return new KNXNetworkLinkIP(TUNNELING, localEP, remoteEP, useNAT, settings);
	}

	/**
	 * Creates a new network link using the {@link KNXnetIPRouting} protocol, with the local endpoint specified by a
	 * network interface.
	 *
	 * @param netIf local network interface used to join the multicast group and for sending, use <code>null</code> for
	 *        the host's default multicast interface
	 * @param mcGroup address of the multicast group to join, use {@link #DefaultMulticast} for the default KNX IP
	 *        multicast address
	 * @param settings medium settings defining device and medium specifics needed for communication
	 * @return the network link in open state
	 * @throws KNXException on failure establishing link using the KNXnet/IP connection
	 */
	public static KNXNetworkLinkIP newRoutingLink(final NetworkInterface netIf, final InetAddress mcGroup,
		final KNXMediumSettings settings) throws KNXException
	{
		return new KNXNetworkLinkIP(ROUTING, new KNXnetIPRouting(netIf, mcGroup), settings);
	}

	/**
	 * Creates a new network link using the {@link KNXnetIPRouting} protocol, with the local endpoint specified by an IP
	 * address.
	 *
	 * @param localEP the IP address bound to a local network interface for joining the multicast group
	 * @param mcGroup address of the multicast group to join, use {@link #DefaultMulticast} for the default KNX IP
	 *        multicast address
	 * @param settings medium settings defining device and medium specifics needed for communication
	 * @return the network link in open state
	 * @throws KNXException on failure establishing link using the KNXnet/IP connection
	 */
	public static KNXNetworkLinkIP newRoutingLink(final InetAddress localEP, final InetAddress mcGroup,
		final KNXMediumSettings settings) throws KNXException
	{
		try {
			return newRoutingLink(NetworkInterface.getByInetAddress(localEP), mcGroup, settings);
		}
		catch (final SocketException e) {
			throw new KNXException("error getting network interface: " + e.getMessage());
		}
	}

	/**
	 * Creates a new network link based on the KNXnet/IP protocol, using a {@link KNXnetIPConnection}.
	 *
	 * @param serviceMode mode of communication to open, <code>serviceMode</code> is one of the service mode constants
	 *        (e.g. {@link #TUNNELING}); depending on the mode set, the expected local/remote endpoints might differ
	 * @param localEP the local endpoint of the link to use;<br>
	 *        - in tunneling mode (point-to-point), this is the client control endpoint, use <code>null</code> for the
	 *        default local host and an ephemeral port number<br>
	 *        - in {@link #ROUTING} mode, specifies the multicast interface, i.e., the local network interface is taken
	 *        that has the IP address bound to it (if IP address is bound more than once, it's undefined which interface
	 *        is returned), the port is not used; use <code>null</code> for <code>localEP</code> or an unresolved IP
	 *        address to take the host's default multicast interface
	 * @param remoteEP the remote endpoint of the link to communicate with;<br>
	 *        - in tunneling mode (point-to-point), this is the server control endpoint <br>
	 *        - in {@link #ROUTING} mode, the IP address specifies the multicast group to join, the port is not used;
	 *        use <code>null</code> for <code>remoteEP</code> or an unresolved IP address to take the default multicast
	 *        group
	 * @param useNAT <code>true</code> to use network address translation in tunneling service mode, <code>false</code>
	 *        to use the default (non aware) mode; parameter is ignored for routing
	 * @param settings medium settings defining device and medium specifics needed for communication
	 * @throws KNXException on failure establishing link using the KNXnet/IP connection
	 * @throws InterruptedException on interrupted thread while establishing link
	 */
	protected KNXNetworkLinkIP(final int serviceMode, final InetSocketAddress localEP, final InetSocketAddress remoteEP,
		final boolean useNAT, final KNXMediumSettings settings) throws KNXException, InterruptedException
	{
		this(serviceMode, newConnection(serviceMode, localEP, remoteEP, useNAT), settings);
		configureWithServerSettings(localEP, remoteEP, useNAT);
	}

	/**
	 * Creates a new network link with <code>serviceMode</code> based on the supplied KNXnet/IP connection.
	 *
	 * @param serviceMode mode of communication, one of the service mode constants {@link #TUNNELING} or
	 *        {@link #ROUTING}
	 * @param c a KNXnet/IP tunneling or routing connection in open state
	 * @param settings medium settings defining device and medium specifics needed for communication
	 */
	protected KNXNetworkLinkIP(final int serviceMode, final KNXnetIPConnection c, final KNXMediumSettings settings)
	{
		super(c, createLinkName(c.getRemoteAddress()), settings);
		cEMI = true;

		mode = serviceMode;
		conn.addConnectionListener(notifier);
	}

	/**
	 * {@inheritDoc} When communicating with a KNX network which uses open medium, messages are broadcasted within
	 * domain (as opposite to system broadcast) by default. Specify <code>dst = null</code> for system broadcast.
	 */
	@Override
	public void sendRequest(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXLinkClosedException, KNXTimeoutException
	{
		final int mc = mode == TUNNELING ? CEMILData.MC_LDATA_REQ : CEMILData.MC_LDATA_IND;
		send(mc, dst, p, nsdu, false);
	}

	/**
	 * {@inheritDoc} When communicating with a KNX network which uses open medium, messages are broadcasted within
	 * domain (as opposite to system broadcast) by default. Specify <code>dst null</code> for system broadcast.
	 */
	@Override
	public void sendRequestWait(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		final int mc = mode == TUNNELING ? CEMILData.MC_LDATA_REQ : CEMILData.MC_LDATA_IND;
		send(mc, dst, p, nsdu, true);
	}

	@Override
	public String toString()
	{
		return (mode == TUNNELING ? "tunneling " : "routing ") + super.toString();
	}

	@Override
	protected void onSend(final KNXAddress dst, final byte[] msg, final boolean waitForCon)
	{
		throw new IllegalStateException("KNXnet/IP uses cEMI only");
	}

	@Override
	protected void onSend(final CEMILData msg, final boolean waitForCon)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		try {
			logger.debug("send {}{}", (waitForCon ? "(wait for confirmation) " : ""), msg);
			conn.send(msg, waitForCon ? WaitForCon : WaitForAck);
			logger.trace("send {}->{} succeeded", msg.getSource(), msg.getDestination());
		}
		catch (InterruptedException | KNXConnectionClosedException e) {
			logger.error("send error, closing link", e);
			close();
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
	}

	@Override
	void onSend(final CEMIDevMgmt frame)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException {
		mgmt.send(frame, WaitForCon);
	}

	private void configureWithServerSettings(final InetSocketAddress localEP, final InetSocketAddress serverCtrlEP,
		final boolean useNat) throws InterruptedException {
		try (KNXnetIPDevMgmt mgmt = new KNXnetIPDevMgmt(new InetSocketAddress(localEP.getAddress(), 0), serverCtrlEP, useNat)) {
			this.mgmt = mgmt;
			mgmt.addConnectionListener(new KNXListener() {
				@Override
				public void frameReceived(final FrameEvent e) {
					onDevMgmt((CEMIDevMgmt) e.getFrame());
				}

				@Override
				public void connectionClosed(final CloseEvent e) {}
			});

			setMaxApduLength();
		}
		catch (KNXException | RuntimeException e) {
			logger.warn("skip link configuration (use defaults)", e);
		}
	}

	private static KNXnetIPConnection newConnection(final int serviceMode, final InetSocketAddress localEP,
		final InetSocketAddress remoteEP, final boolean useNAT) throws KNXException, InterruptedException
	{
		switch (serviceMode) {
		case TUNNELING:
			final InetSocketAddress local = localEP == null ? new InetSocketAddress(0) : localEP;
			return new KNXnetIPTunnel(LinkLayer, local, remoteEP, useNAT);
		case ROUTING:
			NetworkInterface netIf = null;
			if (localEP != null && !localEP.isUnresolved())
				try {
					netIf = NetworkInterface.getByInetAddress(localEP.getAddress());
				}
				catch (final SocketException e) {
					throw new KNXException("error getting network interface: " + e.getMessage());
				}
			final InetAddress mcast = remoteEP != null ? remoteEP.getAddress() : null;
			return new KNXnetIPRouting(netIf, mcast);
		default:
			throw new KNXIllegalArgumentException("unknown service mode " + serviceMode);
		}
	}

	private static String createLinkName(final InetSocketAddress endpt)
	{
		if (endpt == null)
			return KNXnetIPRouting.DEFAULT_MULTICAST;
		// do our own IP:port string, since InetAddress.toString() always prepends a '/'
		final String host = (endpt.isUnresolved() ? endpt.getHostString() : endpt.getAddress().getHostAddress());
		final int p = endpt.getPort();
		if (p > 0)
			return host + ":" + p;
		return host;
	}
}

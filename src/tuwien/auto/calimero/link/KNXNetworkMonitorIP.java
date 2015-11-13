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

package tuwien.auto.calimero.link;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.BusMonitorLayer;

/**
 * Implementation of the KNX network monitor link based on the KNXnet/IP protocol, using a
 * {@link KNXnetIPConnection}. Once a monitor has been closed, it is not available for further link
 * communication, i.e., it can't be reopened.
 * <p>
 * Pay attention to the IP address consideration stated in the API documentation of class
 * {@link KNXNetworkLinkIP}.
 *
 * @author B. Malinowsky
 */
public class KNXNetworkMonitorIP extends AbstractMonitor
{
	/**
	 * Creates a new network monitor based on the KNXnet/IP protocol for accessing the KNX network.
	 * <p>
	 *
	 * @param localEP the local endpoint to use for the link, this is the client control endpoint,
	 *        use <code>null</code> for the default local host and an ephemeral port number
	 * @param remoteEP the remote endpoint of the link; this is the server control endpoint
	 * @param useNAT <code>true</code> to use network address translation in the KNXnet/IP protocol,
	 *        <code>false</code> to use the default (non aware) mode
	 * @param settings medium settings defining the specific KNX medium needed for decoding raw
	 *        frames received from the KNX network
	 * @throws KNXException on failure establishing the link
	 * @throws InterruptedException on interrupted thread while establishing link
	 */
	public KNXNetworkMonitorIP(final InetSocketAddress localEP, final InetSocketAddress remoteEP,
		final boolean useNAT, final KNXMediumSettings settings)
			throws KNXException, InterruptedException
	{
		this(new KNXnetIPTunnel(BusMonitorLayer, localEndpoint(localEP), remoteEP,
				useNAT), settings);
		logger.info("in busmonitor mode - ready to receive");
		((KNXnetIPTunnel) super.conn).addConnectionListener(notifier);
	}

	/**
	 * Creates a new network monitor using the supplied the KNXnet/IP protocol for accessing the KNX
	 * network.
	 *
	 * @param conn an open KNXnet/IP connection in busmonitor layer, the link takes ownership
	 * @param settings medium settings defining the specific KNX medium needed for decoding raw
	 *        frames received from the KNX network
	 */
	protected KNXNetworkMonitorIP(final KNXnetIPConnection conn, final KNXMediumSettings settings)
	{
		super(conn, monitorName(conn.getRemoteAddress()), settings);
	}

	private static InetSocketAddress localEndpoint(final InetSocketAddress local)
		throws KNXException
	{
		if (local != null)
			return local;
		try {
			return new InetSocketAddress(InetAddress.getLocalHost(), 0);
		}
		catch (final UnknownHostException e) {
			throw new KNXException("no local host available");
		}
	}

	private static String monitorName(final InetSocketAddress remote)
	{
		// do our own IP:port string, since InetAddress.toString() always prepends a '/'
		final String host = (remote.isUnresolved() ? remote.getHostString() : remote.getAddress().getHostAddress());
		return "monitor " + host + ":" + remote.getPort();
	}
}

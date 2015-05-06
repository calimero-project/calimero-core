/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015 B. Malinowsky

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

package tuwien.auto.calimero.internal;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.io.AccessPoint;
import javax.microedition.io.NetworkInterface;
import javax.microedition.io.NetworkUtilities;
import javax.microedition.io.UDPDatagramConnection;

/**
 * Contains endpoint address information for various Calimero KNX connections.
 *
 * @author B. Malinowsky
 */
public class EndpointAddress
{
	// NYI parse IPv6

	// IPv4 0.0.0.0 with port 0
	private static final EndpointAddress anyLocalIp = new EndpointAddress();
	private static final EndpointAddress loopback;
	
	// the JME specified protocol identifier, e.g., datagram://
	private final String protocol;
	private final String addr;
	private final int port;

	static {
		// get loopback address (requires socket protocol permission)
		String lo = "127.0.0.1";
		try {
			lo = NetworkUtilities.getByName(null);
		} catch (UnknownHostException e) {}
		loopback = EndpointAddress.of(lo);
	}
	
	public static EndpointAddress anyLocal()
	{
		return anyLocalIp;
	}

	public static EndpointAddress localHost() throws UnknownHostException
	{
		String host = "localhost";
		try {
			UDPDatagramConnection c = Connection.openDatagram("localhost", 80, 0, true);
			host = c.getLocalAddress();
		} catch (UnknownHostException e) {
			throw e;
		} catch (IOException e) {
			JavaME.logger.warn("cannot get local host", e);
		}
		return new EndpointAddress("datagram://", host, 0);
	}

	public static EndpointAddress of(final String epAddress)
	{
		return new EndpointAddress(epAddress);
	}

	public static EndpointAddress of(final String host, final int port)
	{
		return new EndpointAddress(null, host, port);
	}

	public static EndpointAddress of(final String protocol, final String host, final int port)
	{
		return new EndpointAddress(protocol, host, port);
	}

	public static EndpointAddress of(final byte[] address)
	{
		// XXX IPv4 only
		return new EndpointAddress((address[0] & 0xff) + "." + (address[1] & 0xff) + "."
				+ (address[2] & 0xff) + "." + (address[3] & 0xff));
	}

	public static EndpointAddress newUdp(final String host, final int port)
	{
		return EndpointAddress.of("datagram://", host, port);
	}

	public static EndpointAddress newUdp(final int port)
	{
		return newUdp((String) null, port);
	}

	private EndpointAddress()
	{
		this(null, null, 0);
	}

	private EndpointAddress(final String protocol, final String host, final int port)
	{
		this.protocol = protocol;
		addr = host;
		this.port = port;
	}

	private EndpointAddress(final String epAddress)
	{
		final int addridx = epAddress.indexOf("://");
		protocol = addridx == -1 ? null : epAddress.substring(0, addridx);
		final int portidx = epAddress.indexOf(':', addridx + 1);
		addr = portidx == -1 ? epAddress.substring(addridx + 1) 
				: epAddress.substring(addridx + 1, portidx);
		port = portidx == -1 ? 0 : Integer.parseInt(epAddress.substring(portidx + 1));
	}

	public final String getHost()
	{
		return addr;
	}

	public final byte[] getAddress()
	{
		final byte[] raw = new byte[4];
		if (addr == null || addr.isEmpty())
			return raw;
		String a = addr;
		try {
			if ("localhost".equals(addr))
				a = localHost().getHost();
		} catch (UnknownHostException e) {}
		if ("localhost".equals(a)) {
			raw[0] = 127;
			raw[3] = 1;
			return raw;
		}
		int from = 0;
		for (int i = 0; i < 3; i++) {
			final int to = a.indexOf('.', from);
			raw[i] = (byte) Integer.parseInt(a.substring(from, to));
			from = to + 1;
		}
		raw[3] = (byte) Integer.parseInt(a.substring(from));
		return raw;
	}

	public final String toAddress()
	{
		String p;
		if (protocol != null)
			p = protocol;
		else
			p = isMulticast() ? "multicast://" : "datagram://";
		return p + (addr == null ? "" : addr) + ":" + (port == 0 ? "" : port);
	}

	public final int getPort()
	{
		return port;
	}

	public final boolean isAnyLocal()
	{
		return equals(anyLocalIp);
	}

	public final boolean isMulticast()
	{
		final byte[] addr = getAddress();
		return (addr[0] & 0xf0) == 0xe0;
	}

	public final boolean isUnresolved()
	{
		return addr == null;
	}

	public final boolean isLoopback()
	{
		final byte[] addr = getAddress();
		return addr[0] == 127;
	}

	public final boolean isLinkLocal()
	{
		final byte[] addr = getAddress();
		return ((addr[0] & 0xff) == 169) && ((addr[1] & 0xff) == 254);
	}

	public final NetworkInterface getInterface()
	{
		// XXX prob should return array
		throw new UnsupportedOperationException("EndpointAddress::getInterface");
	}

	public final AccessPoint[] getAccessPoints()
	{
		String a = isLoopback() ? loopback.getHost() : addr;
		List<AccessPoint> found = new ArrayList();
		for (AccessPoint ap : AccessPoint.getAccessPoints(false)) {
			JavaME.logger.trace("AP " + ap.getName());
			for (String ip : ap.getPropertyValues("ipaddr")) {
				JavaME.logger.trace("    IP " + ip);
				if (ip.equals(a))
					found.add(ap);
			}
		}
		return found.toArray(new AccessPoint[found.size()]);
	}

	public static List<NetworkInterface> getAllNetworkInterfaces()
	{
		final List<NetworkInterface> l = new ArrayList<>();
		final String[] types = NetworkInterface.getNetworkTypes();
		for (final String type : types) {
			JavaME.logger.trace("Getting network interfaces for type " + type);
			l.addAll(Arrays.asList(NetworkInterface.getNetworkInterfaces(type)));
		}
		return l;
	}

	public static AccessPoint[] getByNetworkInterface(final NetworkInterface ni)
	{
		if (ni == null)
			return new AccessPoint[0];
		return ni.getConnectedAccessPoints();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result + addr.hashCode();
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final EndpointAddress other = (EndpointAddress) obj;
		if (port != other.port)
			return false;
		if (!addr.equals(other.addr))
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		}
		else if (!protocol.equals(other.protocol))
			return false;
		return true;
	}

	@Override
	public final String toString()
	{
		final String s = addr + ":" + port;
		return s;
	}
}

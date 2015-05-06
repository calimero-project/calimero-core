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

import static javax.microedition.io.Connector.READ_WRITE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.io.AccessPoint;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.ConnectionOption;
import javax.microedition.io.Connector;
import javax.microedition.io.NetworkInterface;
import javax.microedition.io.UDPDatagramConnection;
import javax.microedition.io.UDPMulticastConnection;

/**
 * @author B. Malinowsky
 */
public class Connection
{
	public static UDPDatagramConnection openDatagram(final String host, final int port,
			final int timeout, final boolean preferIPv4) throws IOException
	{
		JavaME.logger.debug("open {}:{}, timeout {}, prefer IPv4 {}", host, port, timeout, preferIPv4);
		String p = port == 0 ? "" : Integer.toString(port);
		ConnectionOption<Integer> to = new ConnectionOption<>("Timeout", timeout);
		ConnectionOption<String> ipv = new ConnectionOption<>("IPVersion", preferIPv4 ? "ipv4" : "ipv6");
		return (UDPDatagramConnection) Connector.open("datagram://" + host + ":" + p, READ_WRITE, 
				true, to, ipv);
	}

//	public static UDPDatagramConnection open(final EndpointAddress remote,
//		final ConnectionOption<?>... settings) throws IOException
//	{
//		return (UDPDatagramConnection) Connector.open(remote.toAddress(), READ_WRITE, settings);
//	}

	public static UDPDatagramConnection open(final EndpointAddress local,
		final EndpointAddress remote, final ConnectionOption<?>... settings) throws IOException
	{
		JavaME.logger.trace("open with local endpoint " + local.toAddress());
		return open(local.getAccessPoints()[0], remote);
	}

	public static UDPDatagramConnection open(final AccessPoint local, final EndpointAddress remote,
		final ConnectionOption<?>... settings) throws IOException
	{
		String ss = settings.length == 0 ? " -" : "";
		for (ConnectionOption<?> s : settings)
			ss += " " + s.getName() + "=" + s.getValue();
		JavaME.logger.trace("open AP {}, remote {}, settings{}", 
				local == null ? "n/a" : local.getName(), remote, ss);
		
		if (local == null && settings.length == 0) {
			try {
				return (UDPDatagramConnection) Connector.open(remote.toAddress(), READ_WRITE, true);
			} catch (ConnectionNotFoundException e) {
				JavaME.logger.error("Platform does not support IP multicasting (?)");
				throw e;				
			}
		}
		final List<ConnectionOption<?>> l = new ArrayList<>();
		if (local != null)
			l.add(new ConnectionOption<>("AccessPoint", local));
		l.addAll(Arrays.asList(settings));
		try {
			return (UDPDatagramConnection) Connector.open(remote.toAddress(), READ_WRITE, true,
					l.toArray(settings));
		} catch (ConnectionNotFoundException e) {
			JavaME.logger.error("Platform does not support IP multicasting (?)");
			throw e;
		}
	}

	public static UDPMulticastConnection openMulticast(final String group) throws IOException
	{
		return openMcast("multicast://" + group + ":");
	}

	private static UDPMulticastConnection openMcast(final String remote,
			final ConnectionOption<?>... settings) throws IOException
	{
		try {
			return (UDPMulticastConnection) Connector.open(remote, READ_WRITE, true,
					settings);
		} catch (ConnectionNotFoundException e) {
			JavaME.logger.error("Platform does not support IP multicasting");
			throw e;
		}
	}
	
	public static NetworkInterface getNetworkInterface(final UDPDatagramConnection c)
		throws IOException
	{
		return c.getAccessPoints()[0].getNetworkInterface();
	}
}

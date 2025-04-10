/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

package io.calimero.knxnetip.util;

import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * KNXnet/IP Host Protocol Address Information (HPAI).
 * <p>
 * The address information is used to describe a communication channel. Its structure
 * is implemented for KNX IPv4 UDP/TCP. For TCP communication channels, use {@link #Tcp}.
 * For IP networks with NAT (network address translation), use {@link #Nat}.
 *
 * @param hostProtocol the host protocol code of this HPAI, {@link #IPV4_UDP} or {@link #IPV4_TCP}
 * @param endpoint     IP address and port number
 * @author B. Malinowsky
 */
public record HPAI(int hostProtocol, InetSocketAddress endpoint) {
	/**
	 * Internet protocol version 4 address, UDP communication.
	 */
	public static final int IPV4_UDP = 0x01;

	/**
	 * Internet protocol version 4 address, TCP communication.
	 */
	public static final int IPV4_TCP = 0x02;

	/**
	 * Host protocol address information for TCP, always set to route-back endpoint.
	 */
	public static final HPAI Tcp = new HPAI(IPV4_TCP, new InetSocketAddress(0));

	/**
	 * Host protocol address information for UDP with NAT.
	 */
	public static final HPAI Nat = new HPAI(IPV4_UDP, new InetSocketAddress(0));


	private static final int HPAI_SIZE = 8;

	/**
	 * Creates a new HPAI out of a byte array.
	 *
	 * @param data   byte array containing the HPAI structure
	 * @param offset start offset of HPAI in {@code data}
	 * @throws KNXFormatException if no HPAI found, invalid structure or unknown host protocol
	 */
	public static HPAI from(final byte[] data, final int offset) throws KNXFormatException {
		if (data.length - offset < HPAI_SIZE)
			throw new KNXFormatException("buffer too short for HPAI");
		int i = offset;
		final int length = data[i++] & 0xFF;
		if (length != HPAI_SIZE)
			throw new KNXFormatException("invalid HPAI size", length);

		final int hostProtocol = data[i++] & 0xFF;
		final byte[] address = Arrays.copyOfRange(data, i, i += 4);
		final int port = ((data[i++] & 0xFF) << 8) | data[i] & 0xFF;

		try {
			return new HPAI(hostProtocol, InetAddress.getByAddress(address), port);
		} catch (UnknownHostException | KNXIllegalArgumentException e) {
			throw new KNXFormatException(e.getMessage());
		}
	}

	/**
	 * Creates a HPAI for UDP communication with the given address information, using UDP as communication mode
	 * (default in KNXnet/IP). The following first matching rule is used for the {@code addr} argument:<br>
	 * 1) {@code addr} holds an {@link InetAddress}, use that address<br>
	 * 2) {@code addr} is {@code null}, the local host is retrieved by {@link InetAddress#getLocalHost()}<br>
	 * 3) if no local host could be found, fall back to safe state and initialize IP <b>and</b> port to 0 (NAT aware
	 * mode)
	 *
	 * @param addr local IP address, use {@code null} for setting local host
	 * @param port local port number to set, 0 &lt;= {@code port} &lt;= 0xFFFF
	 */
	public HPAI(final InetAddress addr, final int port) {
		this(IPV4_UDP, addrOrDefault(addr), port);
	}

	/**
	 * Creates a HPAI with the given address information.
	 *
	 * @param hostProtocol host protocol code (UDP or TCP on IP)
	 * @param addr         local IP address
	 * @param port         local port number to set, 0 &lt;= {@code port} &lt;= 0xFFFF
	 */
	public HPAI(final int hostProtocol, final InetAddress addr, final int port) {
		this(hostProtocol, new InetSocketAddress(addr, port));
	}

	public HPAI {
		if (hostProtocol != IPV4_UDP && hostProtocol != IPV4_TCP)
			throw new KNXIllegalArgumentException("unknown host protocol " + Integer.toHexString(hostProtocol));

		final InetAddress a = endpoint.getAddress();
		if (a == null)
			throw new KNXIllegalArgumentException(endpoint + " is an unresolved IP address");
		if (!(a instanceof Inet4Address))
			throw new KNXIllegalArgumentException(a + " is not an IPv4 address");
		if (a.isAnyLocalAddress() && endpoint.getPort() != 0)
			throw new KNXIllegalArgumentException(a + " is a wildcard IP address");
		if (hostProtocol == IPV4_TCP && !routeBack(endpoint))
			throw new KNXIllegalArgumentException("HPAI for TCP does not contain route-back endpoint: " + hostPort(endpoint));
	}

	/**
	 * {@return {@code true} if this HPAI is a route-back HPAI (required for UDP NAT v2 and TCP connections), {@code false} otherwise}
	 */
	public boolean isRouteBack() { return routeBack(endpoint); }

	private static boolean routeBack(final InetSocketAddress endpoint) {
		return endpoint.getAddress().isAnyLocalAddress() && endpoint.getPort() == 0;
	}

	/**
	 * {@return {@code true} if this HPAI indicates NAT (for UDP NAT), {@code false} otherwise}
	 */
	public boolean nat() {
		return endpoint.getAddress().isAnyLocalAddress() || endpoint.getPort() == 0;
	}

	/**
	 * {@return structure length of this HPAI as unsigned byte}
	 */
	public int getStructLength() {
		return HPAI_SIZE;
	}

	/**
	 * {@return the byte array representation of the whole HPAI structure}
	 */
	public byte[] toByteArray() {
		final ByteArrayOutputStream os = new ByteArrayOutputStream(HPAI_SIZE);
		os.write(HPAI_SIZE);
		os.write(hostProtocol);
		os.write(endpoint.getAddress().getAddress(), 0, 4);
		os.write(endpoint.getPort() >> 8);
		os.write(endpoint.getPort());
		return os.toByteArray();
	}

	/**
	 * {@return a textual representation of this HPAI as string}
	 */
	@Override
	public String toString() {
		return hostPort(endpoint) + " (IPv4 " + (hostProtocol == IPV4_UDP ? "UDP" : "TCP") + ")";
	}

	private static String hostPort(final InetSocketAddress endpoint) {
		return endpoint.getAddress().getHostAddress() + ":" + endpoint.getPort();
	}

	private static InetAddress addrOrDefault(final InetAddress addr) {
		try {
			return addr != null ? addr : InetAddress.getLocalHost();
		}
		catch (final UnknownHostException e) {
			try {
				return InetAddress.getByAddress(new byte[4]);
			}
			catch (final UnknownHostException unreachable) {
				throw new Error("raw IPv4 addresses should always work", unreachable);
			}
		}
	}
}

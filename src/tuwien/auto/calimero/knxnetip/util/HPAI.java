/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2021 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip.util;

import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * KNXnet/IP Host Protocol Address Information (HPAI).
 * <p>
 * The address information is used to describe a communication channel. Its structure
 * varies according to the used underlying protocol. This class is implemented for IPv4.
 * <br>
 * For IP networks with NAT, consider use of {@link #HPAI(int, InetSocketAddress)}.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 */
public class HPAI
{
	/**
	 * Internet protocol version 4 address, UDP communication.
	 */
	public static final int IPV4_UDP = 0x01;

	/**
	 * Internet protocol version 4 address, TCP communication.
	 */
	public static final int IPV4_TCP = 0x02;

	/** Host protocol address information for TCP, set to route back endpoint. */
	public static final HPAI Tcp = new HPAI(IPV4_TCP, null);

	private static final int HPAI_SIZE = 8;

	private final int length;
	private final int hostprot;
	private final InetSocketAddress addr;

	/**
	 * Creates a HPAI out of a byte array.
	 *
	 * @param data byte array containing the HPAI structure
	 * @param offset start offset of HPAI in <code>data</code>
	 * @throws KNXFormatException if no HPAI found, invalid structure or unknown host protocol
	 */
	public HPAI(final byte[] data, final int offset) throws KNXFormatException
	{
		if (data.length - offset < HPAI_SIZE)
			throw new KNXFormatException("buffer too short for HPAI");
		int i = offset;
		length = data[i++] & 0xFF;
		if (length != HPAI_SIZE)
			throw new KNXFormatException("unknown HPAI size", length);
		hostprot = data[i++] & 0xFF;
		if (hostprot != IPV4_UDP && hostprot != IPV4_TCP)
			throw new KNXFormatException("unknown host protocol", hostprot);

		final byte[] address = Arrays.copyOfRange(data, i, i += 4);
		final int port = ((data[i++] & 0xFF) << 8) | data[i] & 0xFF;
		try {
			addr = new InetSocketAddress(InetAddress.getByAddress(address), port);
		}
		catch (final UnknownHostException e) {
			throw new KNXFormatException("invalid IPv4 address", e);
		}

		if (hostprot == IPV4_TCP && !isRouteBack())
			throw new KNXFormatException("HPAI for TCP does not contain route back endpoint", hostPort());
	}

	/**
	 * Creates a HPAI for UDP communication with the given address information, using UDP as communication mode (default
	 * in KNXnet/IP). The following first matching rule is used for the <code>addr</code> argument:<br>
	 * 1) <code>addr</code> holds an {@link InetAddress}, use that address<br>
	 * 2) <code>addr</code> is <code>null</code>, the local host is retrieved by {@link InetAddress#getLocalHost()}<br>
	 * 3) if no local host could be found, fall back to safe state and initialize IP <b>and</b> port to 0 (NAT aware
	 * mode)
	 *
	 * @param addr local IP address, use <code>null</code> for setting local host
	 * @param port local port number to set, 0 &lt;= <code>port</code> &lt;= 0xFFFF
	 */
	public HPAI(final InetAddress addr, final int port)
	{
		this(IPV4_UDP, addrOrDefault(addr), port);
	}

	/**
	 * Creates a HPAI with the given address information.
	 *
	 * @param hostProtocol host protocol code (UDP or TCP on IP)
	 * @param addr local IP address
	 * @param port local port number to set, 0 &lt;= <code>port</code> &lt;= 0xFFFF
	 */
	public HPAI(final int hostProtocol, final InetAddress addr, final int port)
	{
		this(hostProtocol, new InetSocketAddress(addr, port));
	}

	/**
	 * Creates a HPAI with the given address information.
	 * <p>
	 * To indicate the use of network address translation (NAT) to the receiver, leave
	 * <code>addr</code> <code>null</code>.
	 *
	 * @param hostProtocol host protocol code (UDP or TCP, see class constants)
	 * @param addr socket address with IP address and port number, if <code>addr</code> =
	 *        <code>null</code> address and port are initialized to 0
	 */
	public HPAI(final int hostProtocol, final InetSocketAddress addr)
	{
		length = HPAI_SIZE;
		if (hostProtocol != IPV4_UDP && hostProtocol != IPV4_TCP)
			throw new KNXIllegalArgumentException("unknown host protocol");
		hostprot = hostProtocol;

		if (addr != null) {
			final InetAddress a = addr.getAddress();
			if (a == null)
				throw new KNXIllegalArgumentException(addr + " is an unresolved IP address");
			if (!(a instanceof Inet4Address))
				throw new KNXIllegalArgumentException(a + " is not an IPv4 address");
			if (a.isAnyLocalAddress() && addr.getPort() != 0)
				throw new KNXIllegalArgumentException(a + " is a wildcard IP address");
			this.addr = addr;
			if (hostprot == IPV4_TCP && !isRouteBack())
				throw new KNXIllegalArgumentException("HPAI for TCP does not contain route back endpoint: " + hostPort());
		}
		else {
			this.addr = new InetSocketAddress(0);
		}
	}

	/**
	 * Returns the host protocol of this HPAI.
	 *
	 * @return host protocol code as unsigned byte
	 */
	public final int getHostProtocol()
	{
		return hostprot;
	}

	/**
	 * Indicates whether this HPAI is a route back HPAI, required for UDP NAT and TCP connections.
	 *
	 * @return <code>true</code> if this HPAI is a route back HPAI, <code>false</code> otherwise
	 */
	public final boolean isRouteBack() {
		return addr.getAddress().isAnyLocalAddress() && addr.getPort() == 0;
	}

	/**
	 * Returns the IP address and port number information.
	 *
	 * @return IP address and port number as InetSocketAddress
	 */
	public final InetSocketAddress endpoint() { return addr; }

	/**
	 * Returns the IP network address as {@link InetAddress} representation.
	 *
	 * @return IP address as InetAddress object
	 */
	public final InetAddress getAddress()
	{
		return addr.getAddress();
	}

	/**
	 * Returns the port number of this HPAI.
	 *
	 * @return port as unsigned 16 bit value
	 */
	public final int getPort()
	{
		return addr.getPort();
	}

	/**
	 * Returns the structure length of this HPAI in bytes.
	 *
	 * @return structure length as unsigned byte
	 */
	public final int getStructLength()
	{
		return length;
	}

	/**
	 * Returns the byte representation of the whole HPAI structure.
	 *
	 * @return byte array containing structure
	 */
	public final byte[] toByteArray()
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream(HPAI_SIZE);
		os.write(length);
		os.write(hostprot);
		os.write(addr.getAddress().getAddress(), 0, 4);
		os.write(addr.getPort() >> 8);
		os.write(addr.getPort());
		return os.toByteArray();
	}

	/**
	 * Returns this HPAI representation in textual format.
	 *
	 * @return a string representation of the HPAI object
	 */
	@Override
	public String toString() {
		return hostPort() + " (IPv4 " + (hostprot == IPV4_UDP ? "UDP" : "TCP") + ")";
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof HPAI))
			return false;
		final HPAI other = (HPAI) obj;
		return length == other.length && hostprot == other.hostprot && addr.equals(other.addr);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(length, hostprot, addr);
	}

	private String hostPort() { return addr.getAddress().getHostAddress() + ":" + addr.getPort(); }

	private static InetAddress addrOrDefault(final InetAddress addr)
	{
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

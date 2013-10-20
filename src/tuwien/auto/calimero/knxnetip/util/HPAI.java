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

package tuwien.auto.calimero.knxnetip.util;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * KNXnet/IP Host Protocol Address Information (HPAI).
 * <p>
 * The address information is used to describe a communication channel. Its structure
 * varies according to the used underlying protocol. This class is implemented for IPv4.
 * <br>
 * For IP networks with NAT, consider use of {@link #HPAI(short, InetSocketAddress)}.<br>
 * UDP is the default communication mode with mandatory support used in KNXnet/IP.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 */
public class HPAI
{
	/**
	 * Internet protocol version 4 address, UDP communication.
	 * <p>
	 */
	public static final int IPV4_UDP = 0x01;

	/**
	 * Internet protocol version 4 address, TCP communication.
	 * <p>
	 */
	public static final int IPV4_TCP = 0x02;

	private static final int HPAI_SIZE = 8;

	private byte[] address;
	private int hostprot;
	private int port;
	private int length;

	/**
	 * Creates a HPAI out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing the HPAI structure
	 * @param offset start offset of HPAI in <code>data</code>
	 * @throws KNXFormatException if no HPAI found, invalid structure or unknown host
	 *         protocol
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

		address = new byte[4];
		address[0] = data[i++];
		address[1] = data[i++];
		address[2] = data[i++];
		address[3] = data[i++];

		port = (data[i++] & 0xFF) << 8;
		port |= data[i++] & 0xFF;
	}

	/**
	 * Creates a HPAI for UDP communication with the given address information.
	 * <p>
	 * The constructor uses UDP as communication mode (default in KNXnet/IP).<br>
	 * The following first matching rule is used for the <code>addr</code> argument:<br>
	 * 1) <code>addr</code> holds an {@link InetAddress}, use that address<br>
	 * 2) <code>addr</code> is <code>null</code>, the local host is retrieved by
	 * {@link InetAddress#getLocalHost()}<br>
	 * 3) if no local host could be found, fall back to safe state and initialize IP
	 * <b>and</b> port to 0 (NAT aware mode)<br>
	 * 
	 * @param addr local IP address, use <code>null</code> for setting local host
	 * @param port local port number to set, 0 &lt;= <code>port</code> &lt;= 0xFFFF
	 */
	public HPAI(final InetAddress addr, final int port)
	{
		try {
			final InetAddress ia = addr != null ? addr : InetAddress.getLocalHost();
			init(IPV4_UDP, ia.getAddress(), port);
		}
		catch (final UnknownHostException e) {
			init(IPV4_UDP, new byte[4], 0);
		}
	}

	/**
	 * Creates a HPAI with the given address information.
	 * <p>
	 * 
	 * @param hostProtocol host protocol code (UDP or TCP on IP)
	 * @param addr local IP address
	 * @param port local port number to set, 0 &lt;= <code>port</code> &lt;= 0xFFFF
	 */
	public HPAI(final int hostProtocol, final InetAddress addr, final int port)
	{
		init(hostProtocol, addr.getAddress(), port);
	}

	/**
	 * Creates a HPAI with the given address information.
	 * <p>
	 * To indicate the use of network address translation (NAT) to the receiver, leave
	 * <code>addr</code> <code>null</code>.
	 * 
	 * @param hostProtocol host protocol code (UDP or TCP, see class constants)
	 * @param addr socket with IP address and port number, if <code>addr</code> =
	 *        <code>null</code> address and port are initialized to 0
	 */
	public HPAI(final int hostProtocol, final InetSocketAddress addr)
	{
		if (addr == null)
			init(hostProtocol, new byte[4], 0);
		else {
			final InetAddress a = addr.getAddress();
			if (a == null)
				throw new KNXIllegalArgumentException("unresolved IP address");
			if (a.isAnyLocalAddress())
				throw new KNXIllegalArgumentException("wildcard IP address");
			init(hostProtocol, a.getAddress(), addr.getPort());
		}
	}

	/**
	 * Returns the host protocol of this HPAI.
	 * <p>
	 * 
	 * @return host protocol code as unsigned byte
	 */
	public final int getHostProtocol()
	{
		return hostprot;
	}

	/**
	 * Returns the raw IP network address.
	 * <p>
	 * 
	 * @return byte array with IP address in network byte order
	 */
	public final byte[] getRawAddress()
	{
		return (byte[]) address.clone();
	}

	/**
	 * Returns the IP network address as {@link InetAddress} representation.
	 * <p>
	 * 
	 * @return IP address as InetAddress object
	 */
	public final InetAddress getAddress()
	{
		try {
			return InetAddress.getByAddress(address);
		}
		catch (final UnknownHostException ignore) {}
		return null;
	}

	/**
	 * Returns the port number of this HPAI.
	 * <p>
	 * 
	 * @return port as unsigned 16 bit value
	 */
	public final int getPort()
	{
		return port;
	}

	/**
	 * Returns the structure length of this HPAI in bytes.
	 * <p>
	 * 
	 * @return structure length as unsigned byte
	 */
	public final int getStructLength()
	{
		return length;
	}

	/**
	 * Returns the byte representation of the whole HPAI structure.
	 * <p>
	 * 
	 * @return byte array containing structure
	 */
	public final byte[] toByteArray()
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream(HPAI_SIZE);
		os.write(length);
		os.write(hostprot);
		os.write(address, 0, address.length);
		os.write(port >> 8);
		os.write(port);
		return os.toByteArray();
	}

	/**
	 * Returns this HPAI representation in textual format.
	 * <p>
	 * 
	 * @return a string representation of the HPAI object
	 */
	public String toString()
	{
		return "IPv4 " + (hostprot == IPV4_UDP ? "UDP" : "TCP") + " host " + getAddressString()
				+ " port " + port;
	}

	private String getAddressString()
	{
		return (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." + (address[2] & 0xFF) + "."
				+ (address[3] & 0xFF);
	}

	private void init(final int prot, final byte[] addr, final int p)
	{
		if (addr.length != 4)
			throw new KNXIllegalArgumentException("not an IPv4 address");
		if (port < 0 || port > 0xffff)
			throw new KNXIllegalArgumentException("port number out of range [0..65535]");
		if (prot != IPV4_UDP && prot != IPV4_TCP)
			throw new KNXIllegalArgumentException("unknown host protocol");
		length = HPAI_SIZE;
		hostprot = prot;
		port = p;
		address = addr;
	}
}

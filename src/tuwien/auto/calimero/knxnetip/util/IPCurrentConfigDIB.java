/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

import java.io.ByteArrayInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Represents an IP current configuration description information block. DIBs of this type are used
 * for KNXnet/IP remote diagnosis and configuration, and optionally in a description response. The
 * {@link IPCurrentConfigDIB} contains information about the currently used IP settings.<br>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse
 */
public final class IPCurrentConfigDIB extends DIB
{
	private static final int DIB_SIZE = 20;

	private final byte[] ip;
	private final byte[] subnet;
	private final byte[] gw;
	private final byte[] dhcp;
	private final int assignment;

	/**
	 * Creates a IP current configuration DIB out of a byte array.
	 * <p>
	 *
	 * @param data byte array containing device DIB structure
	 * @param offset start offset of DIB in <code>data</code>
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public IPCurrentConfigDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != IP_CURRENT_CONFIG)
			throw new KNXFormatException("no IP current config DIB, wrong type ID " + type);
		if (size < DIB_SIZE)
			throw new KNXFormatException("IP current config DIB too short, < " + DIB_SIZE, size);
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset + 2, data.length
				- offset - 2);
		ip = new byte[4];
		subnet = new byte[4];
		gw = new byte[4];
		dhcp = new byte[4];
		is.read(ip, 0, ip.length);
		is.read(subnet, 0, subnet.length);
		is.read(gw, 0, gw.length);
		is.read(dhcp, 0, dhcp.length);
		assignment = is.read();
		final int reserved = is.read();
		if (reserved != 0)
			throw new KNXFormatException("reserved field shall be set 0", reserved);
	}

	/**
	 * Creates a IP current configuration DIB using the supplied device information.
	 * <p>
	 *
	 * @param ip currently used IP address, use <code>null</code> or 0.0.0.0 if the device does not
	 *        provide this information
	 * @param subnetMask currently used subnet mask, use <code>null</code> or 0.0.0.0 if the device
	 *        does not provide this information
	 * @param gateway address of the currently used default gateway, use <code>null</code> or
	 *        0.0.0.0 if the device does not provide this information
	 * @param dhcp address of the currently used DHCP server, use <code>null</code> or 0.0.0.0 if
	 *        the device does not provide this information
	 * @param ipAssignmentMethod the IP address assignment method applied to set the currently used
	 *        IP address, a bitset with at most one set bit: Bit 0 = manually, 1 = BootP, 2 = DHCP,
	 *        3 = AutoIP
	 */
	public IPCurrentConfigDIB(final Inet4Address ip, final Inet4Address subnetMask,
		final Inet4Address gateway, final Inet4Address dhcp, final int ipAssignmentMethod)
	{
		super(DIB_SIZE, IP_CURRENT_CONFIG);
		this.ip = ip != null ? ip.getAddress() : new byte[4];
		this.subnet = subnetMask != null ? subnetMask.getAddress() : new byte[4];
		this.gw = gateway != null ? gateway.getAddress() : new byte[4];
		this.dhcp = dhcp != null ? dhcp.getAddress() : new byte[4];

		// NYI JRE >= 1.5: should use Integer.bitCount()
		if (ipAssignmentMethod < 0 || ipAssignmentMethod > 0x15)
			throw new KNXIllegalArgumentException("IP assignment method out of range [0..b1111]: "
					+ ipAssignmentMethod);
		assignment = ipAssignmentMethod;
	}

	/**
	 * @return the current IPv4 address
	 */
	public Inet4Address getIPAddress()
	{
		return asInet4Address(ip);
	}

	/**
	 * @return the current IPv4 subnet mask
	 */
	public Inet4Address getSubnetMask()
	{
		return asInet4Address(subnet);
	}

	/**
	 * @return the current default gateway IPv4 address
	 */
	public Inet4Address getDefaultGateway()
	{
		return asInet4Address(gw);
	}

	/**
	 * @return the current DHCP server IPv4 address
	 */
	public Inet4Address getDhcpServer()
	{
		return asInet4Address(dhcp);
	}

	/**
	 * @return the applied IP assignment method, contained in bits 0 to 7
	 */
	public int getIPAssignmentMethod()
	{
		return assignment;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		try {
			return "current IP " + InetAddress.getByAddress(ip) + ", subnet mask "
					+ InetAddress.getByAddress(subnet) + ", default gateway "
					+ InetAddress.getByAddress(gw) + ", DHCP server "
					+ InetAddress.getByAddress(dhcp) + ", IP assignment method " + assignment;
		}
		catch (final UnknownHostException ignore) {}
		return "IP current config DIB";
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.util.DIB#toByteArray()
	 */
	@Override
	public byte[] toByteArray()
	{
		final byte[] buf = super.toByteArray();
		int i = 2;
		for (int k = 0; k < 4; ++k)
			buf[i++] = ip[k];
		for (int k = 0; k < 4; ++k)
			buf[i++] = subnet[k];
		for (int k = 0; k < 4; ++k)
			buf[i++] = gw[k];
		for (int k = 0; k < 4; ++k)
			buf[i++] = dhcp[k];

		buf[i++] = (byte) assignment;
		buf[i++] = 0;
		return buf;
	}

	private static Inet4Address asInet4Address(final byte[] addr)
	{
		try {
			return (Inet4Address) InetAddress.getByAddress(addr);
		}
		catch (final UnknownHostException ignore) {
			throw new IllegalArgumentException("illegal length of IPv4 address", ignore);
		}
	}
}

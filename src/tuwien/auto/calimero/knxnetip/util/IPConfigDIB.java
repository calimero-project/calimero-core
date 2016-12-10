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
 * Represents an IP configuration description information block. DIBs of this type are used for
 * KNXnet/IP remote diagnosis and configuration, and optionally in a description response. The
 * {@link IPConfigDIB} contains information about the IP settings as configured by a configuration
 * tool. Those settings are used when a manual address assignment is enabled.<br>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse
 */
public final class IPConfigDIB extends DIB
{
	private static final int DIB_SIZE = 16;

	private final byte[] ip;
	private final byte[] subnet;
	private final byte[] gw;
	private final int caps;
	private final int assignment;

	/**
	 * Creates a IP configuration DIB out of a byte array.
	 * <p>
	 *
	 * @param data byte array containing device DIB structure
	 * @param offset start offset of DIB in <code>data</code>
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public IPConfigDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != IP_CONFIG)
			throw new KNXFormatException("no IP config DIB, wrong type ID " + type);
		if (size < DIB_SIZE)
			throw new KNXFormatException("IP config DIB too short, " + size + " < " + DIB_SIZE);
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset + 2, data.length
				- offset - 2);
		ip = new byte[4];
		subnet = new byte[4];
		gw = new byte[4];
		is.read(ip, 0, ip.length);
		is.read(subnet, 0, subnet.length);
		is.read(gw, 0, gw.length);
		caps = is.read();
		assignment = is.read();
	}

	/**
	 * Creates a IP configuration DIB using the supplied device information.
	 * <p>
	 *
	 * @param ip the configured fixed IP address, use <code>null</code> or 0.0.0.0 if the device
	 *        does not provide this information
	 * @param subnetMask the configured subnet mask, use <code>null</code> or 0.0.0.0 if the device
	 *        does not provide this information
	 * @param gateway address of the configured default gateway, use <code>null</code> or 0.0.0.0 if
	 *        the device does not provide this information
	 * @param ipCapabilities supported IP capabilities, a bitset of available assignment methods;
	 *        set the following bits to indicate support: Bit 0 = BootP, 1 = DHCP, 2 = AutoIP
	 * @param ipAssignmentMethods the enabled IP address assignment methods for setting the current
	 *        IP address, a bitset of enabled methods: Bit 0 = manually, 1 = BootP, 2 = DHCP, 3 =
	 *        AutoIP
	 */
	public IPConfigDIB(final Inet4Address ip, final Inet4Address subnetMask,
		final Inet4Address gateway, final int ipCapabilities, final int ipAssignmentMethods)
	{
		super(DIB_SIZE, IP_CONFIG);
		this.ip = ip != null ? ip.getAddress() : new byte[4];
		this.subnet = subnetMask != null ? subnetMask.getAddress() : new byte[4];
		this.gw = gateway != null ? gateway.getAddress() : new byte[4];
		if (ipCapabilities < 0 || ipCapabilities > 0x7)
			throw new KNXIllegalArgumentException("IP capabilities out of range [0..b111]: "
					+ ipCapabilities);
		caps = ipCapabilities;
		if (ipAssignmentMethods < 0 || ipAssignmentMethods > 0x15)
			throw new KNXIllegalArgumentException("IP assignment methods out of range [0..b1111]: "
					+ ipAssignmentMethods);
		assignment = ipAssignmentMethods;
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
	 * @return the IP capabilities, contained in bits 0 to 7
	 */
	public int getIPCapabilities()
	{
		return caps;
	}

	/**
	 * @return the enabled IP assignment methods, contained in bits 0 to 7
	 */
	public int getIPAssignmentMethods()
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
			return "IP " + InetAddress.getByAddress(ip) + ", subnet mask "
					+ InetAddress.getByAddress(subnet) + ", default gateway "
					+ InetAddress.getByAddress(gw) + ", IP capabilities " + caps
					+ ", enabled IP assignment methods " + assignment;
		}
		catch (final UnknownHostException ignore) {}
		return "IP config DIB";
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

		buf[i++] = (byte) caps;
		buf[i++] = (byte) assignment;
		return buf;
	}

	private static Inet4Address asInet4Address(final byte[] addr)
	{
		try {
			return (Inet4Address) InetAddress.getByAddress(addr);
		}
		catch (final UnknownHostException ignore) { 
			throw new KNXIllegalArgumentException("illegal length of IPv4 address", ignore);
		}
	}
}

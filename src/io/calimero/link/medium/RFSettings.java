/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.link.medium;

import java.util.HexFormat;

import io.calimero.IndividualAddress;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.SerialNumber;

/**
 * Provides settings necessary for communication on RF medium.
 *
 * @author B. Malinowsky
 */
public class RFSettings extends KNXMediumSettings
{
	private static final byte[] broadcastDomain = new byte[6];

	private byte[] doa;
	private final SerialNumber sno;
	private final boolean unidir;

	/**
	 * Creates a new settings container with the device individual address for RF medium.
	 * <p>
	 * The domain address is initialized to broadcast domain, serial number is 0, unidirectional is set
	 * {@code false}.
	 *
	 * @param device device individual device address to use as source address in KNX messages
	 */
	public RFSettings(final IndividualAddress device)
	{
		super(device);
		doa = broadcastDomain;
		sno = SerialNumber.Zero;
		unidir = false;
	}

	/**
	 * Creates a new settings container with RF medium specific information.
	 *
	 * @param device device individual device address to use as source address in KNX messages
	 * @param domain byte array containing the domain address to use in KNX messages, address is given in network byte
	 *        order, {@code domain.length} = 6, supplying {@code null} defaults to the broadcast domain
	 * @param serialNumber serial number of the device
	 * @param unidirectional {@code true} to indicate a unidirectional device, {@code false} otherwise
	 */
	public RFSettings(final IndividualAddress device, final byte[] domain, final SerialNumber serialNumber,
			final boolean unidirectional) {
		super(device);
		setDomainAddress(domain);
		sno = serialNumber;
		unidir = unidirectional;
	}

	/**
	 * Sets a new domain address.
	 *
	 * @param domain byte array containing the domain address to use in KNX messages,
	 *        address is given in network byte order, {@code domain.length} = 6,
	 *        supplying {@code null} defaults to the broadcast domain
	 */
	public final synchronized void setDomainAddress(final byte[] domain)
	{
		if (domain == null)
			doa = broadcastDomain;
		else if (domain.length != 6)
			throw new KNXIllegalArgumentException("invalid length of domain address");
		else
			doa = domain.clone();
	}

	/**
	 * {@return the domain address in network byte order, address byte array length = 6}
	 */
	public final synchronized byte[] getDomainAddress()
	{
		return doa.clone();
	}

	/**
	 * {@return the RF device serial number}
	 */
	public final SerialNumber serialNumber() { return sno; }

	/**
	 * Returns whether unidirectional device is set.
	 *
	 * @return {@code true} if unidirectional, {@code false} otherwise
	 */
	public final boolean isUnidirectional()
	{
		return unidir;
	}

	@Override
	public int getMedium()
	{
		return MEDIUM_RF;
	}

	@Override
	public String toString()
	{
		return super.toString() + " domain 0x" + HexFormat.of().formatHex(doa) + " S/N "
				+ sno + (unidir ? " unidirectional" : "");
	}
}

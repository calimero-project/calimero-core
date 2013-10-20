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

package tuwien.auto.calimero.link.medium;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Provides settings necessary for communication on PL (powerline) medium.
 * <p>
 * This settings type is used for powerline medium PL132 and PL110.
 * 
 * @author B. Malinowsky
 */
public class PLSettings extends KNXMediumSettings
{
	private static final byte[] broadcastDomain = new byte[2];

	private byte[] doa;
	private final boolean pl132;

	/**
	 * Creates a new settings container with PL medium specific information.
	 * <p>
	 * 
	 * @param device individual device address to use as source address in KNX messages,
	 *        specifying <code>null</code> uses the individual address 0.0.0
	 * @param domain byte array containing the domain address to use in KNX messages,
	 *        address is given in network byte order, <code>domain.length</code> = 2,
	 *        supplying <code>null</code> defaults to the broadcast domain
	 * @param mediumPL132 <code>true</code> if communicating on PL132,
	 *        <code>false</code> if communicating on PL110
	 */
	public PLSettings(final IndividualAddress device, final byte[] domain, final boolean mediumPL132)
	{
		super(device);
		pl132 = mediumPL132;
		setDomainAddress(domain);
	}

	/**
	 * Creates a new default container with settings for PL medium.
	 * <p>
	 * The device address is initialized to 0.0.0 and domain address is set to broadcast
	 * domain.
	 * 
	 * @param mediumPL132 <code>true</code> if communicating on PL132,
	 *        <code>false</code> if communicating on PL110
	 */
	public PLSettings(final boolean mediumPL132)
	{
		super(null);
		pl132 = mediumPL132;
		doa = broadcastDomain;
	}

	/**
	 * Sets a new domain address.
	 * <p>
	 * 
	 * @param domain byte array containing the domain address to use in KNX messages,
	 *        address is given in network byte order, <code>domain.length</code> = 2,
	 *        supplying <code>null</code> defaults to the broadcast domain
	 */
	public final synchronized void setDomainAddress(final byte[] domain)
	{
		if (domain == null)
			doa = broadcastDomain;
		else if (domain.length != 2)
			throw new KNXIllegalArgumentException("invalid length of domain address");
		else
			doa = (byte[]) domain.clone();
	}

	/**
	 * Returns the domain address.
	 * <p>
	 * The address is returned in network byte order.
	 * 
	 * @return domain address as byte array of length = 2
	 */
	public final synchronized byte[] getDomainAddress()
	{
		return (byte[]) doa.clone();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.KNXMediumSettings#getMedium()
	 */
	public int getMedium()
	{
		return pl132 ? MEDIUM_PL132 : MEDIUM_PL110;
	}
	
	/**
	 * Returns whether settings are for communication on PL132 medium or PL110 medium.
	 * <p>
	 * 
	 * @return <code>true</code> for PL132, <code>false</code> for PL110
	 */
	public final boolean isPL132()
	{
		return pl132;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.KNXMediumSettings#toString()
	 */
	public String toString()
	{
		return super.toString() + " domain " + ((doa[0] & 0xff) << 8 | doa[1] & 0xff);
	}
}

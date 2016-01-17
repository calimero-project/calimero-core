/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

package tuwien.auto.calimero.link.medium;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Provides settings necessary for communication on RF medium.
 *
 * @author B. Malinowsky
 */
public class RFSettings extends KNXMediumSettings
{
	private static final byte[] broadcastDomain = new byte[6];

	private byte[] doa;
	private byte[] sno;
	private final boolean unidir;

	/**
	 * Creates a new settings container with the device individual address for RF medium.
	 * <p>
	 * The domain address is initialized to broadcast domain, serial number is 0,
	 * unidirectional is set <code>false</code>.
	 *
	 * @param device device individual device address to use as source address in KNX
	 *        messages, specifying <code>null</code> uses the individual address 0.0.0
	 */
	public RFSettings(final IndividualAddress device)
	{
		super(device);
		doa = broadcastDomain;
		sno = new byte[6];
		unidir = false;
	}

	/**
	 * Creates a new settings container with RF medium specific information.
	 * <p>
	 *
	 * @param device device individual device address to use as source address in KNX
	 *        messages, specifying <code>null</code> uses the individual address 0.0.0
	 * @param domain byte array containing the domain address to use in KNX messages,
	 *        address is given in network byte order, <code>domain.length</code> = 6,
	 *        supplying <code>null</code> defaults to the broadcast domain
	 * @param serialNumber serial number of the device, <code>serialNumber.length</code> =
	 *        6
	 * @param unidirectional <code>true</code> to indicate an unidirectional device,
	 *        <code>false</code> otherwise
	 */
	public RFSettings(final IndividualAddress device, final byte[] domain,
		final byte[] serialNumber, final boolean unidirectional)
	{
		super(device);
		setDomainAddress(domain);
		setSerial(serialNumber);
		unidir = unidirectional;
	}

	/**
	 * Sets a new domain address.
	 *
	 * @param domain byte array containing the domain address to use in KNX messages,
	 *        address is given in network byte order, <code>domain.length</code> = 6,
	 *        supplying <code>null</code> defaults to the broadcast domain
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
	 * Returns the domain address in network byte order.
	 *
	 * @return domain address as byte array of length = 6
	 */
	public final synchronized byte[] getDomainAddress()
	{
		return doa.clone();
	}

	/**
	 * Returns the serial number of the device.
	 *
	 * @return serial number as byte array of length = 6
	 */
	public final byte[] getSerialNumber()
	{
		return sno.clone();
	}

	/**
	 * Returns whether unidirectional device is set.
	 *
	 * @return <code>true</code> if unidirectional, <code>false</code> otherwise
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
		return super.toString() + " domain 0x" + DataUnitBuilder.toHex(doa, null) + " s/n 0x"
				+ DataUnitBuilder.toHex(sno, null) + (unidir ? " unidirectional" : "");
	}

	/**
	 * Sets a new serial number.
	 *
	 * @param serial serial number of the device, <code>serial.length</code> = 6,
	 */
	private void setSerial(final byte[] serial)
	{
		if (serial.length != 6)
			throw new KNXIllegalArgumentException("invalid length of serial number");
		sno = serial.clone();
	}
}

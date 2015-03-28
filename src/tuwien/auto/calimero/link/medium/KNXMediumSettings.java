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

import tuwien.auto.calimero.IndividualAddress;

/**
 * Container for device and medium specific settings for the KNX network.
 * <p>
 * Used by a KNX link interface to obtain medium/device information necessary to handle
 * communication with the KNX network based on the particular communication medium.
 * 
 * @author B. Malinowsky
 */
public abstract class KNXMediumSettings
{
	// medium codes are equal to the ones specified in KNXnet/IP

	/**
	 * KNX medium code for twisted pair 0 (2400 bit/s), inherited from BatiBUS.
	 * <p>
	 */
	public static final int MEDIUM_TP0 = 0x01;

	/**
	 * KNX medium code for twisted pair 1 (9600 bit/s).
	 * <p>
	 */
	public static final int MEDIUM_TP1 = 0x02;

	/**
	 * KNX medium code for power line 110 kHz (1200 bit/s).
	 * <p>
	 */
	public static final int MEDIUM_PL110 = 0x04;

	/**
	 * KNX medium code for power line 132 kHz (2400 bit/s), inherited from EHS.
	 * <p>
	 */
	public static final int MEDIUM_PL132 = 0x08;

	/**
	 * KNX medium code for radio frequency (868 MHz).
	 * <p>
	 */
	public static final int MEDIUM_RF = 0x10;

	// local device address if in transparent server mode
	private IndividualAddress dev;

	/**
	 * Creates a new container for KNX device/medium settings.
	 * <p>
	 * 
	 * @param device individual device address to use as source address in KNX messages,
	 *        specifying <code>null</code> uses the individual address 0.0.0
	 */
	protected KNXMediumSettings(final IndividualAddress device)
	{
		dev = device != null ? device : new IndividualAddress(0);
	}

	/**
	 * Sets the device individual address.
	 * <p>
	 * 
	 * @param device individual address to use as new source address in KNX messages
	 */
	public final synchronized void setDeviceAddress(final IndividualAddress device)
	{
		dev = device;
	}

	/**
	 * Returns the device individual address.
	 * <p>
	 * 
	 * @return individual address
	 */
	public final synchronized IndividualAddress getDeviceAddress()
	{
		return dev;
	}

	/**
	 * Returns the KNX medium type identifier specifying the communication medium this
	 * setting object is for.
	 * <p>
	 * 
	 * @return KNX medium type ID
	 */
	public abstract int getMedium();
	
	/**
	 * Returns a textual representation of the KNX medium type.
	 * <p>
	 * 
	 * @return KNX medium as string
	 * @see #getMedium()
	 */
	public String getMediumString()
	{
		switch (getMedium()) {
		case MEDIUM_TP0:
			return "TP0";
		case MEDIUM_TP1:
			return "TP1";
		case MEDIUM_PL110:
			return "PL110";
		case MEDIUM_PL132:
			return "PL132";
		case MEDIUM_RF:
			return "RF";
		default:
			return "unknown";
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getMediumString() + " medium device " + dev;
	}
}

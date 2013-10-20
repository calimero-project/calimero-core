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

/**
 * Provides settings necessary for communication on TP (twisted pair) medium.
 * <p>
 * This settings type is used for twisted pair medium TP0 and TP1.
 * 
 * @author B. Malinowsky
 */
public class TPSettings extends KNXMediumSettings
{
	/**
	 * Default setting for TP0, device address is 0.0.0.
	 * <p>
	 */
	public static final TPSettings TP0 = new TPSettings(false);

	/**
	 * Default setting for TP1, device address is 0.0.0.
	 * <p>
	 */
	public static final TPSettings TP1 = new TPSettings(true);

	private final boolean tp1;

	/**
	 * Creates a new settings container with TP medium specific information.
	 * <p>
	 * 
	 * @param device individual device address to use as source address in KNX messages,
	 *        specifying <code>null</code> uses the individual address 0.0.0
	 * @param mediumTP1 <code>true</code> if communicating on TP1, <code>false</code>
	 *        if communicating on TP0
	 */
	public TPSettings(final IndividualAddress device, final boolean mediumTP1)
	{
		super(device);
		tp1 = mediumTP1;
	}

	/**
	 * Creates a new default container with settings for TP medium.
	 * <p>
	 * The device address is initialized to 0.0.0.
	 * 
	 * @param mediumTP1 <code>true</code> if communicating on TP1, <code>false</code>
	 *        if communicating on TP0
	 */
	public TPSettings(final boolean mediumTP1)
	{
		super(null);
		tp1 = mediumTP1;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.KNXMediumSettings#getMedium()
	 */
	public int getMedium()
	{
		return tp1 ? MEDIUM_TP1 : MEDIUM_TP0;
	}

	/**
	 * Returns whether this setting is for communication on TP1 medium or TP0.
	 * <p>
	 * 
	 * @return <code>true</code> for TP1, <code>false</code> for TP0
	 */
	public final boolean isTP1()
	{
		return tp1;
	}
}

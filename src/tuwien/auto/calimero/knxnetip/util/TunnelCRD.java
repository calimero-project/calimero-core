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

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel;

/**
 * Connection response data used for KNX tunneling connection.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class TunnelCRD extends CRD
{
	/**
	 * Creates a new CRD for tunnel connection type out of a byte array.
	 * <p>
	 * The CRD structure has a length of 4 bytes.<br>
	 * 
	 * @param data byte array containing a CRD structure,
	 *        <code>data.length - offset = 4</code>
	 * @param offset start offset of CRD in <code>data</code>
	 * @throws KNXFormatException if no CRD found or invalid structure
	 */
	public TunnelCRD(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (getConnectionType() != KNXnetIPTunnel.TUNNEL_CONNECTION)
			throw new KNXFormatException("not a tunneling CRD", getConnectionType());
		if (getStructLength() != 4)
			throw new KNXFormatException("wrong length for tunneling CRD");
	}

	/**
	 * Creates a new CRD for tunnel connection type containing the given address
	 * information.
	 * <p>
	 * 
	 * @param addr individual address assigned to the tunneling connection
	 */
	public TunnelCRD(final IndividualAddress addr)
	{
		super(KNXnetIPTunnel.TUNNEL_CONNECTION, addr.toByteArray());
	}

	/**
	 * Creates a CRD for tunnel connection type containing optional data.
	 * <p>
	 * Note, the optional data field contains the assigned individual address.<br>
	 * 
	 * @param optionalData byte array containing tunneling host protocol data, this
	 *        information is located starting at offset 2 in the CRD structure,
	 *        <code>optionalData.length</code> = 2
	 */
	TunnelCRD(final byte[] optionalData)
	{
		super(KNXnetIPTunnel.TUNNEL_CONNECTION, (byte[]) optionalData.clone());
		if (getStructLength() != 4)
			throw new KNXIllegalArgumentException("wrong length for tunneling CRD");
	}

	/**
	 * Returns the assigned address for the tunneling connection.
	 * <p>
	 * 
	 * @return individual address as {@link IndividualAddress}
	 */
	public final IndividualAddress getAssignedAddress()
	{
		return new IndividualAddress(opt);
	}

	/**
	 * Returns a textual representation of this tunnel CRD.
	 * <p>
	 * 
	 * @return a string representation of the object
	 */
	public String toString()
	{
		return "tunneling CRD, assigned address " + getAssignedAddress();
	}
}

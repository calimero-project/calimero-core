/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

package io.calimero.knxnetip.util;

import static io.calimero.knxnetip.KNXnetIPTunnel.TUNNEL_CONNECTION;

import java.util.Optional;

import io.calimero.IndividualAddress;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;

/**
 * Connection request information used for KNX tunneling connection.
 *
 * @author B. Malinowsky
 */
public class TunnelCRI extends CRI
{
	/**
	 * Creates a new CRI for tunnel connection type out of a byte array. The CRI structure has a length of 4 or 6 bytes.
	 *
	 * @param data byte array containing a CRI structure, {@code data.length - offset = 4 or 6}
	 * @param offset start offset of CRI in {@code data}
	 * @throws KNXFormatException if no CRI found or invalid structure
	 */
	public TunnelCRI(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (getConnectionType() != TUNNEL_CONNECTION)
			throw new KNXFormatException("not a tunneling CRI", getConnectionType());
		if (!validLength())
			throw new KNXFormatException("wrong length for tunneling CRI");
	}

	/**
	 * Creates a new CRI for tunnel connection type on the given KNX layer.
	 *
	 * @param knxLayer KNX layer specifying the kind of tunnel, e.g., link layer tunnel
	 */
	public TunnelCRI(final TunnelingLayer knxLayer)
	{
		super(TUNNEL_CONNECTION, new byte[] { (byte) knxLayer.getCode(), 0 });
	}

	/**
	 * Creates a new extended CRI for tunnel connection type on the given KNX layer and the tunneling invdividual
	 * address.
	 *
	 * @param knxLayer KNX layer specifying the kind of tunnel, e.g., link layer tunnel
	 * @param tunnelingAddress request this specific address to be used for the tunneling connection
	 */
	public TunnelCRI(final TunnelingLayer knxLayer, final IndividualAddress tunnelingAddress) {
		super(TUNNEL_CONNECTION, new byte[] { (byte) knxLayer.getCode(), 0, (byte) (tunnelingAddress.getRawAddress() >> 8),
			(byte) tunnelingAddress.getRawAddress() });
	}

	/**
	 * Creates a CRI for tunnel connection type containing optional data.
	 * <p>
	 * Note, the optional data field contains the KNX tunnel layer.<br>
	 *
	 * @param optionalData byte array containing tunneling host protocol data, this
	 *        information is located starting at offset 2 in the CRI structure,
	 *        {@code optionalData.length} = 2
	 */
	TunnelCRI(final byte[] optionalData)
	{
		super(TUNNEL_CONNECTION, optionalData.clone());
		if (!validLength())
			throw new KNXIllegalArgumentException("wrong length for tunneling CRI");
	}

	/**
	 * Returns the requested tunneling layer.
	 *
	 * @return tunneling layer
	 */
	public final TunnelingLayer tunnelingLayer()
	{
		return TunnelingLayer.from(opt[0] & 0xFF);
	}

	public final Optional<IndividualAddress> tunnelingAddress() {
		if (getStructLength() == 6)
			return Optional.of(new IndividualAddress(new byte[] { opt[2], opt[3] }));
		return Optional.empty();
	}

	/**
	 * Returns a textual representation of this tunnel CRI.
	 *
	 * @return a string representation of the object
	 */
	@Override
	public String toString()
	{
		return "tunneling CRI, KNX " + tunnelingLayer();
	}

	private boolean validLength() {
		return getStructLength() == 4 || getStructLength() == 6;
	}
}

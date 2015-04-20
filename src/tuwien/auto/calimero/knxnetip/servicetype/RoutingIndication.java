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

package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIFactory;

/**
 * Represents a routing indication for routing services.
 * <p>
 * The data format contained in routing messages is cEMI. It is used to send a cEMI
 * message over IP networks. The routing indication service is unconfirmed.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 */
public class RoutingIndication extends ServiceType
{
	private final CEMI cemi;

	/**
	 * Creates a routing indication carrying the given cEMI frame.
	 * <p>
	 * 
	 * @param frame cEMI frame to be routed over IP networks
	 */
	public RoutingIndication(final CEMI frame)
	{
		super(KNXnetIPHeader.ROUTING_IND);
		cemi = CEMIFactory.copy(frame);
	}

	/**
	 * Creates a routing indication out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a indication structure
	 * @param offset start offset of indication in <code>data</code>
	 * @param length length in bytes of whole indication structure
	 * @throws KNXFormatException if the contained cEMI frame could not be created
	 */
	public RoutingIndication(final byte[] data, final int offset, final int length)
		throws KNXFormatException
	{
		super(KNXnetIPHeader.ROUTING_IND);
		cemi = CEMIFactory.create(data, offset, length);
	}

	/**
	 * Returns the cEMI frame contained in the indication.
	 * <p>
	 * 
	 * @return a cEMI type
	 */
	public final CEMI getCEMI()
	{
		return CEMIFactory.copy(cemi);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return cemi.getStructLength();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		final byte[] buf = cemi.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

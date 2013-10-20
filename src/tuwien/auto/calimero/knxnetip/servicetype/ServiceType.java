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

package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Common base for the different service type structures.
 * <p>
 * 
 * @author B. Malinowsky
 */
abstract class ServiceType
{
	static final LogService logger =
		LogManager.getManager().getLogService("KNXnet/IP service");

	final int svcType;

	ServiceType(final int serviceType)
	{
		if (serviceType < 0 || serviceType > 0xffff)
			throw new KNXIllegalArgumentException("service type out of range [0..0xffff]");
		svcType = serviceType;
	}

	/**
	 * Returns the service type structure formatted into a byte array.
	 * <p>
	 * 
	 * @return service type structure as byte array
	 * @see PacketHelper
	 */
	public final byte[] toByteArray()
	{
		return toByteArray(new ByteArrayOutputStream(50));
	}

	/**
	 * Returns the service type name of this service type.
	 * <p>
	 * 
	 * @return service type as string
	 */
	public String toString()
	{
		return KNXnetIPHeader.getSvcName(svcType);
	}

	abstract byte[] toByteArray(ByteArrayOutputStream os);

	abstract int getStructLength();
}

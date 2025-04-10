/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;

import io.calimero.KNXIllegalArgumentException;
import io.calimero.log.LogService;

/**
 * Common base for the different service type structures.
 *
 * @author B. Malinowsky
 */
abstract class ServiceType implements io.calimero.ServiceType
{
	static final Logger logger = LogService.getLogger("KNXnet/IP service");

	final int svcType;

	ServiceType(final int serviceType)
	{
		if (serviceType < 0 || serviceType > 0xffff)
			throw new KNXIllegalArgumentException("service type out of range [0..0xffff]");
		svcType = serviceType;
	}

	/**
	 * Returns the service type structure formatted into a byte array.
	 *
	 * @return service type structure as byte array
	 * @see PacketHelper
	 */
	@Override
	public final byte[] toByteArray()
	{
		return toByteArray(new ByteArrayOutputStream(50));
	}

	/**
	 * Returns the service type name of this service type.
	 *
	 * @return service type as string
	 */
	@Override
	public String toString()
	{
		return KNXnetIPHeader.getSvcName(svcType);
	}

	abstract byte[] toByteArray(ByteArrayOutputStream os);
}

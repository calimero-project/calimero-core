/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019 B. Malinowsky

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

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * Represents a KNX IP routing system broadcast, containing a system broadcast frame in cEMI format.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 */
public class RoutingSystemBroadcast extends ServiceType {
	private final CEMI cemi;

	/**
	 * Creates a routing system broadcast carrying the given cEMI frame.
	 *
	 * @param frame system broadcast cEMI frame to be routed over IP networks
	 */
	public RoutingSystemBroadcast(final CEMI frame) {
		super(KNXnetIPHeader.RoutingSystemBroadcast);
		if (!isSystemBroadcast(frame))
			throw new KNXIllegalArgumentException("invalid frame for IP system broadcast");
		cemi = CEMIFactory.copy(frame);
	}

	/**
	 * Creates a routing system broadcast from a byte array.
	 *
	 * @param data byte array containing an indication structure
	 * @param offset start offset of indication in <code>data</code>
	 * @param length length in bytes of whole indication structure
	 * @throws KNXFormatException if the contained cEMI frame could not be created or is not a valid system broadcast
	 */
	public RoutingSystemBroadcast(final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(KNXnetIPHeader.RoutingSystemBroadcast);
		cemi = CEMIFactory.create(data, offset, length);
		if (!isSystemBroadcast(cemi))
			throw new KNXFormatException("cEMI frame is not a valid system broadcast " + cemi);
	}

	/**
	 * Returns the cEMI frame contained in this routing system broadcast.
	 *
	 * @return a cEMI type
	 */
	public final CEMI cemi() { return CEMIFactory.copy(cemi); }

	@Override
	int getStructLength() { return cemi.getStructLength(); }

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os) {
		final byte[] buf = cemi.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}

	// with a secure APDU or sync.req/res, the SBC currently has to be checked at an upper layer
	public static boolean isSystemBroadcast(final CEMI frame) {
		if (frame.getMessageCode() == CEMILData.MC_LDATA_IND && frame instanceof CEMILData) {
			final CEMILData ldata = (CEMILData) frame;
			final KNXAddress dst = ldata.getDestination();
			return ldata.isSystemBroadcast() && dst instanceof GroupAddress && dst.getRawAddress() == 0;
		}
		return false;
	}
}

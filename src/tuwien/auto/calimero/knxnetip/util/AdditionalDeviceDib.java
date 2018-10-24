/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip.util;

import java.nio.ByteBuffer;

import tuwien.auto.calimero.DeviceDescriptor;
import tuwien.auto.calimero.DeviceDescriptor.DD0;
import tuwien.auto.calimero.KNXFormatException;;

/**
 * Device DIB, providing additional information to {@link DeviceDIB}. Objects of this type are immutable.
 */
public class AdditionalDeviceDib extends DIB {
	private static final int dibSize = 8;

	private final int status;
	private final int maxApduLength;
	private final DD0 dd;

	// medium status is PID_MEDIUM_STATUS (PID = 51)
	public AdditionalDeviceDib(final int mediumStatus, final int maxApduLength, final DeviceDescriptor.DD0 dd) {
		super(dibSize, DIB.AdditionalDeviceInfo);
		this.status = mediumStatus;
		this.maxApduLength = maxApduLength;
		this.dd = dd;
	}

	public AdditionalDeviceDib(final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(data, offset);
		if (type != AdditionalDeviceInfo)
			throw new KNXFormatException("not a device DIB", type);
		if (length != dibSize)
			throw new KNXFormatException("device DIB with wrong size, expected " + dibSize + " bytes", length);

		final ByteBuffer buf = ByteBuffer.wrap(data, offset + 2, length - 2);
		status = buf.get() & 0xff;
		buf.get(); // reserved
		maxApduLength = buf.getShort() & 0xffff;
		dd = DD0.from(buf.getShort() & 0xffff);
	}

	public final int maxApduLength() {
		return maxApduLength;
	}

	public final DD0 deviceDescriptor() {
		return dd;
	}

	// bit 0 of medium status is communication possible/impossible
	boolean communicationPossible() {
		return (status & 1) == 0;
	}

	@Override
	public String toString() {
		final String s = communicationPossible() ? "possible" : "impossible";
		return "DD0 " + dd + " communication " + s + " max APDU length " + maxApduLength;
	}

	@Override
	public byte[] toByteArray() {
		final ByteBuffer buf = ByteBuffer.wrap(super.toByteArray()).position(2);
		buf.put((byte) status);
		buf.put((byte) 0);
		buf.putShort((short) maxApduLength);
		buf.put(dd.toByteArray());
		return buf.array();
	}
}

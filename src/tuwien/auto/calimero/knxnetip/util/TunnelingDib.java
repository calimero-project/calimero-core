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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Tunneling DIB. Objects of this type are immutable.
 */
public class TunnelingDib extends DIB {
	private final short maxApduLength;
	private final IndividualAddress[] addresses;
	// Each address comes with a status, being a bit field:
	// Bit 0: Free, slot is not occupied
	// Bit 1: Authorized, client requires authorization for this slot
	// Bit 2: Usable, slot is currently usable
	private final int[] status;

	public TunnelingDib(final List<IndividualAddress> addresses, final int[] status) {
		this((short) 0xfe, addresses, status);
	}

	public TunnelingDib(final short maxApduLength, final List<IndividualAddress> addresses, final int[] status) {
		super(2 + 2 + 4 * addresses.size(), DIB.Tunneling);

		if (addresses.isEmpty())
			throw new KNXIllegalArgumentException("at least one address must be given");
		if (addresses.size() != status.length)
			throw new KNXIllegalArgumentException("list sizes of addresses and status must be equal");

		this.maxApduLength = maxApduLength;
		this.addresses = addresses.toArray(new IndividualAddress[0]);
		this.status = status.clone();
	}

	public TunnelingDib(final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(data, offset);

		if (type != Tunneling)
			throw new KNXFormatException("not a tunneling DIB", type);
		if (length < 4)
			throw new KNXFormatException("tunneling DIB too short", length);

		final ByteBuffer buf = ByteBuffer.wrap(data, offset + 2, length - 2);
		maxApduLength = buf.getShort();

		final int entries = buf.remaining() / 4;
		addresses = new IndividualAddress[entries];
		status = new int[entries];
		for (int i = 0; i < entries; i++) {
			addresses[i] = new IndividualAddress(buf.getShort() & 0xffff);
			buf.get(); // reserved
			status[i] = buf.get() & 0xff;
		}
	}

	private static String formatStatus(final int status) {
		final List<String> l = new ArrayList<>();
		l.add((status & 1) == 1 ? "free" : "occupied");
		if ((status & 2) == 2)
			l.add("authorized");
		if ((status & 4) == 4)
			l.add("usable");
		return l.stream().collect(Collectors.joining(", "));
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("maximum APDU length: " + String.valueOf(maxApduLength) + " ");
		for (int i = 0; i < addresses.length; i++)
			sb.append(addresses[i] + " (" + formatStatus(status[i]) + ") ");
		return sb.toString();
	}

	@Override
	public byte[] toByteArray() {
		final ByteBuffer buf = ByteBuffer.wrap(super.toByteArray()).position(2);
		buf.putShort(maxApduLength);
		for (int k = 0; k < addresses.length; k++) {
			buf.put(addresses[k].toByteArray());
			buf.put((byte) 0); // reserved
			buf.put((byte) status[k]);
		}
		return buf.array();
	}
}

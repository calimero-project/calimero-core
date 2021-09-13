/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2021 B. Malinowsky

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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Tunneling DIB, containing the maximum supported APDU length and tunneling slot information of the tunneling interface.
 * The tunneling slot information indicates how many tunneling connections the interface can serve, which individual
 * addresses will be used for those connections, and the current tunneling slot status.<br>
 * Objects of this type are immutable.
 */
public class TunnelingDib extends DIB {

	/**
	 * Tunneling slot status.
	 */
	public enum SlotStatus {
		/**
		 * Indicates whether a tunneling slot is currently free or not (occupied).
		 */
		Free,

		/**
		 * Indicates the authorization of a tunneling slot. The interpretation is as follows:
		 * <ul>
		 * <li>Within a KNX IP Secure session: set if the client would be authorized to use that slot, cleared otherwise</li>
		 * <li>Outside a KNX IP Secure session: indicates whether the slot requires authorization or not; set if no
		 * authorization is required, cleared otherwise</li>
		 * </ul>
		 */
		Authorized,

		/**
		 * Indicates whether a tunneling slot is currently usable or not.
		 * A tunneling slot might not be usable due to, e.g., otherwise used address of the tunneling slot's address,
		 * disrupted connection to the KNX medium, or implementation-specific criteria.
		 */
		Usable;

		public int value() { return (int) Math.pow(2, ordinal()); }

		public static SlotStatus of(final int value) {
			switch (value) {
				case 1: return Free;
				case 2: return Authorized;
				case 4: return Usable;
			}
			throw new KNXIllegalArgumentException(value + " is not a valid status value");
		}
	}

	private final int maxApduLength;
	private final Map<IndividualAddress, EnumSet<SlotStatus>> slots;


	public TunnelingDib(final Map<IndividualAddress, EnumSet<SlotStatus>> tunnelingSlots) {
		this((short) 0xfe, tunnelingSlots);
	}

	public TunnelingDib(final int maxApduLength, final Map<IndividualAddress, EnumSet<SlotStatus>> tunnelingSlots) {
		super(2 + 2 + 4 * tunnelingSlots.size(), DIB.TunnelingInfo);

		if (tunnelingSlots.isEmpty())
			throw new KNXIllegalArgumentException("at least one address must be given");
		if (2 + 2 + 4 * tunnelingSlots.size() > 254)
			throw new KNXIllegalArgumentException(tunnelingSlots.size() + " slots exceed DIB size limit");

		this.maxApduLength = maxApduLength;
		slots = deepCopy(tunnelingSlots);
	}

	public TunnelingDib(final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(data, offset);

		if (type != TunnelingInfo)
			throw new KNXFormatException("not a tunneling DIB", type);
		if (length < 8)
			throw new KNXFormatException("tunneling DIB too short", length);
		if (length > 254)
			throw new KNXFormatException("tunneling DIB exceeds max size", length);

		final ByteBuffer buf = ByteBuffer.wrap(data, offset + 2, length - 2);
		maxApduLength = buf.getShort() & 0xffff;

		slots = new LinkedHashMap<>();
		final int entries = buf.remaining() / 4;
		for (int i = 0; i < entries; i++) {
			final var address = new IndividualAddress(buf.getShort() & 0xffff);
			buf.get(); // reserved
			final int status = buf.get() & 0x07;
			slots.put(address, toStatusSet(status));
		}
	}

	/**
	 * @return the maximum supported APDU length of the tunneling interface for KNX network access
	 */
	public int maxApduLength() { return maxApduLength; }

	/**
	 * @return tunneling slot information
	 */
	public final Map<IndividualAddress, EnumSet<SlotStatus>> slots() {
		return deepCopy(slots);
	}

	@Override
	public String toString() {
		final var joiner = Collectors.joining(", ", "maximum APDU length: " + maxApduLength + ", tunneling slots: ", "");
		return slots.entrySet().stream().map(slot -> slot.getKey() + " " + slot.getValue()).collect(joiner);
	}

	@Override
	public byte[] toByteArray() {
		final ByteBuffer buf = ByteBuffer.wrap(super.toByteArray()).position(2);
		buf.putShort((short) maxApduLength);
		for (final var slot : slots.entrySet()) {
			buf.put(slot.getKey().toByteArray());
			buf.put((byte) 0xff); // reserved
			final int status = slot.getValue().stream().mapToInt(SlotStatus::value).sum();
			buf.put((byte) (status | 0xf8));
		}
		return buf.array();
	}

	@Override
	public int hashCode() {
		return Objects.hash(maxApduLength, slots);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof TunnelingDib))
			return false;
		final TunnelingDib other = (TunnelingDib) obj;
		return maxApduLength == other.maxApduLength && slots.equals(other.slots);
	}

	private static EnumSet<SlotStatus> toStatusSet(final int status) {
		final var set = EnumSet.noneOf(SlotStatus.class);
		for (final var v : SlotStatus.values())
			if (((status >> v.ordinal()) & 1) == 1)
				set.add(v);
		return set;
	}

	private static Map<IndividualAddress, EnumSet<SlotStatus>> deepCopy(final Map<IndividualAddress, EnumSet<SlotStatus>> map) {
		final var copy = new LinkedHashMap<>(map);
		copy.replaceAll((__, set) -> set.clone());
		return copy;
	}
}

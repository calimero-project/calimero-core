/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2015, 2023 B. Malinowsky

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.calimero.IndividualAddress;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * Represents a KNX addresses description information block. DIBs of this type are used for
 * KNXnet/IP remote diagnosis and configuration, and optionally in a description response. Objects
 * of this type are immutable.
 *
 * @author B. Malinowsky
 * @see io.calimero.knxnetip.servicetype.DescriptionResponse
 */
public final class KnxAddressesDIB extends DIB
{
	private static final int DIB_MIN_SIZE = 4;

	private final List<IndividualAddress> addresses = new ArrayList<>();

	/**
	 * Creates a KNX addresses DIB out of a byte array.
	 *
	 * @param data byte array containing device DIB structure
	 * @param offset start offset of DIB in {@code data}
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public KnxAddressesDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != KNX_ADDRESSES)
			throw new KNXFormatException("no KNX addresses DIB, wrong type ID", type);
		if (size < DIB_MIN_SIZE)
			throw new KNXFormatException("KNX addresses DIB too short, < " + DIB_MIN_SIZE, size);
		if (size % 2 != 0)
			throw new KNXFormatException("KNX address DIB requires even size");
		for (int i = offset + 2; i < offset + size; i += 2) {
			final int a = (data[i] & 0xff) << 8 | (data[i + 1] & 0xff);
			addresses.add(new IndividualAddress(a));
		}
	}

	/**
	 * Creates a KNX addresses DIB using the supplied KNX individual addresses.
	 *
	 * @param knxAddresses the KNX {@link IndividualAddress}es to add to the description
	 *        information, containing at least one KNX individual address
	 */
	public KnxAddressesDIB(final Collection<? extends IndividualAddress> knxAddresses)
	{
		super(2 + 2 * knxAddresses.size(), KNX_ADDRESSES);
		if (knxAddresses.isEmpty())
			throw new KNXIllegalArgumentException("at least one KNX address is required");
		addresses.addAll(knxAddresses);
	}

	/**
	 * {@return an unmodifiable list with the {@link IndividualAddress}es contained in this DIB, list size &ge; 1}
	 */
	public List<IndividualAddress> getAddresses()
	{
		return Collections.unmodifiableList(addresses);
	}

	@Override
	public String toString()
	{
		return addresses.stream().map(IndividualAddress::toString).collect(Collectors.joining(", "));
	}

	@Override
	public byte[] toByteArray()
	{
		final byte[] buf = super.toByteArray();
		int i = 2;
		for (final IndividualAddress ia : addresses) {
			final byte[] raw = ia.toByteArray();
			buf[i++] = raw[0];
			buf[i++] = raw[1];
		}
		return buf;
	}

	@Override
	public int hashCode() {
		return addresses.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final KnxAddressesDIB other))
			return false;
		return addresses.equals(other.addresses);
	}
}

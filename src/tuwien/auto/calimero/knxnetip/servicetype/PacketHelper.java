/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2021 B. Malinowsky

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
import java.nio.ByteBuffer;

import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.util.HPAI;

/**
 * Little helpers to handle KNXnet/IP packets and service types.
 *
 * @author B. Malinowsky
 */
public final class PacketHelper
{
	private PacketHelper() {}

	/**
	 * Creates a packet with a KNXnet/IP message header v1.0, containing the specified
	 * service <code>type</code>, and generates the corresponding byte representation
	 * of this structure.
	 *
	 * @param type service type to pack
	 * @return the packet as byte array
	 */
	public static byte[] toPacket(final ServiceType type)
	{
		final KNXnetIPHeader h = new KNXnetIPHeader(type.svcType, type.getStructLength());
		final ByteArrayOutputStream os = new ByteArrayOutputStream(h.getTotalLength());
		os.write(h.toByteArray(), 0, h.getStructLength());
		return type.toByteArray(os);
	}

	public static byte[] toPacket(final int version, final ServiceType type) {
		final KNXnetIPHeader h = new KNXnetIPHeader(type.svcType, version, type.getStructLength());
		final ByteArrayOutputStream os = new ByteArrayOutputStream(h.getTotalLength());
		os.write(h.toByteArray(), 0, h.getStructLength());
		return type.toByteArray(os);
	}

	private static final int SecureSessionRequest = 0x0951;
	private static final int keyLength = 32;

	public static byte[] newChannelRequest(final HPAI hpai, final byte[] ecdhPublicKey) {
		if (ecdhPublicKey.length != keyLength)
			throw new KNXIllegalArgumentException("Diffie-Hellman key required to be 32 bytes");

		final int length = hpai.getStructLength() + ecdhPublicKey.length;
		final KNXnetIPHeader header = new KNXnetIPHeader(SecureSessionRequest, length);

		final ByteBuffer buffer = ByteBuffer.allocate(header.getTotalLength());
		buffer.put(header.toByteArray());
		buffer.put(hpai.toByteArray());
		buffer.put(ecdhPublicKey);
		return buffer.array();
	}
}

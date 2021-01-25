/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2021 B. Malinowsky

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

package tuwien.auto.calimero;

import java.nio.ByteBuffer;

/**
 * Represents a KNX serial number.
 */
public final class SerialNumber {

	/** Size of a KNX serial number in bytes. */
	public static final int Size = 6;

	/** Serial number 0. */
	public static final SerialNumber Zero = SerialNumber.of(0);

	private final long sno;

	private SerialNumber(final long serialNumber) { sno = serialNumber & 0xffff_ffff_ffffL; }

	public static SerialNumber of(final long serialNumber) { return new SerialNumber(serialNumber); }

	/**
	 * Parses the supplied byte array into a serial number.
	 *
	 * @param serialNumber serial number, {@code serialNumber.length = 6}
	 * @return serial number instance
	 */
	public static SerialNumber from(final byte[] serialNumber) { return new SerialNumber(unsigned(serialNumber)); }

	public long number() { return sno; }

	public byte[] array() { return ByteBuffer.allocate(6).putShort((short) (sno >> 32)).putInt((int) sno).array(); }

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SerialNumber && ((SerialNumber) obj).sno == sno;
	}

	@Override
	public int hashCode() { return Long.hashCode(sno); }

	@Override
	public String toString() { return String.format("%04x:%08x", sno >> 32, sno & 0xffff_ffff); }

	private static long unsigned(final byte[] data) {
		if (data.length != Size)
			throw new KNXIllegalArgumentException("invalid size for a KNX serial number");
		long l = 0;
		for (final byte b : data)
			l = (l << 8) + (b & 0xff);
		return l;
	}
}
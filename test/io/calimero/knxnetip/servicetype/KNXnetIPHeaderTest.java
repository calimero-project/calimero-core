/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2026 B. Malinowsky

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

import java.io.IOException;
import java.util.stream.IntStream;

import io.calimero.KNXFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class KNXnetIPHeaderTest {
	// Byte offsets in KNXnet/IP header layout:
	// +---------+--------+-------------+------------+
	// |   0     |   1    |   2     3   |   4    5   |
	// +---------+--------+-------------+------------+
	// | Hdr Size| Version| Service ID  | Total Size |
	// +---------+--------+-------------+------------+

	private static final KNXnetIPHeader hdr = new KNXnetIPHeader(KNXnetIPHeader.CONNECT_REQ, 100);

	@ParameterizedTest
	@MethodSource("ubyteRange")
	void rejectInvalidHeaderSize(final int size) throws IOException, KNXFormatException, InterruptedException {
		final byte[] struct = hdr.toByteArray();
		struct[0] = (byte) size;
		if (size == KNXnetIPHeader.HEADER_SIZE_10)
			KNXnetIPHeader.from(struct, 0);
		else
			assertThrowsFormatException(struct);
	}

	@ParameterizedTest
	@MethodSource("ubyteRange")
	void rejectInvalidVersion(final int version) throws KNXFormatException {
		final byte[] struct = hdr.toByteArray();
		struct[1] = (byte) version;
		if (version == KNXnetIPHeader.KNXNETIP_VERSION_10)
			validHeader(struct);
		else
			assertThrowsFormatException(struct);
	}

	@ParameterizedTest
	@MethodSource("ubyteRange")
	void rejectInvalidTotalLength(final int length) throws KNXFormatException {
		final byte[] struct = hdr.toByteArray();
		struct[4] = 0; // no need for an exhaustive check; != 0 is always valid
		struct[5] = (byte) length;
		if (length < KNXnetIPHeader.HEADER_SIZE_10)
			assertThrowsFormatException(struct);
		else
			validHeader(struct);
	}

	@Test
	void validMaxTotalLength() throws KNXFormatException {
		final byte[] struct = hdr.toByteArray();
		struct[4] = (byte) 0xff;
		struct[5] = (byte) 0xff;
		validHeader(struct);
	}

	private static IntStream ubyteRange() {
		return IntStream.rangeClosed(0, 255);
	}

	private static void assertThrowsFormatException(final byte[] struct) {
		assertThrows(KNXFormatException.class, () -> KNXnetIPHeader.from(struct, 0));
		assertThrows(KNXFormatException.class, () -> new KNXnetIPHeader(struct, 0));
	}

	private static void validHeader(final byte[] struct) throws KNXFormatException {
		KNXnetIPHeader.from(struct, 0);
		new KNXnetIPHeader(struct, 0);
	}
}

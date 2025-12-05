/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2025, 2025 B. Malinowsky

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

package io.calimero.dptxlator;

import java.util.Locale;

import io.calimero.KNXIllegalArgumentException;

/**
 * A datapoint type (DPT) identifier, using a 16-bit main number separated by a dot from a 16-bit subnumber.
 *
 * @param mainNumber DPT main number, {@code 0 < mainNumber < 65536 }
 * @param subNumber DPT subnumber, {@code 0 ≤ subNumber < 65536 }
 */
public record DptId(int mainNumber, int subNumber) {
	public DptId {
		if (mainNumber < 1 || mainNumber > 0xffff)
			throw new KNXIllegalArgumentException("main number ouf of range [1..65535]");
		if (subNumber < 0 || subNumber > 0xffff)
			throw new KNXIllegalArgumentException("subnumber ouf of range [0..65535]");
	}

	public static DptId from(final String dpt) {
		final var split = dpt.split("\\.");
		if (split.length != 2)
			throw new KNXIllegalArgumentException(dpt + " is not a valid DPT identifier");
		return new DptId(Integer.parseUnsignedInt(split[0]), Integer.parseUnsignedInt(split[1]));
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%d.%03d", mainNumber, subNumber);
	}
}

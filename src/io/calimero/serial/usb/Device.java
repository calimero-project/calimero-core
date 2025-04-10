/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2023, 2023 B. Malinowsky

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

package io.calimero.serial.usb;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Provides vendor:product identification and descriptors of a USB device.
 *
 * @param vendorId vendor ID, 0 if not set
 * @param productId product ID, 0 if not set
 * @param serialNumber device serial number (optional), "" if not set
 * @param manufacturer manufacturer description (optional), "" if not set
 * @param product product description (optional), "" if not set
 */
public record Device(int vendorId, int productId, String serialNumber, String manufacturer, String product) {
	public Device {
		Objects.requireNonNull(serialNumber);
		Objects.requireNonNull(manufacturer);
		Objects.requireNonNull(product);
	}

	public Device(final int vendorId, final int productId) {
		this(vendorId, productId, "", "", "");
	}

	public Device(final String serialNumber) {
		this(0, 0, serialNumber, "", "");
	}

	@Override
	public String toString() {
		final var joiner = new StringJoiner(" ").add(String.format("%04x:%04x", vendorId, productId));
		if (!manufacturer.isEmpty())
			joiner.add(manufacturer);
		if (!product.isEmpty())
			joiner.add(product);
		if (!serialNumber.isEmpty())
			joiner.add("S/N").add(serialNumber);
		return joiner.toString();
	}
}

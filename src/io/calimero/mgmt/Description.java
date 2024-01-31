/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

package io.calimero.mgmt;

import java.util.Optional;

/**
 * Holds description information of a KNX interface object property.
 * <p>
 * The supported description information and the expected description structure layout is
 * according to the application layer (extended) property description service.<br>
 * When used together with local device management (LDM), not all description information will be available.
 * <p>
 * Objects of this type are immutable.
 *
 * @param objectIndex index of the interface object in the interface object server containing the property, starting with 0
 * @param objectType interface object type the property belongs to
 * @param objectInstance object instance when used with extended property description services, starting with 1 (0 otherwise)
 * @param pid property identifier, a 6 Bit identifier
 * @param propIndex property index in the object, starting with 0
 * @param pdt property data type; with local device management, the PDT is not available and -1 is used
 * @param dpt datapoint type of the property when used with extended property description services,
 *            otherwise always the empty optional
 * @param writeEnabled specifies if the property is write-enabled or read-only
 * @param currentElements current number of elements in the property
 * @param maxElements maximum number of elements allowed in the property; with local device management,
 *                    this attribute is not available and 0 is used
 * @param readLevel read-access level for the property, a 4 bit value in the range of 0 (maximum access rights) &lt;= level &lt;= 15 (minimum access rights)
 * @param writeLevel write-access level, a 4 bit value in the range of 0 (maximum access rights) &lt;= level &lt;= 15 (minimum access rights)
 *
 * @author B. Malinowsky
 */
public record Description(int objectIndex, int objectType, int objectInstance, int pid, int propIndex, int pdt,
		Optional<String> dpt, boolean writeEnabled, int currentElements, int maxElements, int readLevel,
		int writeLevel) {

	/**
	 * Creates a new description object for a property using the given parameters.
	 *
	 * @param objectIndex index of the object in the device, starting with 0
	 * @param objectType interface object type the property belongs to
	 * @param pid property identifier, a 6 Bit identifier
	 * @param propIndex property index in the object, starting with 0
	 * @param pdt property data type
	 * @param writeEnabled specifies if the property is write-enabled or read only
	 * @param currentElements current number of elements in the property
	 * @param maxElements maximum number of elements allowed in the property
	 * @param readLevel read access level, 0 &lt;= level &lt;= 15
	 * @param writeLevel write access level, 0 &lt;= level &lt;= 15
	 */
	public Description(final int objectIndex, final int objectType, final int pid, final int propIndex, final int pdt,
			final boolean writeEnabled, final int currentElements, final int maxElements, final int readLevel,
			final int writeLevel) {
		this(objectIndex, objectType, 0, pid, propIndex, pdt, Optional.empty(), writeEnabled, currentElements,
				maxElements, readLevel, writeLevel);
	}


	/**
	 * Creates a new description object for a property out of a byte array, together with the interface object type.
	 * <p>
	 * The description structure layout of {@code data} is according to the
	 * application layer standard property description read service.
	 *
	 * @param objectType interface object type the property belongs to
	 * @param data byte array containing property description, starting at {@code data[0]}
	 */
	public static Description from(final int objectType, final byte[] data)
	{
		return from(objectType, 0, data);
	}

	/**
	 * Creates a new description object for a property out of a data byte array, together
	 * with interface object type and number of current elements.
	 * <p>
	 * The description structure layout of {@code data} is according to the
	 * application layer standard property description read service.
	 *
	 * @param objectType interface object type the property belongs to
	 * @param currentElements current number of elements in the property
	 * @param data byte array holding the description information, the structure is
	 *        according to the ASDU of a property description service response
	 */
	public static Description from(final int objectType, final int currentElements, final byte[] data) {
		final int objectInstance = 0;
		final int objectIndex = data[0] & 0xff;
		final int id = data[1] & 0xff;
		final int pindex = data[2] & 0xff;
		final boolean writeEnabled = (data[3] & 0x80) == 0x80;
		final int pdt = data[3] & 0x3f;
		final var dpt = Optional.<String>empty();
		final int maxElements = (data[4] & 0xff) << 8 | (data[5] & 0xff);
		final int readLevel = (data[6] & 0xff) >> 4;
		final int writeLevel = data[6] & 0x0f;

		return new Description(objectIndex, objectType, objectInstance, id, pindex, pdt, dpt, writeEnabled,
				currentElements, maxElements, readLevel, writeLevel);
	}

	// extended property description service
	public static Description fromExtended(final int objectIndex, final byte[] data) {
		final int objectType = (data[0] & 0xff) << 8 | data[1] & 0xff;
		final int objectInstance = (((data[2] & 0xff) << 4) | ((data[3] & 0xf0) >> 4));

		final int pid = (((data[3] & 0xf) << 8) | (data[4] & 0xff));
		// property description type: only type 0 is currently defined
		// reserved for later extensions, e.g., access control with roles
		final int propDescType = (data[5] >> 4) & 0xf;
		final int pindex = (((data[5] & 0xf) << 8) | (data[6] & 0xff));

		final int dptMain = (data[7] & 0xff) << 8 | data[8] & 0xff;
		final int dptSub = (data[9] & 0xff) << 8 | data[10] & 0xff;
		final Optional<String> dpt = dptMain == 0 && dptSub == 0 ? Optional.empty() : Optional.of(dptMain + "." + dptSub);
		final boolean writeEnabled = (data[11] & 0x80) == 0x80;
		final int pdt = data[11] & 0x2f;
		final int maxElements = (data[12] & 0xff) << 8 | data[13] & 0xff;
		final int readLevel = (data[14] & 0xf0) >> 4;
		final int writeLevel = data[14] & 0xf;
		// not provided with extended property services
		final int currentElements = 0;

		return new Description(objectIndex, objectType, objectInstance, pid, pindex, pdt, dpt, writeEnabled,
				currentElements, maxElements, readLevel, writeLevel);
	}

	/**
	 * {@return a textual representation of the property description as string}
	 */
	@Override
	public String toString() {
		final String dptString = dpt.map(s -> " DPT " + s).orElse("");
		return "OT " + objectType + " OI " + objectIndex + " PID " + pid + " PI " + propIndex + " PDT "
				+ (pdt == -1 ? "-" : pdt) + dptString + ", " + currentElements
				+ " elements (max " + maxElements + "), r/w access " + readLevel + "/" + (writeLevel == -1 ? "-" : writeLevel)
				+ (writeEnabled ? " write-enabled" : " read-only");
	}

	/**
	 * {@return the byte representation of this description structure as byte array}
	 */
	public byte[] toByteArray() {
		final byte[] data = new byte[7];
		data[0] = (byte) objectIndex;
		data[1] = (byte) pid;
		data[2] = (byte) propIndex;
		data[3] = (byte) (writeEnabled ? 0x80 : 0x00);
		data[3] |= (byte) (pdt & 0x3f);
		data[4] = (byte) (maxElements >> 8);
		data[5] = (byte) maxElements;
		data[6] = (byte) (readLevel << 4 | (writeLevel & 0x0f));
		return data;
	}

	// set 2 or 4 byte data array with element count, big endian
	static int parseCurrentElements(final byte[] data) {
		int elems = 0;
		for (final byte b : data)
			elems = elems << 8 | (b & 0xff);
		return elems;
	}
}

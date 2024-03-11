/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2022 K.Heimrich
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

package tuwien.auto.calimero.knxnetip.util;

import java.nio.ByteBuffer;
import java.util.Arrays;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.util.ServiceFamiliesDIB.ServiceFamily;

/**
 * Search Request Parameter Block (SRP).
 * <p>
 * A SRP is used to transfer additional information in a KNXnet/IP extended search request.
 * <p>
 * The mandatory flag of the SRP can be used to increase or limit the responses received
 * from an extended search request. If the mandatory flag of the SRP is set, KNXnet/IP router
 * or server will only respond to the extended search request if the search request parameter
 * block is completely satisfied.
 *
 * @param type specifies which kind of search request parameter information is contained in the SRP, one of the
 *        search request parameter block types (see {@link Type})
 * @param isMandatory the mandatory flag of this SRP, {@code true} if the mandatory bit is set, {@code false} otherwise;
 *        to be evaluated by a KNXnet/IP router or server device
 * @param data byte array containing additional data, ignored for select by programming mode SRPs

 * @author Karsten Heimrich
 */
public record Srp(Srp.Type type, boolean isMandatory, byte... data) {
	public Srp {
		switch (type) {
			case Invalid, SelectByProgrammingMode -> data = new byte[0];
			case SelectByMacAddress -> {
				if (data.length != 6)
					throw new KNXIllegalArgumentException("MAC size does not match expected value");
			}
			case SelectByService -> {
				if (data.length != 2)
					throw new KNXIllegalArgumentException("service info size does not match expected value");
			}
			case RequestDibs -> {
				if (data.length < 1)
					throw new KNXIllegalArgumentException("requested DIBs size does not match expected value");
				data = Arrays.copyOf(data, (data.length + 1) / 2 * 2);
			}
		}
	}

	/**
	 * Search request parameter type.
	 */
	public enum Type {
		/**
		 * The invalid type is used to test the behavior of the KNXnet/IP router or server for unknown
		 * SRPs. Please do not use in production.
		 */
		Invalid(0x00),

		/**
		 * The select by programming mode type is used to indicate that a client is interested only in the
		 * response from KNXnet/IP router or server where programming mode is currently enabled.
		 */
		SelectByProgrammingMode(0x01),

		/**
		 * The select by MAC address type is used to indicate that a client is interested only in the
		 * response from KNXnet/IP router or server with the given MAC address.
		 */
		SelectByMacAddress(0x02),

		/**
		 * The select by service type is used to indicate that a client is interested only in the response
		 * from KNXnet/IP router or server supporting the given service family and in at least the given version.
		 */
		SelectByService(0x03),

		/**
		 * The request DIBs type is used to indicate that a client is interested in at least the given
		 * description types.
		 */
		RequestDibs(0x04);

		private final int type;

		private int getValue() {
			return type;
		}

		private static Type from(final int code) {
			for (final Type t : Type.values()) {
				if (code == t.type)
					return t;
			}
			throw new KNXIllegalArgumentException("unspecified SRP type + 0x" + Integer.toHexString(code));
		}

		Type(final int type) {
			this.type = type;
		}
	}


	private static final int SrpHeaderSize = 2;

	/**
	 * Creates a new SRP out of a byte array.
	 *
	 * @param data byte array containing SRP structure
	 * @param offset start offset of SRP in {@code data}
	 * @throws KNXFormatException if no SRP found or invalid structure
	 */
	public static Srp from(final byte[] data, final int offset) throws KNXFormatException {
		if (data.length - offset < SrpHeaderSize)
			throw new KNXFormatException("buffer too short for SRP header");

		final int size = data[offset] & 0xff;
		if (size > data.length - offset)
			throw new KNXFormatException("SRP size bigger than actual data length", size);

		final boolean mandatory = (data[offset + 1] & 0x80) == 0x80;
		final Type type = Type.from(data[offset + 1] & 0x7f);

		byte[] paramData = new byte[0];
		if ((size - SrpHeaderSize) > 0) {
			paramData = new byte[size - SrpHeaderSize];
			System.arraycopy(data, offset + SrpHeaderSize, paramData, 0, size - SrpHeaderSize);
		}

		return new Srp(type, mandatory, paramData);
	}

	/**
	 * Creates a search request parameter block to limit the extended search request to KNXnet/IP router or
	 * server devices where programming mode is currently enabled. The mandatory flag of the SRP is set.
	 *
	 * @return search request parameter block for devices currently in programming mode
	 */
	public static Srp withProgrammingMode() {
		return new Srp(Type.SelectByProgrammingMode, true);
	}

	/**
	 * Creates a search request parameter block to limit the extended search request to KNXnet/IP router
	 * or server devices with the given MAC address. The mandatory flag of the SRP is set.
	 *
	 * @param macAddress the MAC address used in the search request parameter block
	 * @return search request parameter block for devices with a given MAC address
	 */
	public static Srp withMacAddress(final byte[] macAddress) {
		return new Srp(Type.SelectByMacAddress, true, macAddress);
	}

	/**
	 * Creates a search request parameter block to limit the extended search request to KNXnet/IP router
	 * or server devices with the given service family and corresponding family version. The mandatory flag
	 * of the SRP is set.
	 *
	 * @param family the service family used in the search request parameter block
	 * @param familyVersion the family version used in the search request parameter block
	 * @return search request parameter block for devices with a given service family and version
	 */
	public static Srp withService(final ServiceFamily family, final int familyVersion) {
		return new Srp(Type.SelectByService, true, (byte) family.id(), (byte) familyVersion);
	}

	/**
	 * Creates a search request parameter block with a set of description types to indicate a KNXnet/IP router
	 * or server to include corresponding DIBs in the search response. The mandatory flag of the SRP is set.
	 *
	 * @param descriptionType the description type used in the search request parameter block
	 * @param additionalDescriptionTypes additional description types used in the search request parameter block
	 * @return search request parameter block with a set of description types
	 */
	public static Srp withDeviceDescription(final int descriptionType, final int... additionalDescriptionTypes) {
		final ByteBuffer buffer = ByteBuffer.allocate(additionalDescriptionTypes.length + 1);
		for (final int dt : additionalDescriptionTypes)
			buffer.put((byte) dt);
		buffer.put((byte) descriptionType);

		return new Srp(Type.RequestDibs, true, buffer.array());
	}

	/**
	 * Returns the structure length of this SRP in bytes.
	 *
	 * @return structure length as unsigned byte
	 */
	public int structLength() {
		return switch (type) {
			case Invalid, SelectByProgrammingMode -> SrpHeaderSize;
			case SelectByMacAddress -> SrpHeaderSize + 6;
			case SelectByService -> SrpHeaderSize + 2;
			case RequestDibs -> SrpHeaderSize + this.data.length;
		};
	}

	/**
	 * Returns a copy of the data field.
	 * <p>
	 * Data starts at offset 2 in the SRP structure.
	 *
	 * @return byte array with SRP data, can be empty
	 */
	public byte[] data() {
		return data.clone();
	}

	/**
	 * Returns the byte representation of the whole SRP structure.
	 *
	 * @return byte array containing the SRP structure
	 */
	public byte[] toByteArray() {
		final int size = structLength();
		final byte[] buf = new byte[size];
		buf[0] = (byte) size;
		buf[1] = (byte) (isMandatory ? 0x80 : 0x00);
		buf[1] |= (byte) (type.getValue() & 0x07);
		if (data.length > 0)
			System.arraycopy(data, 0, buf, SrpHeaderSize, data.length);
		return buf;
	}
}

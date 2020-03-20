/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2020 K.Heimrich

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

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Search Request Parameter Block (SRP).
 * <p>
 * A SRP is used to transfer additional information in a KNXnet/IP extended search request.
 * <p>
 * The mandatory flag of the SRP can be used to increase or limit the responses received
 * from an extended search request. If the mandatory flag of the SRP is set, KNXnet/IP router
 * or server will only respond to the extended search request if the search request parameter
 * block is completely satisfied.
 * <p>
 *
 * @author Karsten Heimrich
 */
public final class Srp
{
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

	private final int size;
	private final boolean mandatory;
	private final Type type;
	private final byte[] data;

	private static final int SrpHeaderSize = 2;

	/**
	 * Creates a new SRP and initializes basic fields.
	 *
	 * @param srpType one of the search request parameter block types (see {@link Type})
	 * @param isMandatory to be evaluated by a KNXnet/IP router or server device
	 * @param data byte array containing additional data, ignored for select by programming mode SRPs
	 */
	public Srp(final Srp.Type srpType, final boolean isMandatory, final byte... data) {
		type = srpType;
		mandatory = isMandatory;

		switch (srpType) {
		case Invalid:
		case SelectByProgrammingMode:
			size = SrpHeaderSize;
			this.data = new byte[0];
			break;
		case SelectByMacAddress:
			if (data.length != 6)
				throw new KNXIllegalArgumentException("MAC size does not match expected value");
			size = SrpHeaderSize + 6;
			this.data = data;
			break;
		case SelectByService:
			if (data.length != 2)
				throw new KNXIllegalArgumentException("service info size does not match expected value");
			size = SrpHeaderSize + 2;
			this.data = data;
			break;
		case RequestDibs:
			if (data.length < 1)
				throw new KNXIllegalArgumentException("requested DIBs size does not match expected value");
			this.data = new byte[(data.length % 2) == 0 ? data.length : data.length + 1];
			System.arraycopy(data, 0, this.data, 0, data.length);
			size = SrpHeaderSize + this.data.length;
			break;
		default:
			throw new KNXIllegalArgumentException("illegal SRP type + 0x" + Integer.toHexString(srpType.getValue()));
		}
	}

	/**
	 * Creates a new SRP out of a byte array.
	 *
	 * @param data byte array containing SRP structure
	 * @param offset start offset of SRP in <code>data</code>
	 * @throws KNXFormatException if no SRP found or invalid structure
	 */
	public Srp(final byte[] data, final int offset) throws KNXFormatException {
		if (data.length - offset < SrpHeaderSize)
			throw new KNXFormatException("buffer too short for SRP header");

		size = data[offset] & 0xff;
		if (size > data.length - offset)
			throw new KNXFormatException("SRP size bigger than actual data length", size);

		mandatory = (data[offset + 1] & 0x80) == 0x80;
		type = Srp.Type.from(data[offset + 1] & 0x7f);

		if ((size - SrpHeaderSize) > 0) {
			this.data = new byte[size - SrpHeaderSize];
			System.arraycopy(data, offset + SrpHeaderSize, this.data, 0, size - SrpHeaderSize);
		} else {
			this.data = new byte[0];
		}
	}

	/**
	 * Creates a search request parameter block to limit the extended search request to KNXnet/IP router or
	 * server devices where programming mode is currently enabled. The mandatory flag of the SRP is not set.
	 *
	 * @return search request parameter block for devices currently in programming mode
	 */
	public static Srp withProgrammingMode() {
		return new Srp(Type.SelectByProgrammingMode, true);
	}

	/**
	 * Creates a search request parameter block to limit the extended search request to KNXnet/IP router
	 * or server devices with the given MAC address. The mandatory flag of the SRP is not set.
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
	 * of the SRP is not set.
	 *
	 * @param familyId the family ID used in the in the search request parameter block
	 * @param familyVersion the family version used in the in the search request parameter block
	 * @return search request parameter block for devices with a given service family and version
	 */
	public static Srp withService(final int familyId, final int familyVersion) {
		return new Srp(Type.SelectByService, true, (byte) familyId, (byte) familyVersion);
	}

	/**
	 * Creates a search request parameter block with a set of description types to indicate a KNXnet/IP router
	 * or server to include corresponding DIBs in the search response. The mandatory flag of the SRP is not set.
	 *
	 * @param descriptionType the description type used in the in the search request parameter block
	 * @param additionalDescriptionTypes additional description types used in the in the search request parameter block
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
	public int getStructLength() {
		return size;
	}

	/**
	 * Returns the type of this SRP.
	 * <p>
	 * The type specifies which kind of search request parameter information is contained in
	 * the SRP.
	 *
	 * @return search request parameter type (see {@link Type})
	 */
	public Srp.Type getType() {
		return type;
	}

	/**
	 * Returns the mandatory flag of this SRP.
	 *
	 * @return <code>true</code> if the mandatory bit is set, <code>false</code> otherwise
	 */
	public boolean isMandatory() {
		return mandatory;
	}

	/**
	 * Returns a copy of the data field.
	 * <p>
	 * Data starts at offset 2 in the SRP structure.
	 *
	 * @return byte array with SRP data, can be empty
	 */
	public byte[] getData() {
		return data.clone();
	}

	/**
	 * Returns the byte representation of the whole SRP structure.
	 *
	 * @return byte array containing the SRP structure
	 */
	public byte[] toByteArray() {
		final byte[] buf = new byte[size];
		buf[0] = (byte) size;
		buf[1] = (byte) (mandatory ? 0x80 : 0x00);
		buf[1] |= (byte) (type.getValue() & 0x07);
		if (data.length > 0)
			System.arraycopy(data, 0, buf, SrpHeaderSize, data.length);
		return buf;
	}
}

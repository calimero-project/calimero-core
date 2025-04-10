/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2019, 2023 B. Malinowsky

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

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import io.calimero.DataUnitBuilder;
import io.calimero.GroupAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIFactory;
import io.calimero.cemi.CEMILData;

/**
 * Represents a KNX IP routing system broadcast, containing a system broadcast frame in cEMI format.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 */
public class RoutingSystemBroadcast extends ServiceType {
	private final CEMI cemi;

	/**
	 * Creates a routing system broadcast carrying the given cEMI frame.
	 *
	 * @param frame system broadcast cEMI frame to be routed over IP networks
	 */
	public RoutingSystemBroadcast(final CEMI frame) {
		super(KNXnetIPHeader.RoutingSystemBroadcast);
		if (!validSystemBroadcast(frame))
			throw new KNXIllegalArgumentException("invalid frame for IP system broadcast");
		cemi = CEMIFactory.copy(frame);
	}

	/**
	 * Creates a routing system broadcast from a byte array.
	 *
	 * @param data byte array containing an indication structure
	 * @param offset start offset of indication in {@code data}
	 * @param length length in bytes of whole indication structure
	 * @throws KNXFormatException if the contained cEMI frame could not be created or is not a valid system broadcast
	 */
	public RoutingSystemBroadcast(final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(KNXnetIPHeader.RoutingSystemBroadcast);
		cemi = CEMIFactory.create(data, offset, length);
		if (!validSystemBroadcast(cemi))
			throw new KNXFormatException("cEMI frame is not a valid system broadcast " + cemi);
	}

	/**
	 * Returns the cEMI frame contained in this routing system broadcast.
	 *
	 * @return a cEMI type
	 */
	public final CEMI cemi() { return CEMIFactory.copy(cemi); }

	@Override
	public int length() { return cemi.getStructLength(); }

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os) {
		final byte[] buf = cemi.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}

	private static final int SystemNetworkParamRead = 0b0111001000;
	private static final int SystemNetworkParamResponse = 0b0111001001;
	private static final int DoASerialNumberWrite = 0b1111101110;
	private static final int SecureService = 0b1111110001;

	private static final int SecureDataPdu = 0;
	private static final int SecureSyncRequest = 2;
	private static final int SecureSyncResponse = 3;

	/**
	 * Is the cEMI {@code frame} a subnet broadcast message that shall be forwarded as IP system broadcast on IP side.
	 *
	 * Note: this condition is important for routing from TP1 to IP, when IP System Broadcast Routing mode is enabled.
	 *
	 * @param frame the frame to check
	 * @return {@code true} if {@code frame} matches the rules for an IP system broadcast, {@code false} otherwise
	 */
	public static boolean isSubnetSystemBroadcast(final CEMI frame) {
		return payload(frame).map(RoutingSystemBroadcast::subnetSystemBroadcast).orElse(false);
	}

	/**
	 * Is the cEMI {@code frame} contained in a received IP system broadcast applicable to be forwarded as subnet
	 * broadcast.
	 *
	 * Note: this condition is important for routing from IP to TP1, when IP System Broadcast Routing mode is enabled.
	 *
	 * @param frame the frame to check
	 * @return {@code true} if {@code frame} matches the rules for a subnet broadcast, {@code false} otherwise
	 */
	public static boolean isIpSystemBroadcast(final CEMI frame) {
		return payload(frame).map(RoutingSystemBroadcast::ipSystemBroadcast).orElse(false);
	}

	public static boolean validSystemBroadcast(final CEMI frame) {
		return payload(frame).map(tpdu -> subnetSystemBroadcast(tpdu) || ipSystemBroadcast(tpdu)).orElse(false);
	}

	private static boolean subnetSystemBroadcast(final byte[] tpdu) {
		return switch (DataUnitBuilder.getAPDUService(tpdu)) {
			case SystemNetworkParamRead ->
				// pdu: 2 bytes APCI + 2 bytes obj type + 2 bytes PID + 1 byte operand
				tpdu.length == 7 && tpdu[2] == 0 && tpdu[3] == 0 && tpdu[4] == 0 && (tpdu[5] & 0xff) == (11 << 4)
							&& tpdu[6] == 1;

			// DoA serial number write service with IP domain
			case DoASerialNumberWrite -> tpdu.length == 2 + 6 + 4; // APCI + SNo + mcast group

			// we look for secure data or sync request, and require tool access, system broadcast, A+C
			case SecureService -> secureService(tpdu, SecureDataPdu, SecureSyncRequest);
			default -> false;
		};
	}

	private static boolean ipSystemBroadcast(final byte[] tpdu) {
		return switch (DataUnitBuilder.getAPDUService(tpdu)) {
			case SystemNetworkParamResponse ->
				// pdu: 2 bytes APCI + 2 bytes obj type + 2 bytes PID + 1 byte operand + 6 bytes serial no
				tpdu.length == 13 && tpdu[2] == 0 && tpdu[3] == 0 && tpdu[4] == 0 && (tpdu[5] & 0xff) == (11 << 4)
						&& tpdu[6] == 1;

			// we look for sync response, and require tool access, system broadcast, A+C
			case SecureService -> secureService(tpdu, SecureSyncResponse, SecureSyncResponse);
			default -> false;
		};
	}

	private static boolean secureService(final byte[] tpdu, final int svc1, final int svc2) {
		final int scf = tpdu[2] & 0xff;
		final boolean toolAccess = (scf & 128) == 128;
		final int algorithmId = (scf >> 4) & 0x7;
		final boolean systemBroadcast = (scf & 0x8) == 0x8;
		final int service = scf & 0x3;
		return (service == svc1 || service == svc2) && toolAccess && systemBroadcast && algorithmId == 1;
	}

	private static Optional<byte[]> payload(final CEMI frame) {
		if (frame.getMessageCode() == CEMILData.MC_LDATA_IND && frame instanceof final CEMILData ldata) {
			final KNXAddress dst = ldata.getDestination();
			// L-Data system bcast check is not mandated, it could cause false negatives
			if (dst instanceof GroupAddress && dst.getRawAddress() == 0) {
				final byte[] tpdu = ldata.getPayload();
				if (tpdu.length >= 2)
					return Optional.of(tpdu);
			}
		}
		return Optional.empty();
	}
}

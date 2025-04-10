/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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
import java.nio.ByteBuffer;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * The common KNXnet/IP header used at the first position in every KNXnet/IP frame.
 * <p>
 * It contains the protocol version, length of this header, the total frame length and the
 * service type identifier of the service carried by the frame. The header, most
 * certainly, is followed by a KNXnet/IP body, depending on the service type, i.e., every
 * KNXnet/IP frame consists at least of this header.
 * <p>
 * The header itself, however, only consists of the pure header structure, it will not
 * store or read any frame body content.<br>
 * It is used for KNXnet/IP frame to retrieve general information in the first place,
 * followed by processing of the frame through more specific service types.<br>
 * Most service type identifiers are listed as public constants in this header interface.
 * <p>
 * The KNXnet/IP implementation status is 1.0.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 */
public class KNXnetIPHeader
{
	/**
	 * Service type identifier for a connect request.
	 */
	public static final int CONNECT_REQ = 0x0205;

	/**
	 * Service type identifier for a connect response.
	 */
	public static final int CONNECT_RES = 0x0206;

	/**
	 * Service type identifier for a connection-state request.
	 */
	public static final int CONNECTIONSTATE_REQ = 0x0207;

	/**
	 * Service type identifier for a connection-state response.
	 */
	public static final int CONNECTIONSTATE_RES = 0x0208;

	/**
	 * Service type identifier for a disconnect request.
	 */
	public static final int DISCONNECT_REQ = 0x0209;

	/**
	 * Service type identifier for a disconnect response.
	 */
	public static final int DISCONNECT_RES = 0x020A;

	/**
	 * Service type identifier for a description request.
	 */
	public static final int DESCRIPTION_REQ = 0x0203;

	/**
	 * Service type identifier for a description response.
	 */
	public static final int DESCRIPTION_RES = 0x204;

	/**
	 * Service type identifier for a search request.
	 */
	public static final int SEARCH_REQ = 0x201;

	/**
	 * Service type identifier for a search response.
	 */
	public static final int SEARCH_RES = 0x202;

	/**
	 * Service type identifier for configuration request (read / write device
	 * configuration data, interface properties).
	 */
	public static final int DEVICE_CONFIGURATION_REQ = 0x0310;

	/**
	 * Service type identifier to confirm the reception of the configuration request.
	 */
	public static final int DEVICE_CONFIGURATION_ACK = 0x0311;

	/**
	 * Service type identifier to send and receive single KNX frames between client and
	 * server.
	 */
	public static final int TUNNELING_REQ = 0x0420;

	/**
	 * Service type identifier to confirm the reception of a tunneling request.
	 */
	public static final int TUNNELING_ACK = 0x0421;

	/**
	 * Service type identifier for sending KNX telegrams over IP networks with multicast.
	 */
	public static final int ROUTING_IND = 0x0530;

	/**
	 * Service type identifier to indicate the loss of routing messages with multicast.
	 */
	public static final int ROUTING_LOST_MSG = 0x0531;

	/**
	 * Service type identifier for a buffer overflow warning indication with multicast.
	 */
	public static final int ROUTING_BUSY = 0x0532;

	/**
	 * Service type identifier for a routing system broadcast with multicast.
	 */
	public static final int RoutingSystemBroadcast = 0x0533;

	/**
	 * Version identifier for KNXnet/IP protocol version 1.0.
	 */
	public static final int KNXNETIP_VERSION_10 = 0x10;

	// Search for KNX IP Secure Unicasts Setups

	public static final int SearchRequest = 0x20b;
	public static final int SearchResponse = 0x20c;

	// KNX IP Secure

	public static final int SecureWrapper = 0x0950;
	private static final int SecureSessionRequest = 0x0951;
	private static final int SecureSessionResponse = 0x0952;
	private static final int SecureSessionAuth = 0x0953;
	private static final int SecureSessionStatus = 0x0954;
	private static final int SecureGroupSync = 0x0955;

	// KNX IP Tunneling Mgmt

	public static final int TunnelingFeatureGet = 0x0422;
	public static final int TunnelingFeatureResponse = 0x0423;
	public static final int TunnelingFeatureSet = 0x0424;
	public static final int TunnelingFeatureInfo = 0x0425;

	// BAOS ObjectServer

	public static final int ObjectServerRequest = 0xf080;
	public static final int ObjectServerAck = 0xf081;


	private static final int HEADER_SIZE_10 = 0x06;

	private final int headersize;
	private final int service;
	private final int totalsize;
	private final int version;


	public static KNXnetIPHeader from(final byte[] frame, final int offset) throws KNXFormatException {
		final var buf = ByteBuffer.wrap(frame, offset, frame.length);
		if (buf.remaining() < HEADER_SIZE_10)
			throw new KNXFormatException("buffer too short for KNXnet/IP header");

		final int headersize = buf.get() & 0xFF;
		if (headersize != HEADER_SIZE_10)
			throw new KNXFormatException("unsupported header size, expected " + HEADER_SIZE_10, headersize);

		final int version = buf.get() & 0xFF;
		final int service = buf.getShort() & 0xffff;
		final int totalsize = buf.getShort() & 0xffff;
		return new KNXnetIPHeader(service, version, totalsize - HEADER_SIZE_10);
	}

	/**
	 * Creates a new KNXnet/IP header by reading in the header of a KNXnet/IP frame.
	 *
	 * @param frame byte array with contained KNXnet/IP frame
	 * @param offset start offset of KNXnet/IP header structure in {@code frame}
	 * @throws KNXFormatException if {@code frame} is too short for header, on
	 *         wrong header size or not supported KNXnet/IP protocol version
	 */
	public KNXnetIPHeader(final byte[] frame, final int offset) throws KNXFormatException
	{
		if (frame.length - offset < HEADER_SIZE_10)
			throw new KNXFormatException("buffer too short for KNXnet/IP header");
		int i = offset;
		headersize = frame[i++] & 0xFF;
		version = frame[i++] & 0xFF;

		int high = frame[i++] & 0xFF;
		int low = frame[i++] & 0xFF;
		service = (high << 8) | low;
		high = frame[i++] & 0xFF;
		low = frame[i++] & 0xFF;
		totalsize = (high << 8) | low;

		if (headersize != HEADER_SIZE_10)
			throw new KNXFormatException("unsupported header size, expected " + HEADER_SIZE_10, headersize);
		final int expectedVersion = version(service);
		if (version != expectedVersion)
			throw new KNXFormatException(
					String.format("unsupported KNXnet/IP protocol version, expected 0x%1h", expectedVersion), version);
		if (totalsize < HEADER_SIZE_10)
			throw new KNXFormatException(
					String.format("KNXnet/IP header contains invalid total length < %d", HEADER_SIZE_10), totalsize);
	}

	/**
	 * Creates a new KNXnet/IP header for the given service.
	 *
	 * @param serviceType service type identifier specifying the service followed after
	 *        the header, 0 &lt;= type &lt;= 0xFFFF
	 * @param serviceLength length of the service structure in bytes
	 */
	public KNXnetIPHeader(final int serviceType, final int serviceLength)
	{
		this(serviceType, version(serviceType), serviceLength);
	}

	/**
	 * Creates a new KNXnet/IP header for the given service.
	 *
	 * @param serviceType service type identifier specifying the service followed after
	 *        the header, 0 &lt;= type &lt;= 0xFFFF
	 * @param version protocol version, 0x10 &le; version &le; 0xff
	 * @param serviceLength length of the service structure in bytes
	 */
	public KNXnetIPHeader(final int serviceType, final int version, final int serviceLength) {
		if (serviceType < 0 || serviceType > 0xFFFF)
			throw new KNXIllegalArgumentException("service type out of range [0..0xFFFF]");
		if (version < 0x10 || version > 0xff)
			throw new KNXIllegalArgumentException("version out of range [0x10..0xFF]");
		if (serviceLength < 0)
			throw new KNXIllegalArgumentException("negative length of message body");
		headersize = HEADER_SIZE_10;
		service = serviceType;
		this.version = version;
		totalsize = headersize + serviceLength;
	}

	public final boolean isSecure() {
		return getVersion() == KNXNETIP_VERSION_10 && ((getServiceType() & SecureWrapper) == SecureWrapper);
	}

	/**
	 * Returns the service type identifier.
	 *
	 * @return service type as unsigned 16 bit value
	 */
	public final int getServiceType()
	{
		return service;
	}

	/**
	 * Returns the KNXnet/IP protocol version of the frame.
	 *
	 * @return protocol version as unsigned 8 bit value
	 */
	public int getVersion()
	{
		return version;
	}

	/**
	 * Returns the length of the KNXnet/IP header structure.
	 *
	 * @return the length in bytes
	 */
	public int getStructLength()
	{
		return HEADER_SIZE_10;
	}

	/**
	 * Returns the total length of the KNXnet/IP frame the header is part of.
	 * <p>
	 * The total length is calculated by adding the header length and the length of the
	 * service contained in the frame.
	 *
	 * @return the total length in bytes
	 */
	public final int getTotalLength()
	{
		return totalsize;
	}

	/**
	 * Returns the byte representation of the KNXnet/IP header structure.
	 *
	 * @return byte array containing structure
	 */
	public byte[] toByteArray()
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(headersize);
		os.write(version);
		os.write(service >> 8);
		os.write(service);
		os.write(totalsize >> 8);
		os.write(totalsize);
		return os.toByteArray();
	}

	/**
	 * Returns a textual representation of this KNXnet/IP header.
	 *
	 * @return a string representation of the object
	 */
	@Override
	public String toString()
	{
		return "KNXnet/IP " + getSvcName(service) + " (0x" + Integer.toHexString(service)
				+ " v" + (version / 0x10) + "." + (version % 0x10) + ") length " + totalsize;
	}

	static String getSvcName(final int svcType)
	{
		return switch (svcType) {
			case CONNECT_REQ -> "connect.req";
			case CONNECT_RES -> "connect.res";
			case CONNECTIONSTATE_REQ -> "connection-state.req";
			case CONNECTIONSTATE_RES -> "connection-state.res";
			case DISCONNECT_REQ -> "disconnect.req";
			case DISCONNECT_RES -> "disconnect.res";
			case DESCRIPTION_REQ -> "description.req";
			case DESCRIPTION_RES -> "description.res";
			case SEARCH_REQ -> "search.req";
			case SEARCH_RES -> "search.res";
			case DEVICE_CONFIGURATION_REQ -> "device-configuration.req";
			case DEVICE_CONFIGURATION_ACK -> "device-configuration.ack";
			case TUNNELING_REQ -> "tunneling.req";
			case TUNNELING_ACK -> "tunneling.ack";
			case ROUTING_IND -> "routing.ind";
			case ROUTING_LOST_MSG -> "routing-lost.msg";
			case ROUTING_BUSY -> "routing-busy.ind";
			case RoutingSystemBroadcast -> "routing-system-broadcast";
			case SearchRequest -> "search.req";
			case SearchResponse -> "search.res";
			case SecureWrapper -> "secure-msg";
			case SecureSessionRequest -> "session.req";
			case SecureSessionResponse -> "session.res";
			case SecureSessionAuth -> "session-auth";
			case SecureSessionStatus -> "session-status";
			case SecureGroupSync -> "group-sync";
			case TunnelingFeatureGet -> "tunneling-feat.get";
			case TunnelingFeatureResponse -> "tunneling-feat.res";
			case TunnelingFeatureSet -> "tunneling-feat.set";
			case TunnelingFeatureInfo -> "tunneling-feat.info";
			case ObjectServerRequest -> "object-server.req";
			case ObjectServerAck -> "object-server.ack";
			default -> "unknown service";
		};
	}

	private static int version(final int serviceType) {
		return serviceType == ObjectServerRequest || serviceType == ObjectServerAck ? 0x20 : KNXNETIP_VERSION_10;
	}
}

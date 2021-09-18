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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KnxRuntimeException;
import tuwien.auto.calimero.ServiceType;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIFactory;

/**
 * Common service request structure, used to send requests over established KNXnet/IP
 * communication channels.
 * <p>
 * Such a service request is used for tunnel or device management connections. It carries
 * a cEMI frame containing the actual KNX frame data.<br>
 * A service request is contained in the body of a KNXnet/IP frame.
 *
 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceAck
 */
public class ServiceRequest<T extends ServiceType> extends tuwien.auto.calimero.knxnetip.servicetype.ServiceType
{
	private static final int CONN_HEADER_SIZE = 4;
	private final int channelid;
	private final int seq;
	private final Supplier<T> svcSupplier;
	private volatile T svc;

	private static Function<ByteBuffer, CEMI> cemiParser = buf -> {
		try {
			return CEMIFactory.create(buf.array(), buf.position(), buf.remaining());
		}
		catch (final KNXFormatException e) {
			throw new KnxRuntimeException("parsing cEMI", e);
		}
	};

	private static BiFunction<Integer, ByteBuffer, TunnelingFeature> tunnelingFeatureParser = (svc, buf) -> {
		try {
			return TunnelingFeature.from(svc, buf);
		}
		catch (final KNXFormatException e) {
			throw new KnxRuntimeException("parsing tunneling feature", e);
		}
	};


	public static ServiceRequest<ServiceType> from(final KNXnetIPHeader h, final byte[] data, final int offset)
			throws KNXFormatException {
		return new ServiceRequest<>(h.getServiceType(), data, offset, h.getTotalLength() - h.getStructLength());
	}

	public static <U extends ServiceType> ServiceRequest<U> from(final KNXnetIPHeader h, final byte[] data,
			final int offset, final Function<ByteBuffer, U> svcParser) throws KNXFormatException {
		return from(h, ByteBuffer.wrap(data, h.getStructLength(), h.getTotalLength() - h.getStructLength()), svcParser);
	}

	private static <U extends ServiceType> ServiceRequest<U> from(final KNXnetIPHeader h, final ByteBuffer buf,
			final Function<ByteBuffer, U> svcParser) throws KNXFormatException {
		return new ServiceRequest<>(h.getServiceType(), buf, svcParser);
	}

	/**
	 * Creates a new service request out of a byte array.
	 *
	 * @param serviceType service request type identifier describing the request in
	 *        <code>data</code>, 0 &lt;= type &lt;= 0xFFFF
	 * @param data byte array containing a service request structure
	 * @param offset start offset in bytes of request in <code>data</code>
	 * @param length the length in bytes of the whole request contained in <code>data</code>
	 * @throws KNXFormatException if buffer is too short for request, on unsupported
	 *         service type or connection header structure
	 */
	public ServiceRequest(final int serviceType, final byte[] data, final int offset, final int length) throws
			KNXFormatException {
		this(serviceType, ByteBuffer.wrap(data, offset, length), parser(serviceType));
	}

	@SuppressWarnings("unchecked")
	private static <T> Function<ByteBuffer, T> parser(final int serviceType) throws KNXFormatException {
		if (serviceType == KNXnetIPHeader.TUNNELING_REQ || serviceType == KNXnetIPHeader.DEVICE_CONFIGURATION_REQ)
			return (Function<ByteBuffer, T>) cemiParser;

		if (serviceType == KNXnetIPHeader.TunnelingFeatureGet || serviceType == KNXnetIPHeader.TunnelingFeatureResponse
				|| serviceType == KNXnetIPHeader.TunnelingFeatureSet
				|| serviceType == KNXnetIPHeader.TunnelingFeatureInfo) {
			final Function<ByteBuffer, TunnelingFeature> parse = buf -> tunnelingFeatureParser.apply(serviceType, buf);
			return (Function<ByteBuffer, T>) parse;
		}
		throw new KNXFormatException("unsupported service type " + Integer.toHexString(serviceType));
	}

	private ServiceRequest(final int serviceType, final ByteBuffer buf,
			final Function<ByteBuffer, T> svcParser) throws KNXFormatException {
		super(serviceType);
		if (buf.remaining() < CONN_HEADER_SIZE)
			throw new KNXFormatException("buffer too short for service request");
		if ((buf.get() & 0xff) != CONN_HEADER_SIZE)
			throw new KNXFormatException("unsupported connection header");
		channelid = buf.get() & 0xff;
		seq = buf.get() & 0xff;
		/* final int reserved = */buf.get();
		svcSupplier = () -> svcParser.apply(buf);
	}

	/**
	 * Creates a new service request.
	 *
	 * @param serviceType service request type identifier, 0 &lt;= type &lt;= 0xFFFF
	 * @param channelID channel ID of communication this request belongs to, 0 &lt;= id
	 *        &lt;= 255
	 * @param seqNumber the sending sequence number of the communication channel, 0 &lt;=
	 *        number &lt;= 255
	 * @param service service type carried with the request
	 */
	@SuppressWarnings("unchecked")
	public ServiceRequest(final int serviceType, final int channelID, final int seqNumber, final T service)
	{
		super(serviceType);
		if (serviceType < 0 || serviceType > 0xffff)
			throw new KNXIllegalArgumentException("service request out of range [0..0xffff]");
		if (channelID < 0 || channelID > 0xff)
			throw new KNXIllegalArgumentException("channel ID out of range [0..0xff]");
		if (seqNumber < 0 || seqNumber > 0xff)
			throw new KNXIllegalArgumentException("sequence number out of range [0..0xff]");
		channelid = channelID;
		seq = seqNumber;

		svc = service instanceof CEMI ? (T) CEMIFactory.copy((CEMI) service) : service;
		svcSupplier = null;
	}

	/**
	 * Returns the service type identifier of the request.
	 *
	 * @return service type as unsigned 16 bit value
	 */
	public final int getServiceType()
	{
		return svcType;
	}

	/**
	 * Returns the communication channel identifier associated with the request.
	 *
	 * @return communication channel ID as unsigned byte
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * Returns the sequence number of the sending endpoint.
	 *
	 * @return sequence number as unsigned byte
	 */
	public final int getSequenceNumber()
	{
		return seq;
	}

	@SuppressWarnings("unchecked")
	public final <R extends T> R service() {
		if (svc == null)
			svc = svcSupplier.get();
		return (R) svc;
	}

	/**
	 * @deprecated
	 * Returns the cEMI frame carried by the request.
	 *
	 * @return a cEMI type
	 */
	@Deprecated
	public final CEMI getCEMI()
	{
		return CEMIFactory.copy((CEMI) svc);
	}

	@Override
	int getStructLength()
	{
		return CONN_HEADER_SIZE + service().length();
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(CONN_HEADER_SIZE);
		os.write(channelid);
		os.write(seq);
		os.write(0);
		final byte[] buf = service().toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

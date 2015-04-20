/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
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
public class ServiceRequest extends ServiceType
{
	private static final int CONN_HEADER_SIZE = 4;
	private final int channelid;
	private final int seq;
	private CEMI cemi;

	/**
	 * Creates a new service request out of a byte array.
	 * <p>
	 * 
	 * @param serviceType service request type identifier describing the request in
	 *        <code>data</code>, 0 &lt;= type &lt;= 0xFFFF
	 * @param data byte array containing a service request structure
	 * @param offset start offset in bytes of request in <code>data</code>
	 * @param length the length in bytes of the whole request contained in
	 *        <code>data</code>
	 * @throws KNXFormatException if buffer is too short for request, on unsupported
	 *         service type or connection header structure
	 */
	public ServiceRequest(final int serviceType, final byte[] data, final int offset,
		final int length) throws KNXFormatException
	{
		this(serviceType, data, offset, length, null);
		if (svcType == KNXnetIPHeader.TUNNELING_REQ)
			cemi = CEMIFactory.create(data, offset + CONN_HEADER_SIZE, length - CONN_HEADER_SIZE);
		else if (svcType == KNXnetIPHeader.DEVICE_CONFIGURATION_REQ)
			cemi = new CEMIDevMgmt(data, offset + CONN_HEADER_SIZE, length - CONN_HEADER_SIZE);
		else
			throw new KNXIllegalArgumentException("unsupported service request type");
	}

	/**
	 * Creates a new service request.
	 * <p>
	 * 
	 * @param serviceType service request type identifier, 0 &lt;= type &lt;= 0xFFFF
	 * @param channelID channel ID of communication this request belongs to, 0 &lt;= id
	 *        &lt;= 255
	 * @param seqNumber the sending sequence number of the communication channel, 0 &lt;=
	 *        number &lt;= 255
	 * @param frame cEMI frame carried with the request
	 */
	public ServiceRequest(final int serviceType, final int channelID, final int seqNumber,
		final CEMI frame)
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
		cemi = CEMIFactory.copy(frame);
	}

	// frame might be null, toByteArray will throw NPE then
	ServiceRequest(final int serviceType, final byte[] data, final int offset, final int length,
		final CEMI frame) throws KNXFormatException
	{
		super(serviceType);
		if (length < CONN_HEADER_SIZE)
			throw new KNXFormatException("buffer too short for service request");
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
		if (is.read() != CONN_HEADER_SIZE)
			throw new KNXFormatException("unsupported connection header");
		channelid = is.read();
		seq = is.read();
		/* final int reserved = */is.read();
		cemi = frame;
	}
	
	/**
	 * Returns the service type identifier of the request.
	 * <p>
	 * 
	 * @return service type as unsigned 16 bit value
	 */
	public final int getServiceType()
	{
		return svcType;
	}

	/**
	 * Returns the communication channel identifier associated with the request.
	 * <p>
	 * 
	 * @return communication channel ID as unsigned byte
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * Returns the sequence number of the sending endpoint.
	 * <p>
	 * 
	 * @return sequence number as unsigned byte
	 */
	public final int getSequenceNumber()
	{
		return seq;
	}

	/**
	 * Returns the cEMI frame carried by the request.
	 * <p>
	 * 
	 * @return a cEMI type
	 */
	public final CEMI getCEMI()
	{
		return CEMIFactory.copy(cemi);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return CONN_HEADER_SIZE + (cemi != null ? cemi.getStructLength() : 0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(CONN_HEADER_SIZE);
		os.write(channelid);
		os.write(seq);
		os.write(0);
		final byte[] buf = cemi.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

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

import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Common KNXnet/IP acknowledgment structure, used to acknowledge a service receipt over
 * established KNXnet/IP communication channels.
 * <p>
 * A service acknowledgment is done in reply to a service request, to confirm the
 * reception of the request over the IP communication channel. Note, an IP acknowledgment
 * does not indicate any delivery on KNX networks.
 * 
 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest
 */
public class ServiceAck extends ServiceType
{
	private static final int CONN_HEADER_SIZE = 4;
	private final int channelid;
	private final int seq;
	private final int status;

	/**
	 * Creates a new service acknowledgment out of a byte array.
	 * <p>
	 * 
	 * @param serviceType service type identifier describing the service to acknowledge in
	 *        <code>data</code>, 0 &lt;= type &lt;= 0xFFFF
	 * @param data byte array containing a service acknowledgment structure
	 * @param offset start offset of the acknowledgment structure in <code>data</code>
	 * @throws KNXFormatException if buffer is too short for the expected acknowledgment
	 *         structure, on unsupported service type or connection header structure
	 */
	public ServiceAck(final int serviceType, final byte[] data, final int offset)
		throws KNXFormatException
	{
		super(serviceType);
		if (data.length - offset < CONN_HEADER_SIZE)
			throw new KNXFormatException("buffer too short for service ack");
		int i = offset;
		if ((data[i++] & 0xFF) != CONN_HEADER_SIZE)
			throw new KNXFormatException("unsupported connection header");
		channelid = data[i++] & 0xFF;
		seq = data[i++] & 0xFF;
		status = data[i++] & 0xFF;
	}

	/**
	 * Creates a new service acknowledgment.
	 * <p>
	 * 
	 * @param serviceType service type identifier, 0 &lt;= type &lt;= 0xFFFF
	 * @param channelId channel identifier of the communication channel this service
	 *        acknowledgment belongs to, 0 &lt;= id &lt;= 255
	 * @param seqNumber the sequence number of the communication channel, value
	 *        corresponds to the received request, 0 &lt;= number &lt;= 255
	 * @param status status code to the corresponding request, 0 &lt;= status &lt;= 255
	 */
	public ServiceAck(final int serviceType, final int channelId, final int seqNumber,
		final int status)
	{
		super(serviceType);
		if (serviceType < 0 || serviceType > 0xffff)
			throw new KNXIllegalArgumentException(
				"ack service type out of range [0..0xffff]");
		if (channelId < 0 || channelId > 0xff)
			throw new KNXIllegalArgumentException("channel ID out of range [0..0xff]");
		if (seqNumber < 0 || seqNumber > 0xff)
			throw new KNXIllegalArgumentException(
				"sequence number out of range [0..0xff]");
		if (status < 0 || status > 0xff)
			throw new KNXIllegalArgumentException("status code out of range [0..0xff]");
		channelid = channelId;
		seq = seqNumber;
		this.status = status;
	}

	/**
	 * Returns the service type identifier of this acknowledgment.
	 * <p>
	 * 
	 * @return service type as unsigned 16 bit value
	 */
	public final int getServiceType()
	{
		return svcType;
	}

	/**
	 * Returns the communication channel identifier of the communication channel this
	 * service acknowledgment belongs to.
	 * <p>
	 * 
	 * @return communication channel identifier as unsigned byte
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * Returns the sequence number.
	 * 
	 * @return sequence number as unsigned byte
	 */
	public final int getSequenceNumber()
	{
		return seq;
	}

	/**
	 * Returns the status code of this acknowledgment, regarding the corresponding
	 * request.
	 * <p>
	 * 
	 * @return status code as unsigned byte
	 */
	public final int getStatus()
	{
		return status;
	}

	/**
	 * Returns a textual representation of the status code.
	 * <p>
	 * 
	 * @return short description of status as string, not <code>null</code>
	 */
	public String getStatusString()
	{
		return ErrorCodes.getErrorMessage(status);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return CONN_HEADER_SIZE;
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
		os.write(status);
		return os.toByteArray();
	}
}

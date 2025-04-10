/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2005 B. Erb
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

package io.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * Represents a KNXnet/IP connection state response.
 * <p>
 * Connection state responses are sent by a server in reply to a connection state request.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 * @author Bernhard Erb
 * @see io.calimero.knxnetip.servicetype.ConnectionstateRequest
 */
public class ConnectionstateResponse extends ServiceType
{
	private final int channelid;
	private final int status;

	/**
	 * Creates a connection-state response out of a byte array.
	 *
	 * @param data byte array containing a connection-state response structure
	 * @param offset start offset of response in {@code data}
	 * @throws KNXFormatException if no connection state response was found or invalid structure
	 */
	public ConnectionstateResponse(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.CONNECTIONSTATE_RES);
		if (data.length - offset < 2)
			throw new KNXFormatException("buffer too short for response");
		channelid = data[offset] & 0xFF;
		status = data[offset + 1] & 0xFF;
	}

	/**
	 * Creates a new connection-state response.
	 *
	 * @param channelID communication channel ID passed with the corresponding connection
	 *        state request, 0 &lt;= id &lt;= 255
	 * @param status status of the connection, 0 &lt;= status &lt;= 255
	 */
	public ConnectionstateResponse(final int channelID, final int status)
	{
		super(KNXnetIPHeader.CONNECTIONSTATE_RES);
		if (channelID < 0 || channelID > 0xFF)
			throw new KNXIllegalArgumentException("channel ID out of range [0..255]");
		if (status < 0 || status > 0xFF)
			throw new KNXIllegalArgumentException("status code out of range [0..255]");
		channelid = channelID;
		this.status = status;
	}

	/**
	 * {@return the communication channel ID used for the response, channel ID is an unsigned byte}
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * {@return the status code of the connection as unsigned byte}
	 */
	public final int getStatus()
	{
		return status;
	}

	/**
	 * {@return a short textual description of the status code}
	 */
	public String getStatusString()
	{
		return switch (status) {
			case ErrorCodes.NO_ERROR -> "connection state is normal";
			case ErrorCodes.CONNECTION_ID -> "server could not find active data connection with specified ID";
			case ErrorCodes.DATA_CONNECTION -> "server detected error concerning the data connection";
			case ErrorCodes.KNX_CONNECTION -> "server detected error concerning the KNX bus/subsystem connection";
			default -> "unknown status";
		};
	}

	@Override
	public int length()
	{
		return 2;
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(channelid);
		os.write(status);
		return os.toByteArray();
	}
}

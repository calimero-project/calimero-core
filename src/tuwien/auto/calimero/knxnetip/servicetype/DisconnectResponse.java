/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2005 B. Erb
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
 * Represents a KNXnet/IP disconnect response.
 * <p>
 * Such response is sent in reply to a disconnect request to complete the termination of
 * the logical connection between a client and server. It provides the final status after
 * the closing sequence, indicating success or failure.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author Bernhard Erb
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DisconnectRequest
 */
public class DisconnectResponse extends ServiceType
{
	private final int channelid;
	private final int status;

	/**
	 * Creates a disconnect response out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a disconnect response structure
	 * @param offset start offset of response in <code>data</code>
	 * @throws KNXFormatException if found structure is too short
	 */
	public DisconnectResponse(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.DISCONNECT_RES);
		if (data.length - offset < 2)
			throw new KNXFormatException("buffer too short for disconnect response");
		channelid = data[offset] & 0xFF;
		status = data[offset + 1] & 0xFF;
	}

	/**
	 * Creates a new disconnect response for the terminating connection with the given
	 * channel ID.
	 * <p>
	 * 
	 * @param channelID communication channel ID passed in the disconnect request, 0 &lt;=
	 *        id &lt;= 255
	 * @param status status code giving information of the final state, 0 &lt;= status
	 *        &lt;= 255
	 */
	public DisconnectResponse(final int channelID, final int status)
	{
		super(KNXnetIPHeader.DISCONNECT_RES);
		if (channelID < 0 || channelID > 0xFF)
			throw new KNXIllegalArgumentException("channel ID out of range [0..255]");
		if (status < 0 || status > 0xFF)
			throw new KNXIllegalArgumentException("status code out of range [0..255]");
		channelid = channelID;
		this.status = status;
	}

	/**
	 * Returns the communication channel identifier, matching the ID in the corresponding
	 * disconnect request for closing the connection.
	 * <p>
	 * 
	 * @return communication channel ID as unsigned byte
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * Returns the status code, signaling the final state.
	 * <p>
	 * 
	 * @return status code as unsigned byte
	 */
	public final int getStatus()
	{
		return status;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return 2;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(channelid);
		os.write(status);
		return os.toByteArray();
	}
}

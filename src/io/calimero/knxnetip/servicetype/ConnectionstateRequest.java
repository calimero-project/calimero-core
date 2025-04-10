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
import io.calimero.knxnetip.util.HPAI;

/**
 * Represents a KNXnet/IP connection-state request.
 * <p>
 * Connection-state requests are sent by a client during an established client/server
 * communication connection to check the connection state, i.e., they are part of a
 * heartbeat monitoring procedure.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 * @author Bernhard Erb
 * @see io.calimero.knxnetip.servicetype.ConnectionstateResponse
 */
public class ConnectionstateRequest extends ServiceType
{
	private final int channelid;
	private final HPAI endpt;

	/**
	 * Creates a connection-state request out of a byte array.
	 *
	 * @param data byte array containing a connection-state request structure
	 * @param offset start offset of request in {@code data}
	 * @throws KNXFormatException if no connection-state request was found or invalid
	 *         structure
	 */
	public ConnectionstateRequest(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.CONNECTIONSTATE_REQ);
		if (data.length - offset < 3)
			throw new KNXFormatException("buffer too short for request");
		channelid = data[offset] & 0xFF;
		endpt = HPAI.from(data, offset + 2);
	}

	/**
	 * Creates a new connection-state request.
	 *
	 * @param channelID communication channel ID of the open connection, 0 &lt;= id &lt;= 255
	 * @param ctrlEP control endpoint of the client
	 */
	public ConnectionstateRequest(final int channelID, final HPAI ctrlEP)
	{
		super(KNXnetIPHeader.CONNECTIONSTATE_REQ);
		if (channelID < 0 || channelID > 0xFF)
			throw new KNXIllegalArgumentException("channel ID out of range [0..255]");
		channelid = channelID;
		endpt = ctrlEP;
	}

	/**
	 * {@return the communication channel ID this connection-state request belongs to, channel ID is an unsigned byte}
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * {@return the client control endpoint the server shall reply to}
	 */
	public final HPAI getControlEndpoint()
	{
		return endpt;
	}

	@Override
	public int length()
	{
		return 2 + endpt.getStructLength();
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(channelid);
		os.write(0);
		final byte[] buf = endpt.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

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
*/

package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.util.HPAI;

/**
 * A representation of a KNXnet/IP disconnect request.
 * <p>
 * It is used to request the termination of an established logical connection between a
 * client and server, both control endpoints might send such request.<br>
 * The counterpart sent in reply to this request is a disconnect response.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 * @author Bernhard Erb
 * @see tuwien.auto.calimero.knxnetip.servicetype.DisconnectResponse
 */
public class DisconnectRequest extends ServiceType
{
	private final int channelid;
	private final HPAI endpt;

	/**
	 * Creates a disconnect request out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a disconnect request structure
	 * @param offset start offset of request in <code>data</code>
	 * @throws KNXFormatException if no disconnect request was found or invalid structure
	 */
	public DisconnectRequest(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.DISCONNECT_REQ);
		if (data.length - offset < 3)
			throw new KNXFormatException("buffer too short for disconnect request");
		int i = offset;
		channelid = data[i++] & 0xFF;
		// skip reserved field
		++i;
		endpt = new HPAI(data, i);
	}

	/**
	 * Creates a new disconnect request for the connection identified by the channel ID.
	 * <p>
	 * 
	 * @param channelID communication channel ID uniquely identifying the connection to
	 *        close
	 * @param ctrlEndpoint client control endpoint used for disconnect response
	 */
	public DisconnectRequest(final int channelID, final HPAI ctrlEndpoint)
	{
		super(KNXnetIPHeader.DISCONNECT_REQ);
		if (channelID < 0 || channelID > 0xFF)
			throw new KNXIllegalArgumentException("channel ID out of range [0..255]");
		channelid = channelID;
		endpt = ctrlEndpoint;
	}

	/**
	 * Returns the communication channel identifier of the connection being closed.
	 * <p>
	 * 
	 * @return The communication channel ID as unsigned byte
	 */
	public final int getChannelID()
	{
		return channelid;
	}

	/**
	 * Returns the client control endpoint.
	 * <p>
	 * 
	 * @return control endpoint in a HPAI
	 */
	public final HPAI getEndpoint()
	{
		return endpt;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	int getStructLength()
	{
		return endpt.getStructLength() + 2;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(channelid);
		os.write(0);
		final byte[] buf = endpt.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

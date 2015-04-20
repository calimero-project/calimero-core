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
import java.net.InetAddress;
import java.net.InetSocketAddress;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.knxnetip.util.HPAI;

/**
 * Represents a KNXnet/IP description request.
 * <p>
 * A request for self description is mainly used by a client after discovery of a new
 * remote device endpoint. It is sent to the control endpoint of the server device. The
 * counterpart to this request is the description response.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author Bernhard Erb
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse
 * @see tuwien.auto.calimero.knxnetip.Discoverer
 */
public class DescriptionRequest extends ServiceType
{
	private final HPAI endpoint;

	/**
	 * Creates a description request out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a description request structure
	 * @param offset start offset of request in <code>data</code>
	 * @throws KNXFormatException if no description request was found or invalid structure
	 */
	public DescriptionRequest(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.DESCRIPTION_REQ);
		endpoint = new HPAI(data, offset);
	}

	/**
	 * Creates a new description request with the given client control endpoint for a
	 * description response.
	 * <p>
	 * 
	 * @param ctrlEndpoint client control endpoint used for response
	 */
	public DescriptionRequest(final HPAI ctrlEndpoint)
	{
		super(KNXnetIPHeader.DESCRIPTION_REQ);
		endpoint = ctrlEndpoint;
	}

	/**
	 * Creates a new description request with the client address used for a description
	 * response.
	 * <p>
	 * This request uses the UDP transport protocol.
	 * 
	 * @param responseAddr address of client control endpoint used for response, use
	 *        <code>null</code> if NAT is used on the IP network
	 */
	public DescriptionRequest(final InetSocketAddress responseAddr)
	{
		super(KNXnetIPHeader.DESCRIPTION_REQ);
		endpoint = new HPAI(HPAI.IPV4_UDP, responseAddr);
	}

	/**
	 * Convenience constructor to create a new description request using the UDP transport
	 * protocol and the system default local host with the supplied client port.
	 * 
	 * @param responsePort port number of the client control endpoint used for response, 0
	 *        &lt;= port &lt;= 0xFFFF
	 */
	public DescriptionRequest(final int responsePort)
	{
		super(KNXnetIPHeader.DESCRIPTION_REQ);
		endpoint = new HPAI((InetAddress) null, responsePort);
	}

	/**
	 * Returns the client control endpoint.
	 * <p>
	 * 
	 * @return control endpoint in a HPAI
	 */
	public final HPAI getEndpoint()
	{
		return endpoint;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return endpoint.getStructLength();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		final byte[] buf = endpoint.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

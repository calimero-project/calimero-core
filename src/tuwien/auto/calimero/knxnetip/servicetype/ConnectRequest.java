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
import java.net.InetAddress;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.knxnetip.util.CRI;
import tuwien.auto.calimero.knxnetip.util.HPAI;

/**
 * Represents a KNXnet/IP connect request message.
 * <p>
 * Such request is used to open a logical connection to a server. The request is sent to
 * the control endpoint of the server. <br>
 * The connection request is answered with a connect response.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.ConnectResponse
 */
public class ConnectRequest extends ServiceType
{
	private final CRI cri;
	private final HPAI ctrlPt;
	private final HPAI dataPt;

	/**
	 * Creates a connect request out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a connect request structure
	 * @param offset start offset of request in <code>data</code>
	 * @throws KNXFormatException if no connect request was found or invalid structure
	 */
	public ConnectRequest(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.CONNECT_REQ);
		ctrlPt = new HPAI(data, offset);
		final int i = offset + ctrlPt.getStructLength();
		dataPt = new HPAI(data, i);
		cri = CRI.createRequest(data, i + dataPt.getStructLength());
	}

	/**
	 * Creates a connect request with the specific information of the CRI, and the
	 * endpoint information of the client.
	 * <p>
	 * The control and data endpoint specified are allowed to be equal, i.e all
	 * communication is handled through the same endpoint at the client.
	 * 
	 * @param requestInfo connection specific options, depending on connection type
	 * @param ctrlEndpoint return address information of the client's control endpoint
	 * @param dataEndpoint address information of the client's data endpoint for the
	 *        requested connection
	 */
	public ConnectRequest(final CRI requestInfo, final HPAI ctrlEndpoint, final HPAI dataEndpoint)
	{
		super(KNXnetIPHeader.CONNECT_REQ);
		cri = requestInfo;
		ctrlPt = ctrlEndpoint;
		dataPt = dataEndpoint;
	}

	/**
	 * Creates a connect request for UDP communication, done on the specified local port
	 * and the system default local host.
	 * <p>
	 * 
	 * @param requestInfo connection specific options, depending on connection type
	 * @param localPort local port of client used for connection, 0 &lt;= port &lt;=
	 *        0xFFFF
	 * @see CRI
	 */
	public ConnectRequest(final CRI requestInfo, final int localPort)
	{
		super(KNXnetIPHeader.CONNECT_REQ);
		cri = requestInfo;
		ctrlPt = new HPAI((InetAddress) null, localPort);
		dataPt = ctrlPt;
	}

	/**
	 * Returns the connect request information used in the request.
	 * <p>
	 * 
	 * @return connection specific CRI
	 */
	public final CRI getCRI()
	{
		return cri;
	}

	/**
	 * Returns the local control endpoint used for the connection.
	 * <p>
	 * 
	 * @return control endpoint in a HPAI
	 */
	public final HPAI getControlEndpoint()
	{
		return ctrlPt;
	}

	/**
	 * Returns the local data endpoint used for the connection.
	 * <p>
	 * 
	 * @return data endpoint in a HPAI
	 */
	public final HPAI getDataEndpoint()
	{
		return dataPt;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return ctrlPt.getStructLength() + dataPt.getStructLength() + cri.getStructLength();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		byte[] buf = ctrlPt.toByteArray();
		os.write(buf, 0, buf.length);
		buf = dataPt.toByteArray();
		os.write(buf, 0, buf.length);
		buf = cri.toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

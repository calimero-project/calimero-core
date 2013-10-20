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
*/

package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.knxnetip.util.DeviceDIB;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.knxnetip.util.ServiceFamiliesDIB;

/**
 * Represents a KNXnet/IP search response.
 * <p>
 * Such a response is used during device discovery and sent in reply to a search request.
 * It contains one server control endpoint and self description of the server device.<br>
 * An answering server, supporting more than one KNX connections at the same time, sends a
 * single search response for each of its control endpoints, i.e., one response for one
 * control endpoint.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.SearchRequest
 */
public class SearchResponse extends ServiceType
{
	private final HPAI endpt;
	private final DescriptionResponse desc;

	/**
	 * Creates a new search response out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a search response structure
	 * @param offset start offset of response in <code>data</code>
	 * @throws KNXFormatException if no search response was found or invalid structure
	 */
	public SearchResponse(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.SEARCH_RES);
		endpt = new HPAI(data, offset);
		desc = new DescriptionResponse(data, offset + endpt.getStructLength());
	}

	/**
	 * Creates a new search response for the given control endpoint together with device
	 * information.
	 * <p>
	 * 
	 * @param ctrlEndpoint discovered control endpoint of the server sending this response
	 * @param device server device description information
	 * @param svcFamilies supported service families by the server
	 */
	public SearchResponse(final HPAI ctrlEndpoint, final DeviceDIB device,
		final ServiceFamiliesDIB svcFamilies)
	{
		super(KNXnetIPHeader.SEARCH_RES);
		endpt = ctrlEndpoint;
		desc = new DescriptionResponse(device, svcFamilies);
	}

	/**
	 * Returns the server control endpoint.
	 * <p>
	 * 
	 * @return discovered control endpoint in a HPAI
	 */
	public final HPAI getControlEndpoint()
	{
		return endpt;
	}

	/**
	 * Returns the device description information block of the server contained in the
	 * response.
	 * <p>
	 * 
	 * @return a device DIB
	 */
	public final DeviceDIB getDevice()
	{
		return desc.getDevice();
	}

	/**
	 * Returns the supported service families description information block of the server
	 * contained in the response.
	 * <p>
	 * 
	 * @return a DIB with the supported service families
	 */
	public final ServiceFamiliesDIB getServiceFamilies()
	{
		return desc.getServiceFamilies();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	int getStructLength()
	{
		return endpt.getStructLength() + desc.getDevice().getStructLength()
				+ desc.getServiceFamilies().getStructLength();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		byte[] buf = endpt.toByteArray();
		os.write(buf, 0, buf.length);
		buf = desc.getDevice().toByteArray();
		os.write(buf, 0, buf.length);
		buf = desc.getServiceFamilies().toByteArray();
		os.write(buf, 0, buf.length);
		return os.toByteArray();
	}
}

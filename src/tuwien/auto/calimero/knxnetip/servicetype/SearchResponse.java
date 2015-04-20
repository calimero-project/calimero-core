/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof SearchResponse))
			return false;
		final SearchResponse other = (SearchResponse) obj;
		return endpt.equals(other.endpt) && desc.equals(other.desc);
	}

	@Override
	public int hashCode()
	{
		final int prime = 17;
		return prime * endpt.hashCode() + desc.hashCode();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		return endpt.getStructLength() + desc.getDevice().getStructLength()
				+ desc.getServiceFamilies().getStructLength();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
	@Override
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

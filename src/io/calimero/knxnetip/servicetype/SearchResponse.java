/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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
import java.util.Collection;
import java.util.List;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.knxnetip.util.DIB;
import io.calimero.knxnetip.util.DeviceDIB;
import io.calimero.knxnetip.util.HPAI;
import io.calimero.knxnetip.util.ServiceFamiliesDIB;

/**
 * Represents a KNXnet/IP search response.
 * <p>
 * Such a response is used during device discovery and sent in reply to a search request.
 * It contains one server control endpoint and self-description of the server device.<br>
 * An answering server, supporting more than one KNX connections at the same time, sends a
 * single search response for each of its control endpoints, i.e., one response for one
 * control endpoint.
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 * @see io.calimero.knxnetip.servicetype.SearchRequest
 */
public class SearchResponse extends ServiceType
{
	private final HPAI endpt;
	private final DescriptionResponse desc;

	/**
	 * Creates a new search response from a byte array.
	 *
	 * @param h KNXnet/IP header preceding the search response in the byte array
	 * @param data byte array containing a search response
	 * @param offset start offset of response in {@code data}
	 * @return search response
	 * @throws KNXFormatException on wrong structure size or invalid host protocol address information
	 */
	public static SearchResponse from(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		final int svcType = h.getServiceType();
		if (svcType != KNXnetIPHeader.SEARCH_RES && svcType != KNXnetIPHeader.SearchResponse)
			throw new KNXIllegalArgumentException("not a search response");
		return new SearchResponse(svcType, data, offset, h.getTotalLength() - h.getStructLength());
	}

	/**
	 * Creates a new search response out of a byte array.
	 *
	 * @param data byte array containing a search response structure
	 * @param offset start offset of response in {@code data}
	 * @param length usable length of {@code data}, {@code 0 < length ≤ (data.length - offset)}
	 * @throws KNXFormatException if no search response was found or invalid structure
	 */
	public SearchResponse(final byte[] data, final int offset, final int length) throws KNXFormatException
	{
		this(KNXnetIPHeader.SEARCH_RES, data, offset, length);
	}

	private SearchResponse(final int svcType, final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(svcType);
		endpt = HPAI.from(data, offset);
		desc = new DescriptionResponse(data, offset + endpt.getStructLength(), length - endpt.getStructLength());
	}

	/**
	 * Creates a new search response for the given control endpoint together with device information.
	 *
	 * @param ctrlEndpoint discovered control endpoint of the server sending this response
	 * @param device server device description information
	 * @param svcFamilies supported service families by the server
	 */
	public SearchResponse(final HPAI ctrlEndpoint, final DeviceDIB device, final ServiceFamiliesDIB svcFamilies)
	{
		super(KNXnetIPHeader.SEARCH_RES);
		endpt = ctrlEndpoint;
		desc = new DescriptionResponse(device, svcFamilies);
	}

	/**
	 * Creates a new search response for the given control endpoint together with device information.
	 *
	 * @param ext search response with extra DIBs
	 * @param ctrlEndpoint discovered control endpoint of the server sending this response
	 * @param dibs server device description information
	 */
	public SearchResponse(final boolean ext, final HPAI ctrlEndpoint, final List<DIB> dibs) {
		super(ext ? KNXnetIPHeader.SearchResponse : KNXnetIPHeader.SEARCH_RES);
		endpt = ctrlEndpoint;
		if (!ext) {
			if (dibs.size() < 2)
				throw new KNXIllegalArgumentException("search response shall contain device & service families DIB");
			for (final DIB dib : dibs) {
				switch (dib.getDescTypeCode()) {
					case DIB.TunnelingInfo -> throw new KNXIllegalArgumentException(
							"search response shall not contain Tunneling Information DIB (0x07)");
					case DIB.AdditionalDeviceInfo -> throw new KNXIllegalArgumentException(
							"search response shall not contain Extended Device Information DIB (0x08)");
				}
			}
		}
		desc = new DescriptionResponse(dibs);
	}

	/**
	 * Returns the server control endpoint.
	 *
	 * @return discovered control endpoint in a HPAI
	 */
	public final HPAI getControlEndpoint()
	{
		return endpt;
	}

	/**
	 * Returns the device description information block of the server contained in the response.
	 *
	 * @return a device DIB
	 */
	public final DeviceDIB getDevice()
	{
		return desc.getDevice();
	}

	/**
	 * Returns the supported service families description information block of the server contained in the response.
	 *
	 * @return a DIB with the supported service families
	 */
	public final ServiceFamiliesDIB getServiceFamilies()
	{
		return desc.getServiceFamilies();
	}

	/**
	 * {@return the complete information contained in this response, as list of description information blocks (DIBs)}
	 */
	public final Collection<DIB> description() {
		return desc.getDescription();
	}

	/**
	 * @return {@code true} if this is a KNXnet/IP v2 search response, {@code false} otherwise
	 */
	public final boolean v2() { return svcType == KNXnetIPHeader.SearchResponse; }

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof final SearchResponse other))
			return false;
		return endpt.equals(other.endpt) && desc.equals(other.desc);
	}

	@Override
	public int hashCode()
	{
		final int prime = 17;
		return prime * endpt.hashCode() + desc.hashCode();
	}

	@Override
	public String toString()
	{
		if (endpt.hostProtocol() == HPAI.IPV4_TCP)
			return desc.toString();
		return endpt + " " + desc;
	}

	@Override
	public int length()
	{
		return endpt.getStructLength() + desc.length();
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		byte[] buf = endpt.toByteArray();
		os.write(buf, 0, buf.length);
		for (final DIB dib : desc.getDescription()) {
			buf = dib.toByteArray();
			os.write(buf, 0, buf.length);
		}
		return os.toByteArray();
	}
}

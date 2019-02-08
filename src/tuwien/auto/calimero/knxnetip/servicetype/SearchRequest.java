/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2005 B. Erb
    Copyright (c) 2006, 2019 B. Malinowsky
    Copyright (c) 2018 Karsten Heimrich

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.knxnetip.util.Srp;

/**
 * Represents a KNXnet/IP search request.
 * <p>
 * Such request is sent during KNXnet/IP device discovery destined to the discovery
 * endpoint of listening servers. The communication is done multicast, i.e., using the UDP
 * transport protocol.<br>
 * The counterpart sent in reply to the request are search responses.
 * <p>
 * Objects of this type are immutable.
 *
 * @author Bernhard Erb
 * @author B. Malinowsky
 * @author Karsten Heimrich
 * @see tuwien.auto.calimero.knxnetip.servicetype.SearchResponse
 * @see tuwien.auto.calimero.knxnetip.Discoverer
 */
public class SearchRequest extends ServiceType
{
	private final HPAI endpoint;
	private final List<Srp> srps = new ArrayList<>();

	private static final int MinimumStructSize = 8;

	/**
	 * Creates a new search request from a byte array.
	 *
	 * @param h KNXnet/IP header preceding the search request in the byte array
	 * @param data byte array containing a search request
	 * @param offset start offset of request in <code>data</code>
	 * @return search request
	 * @throws KNXFormatException on wrong structure size or invalid host protocol address information
	 */
	public static SearchRequest from(final KNXnetIPHeader h, final byte[] data, final int offset) throws KNXFormatException {
		final int svcType = h.getServiceType();
		if (svcType != KNXnetIPHeader.SEARCH_REQ && svcType != KNXnetIPHeader.SearchRequest)
			throw new KNXIllegalArgumentException("not a search request");
		return new SearchRequest(svcType, data, offset, h.getTotalLength() - h.getStructLength());
	}

	/**
	 * Creates a search request out of a byte array.
	 *
	 * @param data byte array containing a search request structure
	 * @param offset start offset of request in <code>data</code>
	 * @throws KNXFormatException if no valid host protocol address information was found
	 */
	public SearchRequest(final byte[] data, final int offset) throws KNXFormatException
	{
		this(KNXnetIPHeader.SEARCH_REQ, data, offset, MinimumStructSize);
	}

	private SearchRequest(final int svc, final byte[] data, final int offset, final int length) throws KNXFormatException {
		super(svc);
		if (length < MinimumStructSize)
			throw new KNXFormatException("wrong size for search.req, requires " + MinimumStructSize + " bytes", length);
		endpoint = new HPAI(data, offset);

		if (svc == KNXnetIPHeader.SearchRequest && length > MinimumStructSize) {
			int index = endpoint.getStructLength();
			while (index < length) {
				final Srp srp = new Srp(data, offset + index);
				srps.add(srp);
				index += srp.getStructLength();
			}
		}
	}

	/**
	 * Creates a new search request with the given response address.
	 *
	 * @param responseAddr address of the client discovery endpoint used for the response,
	 *        use <code>null</code> to create a NAT aware search request
	 */
	public SearchRequest(final InetSocketAddress responseAddr)
	{
		super(KNXnetIPHeader.SEARCH_REQ);
		endpoint = new HPAI(HPAI.IPV4_UDP, responseAddr);
	}

	/**
	 * Creates a new search request with the given response address and search parameters.
	 *
	 * @param responseAddr address of the client discovery endpoint used for the response; to create a NAT aware search
	 *        request, use the anylocal/wildcard address with port 0 ({@code new InetSocketAddress(0)})
	 * @param searchParameters list of optional search request parameter (SRP) blocks, specializing the extended search
	 *        request
	 */
	public SearchRequest(final InetSocketAddress responseAddr, final Srp... searchParameters)
	{
		super(KNXnetIPHeader.SearchRequest);
		endpoint = new HPAI(HPAI.IPV4_UDP, responseAddr);
		for (final Srp searchParameter : searchParameters)
			srps.add(searchParameter);
	}

	/**
	 * Convenience constructor to create a new search request using the system default
	 * local host with the given client port.
	 *
	 * @param responsePort port number of the client control endpoint used for the
	 *        response, 0 &lt;= port &lt;= 0xFFFF
	 */
	public SearchRequest(final int responsePort)
	{
		super(KNXnetIPHeader.SEARCH_REQ);
		endpoint = new HPAI((InetAddress) null, responsePort);
	}

	/**
	 * Returns the client discovery endpoint.
	 *
	 * @return discovery endpoint in a HPAI
	 */
	public final HPAI getEndpoint()
	{
		return endpoint;
	}

	/**
	 * @return unmodifiable list of search request parameter (SRP) blocks
	 */
	public final List<Srp> searchParameters() {
		return Collections.unmodifiableList(srps);
	}

	@Override
	int getStructLength()
	{
		return endpoint.getStructLength() + searchParametersSize();
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		byte[] buf = endpoint.toByteArray();
		os.write(buf, 0, buf.length);
		if (svcType == KNXnetIPHeader.SearchRequest) {
			for (final Srp srp : srps) {
				buf = srp.toByteArray();
				os.write(buf, 0, buf.length);
			}
		}
		return os.toByteArray();
	}

	private int searchParametersSize() {
		if (svcType == KNXnetIPHeader.SearchRequest)
			return srps.stream().map(Srp::getStructLength).mapToInt(Integer::intValue).sum();
		return 0;
	}
}

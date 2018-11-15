/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2018 B. Malinowsky

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.knxnetip.util.DIB;
import tuwien.auto.calimero.knxnetip.util.DeviceDIB;
import tuwien.auto.calimero.knxnetip.util.IPConfigDIB;
import tuwien.auto.calimero.knxnetip.util.IPCurrentConfigDIB;
import tuwien.auto.calimero.knxnetip.util.KnxAddressesDIB;
import tuwien.auto.calimero.knxnetip.util.ManufacturerDIB;
import tuwien.auto.calimero.knxnetip.util.ServiceFamiliesDIB;

/**
 * Represents a description response.
 * <p>
 * Such response is sent by a server in reply to a description request from a client.<br>
 * A response contains various description information blocks (DIBs), mandatory being a Device DIB
 * and a Supported Service Families DIB. Optionally, the response might contain additional DIBs with
 * other information. Each DIB can at most occur once, the following DIBs are recognized (others are ignored):
 * <ul>
 * <li>{@link DeviceDIB}</li>
 * <li>{@link ServiceFamiliesDIB}</li>
 * <li>{@link IPConfigDIB}</li>
 * <li>{@link IPCurrentConfigDIB}</li>
 * <li>{@link KnxAddressesDIB}</li>
 * <li>{@link ManufacturerDIB}</li>
 * </ul>
 * <p>
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DescriptionRequest
 */
public class DescriptionResponse extends ServiceType
{
	private final DeviceDIB device;
	private final ServiceFamiliesDIB suppfam;

	// the following DIBs are optional in the specification:
	// IPConfigDIB, IPCurrentConfigDIB, KnxAddressesDIB, ManufacturerDIB
	private final List<DIB> optional;

	/**
	 * Creates a new description response out of a byte array.
	 *
	 * @param data byte array containing a description response structure
	 * @param offset start offset of response in <code>data</code>
	 * @param length usable length of <code>data</code>, <code>0 &lt; length &le; (data.length - offset)</code>
	 * @throws KNXFormatException if no description response was found or invalid structure of DIBs carried in the
	 *         response
	 */
	public DescriptionResponse(final byte[] data, final int offset, final int length) throws KNXFormatException
	{
		super(KNXnetIPHeader.DESCRIPTION_RES);
		device = new DeviceDIB(data, offset);
		suppfam = new ServiceFamiliesDIB(data, offset + device.getStructLength());

		// parse optional DIBs
		optional = new ArrayList<>();
		IPConfigDIB c = null;
		IPCurrentConfigDIB cc = null;
		KnxAddressesDIB a = null;
		ManufacturerDIB m = null;
		int i = offset + device.getStructLength() + suppfam.getStructLength();
		while (i + 1 < offset + length) {
			final int size = data[i] & 0xff;
			final int type = data[i + 1] & 0xff;
			// skip any remaining DIBs on invalid size, as we cannot advance our index
			if (size == 0)
				break;

			boolean unique = true;
			if (type == DIB.IP_CONFIG && (unique = c == null))
				optional.add(c = new IPConfigDIB(data, i));
			else if (type == DIB.IP_CURRENT_CONFIG && (unique = cc == null))
				optional.add(cc = new IPCurrentConfigDIB(data, i));
			else if (type == DIB.KNX_ADDRESSES && (unique = a == null))
				optional.add(a = new KnxAddressesDIB(data, i));
			else if (type == DIB.MFR_DATA && (unique = m == null))
				optional.add(m = new ManufacturerDIB(data, i));
			else if (!unique)
				throw new KNXFormatException("response contains duplicate DIB type", type);
			else
				logger.warn("skip unknown DIB in response with type code {} and size {}", type, size);
			i += size;
		}
	}

	/**
	 * Creates a new description response containing a device DIB, a supported service families DIB, and optional other
	 * DIBs.
	 *
	 * @param device device description
	 * @param svcFamilies supported service families
	 * @param dibs optional DIBs to add to the response
	 */
	public DescriptionResponse(final DeviceDIB device, final ServiceFamiliesDIB svcFamilies, final DIB... dibs)
	{
		super(KNXnetIPHeader.DESCRIPTION_RES);
		this.device = device;
		suppfam = svcFamilies;

		optional = new ArrayList<>(Arrays.asList(dibs));
		if (optional.removeIf(Objects::isNull))
			ServiceType.logger.error("optional DIBs should not contain null elements", new NullPointerException());
	}

	/**
	 * @return the complete description information contained in this response, as list of
	 *         description information blocks (DIBs)
	 */
	public final List<DIB> getDescription()
	{
		final List<DIB> l = new ArrayList<>();
		l.add(device);
		l.add(suppfam);
		l.addAll(optional);
		return l;
	}

	/**
	 * Returns the device description information block contained in the response.
	 *
	 * @return a device DIB
	 */
	public final DeviceDIB getDevice()
	{
		return device;
	}

	/**
	 * Returns the supported service families description information block.
	 *
	 * @return a DIB with the supported service families
	 */
	public final ServiceFamiliesDIB getServiceFamilies()
	{
		return suppfam;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof DescriptionResponse))
			return false;
		final DescriptionResponse other = (DescriptionResponse) obj;
		return device.equals(other.device) && optional.size() == other.optional.size();
	}

	@Override
	public int hashCode()
	{
		final int prime = 17;
		return prime * device.hashCode();
	}

	@Override
	public String toString()
	{
		return getDescription().stream().map(Object::toString).collect(Collectors.joining(", "));
	}

	@Override
	int getStructLength()
	{
		int len = 0;
		for (final Iterator<DIB> i = getDescription().iterator(); i.hasNext();)
			len += i.next().getStructLength();
		return len;
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		for (final Iterator<DIB> i = getDescription().iterator(); i.hasNext();) {
			final byte[] bytes = i.next().toByteArray();
			os.write(bytes, 0, bytes.length);
		}
		return os.toByteArray();
	}
}

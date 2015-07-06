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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * other information. Each DIB can at most occur once, the following DIBs are recognized:
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

	// the following DIBs are optional in the specification
	private final IPConfigDIB config;
	private final IPCurrentConfigDIB currentConfig;
	private final KnxAddressesDIB addresses;
	private final ManufacturerDIB mfr;

	/**
	 * Creates a new description response out of a byte array.
	 * <p>
	 *
	 * @param data byte array containing a description response structure
	 * @param offset start offset of response in <code>data</code>
	 * @throws KNXFormatException if no description response was found or invalid
	 *         structure of DIBs carried in the response
	 */
	public DescriptionResponse(final byte[] data, final int offset) throws KNXFormatException
	{
		super(KNXnetIPHeader.DESCRIPTION_RES);
		device = new DeviceDIB(data, offset);
		suppfam = new ServiceFamiliesDIB(data, offset + device.getStructLength());

		// parse optional DIBs
		IPConfigDIB c = null;
		IPCurrentConfigDIB cc = null;
		KnxAddressesDIB a = null;
		ManufacturerDIB m = null;
		int i = offset + device.getStructLength() + suppfam.getStructLength();
		while (i + 1 < data.length) {
			final int size = data[i] & 0xff;
			final int type = data[i + 1] & 0xff;
			boolean unique = true;
			if (type == DIB.IP_CONFIG && (unique = c == null))
				c = new IPConfigDIB(data, i);
			else if (type == DIB.IP_CURRENT_CONFIG  && (unique = cc == null))
				cc = new IPCurrentConfigDIB(data, i);
			else if (type == DIB.KNX_ADDRESSES  && (unique = a == null))
				a = new KnxAddressesDIB(data, i);
			else if (type == DIB.MFR_DATA  && (unique = m == null))
				m = new ManufacturerDIB(data, i);
			else if (!unique)
				throw new KNXFormatException("description response contains duplicate DIB type",
						type);
			else if (type == 0 || size == 0) // break on invalid field, ensure we always progress i
				break;
			else
				logger.warn("description response contains unknown DIB with type code "
						+ type + " and size " + size + ", ignore");
			i += size;
		}

		config = c;
		currentConfig = cc;
		addresses = a;
		mfr = m;
	}

	/**
	 * Creates a new description response containing a device DIB and a supported service
	 * families DIB.
	 * <p>
	 *
	 * @param device device description
	 * @param suppSvcFam supported service families
	 */
	public DescriptionResponse(final DeviceDIB device, final ServiceFamiliesDIB suppSvcFam)
	{
		this(device, suppSvcFam, null, null, null, null);
	}

	/**
	 * Creates a new description response containing a device DIB, a supported service
	 * families DIB and a manufacturer DIB.
	 * <p>
	 *
	 * @param device device description
	 * @param suppSvcFam supported service families
	 * @param mfr manufacturer specific data
	 */
	public DescriptionResponse(final DeviceDIB device, final ServiceFamiliesDIB suppSvcFam,
		final ManufacturerDIB mfr)
	{
		this(device, suppSvcFam, null, null, null, mfr);
	}

	/**
	 * Creates a new description response containing a device DIB, a supported service
	 * families DIB and a manufacturer DIB.
	 * <p>
	 *
	 * @param device device description
	 * @param suppSvcFamilies supported service families
	 * @param config IP configuration
	 * @param currentConfig IP current configuration
	 * @param addresses KNX individual addresses information
	 * @param mfr manufacturer specific data
	 */
	public DescriptionResponse(final DeviceDIB device, final ServiceFamiliesDIB suppSvcFamilies,
		final IPConfigDIB config, final IPCurrentConfigDIB currentConfig,
		final KnxAddressesDIB addresses, final ManufacturerDIB mfr)
	{
		super(KNXnetIPHeader.DESCRIPTION_RES);
		this.device = device;
		suppfam = suppSvcFamilies;
		this.config = config;
		this.currentConfig = currentConfig;
		this.addresses = addresses;
		this.mfr = mfr;
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
		if (config != null)
			l.add(config);
		if (currentConfig != null)
			l.add(currentConfig);
		if (addresses != null)
			l.add(addresses);
		if (mfr != null)
			l.add(mfr);
		return l;
	}

	/**
	 * Returns the device description information block contained in the response.
	 * <p>
	 *
	 * @return a device DIB
	 */
	public final DeviceDIB getDevice()
	{
		return device;
	}

	/**
	 * Returns the supported service families description information block.
	 * <p>
	 *
	 * @return a DIB with the supported service families
	 */
	public final ServiceFamiliesDIB getServiceFamilies()
	{
		return suppfam;
	}

	/**
	 * Returns the manufacturer data description information block optionally contained in
	 * the response.
	 * <p>
	 * The manufacturer data is not a mandatory part of a description response. It is only
	 * available, if the optional DIB information of a response matches this DIB type.<br>
	 *
	 * @return a manufacturer DIB, or <code>null</code> if no such DIB
	 */
	public final ManufacturerDIB getManufacturerData()
	{
		return mfr;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof DescriptionResponse))
			return false;
		final DescriptionResponse other = (DescriptionResponse) obj;
		// device DIB should suffice for equality
		return device.equals(other.device);
	}

	@Override
	public int hashCode()
	{
		final int prime = 17;
		return prime * device.hashCode();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#getStructLength()
	 */
	@Override
	int getStructLength()
	{
		int len = 0;
		for (final Iterator<DIB> i = getDescription().iterator(); i.hasNext();)
			len += i.next().getStructLength();
		return len;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceType#toByteArray
	 *      (java.io.ByteArrayOutputStream)
	 */
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

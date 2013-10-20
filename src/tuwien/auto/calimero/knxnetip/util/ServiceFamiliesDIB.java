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

package tuwien.auto.calimero.knxnetip.util;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Supported service families description information block.
 * <p>
 * It informs about the service families supported by a device.<br>
 * While this class defines constants of available service family IDs, other service
 * families not listed can also be used in this DIB.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse
 */
public class ServiceFamiliesDIB extends DIB
{
	/**
	 * Service family identifier representing the service type 'KNXnet/IP Core'.
	 * <p>
	 */
	public static final int CORE = 0x02;

	/**
	 * Service family identifier representing the service type 'KNXnet/IP Device
	 * Management'.
	 * <p>
	 */
	public static final int DEVICE_MANAGEMENT = 0x03;

	/**
	 * Service family identifier representing the service type 'KNXnet/IP Tunneling'.
	 * <p>
	 */
	public static final int TUNNELING = 0x04;

	/**
	 * Service family identifier representing the service type 'KNXnet/IP Routing'.
	 * <p>
	 */
	public static final int ROUTING = 0x05;

	/**
	 * Service family identifier representing the service type 'KNXnet/IP Remote Logging'.
	 * <p>
	 */
	public static final int REMOTE_LOGGING = 0x06;

	/**
	 * Service family identifier representing the service type 'KNXnet/IP Remote
	 * Configuration and Diagnosis'.
	 * <p>
	 */
	public static final int REMOTE_CONFIGURATION_DIAGNOSIS = 0x07;

	/**
	 * Service family identifier representing the service type 'KNXnet/IP Object Server'.
	 * <p>
	 */
	public static final int OBJECT_SERVER = 0x08;

	private static final String[] familyNames = { null, null, "Core", "Device Management",
		"Tunneling", "Routing", "Remote Logging", "Remote Configuration/Diagnosis", "Object Server" };

	private final int[] ids;
	private final int[] versions;

	/**
	 * Creates a service families DIB out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing the service families DIB structure
	 * @param offset start offset of DIB in <code>data</code>
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public ServiceFamiliesDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != SUPP_SVC_FAMILIES)
			throw new KNXFormatException("not a supported service families DIB", type);
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset + 2, size - 2);
		ids = new int[size / 2 - 1];
		versions = new int[ids.length];
		for (int i = 0; i < ids.length; ++i) {
			ids[i] = is.read();
			versions[i] = is.read();
		}
	}

	/**
	 * Creates a service families DIB using the provided service families and versions
	 * arrays.
	 * <p>
	 * The service families are added to the DIB in the same order they are listed in the
	 * service family array <code>familyIDs</code>, accessed by increasing index. The two
	 * arrays <code>familyIDs</code> and <code>familyVersions</code> have to be of same
	 * length, <code>familyIDs.length == familyVersions.length</code>. A service family ID
	 * shall be contained only once in <code>familyIDs</code>. Otherwise, all but the last
	 * of that specific service family ID are ignored (as well as their version number).
	 * 
	 * @param familyIDs array containing the supported service family identifiers, use the
	 *        service family identifier constants as provided by this class;
	 *        <code>0 <= familyIDs[i] <= 255</code>, for all i with
	 *        <code>0 <= i < familyIDs.length</code>
	 * @param familyVersions array containing the version of the corresponding items
	 *        listed in the <code>familyIDs</code> parameter with the same index;
	 *        <code>0 <= familyVersions[i] <= 255</code>, for all i with
	 *        <code>0 <= i < familyVersions.length</code>
	 */
	public ServiceFamiliesDIB(final int[] familyIDs, final int[] familyVersions)
	{
		super(2 + 2 * familyIDs.length, SUPP_SVC_FAMILIES);
		// maximum size of 20 is arbitrarily chosen as sanitation measure, but considered
		// a reasonable boundary
		if (familyIDs.length != familyVersions.length || familyIDs.length > 20)
			throw new KNXIllegalArgumentException("size of arrays have to match, with size <= 20");

		ids = new int[familyIDs.length];
		versions = new int[ids.length];
		for (int i = 0; i < ids.length; ++i)
			add(familyIDs[i], familyVersions[i], i);
	}

	/**
	 * Creates a service families DIB using the provided service family entries.
	 * <p>
	 * The family entries are added to the DIB in arbitrary order (for example, it might
	 * be the order as returned by the <code>families</code> entry iterator).
	 * <p>
	 * 
	 * @param families (unmodifiable) map containing the supported service families, with
	 *        the service family of type {@link Integer} being the key, and the version of
	 *        type {@link Integer} being the value.
	 */
	public ServiceFamiliesDIB(final Map families)
	{
		super(2 + 2 * families.size(), SUPP_SVC_FAMILIES);
		// maximum size of 20 is arbitrarily chosen as sanitation measure, but considered
		// a reasonable boundary
		if (families.size() > 20)
			throw new KNXIllegalArgumentException("number of families must not exceed 20");

		ids = new int[families.size()];
		versions = new int[ids.length];
		int count = 0;
		for (final Iterator i = families.entrySet().iterator(); i.hasNext();) {
			final Map.Entry e = (Map.Entry) i.next();
			add(((Integer) e.getKey()).intValue(), ((Integer) e.getValue()).intValue(), count++);
		}
	}

	/**
	 * Returns the service families of this DIB, each family together with the version it
	 * is implemented and supported up to.
	 * <p>
	 * The returned set holds <code>Map.Entry</code> items, with the service family of
	 * type {@link Integer} being the key, and the version of type {@link Integer} being
	 * the value.
	 * 
	 * @return an unmodifiable set containing supported entries (family-version pair)
	 */
	//public final Map getFamilies()
	//{
	//	return Collections.unmodifiableSet(map.entrySet());
	//}

	/**
	 * Returns the service families of this DIB as array of family IDs.
	 * <p>
	 * 
	 * @return a new array containing the IDs of the supported service families, the array
	 *         size reflects the number of supported service families
	 */
	public final int[] getFamilyIds()
	{
		return (int[]) ids.clone();
	}

	/**
	 * Returns the version associated to a given supported service family.
	 * <p>
	 * If the service family is not supported, 0 is returned.
	 * 
	 * @param familyId supported service family ID to lookup
	 * @return version as unsigned byte, or 0
	 */
	public final int getVersion(final int familyId)
	{
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == familyId)
				return versions[i];
		}
		return 0;
	}

	/**
	 * Returns the service family name for the supplied family ID.
	 * <p>
	 * 
	 * @param familyId service family ID to get name for
	 * @return family name as string, or <code>null</code> on no name available
	 */
	public final String getFamilyName(final int familyId)
	{
		return familyId < familyNames.length ? familyNames[familyId] : null;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.util.DIB#toByteArray()
	 */
	public byte[] toByteArray()
	{
		final byte[] buf = super.toByteArray();
		int k = 2;
		for (int i = 0; i < ids.length; i++) {
			buf[k++] = (byte) ids[i];
			buf[k++] = (byte) versions[i];
		}
		return buf;
	}

	/**
	 * Returns a textual representation of this supported service families DIB.
	 * <p>
	 * 
	 * @return a string representation of the DIB object
	 */
	public String toString()
	{
		final StringBuffer buf = new StringBuffer();
		for (int i = 0; i < ids.length; i++) {
			buf.append(getFamilyName(ids[i]));
			buf.append(" version ").append(versions[i]);
			if (i + 1 < ids.length)
				buf.append(", ");
		}
		return buf.toString();
	}

	private void add(final int familyId, final int familyVersion, final int position)
	{
		if (familyId < 0 || familyId > 255 || familyVersion < 0 || familyVersion > 255)
			throw new KNXIllegalArgumentException("value out of range [0..255]");
		ids[position] = familyId;
		versions[position] = familyVersion;
	}
}

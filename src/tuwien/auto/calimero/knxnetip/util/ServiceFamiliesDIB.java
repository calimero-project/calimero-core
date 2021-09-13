/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2021 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip.util;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

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
	 * KNXnet/IP service family identifiers.
	 */
	public enum ServiceFamily {
		Core,
		DeviceManagement,
		Tunneling,
		Routing,
		RemoteLogging,
		RemoteConfigurationDiagnosis,
		ObjectServer,
		Security,
		Baos;

		public int id() {
			if (this == Baos)
				return 0xf0;
			return ordinal() + 2;
		}

		@Override
		public String toString() {
			// ??? Configuration/Diagnosis now has a space, not a slash
			return name().replaceAll("(\\p{Lower})\\B([A-Z])", "$1 $2");
		}

		public static ServiceFamily of(final int familyId) {
			if (familyId == Baos.id())
				return Baos;
			if (familyId < Core.id() || familyId > Security.id())
				throw new KNXIllegalArgumentException(familyId + " is not a supported service family");

			return values()[familyId - 2];
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int CORE = 0x02;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int DEVICE_MANAGEMENT = 0x03;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int TUNNELING = 0x04;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int ROUTING = 0x05;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int REMOTE_LOGGING = 0x06;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int REMOTE_CONFIGURATION_DIAGNOSIS = 0x07;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int OBJECT_SERVER = 0x08;

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final int Security = 0x09;


	private final EnumMap<ServiceFamily, Integer> families = new EnumMap<>(ServiceFamily.class);


	/**
	 * Creates a service families DIB out of a byte array.
	 *
	 * @param data byte array containing the service families DIB structure
	 * @param offset start offset of DIB in <code>data</code>
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public ServiceFamiliesDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != SUPP_SVC_FAMILIES && type != SecureServiceFamilies)
			throw new KNXFormatException("not a supported service families DIB", type);
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset + 2, size - 2);
		final int length = size / 2 - 1;
		try {
			for (int i = 0; i < length; ++i) {
				final int familyId = is.read();
				families.put(ServiceFamily.of(familyId), is.read());
			}
		}
		catch (final KNXIllegalArgumentException e) {
			throw new KNXFormatException(e.getMessage());
		}
	}

	public static ServiceFamiliesDIB newSecureServiceFamilies(final Map<ServiceFamily, Integer> families) {
		return new ServiceFamiliesDIB(true, families);
	}

	/**
	 * Creates a service families DIB using the provided service family entries.
	 *
	 * @param families map containing the supported service families, with the value being the supported version in the
	 * range [0..255]
	 */
	public ServiceFamiliesDIB(final Map<ServiceFamily, Integer> families)
	{
		super(2 + 2 * families.size(), SUPP_SVC_FAMILIES);
		for (final var entry : families.entrySet())
			add(entry.getKey(), entry.getValue());
	}

	private ServiceFamiliesDIB(final boolean secure, final Map<ServiceFamily, Integer> families)
	{
		super(2 + 2 * families.size(), secure ? SecureServiceFamilies : SUPP_SVC_FAMILIES);
		this.families.putAll(families);
	}

	/**
	 * Returns the service families of this DIB, each family together with the version it
	 * is implemented and supported up to.
	 *
	 * @return an unmodifiable map containing family-version mappings
	 */
	public final Map<ServiceFamily, Integer> families() { return Collections.unmodifiableMap(families); }

	@Override
	public byte[] toByteArray()
	{
		final byte[] buf = super.toByteArray();
		int k = 2;
		for (final var entry : families.entrySet()) {
			buf[k++] = (byte) entry.getKey().id();
			buf[k++] = (byte) (int) entry.getValue();
		}
		return buf;
	}

	/**
	 * Returns a textual representation of this supported service families DIB.
	 *
	 * @return a string representation of the DIB object
	 */
	@Override
	public String toString() {
		if (type == SecureServiceFamilies && families.isEmpty())
			return "KNX IP Secure n/a";
		final var buf = new StringJoiner(", ");
		final String prefix = type == SecureServiceFamilies ? "Secure " : "";
		for (final var entry : families.entrySet())
			buf.add(prefix + entry.getKey() + " (v" + entry.getValue() + ")");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		return families.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ServiceFamiliesDIB))
			return false;
		final ServiceFamiliesDIB other = (ServiceFamiliesDIB) obj;
		return families.equals(other.families);
	}

	private void add(final ServiceFamily familyId, final int familyVersion)
	{
		if (familyVersion < 0 || familyVersion > 255)
			throw new KNXIllegalArgumentException("version out of range [0..255]");

		families.put(familyId, familyVersion);
	}
}

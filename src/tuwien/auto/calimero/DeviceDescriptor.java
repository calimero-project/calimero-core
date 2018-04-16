/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2018 B. Malinowsky

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

package tuwien.auto.calimero;

import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.KnxIPSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * The Device Descriptor interface for KNX device descriptor types 0 (DD0) and 2 (DD2). A device
 * descriptor is useful in identifying a remote communication partner, its type, version, among
 * others.
 * <p>
 * The two descriptor types provide parsing of raw descriptor data, and getter methods for the two
 * specified types of KNX device descriptors.
 *
 * @author B. Malinowsky
 */
public interface DeviceDescriptor
{
	int TYPE_SIZE = 2;
	int TYPE2_SIZE = 14;

	@Deprecated
	static DeviceDescriptor fromType(final byte[] data) throws KNXFormatException
	{
		return from(data);
	}

	/**
	 * Construct a device descriptor by parsing raw data.
	 *
	 * @param data the descriptor data, either containing DD0 or DD2 data
	 * @return a {@link DeviceDescriptor}
	 * @throws KNXFormatException if the data does not contain a valid device descriptor
	 */
	static DeviceDescriptor from(final byte[] data) throws KNXFormatException
	{
		if (data.length == TYPE_SIZE)
			return DD0.from(data);
		if (data.length == TYPE2_SIZE)
			return new DD2(data);
		throw new KNXFormatException("unknown device descriptor type of size " + data.length);
	}

	byte[] toByteArray();

	/**
	 * The Device Descriptor Type 0 (DD0) format, providing the available mask versions for type 0.
	 * The terminology 'mask version' is equivalent with DD0.
	 */
	enum DD0 implements DeviceDescriptor
	{
		/** */
		TYPE_0010(0x0010, "System 1 (BCU 1)"),
		/** */
		TYPE_0011(0x0011, "System 1 (BCU 1)"),
		/** */
		TYPE_0012(0x0012, "System 1 (BCU 1)"),
		/** */
		TYPE_0013(0x0013, "System 1 (BCU 1)"),
		/** */
		TYPE_0020(0x0020, "System 2 (BCU 2)"),
		/** */
		TYPE_0021(0x0021, "System 2 (BCU 2)"),
		/** */
		TYPE_0025(0x0025, "System 2 (BCU 2)"),
		/** */
		TYPE_0300(0x0300, "System 300"),
		/** */
		TYPE_0310(0x0310, "TP1 USB interface v1"),
		/** */
		TYPE_0311(0x0311, "TP1 USB interface v2"),
		/** */
		TYPE_0700(0x0700, "BIM M112"),
		/** */
		TYPE_0701(0x0701, "BIM M112"),
		/** */
		TYPE_0705(0x0705, "BIM M112"),
		/** */
		TYPE_07B0(0x07B0, "System B"),
		/** */
		TYPE_0810(0x0810, "IR-Decoder"),
		/** */
		TYPE_0811(0x0811, "IR-Decoder"),
		/** */
		TYPE_0910(0x0910, "Coupler 1.0"),
		/** */
		TYPE_0911(0x0911, "Coupler 1.1"),
		/** */
		TYPE_0912(0x0912, "Coupler 1.2"),
		/** */
		TYPE_091A(0x091A, "KNXnet/IP Router"),
		/** */
		TYPE_0AFD(0x0AFD, "none"),
		/** */
		TYPE_0AFE(0x0AFE, "none"),
		/** */
		TYPE_1012(0x1012, "BCU 1"),
		/** */
		TYPE_1013(0x1013, "BCU 1"),
		/** */
		TYPE_1310(0x1310, "PL110 USB interface v1"),
		/** */
		TYPE_1311(0x1311, "PL110 USB interface v2"),
		/** */
		TYPE_17B0(0x17B0, "System B"),
		/** */
		TYPE_1900(0x1900, "Media Coupler PL-TP"),
		/** */
		TYPE_2010(0x2010, "Bidirectional devices"),
		/** */
		TYPE_2110(0x2110, "Unidirectional devices"),
		/** */
		TYPE_2311(0x2311, "RF USB interface v2"),
		/** */
		TYPE_3012(0x3012, "BCU 1"),
		/** */
		TYPE_4012(0x4012, "BCU 1"),
		/** */
		TYPE_5705(0x5705, "System 7");


		private final int mv;
		private final String profile;

		/**
		 * Returns the device descriptor type 0 from a byte array containing a mask version.
		 *
		 * @param data the type 0 descriptor data (mask version), <code>data.length == 2</code>
		 * @return the corresponding {@link DD0} object
		 */
		public static DD0 from(final byte[] data)
		{
			if (data.length != 2)
				throw new KNXIllegalArgumentException("unspecified device descriptor type 0 using "
						+ "length " + data.length + ": " + DataUnitBuilder.toHex(data, ""));
			final int i = (data[0] & 0xff) << 8 | data[1] & 0xff;
			return from(i);
		}

		/**
		 * Returns the device descriptor type 0 from an integer containing a mask version.
		 *
		 * @param descriptor the type 0 descriptor (mask version), contained in the lower 16 bits
		 * @return the corresponding {@link DD0} object
		 */
		public static DD0 from(final int descriptor) {
			for (final DD0 v : values()) {
				if (v.maskVersion() == descriptor)
					return v;
			}
			throw new KNXIllegalArgumentException("unknown mask version " + maskVersionString(descriptor));
		}

		/**
		 * @param descriptor the type 0 descriptor (mask version), contained in the lower 16 bits
		 * @return a zero-padded string of length 4 holding the hexadecimal representation of the
		 *         mask version (the used format is "%04X")
		 */
		static String maskVersionString(final int descriptor)
		{
			return String.format("%04X", descriptor);
		}

		DD0(final int mask, final String profile)
		{
			this.mv = mask;
			this.profile = profile;
		}

		/**
		 * @return the mask version as 16 bit value
		 */
		public int maskVersion()
		{
			return mv;
		}

		/**
		 * @return the medium type as 4 bit value
		 * @see #medium()
		 */
		public int mediumType()
		{
			return (mv >> 12) & 0x0f;
		}

		/**
		 * @return the KNX medium as {@link KNXMediumSettings}
		 */
		public KNXMediumSettings medium()
		{
			final int type = mediumType();
			switch (type) {
			case 0:
				return TPSettings.TP1;
			case 1:
				return new PLSettings();
			case 2:
				return new RFSettings(KNXMediumSettings.BackboneRouter);
			case 3:
				throw new KNXIllegalArgumentException("TP0 medium not supported any longer");
			case 4:
				throw new KNXIllegalArgumentException("PL132 medium not supported any longer");
			case 5:
				return new KnxIPSettings(KNXMediumSettings.BackboneRouter);
			default:
				throw new KNXIllegalArgumentException("unknown KNX medium type " + type);
			}
		}

		/**
		 * @return the firmware type
		 */
		public int firmwareType()
		{
			return (mv >> 8) & 0x0f;
		}

		/**
		 * @return the firmware version as 4 bit value (bits 7-4 of the device descriptor)
		 */
		public int firmwareVersion()
		{
			return (mv >> 4) & 0x0f;
		}

		/**
		 * @return the firmware version subcode as 4 bit value (lowest 4 bits of the device descriptor)
		 */
		public int firmwareSubcode()
		{
			return mv & 0x0f;
		}

		/**
		 * @return the device profile name as human readable string
		 */
		public String deviceProfile()
		{
			return profile;
		}

		@Override
		public String toString()
		{
			return maskVersionString(mv) + " - " + deviceProfile();
		}

		@Override
		public byte[] toByteArray()
		{
			return new byte[] { (byte) (mv >>> 8), (byte) mv };
		}
	}

	final class DD2 implements DeviceDescriptor
	{
		private final byte[] d;

		/**
		 * Returns the device descriptor type 2 from a descriptor data byte array.
		 *
		 * @param data the type 2 descriptor data, <code>data.length == 14</code>
		 * @return the corresponding {@link DD2} object
		 */
		public static DD2 from(final byte[] data)
		{
			return new DD2(data);
		}

		private DD2(final byte[] descriptor)
		{
			if (descriptor.length != TYPE2_SIZE)
				throw new KNXIllegalArgumentException(
						"unspecified device descriptor type 2 using " + "length " + descriptor.length);
			d = descriptor.clone();
			// check upper two bits, the only values allowed are 0="link mgmt not supported" and 1="supports link mgmt"
			final int v = (d[5] & 0xc0) >> 6;
			if (v > 1)
				throw new KNXIllegalArgumentException("undefined misc field value " + v + " (byte 5)");
		}

		public DD2(final int appManufacturer, final int deviceType, final int version,
			final boolean supportsLinkMgmt, final int logicalTagBase, final int channelInfo1,
			final int channelInfo2, final int channelInfo3, final int channelInfo4)
		{
			d = new byte[TYPE2_SIZE];
			int i = 0;
			d[i++] = (byte) (appManufacturer >> 8);
			d[i++] = (byte) (appManufacturer);
			d[i++] = (byte) (deviceType >> 8);
			d[i++] = (byte) (deviceType);
			d[i++] = (byte) (version);
			d[i++] = (byte) (((supportsLinkMgmt ? 1 : 0) << 6) | logicalTagBase);
			d[i++] = (byte) (channelInfo1 >> 8);
			d[i++] = (byte) (channelInfo1);
			d[i++] = (byte) (channelInfo2 >> 8);
			d[i++] = (byte) (channelInfo2);
			d[i++] = (byte) (channelInfo3 >> 8);
			d[i++] = (byte) (channelInfo3);
			d[i++] = (byte) (channelInfo4 >> 8);
			d[i++] = (byte) (channelInfo4);
		}

		/**
		 * @return the 16 bit KNX application manufacturer code of this descriptor
		 */
		public int applicationManufacturer()
		{
			return get16Bits(0);
		}

		/**
		 * @return the 16 bit manufacturer specific device type of this descriptor
		 */
		public int deviceType()
		{
			return get16Bits(2);
		}

		/**
		 * @return the 8 bit version of the manufacturer specific device type of this descriptor
		 */
		public int version()
		{
			return d[4] & 0xff;
		}

		/**
		 * @return <code>true</code> if network management procedures using A_Link_Read/Write are
		 *         supported, <code>false</code> otherwise
		 */
		public boolean supportsLinkManagement()
		{
			final int i = (d[5] & 0xc0) >> 6;
			return i == 1;
		}

		/**
		 * Returns the current value of the 6 bit Logical Tag Base. If no local selector is active,
		 * returns 0x3f. For 'general, all zones' returns 0x3e. For multi-channel devices, the value
		 * is aligned to the closest lowest authorized value.
		 *
		 * @return the current Logical Tag Base as 6 bit value
		 */
		public int logicalTagBase()
		{
			return d[5] & 0x3f;
		}

		public enum Channel {
			Channel1, Channel2, Channel3, Channel4
		};

		/**
		 * The number of channels implemented in the device for the specified channel type.
		 *
		 * @param channelType the requested channel type
		 * @return the number of channels, <code>1 &le; channels &le; 8</code>
		 */
		public int channels(final Channel channelType)
		{
			final int offset = channelType.ordinal() * 2;
			return (get16Bits(6 + offset) >> 13) + 1;
		}

		/**
		 * @param channelType the requested channel type
		 * @return the 13 bit channel code of the specified channel type
		 */
		public int channelCode(final Channel channelType)
		{
			final int offset = channelType.ordinal() * 2;
			return get16Bits(6 + offset) & 0x1fff;
		}

		@Override
		public String toString()
		{
			return DataUnitBuilder.toHex(d, "");
		}

		@Override
		public byte[] toByteArray()
		{
			return d.clone();
		}

		// offset is counted from MSB
		private int get16Bits(final int byteOffset)
		{
			int v = (d[byteOffset] & 0xff) << 8;
			v |= (d[byteOffset + 1] & 0xff);
			return v;
		}
	}
}

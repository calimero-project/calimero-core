/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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

	/**
	 * Construct a device descriptor by parsing raw data.
	 *
	 * @param data the descriptor data, either containing DD0 or DD2 data
	 * @return a {@link DeviceDescriptor}
	 * @throws KNXFormatException if the data does not contain a valid device descriptor
	 */
	static DeviceDescriptor fromType(final byte[] data) throws KNXFormatException
	{
		if (data.length == TYPE_SIZE)
			return DD0.fromType0(data);
		if (data.length == TYPE2_SIZE)
			return new DD2(data);
		throw new KNXFormatException("unknown device descriptor type of size " + data.length);
	}

	byte[] toByteArray();

	/**
	 * The Device Descriptor Type 0 (DD0) format, providing the available mask versions for type 0.
	 * The terminology 'mask version' is equivalent with DD0.
	 */
	final class DD0 implements DeviceDescriptor
	{
		/** */
		public static final DD0 TYPE_0010 = new DD0(0x0010, "System 1 (BCU 1)");
		/** */
		public static final DD0 TYPE_0011 = new DD0(0x0011, "System 1 (BCU 1)");
		/** */
		public static final DD0 TYPE_0012 = new DD0(0x0012, "System 1 (BCU 1)");
		/** */
		public static final DD0 TYPE_0013 = new DD0(0x0013, "System 1 (BCU 1)");
		/** */
		public static final DD0 TYPE_0020 = new DD0(0x0020, "System 2 (BCU 2)");
		/** */
		public static final DD0 TYPE_0021 = new DD0(0x0021, "System 2 (BCU 2)");
		/** */
		public static final DD0 TYPE_0025 = new DD0(0x0025, "System 2 (BCU 2)");
		/** */
		public static final DD0 TYPE_0300 = new DD0(0x0300, "System 300");
		/** */
		public static final DD0 TYPE_0310 = new DD0(0x0310, "TP1 USB interface v1");
		/** */
		public static final DD0 TYPE_0700 = new DD0(0x0700, "BIM M112");
		/** */
		public static final DD0 TYPE_0701 = new DD0(0x0701, "BIM M112");
		/** */
		public static final DD0 TYPE_0705 = new DD0(0x0705, "BIM M112");
		/** */
		public static final DD0 TYPE_07B0 = new DD0(0x07B0, "System B");
		/** */
		public static final DD0 TYPE_0810 = new DD0(0x0810, "IR-Decoder");
		/** */
		public static final DD0 TYPE_0811 = new DD0(0x0811, "IR-Decoder");
		/** */
		public static final DD0 TYPE_0910 = new DD0(0x0910, "Coupler 1.0");
		/** */
		public static final DD0 TYPE_0911 = new DD0(0x0911, "Coupler 1.1");
		/** */
		public static final DD0 TYPE_0912 = new DD0(0x0912, "Coupler 1.2");
		/** */
		public static final DD0 TYPE_091A = new DD0(0x091A, "KNXnet/IP Router");
		/** */
		public static final DD0 TYPE_0AFD = new DD0(0x0AFD, "none");
		/** */
		public static final DD0 TYPE_0AFE = new DD0(0x0AFE, "none");
		/** */
		public static final DD0 TYPE_1012 = new DD0(0x1012, "BCU 1");
		/** */
		public static final DD0 TYPE_1013 = new DD0(0x1013, "BCU 1");
		/** */
		public static final DD0 TYPE_17B0 = new DD0(0x17B0, "System B");
		/** */
		public static final DD0 TYPE_1900 = new DD0(0x1900, "Media Coupler PL-TP");
		/** */
		public static final DD0 TYPE_2010 = new DD0(0x2010, "Bidirectional devices");
		/** */
		public static final DD0 TYPE_2110 = new DD0(0x2110, "Unidirectional devices");
		/** */
		public static final DD0 TYPE_2311 = new DD0(0x2322, "RF USB interface v2");
		/** */
		public static final DD0 TYPE_3012 = new DD0(0x3012, "BCU 1");
		/** */
		public static final DD0 TYPE_4012 = new DD0(0x4012, "BCU 1");
		/** */
		public static final DD0 TYPE_5705 = new DD0(0x5705, "System 7");

		private static final DD0[] types = new DD0[] { TYPE_0010, TYPE_0011, TYPE_0012, TYPE_0013,
			TYPE_0020, TYPE_0021, TYPE_0025, TYPE_0300, TYPE_0310, TYPE_0700, TYPE_0701, TYPE_0705,
			TYPE_07B0, TYPE_0810, TYPE_0811, TYPE_0910, TYPE_0911, TYPE_0912, TYPE_091A, TYPE_0AFD,
			TYPE_0AFE, TYPE_1012, TYPE_1013, TYPE_17B0, TYPE_1900, TYPE_2010, TYPE_2110, TYPE_2311,
			TYPE_3012, TYPE_4012, TYPE_5705, };

		static {
			// ensure our DD0 array is up-to-date with the declared mask versions
			int i = 0;
			for (final Field f : DD0.class.getDeclaredFields()) {
				final int mod = f.getModifiers();
				if (Modifier.isStatic(mod) && f.getName().startsWith("TYPE_"))
					++i;
			}
			if (i != types.length)
				throw new KNXIllegalStateException("mask versions missing");
		}

		private final int mv;
		private final String profile;

		/**
		 * Returns the device descriptor type 0 from a byte array containing a mask version.
		 *
		 * @param data the type 0 descriptor data (mask version), <code>data.length == 2</code>
		 * @return the corresponding {@link DD0} object
		 */
		public static DD0 fromType0(final byte[] data)
		{
			if (data.length != 2)
				throw new KNXIllegalArgumentException("unspecified device descriptor type 0 using "
						+ "length " + data.length + ": " + DataUnitBuilder.toHex(data, ""));
			final int i = (data[0] & 0xff) << 8 | data[1] & 0xff;
			return fromType0(i);
		}

		/**
		 * Returns the device descriptor type 0 from an integer containing a mask version.
		 *
		 * @param descriptor the type 0 descriptor (mask version), contained in the lower 16 bits
		 * @return the corresponding {@link DD0} object
		 */
		public static DD0 fromType0(final int descriptor)
		{
			for (final DD0 v : types) {
				if (v.getMaskVersion() == descriptor)
					return v;
			}
			throw new KNXIllegalArgumentException("unknown mask version "
					+ getMaskVersionString(descriptor));
		}

		/**
		 * @param descriptor the type 0 descriptor (mask version), contained in the lower 16 bits
		 * @return a zero-padded string of length 4 holding the hexadecimal representation of the
		 *         mask version (the used format is "%04X")
		 */
		// ??? maybe public
		static String getMaskVersionString(final int descriptor)
		{
			return String.format("%04X", descriptor);
		}

		private DD0(final int mask, final String profile)
		{
			this.mv = mask;
			this.profile = profile;
		}

		/**
		 * @return the mask version as 16 bit value
		 */
		public int getMaskVersion()
		{
			return mv;
		}

		/**
		 * @return the medium type as 4 bit value
		 */
		public int getMediumType()
		{
			return (mv >> 12) & 0x0f;
		}

		/**
		 * @return the KNX medium as {@link KNXMediumSettings}
		 */
		public KNXMediumSettings getMedium()
		{
			final int type = getMediumType();
			switch (type) {
			case 0:
				return TPSettings.TP1;
			case 1:
				return new PLSettings();
			case 2:
				return new RFSettings(new IndividualAddress(0));
			case 3:
				throw new KNXIllegalArgumentException("TP0 medium not supported any longer");
			case 4:
				throw new KNXIllegalArgumentException("PL132 medium not supported any longer");
			case 5:
				return new KnxIPSettings(new IndividualAddress(0));
			default:
				throw new KNXIllegalArgumentException("unknown KNX medium type " + type);
			}
		}

		/**
		 * @return the firmware type
		 */
		public int getFirmwareType()
		{
			return (mv >> 8) & 0x0f;
		}

		/**
		 * @return the firmware version
		 */
		public int getFirmwareVersion()
		{
			return (mv >> 4) & 0x0f;
		}

		/**
		 * @return the firmware version subcode part
		 */
		public int getSubcode()
		{
			return (mv >> 0) & 0x0f;
		}

		/**
		 * @return the device profile name as human readable string
		 */
		public String getDeviceProfile()
		{
			return profile;
		}

		@Override
		public String toString()
		{
			return getMaskVersionString(mv) + " - " + getDeviceProfile();
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
		public static DD2 fromType2(final byte[] data)
		{
			return new DD2(data);
		}

		private DD2(final byte[] descriptor)
		{
			if (descriptor.length != TYPE2_SIZE)
				throw new KNXIllegalArgumentException("unspecified device descriptor type 2 using "
						+ "length " + descriptor.length);
			d = descriptor.clone();
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
		public int getApplicationManufacturer()
		{
			return get16Bits(0);
		}

		/**
		 * @return the 16 bit manufacturer specific device type of this descriptor
		 */
		public int getDeviceType()
		{
			return get16Bits(2);
		}

		/**
		 * @return the 8 bit version of the manufacturer specific device type of this descriptor
		 */
		public int getVersion()
		{
			return d[4] & 0xff;
		}

		/**
		 * @return <code>true</code> if network management procedures using A_Link_Read/Write are
		 *         supported, <code>false</code> otherwise
		 * @throws KNXFormatException
		 */
		public boolean isLinkManagementSupported() throws KNXFormatException
		{
			final int i = (d[5] & 0xc0) >> 6;
			if (i > 1)
				throw new KNXFormatException("undefined misc field value " + i);
			return i != 0;
		}

		/**
		 * Returns the current value of the 6 bit Logical Tag Base. If no local selector is active,
		 * returns 0x3f. For 'general, all zones' returns 0x3e. For multi-channel devices, the value
		 * is aligned to the closest lowest authorized value.
		 *
		 * @return the current Logical Tag Base as 6 bit value
		 */
		public int getLogicalTagBase()
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
		public int getChannels(final Channel channelType)
		{
			final int offset = channelType.ordinal() * 2;
			return (get16Bits(6 + offset) >> 13) + 1;
		}

		/**
		 * @param channelType the requested channel type
		 * @return the 13 bit channel code of the specified channel type
		 */
		public int getChannelCode(final Channel channelType)
		{
			final int offset = channelType.ordinal() * 2;
			return get16Bits(6 + offset) & 0x1fff;
		}

		// ??? maybe add, maybe not
//		/**
//		 * The number of channels implemented in the device for channel 1.
//		 *
//		 * @return number of channels, <code>1 &le; channels &le; 8</code>
//		 */
//		public int getChannel1Channels()
//		{
//			return (get16Bits(6) >> 13) + 1;
//		}
//
//		/**
//		 * @return the 13 bit channel code of channel 1
//		 */
//		public int getChannel1Code()
//		{
//			return get16Bits(6) & 0x1fff;
//		}
//
//		public int getChannelInfo2()
//		{
//			return get16Bits(8);
//		}
//
//		/**
//		 * The number of channels implemented in the device for channel 2.
//		 *
//		 * @return number of channels, <code>1 &le; channels &le; 8</code>
//		 */
//		public int getChannel2Channels()
//		{
//			return (get16Bits(8) >> 13) + 1;
//		}
//
//		/**
//		 * @return the 13 bit channel code of channel 2
//		 */
//		public int getChannel2Code()
//		{
//			return get16Bits(8) & 0x1fff;
//		}
//
//		public int getChannelInfo3()
//		{
//			return get16Bits(10);
//		}
//
//		/**
//		 * The number of channels implemented in the device for channel 3.
//		 *
//		 * @return number of channels, <code>1 &le; channels &le; 8</code>
//		 */
//		public int getChannel3Channels()
//		{
//			return (get16Bits(10) >> 13) + 1;
//		}
//
//		/**
//		 * @return the 13 bit channel code of channel 3
//		 */
//		public int getChannel3Code()
//		{
//			return get16Bits(10) & 0x1fff;
//		}
//
//		public int getChannelInfo4()
//		{
//			return get16Bits(12);
//		}
//
//		/**
//		 * The number of channels implemented in the device for channel 4.
//		 *
//		 * @return number of channels, <code>1 &le; channels &le; 8</code>
//		 */
//		public int getChannel4Channels()
//		{
//			return (get16Bits(12) >> 13) + 1;
//		}
//
//		/**
//		 * @return the 13 bit channel code of channel 4
//		 */
//		public int getChannel4Code()
//		{
//			return get16Bits(12) & 0x1fff;
//		}

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

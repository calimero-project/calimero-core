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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Represents a device description information block.
 * <p>
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse
 */
public class DeviceDIB extends DIB
{
	/**
	 * KNX medium code for twisted pair 0 (2400 bit/s), inherited from BatiBUS.
	 * <p>
	 */
	public static final int MEDIUM_TP0 = 0x01;

	/**
	 * KNX medium code for twisted pair 1 (9600 bit/s).
	 * <p>
	 */
	public static final int MEDIUM_TP1 = 0x02;

	/**
	 * KNX medium code for power line 110 kHz (1200 bit/s).
	 * <p>
	 */
	public static final int MEDIUM_PL110 = 0x04;

	/**
	 * KNX medium code for power line 132 kHz (2400 bit/s), inherited from EHS.
	 * <p>
	 */
	public static final int MEDIUM_PL132 = 0x08;

	/**
	 * KNX medium code for radio frequency (868 MHz).
	 * <p>
	 */
	public static final int MEDIUM_RF = 0x10;

	
	private static final int DIB_SIZE = 54;

	private final int status;
	private final int knxmedium;
	private final byte[] serial = new byte[6];
	private final int installationId;
	private final IndividualAddress address;
	private final byte[] mcast = new byte[4];
	private final byte[] mac = new byte[6];
	private final String name;

	/**
	 * Creates a device DIB out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing device DIB structure
	 * @param offset start offset of DIB in <code>data</code>
	 * @throws KNXFormatException if no DIB found or invalid structure
	 */
	public DeviceDIB(final byte[] data, final int offset) throws KNXFormatException
	{
		super(data, offset);
		if (type != DEVICE_INFO)
			throw new KNXFormatException("DIB is not of type device info", type);
		if (size < DIB_SIZE)
			throw new KNXFormatException("device info DIB too short", size);
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset + 2, data.length
				- offset - 2);
		knxmedium = is.read();
		status = is.read();
		address = new IndividualAddress(
			new byte[] { (byte) is.read(), (byte) is.read(), });
		installationId = (is.read() << 8) | is.read();
		is.read(serial, 0, serial.length);
		is.read(mcast, 0, mcast.length);
		is.read(mac, 0, mac.length);

		// device friendly name is optional
		final StringBuffer sbuf = new StringBuffer(30);
		int i = 30;
		for (int c = is.read(); i > 0 && c > 0; --i, c = is.read())
			sbuf.append((char) c);
		name = sbuf.toString();
	}

	/**
	 * Creates a device information DIB using the supplied device information.
	 * <p>
	 * 
	 * @param friendlyName user friendly name to identify the device; a ISO 8859-1 string
	 *        with a maximum length of 29 characters
	 * @param deviceStatus current device status, <code>0 <= deviceStatus <= 0xff</code>
	 *        <ul>
	 *        <li>bit 0 is the programming mode:<br>
	 *        1 = device is in programming mode<br>
	 *        0 = device is not in programming mode</li>
	 *        <li>all other bits are reserved</li>
	 *        </ul>
	 * @param projectInstallationId project-installation identifier of this device;
	 *        uniquely identifies a device in a project with more than one installation.
	 *        The lower 4 bits specify the installation number, bits 4 to 15 (MSB) contain
	 *        the project number.
	 * @param knxMedium KNX medium, one of the predefined KNX medium code constants of
	 *        this class
	 * @param knxAddress KNX individual address
	 * @param serialNumber KNX serial number of the device, used to identify the device,
	 *        <code>serialNumber.length == 6</code>
	 * @param routingMulticast KNXnet/IP routing multicast address for a routing device,
	 *        <code>null</code> 0.0.0.0 if the device does not support routing
	 * @param macAddress device Ethernet MAC address, <code>macAddress.length == 6</code>
	 */
	public DeviceDIB(final String friendlyName, final int deviceStatus,
		final int projectInstallationId, final int knxMedium, final IndividualAddress knxAddress,
		final byte[] serialNumber, final InetAddress routingMulticast, final byte[] macAddress)
	{
		super(DIB_SIZE, DEVICE_INFO);

		// be sure and check for characters in string
		try {
			name = new String(friendlyName.getBytes(), "ISO-8859-1");
		}
		catch (final UnsupportedEncodingException e) {
			// ISO 8859-1 support is mandatory on every Java platform
			throw new Error("missing ISO 8859-1 charset, " + e.getMessage());
		}
		if (name.length() > 29)
			throw new KNXIllegalArgumentException("friendly name exceeds 29 ISO 8859-1 characters");

		if (deviceStatus < 0 || deviceStatus > 0xff)
			throw new KNXIllegalArgumentException("device status out of range [0..255]");
		status = deviceStatus;
		if (status > 1)
			CRBase.logger.warn("device DIB \"" + friendlyName + "\": device status (" + status
					+ ") uses reserved bits");

		if (knxMedium != MEDIUM_TP0 && knxMedium != MEDIUM_TP1 && knxMedium != MEDIUM_PL110
				&& knxMedium != MEDIUM_PL132 && knxMedium != MEDIUM_RF)
			throw new KNXIllegalArgumentException("KNX medium not supported");
		knxmedium = knxMedium;

		if (serialNumber.length != serial.length)
			throw new KNXIllegalArgumentException("serial number length not equal to "
					+ serial.length + " bytes");
		for (int i = 0; i < serial.length; i++)
			serial[i] = serialNumber[i];

		if (projectInstallationId < 0 || projectInstallationId > 0xffff)
			throw new KNXIllegalArgumentException("project installation ID out of range [0..0xffff");
		installationId = projectInstallationId;

		address = knxAddress;

		final byte[] empty = new byte[] { 0, 0, 0, 0 };
		final byte[] rmc = routingMulticast == null ? empty : routingMulticast.getAddress();
		if (!Arrays.equals(rmc, empty) && !routingMulticast.isMulticastAddress())
			throw new KNXIllegalArgumentException(routingMulticast.toString()
					+ " is not a multicast address");
		for (int i = 0; i < mcast.length; i++)
			mcast[i] = rmc[i];

		if (macAddress.length != mac.length)
			throw new KNXIllegalArgumentException("MAC address length not equal to " + mac.length
					+ " bytes");
		for (int i = 0; i < mac.length; i++)
			mac[i] = macAddress[i];
	}

	/**
	 * Returns the device individual address.
	 * <p>
	 * 
	 * @return individual address as {@link IndividualAddress}
	 */
	public final IndividualAddress getAddress()
	{
		return address;
	}

	/**
	 * Returns the device status byte.
	 * <p>
	 * Bit 0 is programming mode flag: 1 = device is in programming mode, 0 = device is
	 * not in programming mode.
	 * 
	 * @return status as unsigned byte
	 */
	public final int getDeviceStatus()
	{
		return status;
	}

	/**
	 * Returns the KNX medium code.
	 * <p>
	 * 
	 * @return KNX medium as unsigned byte
	 */
	public final int getKNXMedium()
	{
		return knxmedium;
	}

	/**
	 * Returns a textual representation of the KNX medium code.
	 * <p>
	 * 
	 * @return KNX medium as string format
	 * @see #getKNXMedium()
	 */
	public String getKNXMediumString()
	{
		switch (knxmedium) {
		case MEDIUM_TP0:
			return "TP0";
		case MEDIUM_TP1:
			return "TP1";
		case MEDIUM_PL110:
			return "PL110";
		case MEDIUM_PL132:
			return "PL132";
		case MEDIUM_RF:
			return "RF";
		default:
			return "unknown";
		}
	}

	/**
	 * Returns the device Ethernet MAC address.
	 * <p>
	 * 
	 * @return byte array containing MAC address
	 */
	public final byte[] getMACAddress()
	{
		return (byte[]) mac.clone();
	}

	/**
	 * Returns a textual representation of the device Ethernet MAC address.
	 * <p>
	 * 
	 * @return MAC address as string format
	 */
	public final String getMACAddressString()
	{
		return DataUnitBuilder.toHex(mac, "-");
	}

	/**
	 * Returns the device routing multicast address.
	 * <p>
	 * For devices which don't implement routing, the multicast address is 0.
	 * 
	 * @return multicast address as byte array
	 */
	public final byte[] getMulticastAddress()
	{
		return (byte[]) mcast.clone();
	}

	/**
	 * Returns the project-installation identifier of this device.
	 * <p>
	 * This ID uniquely identifies a device in a project with more than one installation.
	 * The lowest 4 bits specify the installation number, bit 4 to 15 (MSB) contain the
	 * project number.
	 * 
	 * @return project installation identifier as unsigned 16 bit value
	 */
	public final int getProjectInstallID()
	{
		return installationId;
	}

	/**
	 * Returns the project number for the device.
	 * <p>
	 * The project number is the upper 12 bits of the project-installation identifier.
	 * 
	 * @return project number as 12 bit unsigned value
	 * @see #getProjectInstallID()
	 */
	public final int getProject()
	{
		return installationId >> 4;
	}

	/**
	 * Returns the installation number for the device.
	 * <p>
	 * The installation number is the lower 4 bits of the project-installation identifier.
	 * 
	 * @return installation number as 4 bit unsigned value
	 * @see #getProjectInstallID()
	 */
	public final int getInstallation()
	{
		return installationId & 0x0F;
	}

	/**
	 * Returns the KNX serial number of the device.
	 * <p>
	 * The serial number uniquely identifies a device.
	 * 
	 * @return byte array with serial number
	 */
	public final byte[] getSerialNumber()
	{
		return (byte[]) serial.clone();
	}

	/**
	 * Returns a textual representation of the device KNX serial number.
	 * <p>
	 * 
	 * @return serial number as string
	 */
	public final String getSerialNumberString()
	{
		return DataUnitBuilder.toHex(serial, null);
	}

	/**
	 * Returns the device friendly name.
	 * <p>
	 * This name is used to display a device in textual format. The maximum name length is
	 * 30 characters.
	 * 
	 * @return device name as string
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Returns a textual representation of this device DIB.
	 * <p>
	 * 
	 * @return a string representation of the object
	 */
	public String toString()
	{
		InetAddress mc = null;
		try {
			mc = InetAddress.getByAddress(getMulticastAddress());
		}
		catch (final UnknownHostException ignore) {}
		return "device " + address + " \"" + name + "\", KNX medium " + getKNXMediumString()
				+ ", installation " + getInstallation() + " project " + getProject()
				+ " (project installation ID " + installationId + ")"
				+ ", routing multicast address " + mc + ", MAC address " + getMACAddressString()
				+ ", S/N 0x" + getSerialNumberString();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.util.DIB#toByteArray()
	 */
	public byte[] toByteArray()
	{
		final byte[] buf = super.toByteArray();
		int i = 2;
		buf[i++] = (byte) knxmedium;
		buf[i++] = (byte) status;
		final byte[] addr = address.toByteArray();
		buf[i++] = addr[0];
		buf[i++] = addr[1];
		buf[i++] = (byte) (installationId >> 8);
		buf[i++] = (byte) installationId;
		for (int k = 0; k < 6; ++k)
			buf[i++] = serial[k];
		for (int k = 0; k < 4; ++k)
			buf[i++] = mcast[k];
		for (int k = 0; k < 6; ++k)
			buf[i++] = mac[k];
		for (int k = 0; k < name.length(); ++k)
			buf[i++] = (byte) name.charAt(k);
		return buf;
	}
}

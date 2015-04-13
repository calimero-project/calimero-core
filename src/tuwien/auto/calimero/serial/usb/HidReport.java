/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015 B. Malinowsky

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

package tuwien.auto.calimero.serial.usb;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.serial.usb.HidReportHeader.PacketType;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.BusAccessServerService;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.KnxTunnelEmi;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.Protocol;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.ServiceId;

/**
 * Represents a HID class report.
 *
 * @author B. Malinowsky
 */
public final class HidReport
{
	/*
	  KNX USB HID class report frame structure

		        -------------------------------------------------------------
		Field   | KNX HID Report | Transfer Protocol | EMI Msg Code/ | Data |
		        |     Header     |       Header      |  Feature ID   |      |
				------------------------------------------------------------
	 */

	//
	// Bus Access Server Feature protocol: Feature Identifier
	//
	enum BusAccessServerFeature {
		// Mandatory for Get/Response services, not allowed for other services
		// get the supported EMI types (data length 2 bytes)
		// set bit indicates support: bit 2 = cEMI, bit 1 = EMI2, bit 0 = EMI 1, other bits reserved
		SupportedEmiTypes,

		// Mandatory for Get/Response services, not allowed for other services
		// local device descriptor type 0 (data length 2 bytes)
		// querying descriptor type 0 is not allowed for bus access server with only cEMI interface
		DeviceDescriptorType0,

		// Mandatory for Get/Response/Info services, not allowed for other services
		// KNX bus connection status (data length 1 byte, LSB)
		// set bit indicates status (DPT_State): 1 = active, 0 = inactive
		ConnectionStatus,

		// Mandatory for Get/Response services, not allowed for other services
		// KNX manufacturer code (data length 2 bytes)
		Manufacturer,

		// Mandatory for Get/Response/Set services, not allowed for other services
		// get/set the currently used EMI type (data length 1 byte)
		ActiveEmiType;

		int id()
		{
			return ordinal() + 1;
		}
	}

	private static final int maxReportSize = 64;
	// max allowed data bytes in a transfer protocol body
	private static final int maxDataStartPacket = 52;
	private static final int maxDataPartialPacket = 61;

	// magic value if EMI message code field is N/A (field is only used in start packets)
	private static final int noEmiMsgCode = -1;

	private final HidReportHeader rh;
	private final TransferProtocolHeader tph;

	// device service feature ID or EMI msg code for cEMI/EMI1/EMI2 data frames
	private final int featureId;
	private final byte[] data;

	/**
	 * Creates a new report for use with the Bus Access Server feature service.
	 *
	 * @param service
	 * @param feature
	 * @param frame
	 * @return the created HID report
	 */
	public static HidReport createFeatureService(final BusAccessServerService service,
		final BusAccessServerFeature feature, final byte[] frame)
	{
		return new HidReport(service, feature, frame);
	}

	/**
	 * Creates a new report containing an EMI frame.
	 * @param emi
	 * @param emiMsgCode
	 * @param data
	 * @return the created HID report
	 */
	public static List<HidReport> create(final KnxTunnelEmi emi, final int emiMsgCode,
		final byte[] data)
	{
		final List<HidReport> l = new ArrayList<>();
		int offset = 0;
		int maxData = maxDataStartPacket;
		final EnumSet<PacketType> packetType = EnumSet.of(PacketType.Start);
		if (data.length > maxData)
			packetType.add(PacketType.Partial);
		int msgCode = emiMsgCode;
		for (int i = 1; i < 6; i++) {
			final byte[] range = Arrays.copyOfRange(data, offset, offset + maxData);
			offset += maxData;
			if (range.length <= maxData)
				packetType.add(PacketType.End);
			l.add(new HidReport(i, packetType, Protocol.KnxTunnel, emi, msgCode, range));
			if (range.length <= maxData)
				break;
			packetType.remove(PacketType.Start);
			msgCode = noEmiMsgCode;
			maxData = maxDataPartialPacket;
		}
		return l;
	}

	// XXX sequence number should always be used and > 0, but oldest spec uses 0
	// Anyway, distinction should not be necessary, always use new
	private static final boolean useNoSequence = false;

	private HidReport(final ServiceId serviceId, final BusAccessServerFeature feature,
		final byte[] frame)
	{
		this(useNoSequence ? 0 : 1, EnumSet.of(PacketType.Start, PacketType.End),
				TransferProtocolHeader.Protocol.BusAccessServerFeature, serviceId, feature.id(),
				frame);
	}

	public HidReport(final int sequence, final EnumSet<PacketType> packetType,
		final Protocol protocol, final ServiceId serviceId, final int emiMsgCode, final byte[] frame)
	{
		int packetLength = 1 + frame.length;
		// only start packets have a transfer protocol header and a feature ID / EMI msg code
		if (packetType.contains(PacketType.Start)) {
			if (frame.length > maxDataStartPacket)
				throw new KNXIllegalArgumentException("frame too large: " + frame.length + " > "
						+ maxDataStartPacket);
			tph = new TransferProtocolHeader(1 + frame.length, protocol, serviceId);
			packetLength += tph.getStructLength();
			featureId = emiMsgCode;
			// validate feature ID range if we represent a device feature service
			if (protocol == Protocol.BusAccessServerFeature)
				if (featureId < 1 || featureId > 5)
					throw new KNXIllegalArgumentException("unsupported device service feature ID "
							+ featureId);
		}
		else {
			tph = null;
			featureId = noEmiMsgCode;
			if (frame.length > maxDataPartialPacket)
				throw new KNXIllegalArgumentException("frame too large: " + frame.length + " > "
						+ maxDataPartialPacket);
		}
		rh = new HidReportHeader(sequence, packetType, packetLength);
		data = frame.clone();
	}

	public HidReport(final byte[] frame) throws KNXFormatException
	{
		if (frame.length > maxReportSize)
			throw new KNXFormatException("unsupported KNX USB frame of length " + frame.length
					+ " > " + maxReportSize);
		rh = new HidReportHeader(frame, 0);
		int offset = rh.getStructLength();
		if (rh.getPacketType().contains(PacketType.Start)) {
			tph = new TransferProtocolHeader(frame, offset);
			offset += tph.getStructLength();
			featureId = frame[offset] & 0xff;
			++offset;
			if (tph.getProtocol() == Protocol.BusAccessServerFeature)
				if (featureId < 1 || featureId > 5)
					throw new KNXFormatException("unsupported device service feature ID "
							+ featureId);
		}
		else {
			tph = null;
			featureId = noEmiMsgCode;
		}

		final int datalength = rh.getDataLength() + rh.getStructLength();
		data = Arrays.copyOfRange(frame, offset, datalength);
		// useful check?
		if (datalength > frame.length)
			throw new KNXFormatException("HID class report data length " + datalength
					+ " exceeds frame length " + frame.length);
		// if body contains complete EMI frame (i.e., no partial packets), validate body length
		final EnumSet<PacketType> type = EnumSet.of(PacketType.Start, PacketType.End);
		if (rh.getPacketType().equals(type)) {
			final int bodylength = tph.getBodyLength();
			if (bodylength != 1 + data.length)
				throw new KNXFormatException("unexpected KNX USB protocol body length "
						+ data.length + ", expected " + bodylength);
		}
	}

	public HidReportHeader getReportHeader()
	{
		return rh;
	}

	public TransferProtocolHeader getTransferProtocolHeader()
	{
		return tph;
	}

	public byte[] getData()
	{
		return data;
	}

	byte[] toByteArray()
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream(maxReportSize);
		rh.toByteArray(os);
		if (tph != null) {
			tph.toByteArray(os);
			os.write(featureId);
		}
		os.write(data, 0, data.length);
		while (os.size() < maxReportSize)
			os.write(0);
		return os.toByteArray();
	}
}

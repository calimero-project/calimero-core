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

package tuwien.auto.calimero.link.medium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.cemi.RFMediumInfo.RSS;


/**
 * L-Data frame format on KNX RF communication medium.
 *
 * @author B. Malinowsky
 */
public class RFLData implements RawFrame
{
	// TODO RawFrameBase is right now not the best fit as base type, because
	// RF frames do not use the priority field

	// min length of having 1. and 2. block = [10 + CRC] [16 + CRC]
	private static final int MinLength = 30;
	// magic value for future extensions
	private static final int ReservedLength = 0xff;
	// the KNX RF frame type
	private static final int Send_NoReply = 0x44;

	private static final int Escape = 0xff;

	private static final int TpduOffset = 15;
	private static final int Block2TpduSize = 10;

	/** RF frame TPCI. */
	public enum Tpci {
		UnnumberedData,
		NumberedData,
		UnnumberedCtrl,
		NumberedCtrl,
	}

	// TODO extend with other frame types, or remove
	// Indicates the frame type _group_ (not the actual frame type)
	enum FrameType {
		AsyncLData,
		RfMultiAsyncLData,
		RfMultiAsyncLDataAckReq,
		FastAck,
	}

	// length of frame starting from the C field, excluding CRCs
	private final int length;

	private final RSS rss;
	private final boolean batteryOk;
	private final boolean unidir;

	private final int ctrl;
//	private FrameType ft;

	/*
	   RF domain address is used for
	     - point to point (unicast), CL/CO
	     - point to all points (domain broadcast), CL
	   KNX device serial number
	     - point to multi point (multicast), CL
	     - point to system (system broadcast), CL
	*/

	private final byte[] doa;
	private final IndividualAddress src;
	private final KNXAddress dst;

	// counter shall be 6 for RF Ready and BiBat end devices
	// counter shall be 2 for RF multi end devices
	private final int maxRep;
	private final int lfn;
	private final boolean isDoA;
	private final byte[] tpdu;

	// transmit-only devices
	//   - shall all have IndAddr 0x05ff
	//   - shall use extended group addresses
	//   - datapoints shall be numbered as DP1 = GroupAddr 0x0001, DP2 = 0x0002, ...
	static RFLData newForTransmitOnlyDevice(final boolean batteryOk, final int frameType,
		final int frameNumber, final byte[] serial, final GroupAddress dst, final byte[] tpdu)
	{
		return new RFLData(batteryOk, true, frameType, frameNumber, serial, new IndividualAddress(
				0x05ff), dst, tpdu);
	}

	public RFLData(final boolean batteryOk, final boolean transmitOnlyDevice, final int frameType,
		final int frameNumber, final byte[] doa, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu)
	{
		length = TpduOffset + tpdu.length;
		rss = RSS.Void;
		this.batteryOk = batteryOk;
		unidir = transmitOnlyDevice;
		this.doa = doa.clone();

		ctrl = frameType;
		this.src = src;
		this.dst = dst;
		maxRep = 6;
		lfn = frameNumber;

		final boolean grpbcast = dst instanceof GroupAddress && dst.getRawAddress() == 0;
		this.isDoA = grpbcast || dst instanceof IndividualAddress;

		this.tpdu = tpdu.clone();
	}

	public RFLData(final byte[] frame, final int offset) throws KNXFormatException
	{
		final ByteArrayInputStream is = new ByteArrayInputStream(frame, offset, frame.length
				- offset);
		if (is.available() < MinLength)
			throw new KNXFormatException("minimum data length < " + MinLength, is.available());

		//
		// 1st block, 10 octets
		//

		// frame _data_ length
		length = is.read();
		if (length == ReservedLength)
			throw new KNXFormatException("unsupported RF frame length", ReservedLength);

		final int c = is.read();
		if (c != Send_NoReply)
			throw new KNXFormatException("no KNX RF L-Data frame");

		final int esc = is.read();
		if (esc != Escape)
			throw new KNXFormatException("invalid Escape field", esc);

		// RF info
		final int info = is.read();
		final int hi = info & 0xf0;
		if (hi != 0)
			throw new KNXFormatException("invalid RF info field", info);
		final int rssvalue = (info >>> 2) & 0x03;
		rss = RSS.values()[rssvalue];
		batteryOk = (info & 0x02) == 0x02;
		unidir = (info & 0x01) == 0x01;

		doa = new byte[6];
		is.read(doa, 0, doa.length);

		// 1st block CRC
		final int crc1 = (is.read() << 8) | is.read();
		verifyCrc(crc1, frame, offset, 10);

		//
		// 2nd block, 16 octets
		//

		// KNX control field, contains the frame type
		ctrl = is.read();
		if (ctrl == Escape)
			throw new KNXFormatException("unsupported KNX control field (Escape)");
//		final int format = (ctrl >>> 4) & 0xf;
		// check sync/async frame
//		final boolean syncFrame = (ctrl >>> 6) == 1;
		// RF Multi frame
//		final boolean rfmulti = (ctrl >>> 7) == 1;
		final int extFormat = ctrl & 0xf;
		// check standard frame and LTE extended frame
		final boolean std = extFormat == 0;
		final boolean lteExt = (extFormat & 0x0c) == 0x04;
		if (!std && !lteExt)
			throw new KNXFormatException("unsupported frame format", extFormat);

		// Check HVAC Easy Extension bits
		if (lteExt) {
			// LTE-HEE bits 1 and 0 contain the extension of the group address
			final int ext = extFormat >> 2;
			System.out.println("LTE-HEE address ext = " + ext);
		}

		// KNX source address
		final byte[] addr = new byte[2];
		is.read(addr, 0, 2);
		src = new IndividualAddress(addr);

		// KNX destination address
		// read dst field, Ind/Group address is created below
		is.read(addr, 0, 2);

		// LPCI
		final int lpci = is.read();
		// is dst a group address for std frames
		final boolean group = (lpci & 0x80) == 0x80;
		// max allowed frame repetitions
		maxRep = (lpci >>> 4) & 0x07;
		lfn = (lpci >>> 1) & 0x07;
		isDoA = (lpci & 0x01) == 0x01;

		dst = group ? new GroupAddress(addr) : new IndividualAddress(addr);

		// allocate array for complete TPDU
		final int tpduSize = length - TpduOffset;
		if (tpduSize < 0)
			throw new KNXFormatException("invalid RF L-Data length, TPDU size < 0", length);
		tpdu = new byte[tpduSize];
		// read TPDU contained in 2nd block
		is.read(tpdu, 0, Math.min(Block2TpduSize, tpduSize));
		// make sure we fast forward to the end of 2nd data block for short TPDUs
		for (int i = tpduSize; i < Block2TpduSize; i++)
			if (is.read() != 0)
				throw new KNXFormatException("2nd data block after TPDU not zeroed out");

		// TODO test only: check TPCI/APCI and seq contained in TPDU
		final int pci = tpdu[0] & 0xff;
		final int tpci = (pci >>> 6);
		// LTE has tpci always set 0
		if (lteExt && tpci != Tpci.UnnumberedData.ordinal())
			throw new KNXFormatException("RF LTE extended frame requires TPCI "
					+ Tpci.UnnumberedData);
		final int seq = (pci >>> 2) & 0x0f;
		final int apci = DataUnitBuilder.getAPDUService(tpdu);

		// TODO test only: move LTE-specific ASDU stuff out of L-Data parsing
		// LTE frames contain the interface object type, IO instance, and PID
		if (lteExt) {
			final int iot = ((tpdu[2] & 0xff) << 8) | (tpdu[3] & 0xff);
			final int ioi = tpdu[4] & 0xff;
			final int pid = tpdu[5] & 0xff;
			System.out.println("LTE IOT=" + iot + " IO instance=" + ioi + " PID=" + pid);
		}

		// 2nd block CRC
		final int crc2 = (is.read() << 8) | is.read();
		verifyCrc(crc2, frame, offset + 12, 16);

		//
		// 3rd block ...
		//

		// read all the remaining blocks of [16 bytes data, 2 bytes CRC]
		// 1st block counts for 9 bytes (12 - 1 - CRC), 2nd block for 14 (16 - CRC)
		int block = 3;
		for (int got = Block2TpduSize; got < tpduSize; got += 16) {
			// last block may contain less than 16 bytes
			final int read = Math.min(16, tpduSize - got);
			final byte[] part = new byte[read];
			final int res = is.read(part, 0, part.length);
			if (res != read)
				throw new KNXFormatException("truncated RF frame in block " + block + ": length "
						+ got + " < expected total length " + length + " bytes");
			System.arraycopy(part, 0, tpdu, got, read);

			final int crcn = (is.read() << 8) | is.read();
			verifyCrc(crcn, frame, offset + (block - 1) * 18 - 6, read);
			block++;
		}
	}

	/**
	 * Returns the KNX individual source address.
	 *
	 * @return address of type IndividualAddress
	 */
	public final IndividualAddress getSource()
	{
		return src;
	}

	/**
	 * Returns the KNX destination address.
	 *
	 * @return destination address of type KNXAddress
	 */
	public final KNXAddress getDestination()
	{
		return dst;
	}

	@Override
	public final int getFrameType()
	{
		return ctrl;
	}

	public final RSS getRss()
	{
		return rss;
	}

	public final boolean isBatteryOk()
	{
		return batteryOk;
	}

	public final boolean isTransmitOnlyDevice()
	{
		return unidir;
	}

	// not part of the actual RF medium information structure
	public boolean isSystemBroadcast()
	{
		return !isDoA;
	}

	// SN or DoA according to System Broadcast flag
	// ??? maybe make two methods with dedicated names
	public final byte[] getDoAorSN()
	{
		return doa.clone();
	}

	/**
	 * @return the data link layer frame number (LFN)
	 */
	public final int getFrameNumber()
	{
		return lfn;
	}

	/**
	 * Returns a copy of the TPDU.
	 *
	 * @return TPDU as byte array
	 */
	public final byte[] getTpdu()
	{
		return tpdu.clone();
	}

	public final byte[] toByteArray()
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream();

		// 1st block
		os.write(length);
		os.write(Send_NoReply);
		os.write(Escape);

		final int info = (rss.ordinal() << 2) | (batteryOk ? 0x02 : 0x00) | (unidir ? 0x01 : 0x00);
		os.write(info);

		os.write(doa, 0, doa.length);
		os.write(crc(os.toByteArray(), 0), 0, 2);

		// 2nd block
		os.write(ctrl);
		os.write(src.toByteArray(), 0, 2);
		os.write(dst.toByteArray(), 0, 2);

		int lpci = dst instanceof GroupAddress ? 0x80 : 0x00;
		lpci |= (maxRep << 4) | (lfn << 1) | (isDoA ? 0x01 : 0x00);
		os.write(lpci);

		// fill remainder of 2nd block with TPDU
		final int min = Math.min(Block2TpduSize, tpdu.length);
		os.write(tpdu, 0, min);
		for (int i = min; i < Block2TpduSize; i++)
			os.write(0);
		final byte[] crc2 = crc(os.toByteArray(), 12);
		os.write(crc2, 0, 2);

		// 3rd block ...
		for (int written = Block2TpduSize; written < tpdu.length; written += 16) {
			final int write = Math.min(16, tpdu.length - written);
			os.write(tpdu, written, write);
			final byte[] crcn = crc(tpdu, written, write);
			os.write(crcn, 0, 2);

		}
		return os.toByteArray();
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();

		final boolean lteExt = (ctrl & 0x0c) == 0x04;
		if (lteExt)
			sb.append("LTE ");
		sb.append(getFrameType(ctrl >>> 4));
		sb.append(" ").append(src).append("->").append(dst);

		sb.append(isSystemBroadcast() ? " SN " : " DoA ").append(
				DataUnitBuilder.toHex(getDoAorSN(), ""));
		sb.append(", RSS=").append(getRss());
		sb.append(" Battery ").append(isBatteryOk() ? "OK" : "weak");
		sb.append(", LFN ").append(getFrameNumber());

		sb.append(": ").append(DataUnitBuilder.toHex(tpdu, ""));
		return sb.toString();
	}

	private static String getFrameType(final int format)
	{
		switch (format) {
		case 0:
			return "L-Data (async)";
		case 1:
			return "Fast ACK";
		case 4:
			return "L-Data (sync)";
		case 5:
			return "BiBat Sync";
		case 6:
			return "BiBat Help Call";
		case 7:
			return "BiBat Help Call Res";
		case 8:
			return "RF Multi L-Data (async)";
		case 9:
			return "RF Multi L-Data (async, ACK.req)";
		case 10:
			return "RF Multi Repeater ACK";
		default:
			return "Reserved";
		}
	}

	private static void verifyCrc(final int crc, final byte[] data, final int offset,
		final int length) throws KNXFormatException
	{
		final int calc = crc16(data, offset, length);
		if (calc != crc)
			throw new KNXFormatException("CRC mismatch, expected 0x" + Integer.toHexString(crc)
					+ " vs calculated 0x" + Integer.toHexString(calc));
	}

	private static byte[] crc(final byte[] data, final int offset)
	{
		return crc(data, offset, data.length - offset);
	}

	private static byte[] crc(final byte[] data, final int offset, final int length)
	{
		final int crc = crc16(data, offset, length);
		return new byte[] { (byte) (crc >> 8), (byte) crc };
	}

	static int crc16(final byte[] data, final int offset, final int length)
	{
		// CRC-16-DNP
		// generator polynomial = 2^16 + 2^13 + 2^12 + 2^11 + 2^10 + 2^8 + 2^6 + 2^5 + 2^2 + 2^0
//		final int p = Integer.parseUnsignedInt("10011110101100101", 2);
//		System.out.println(Integer.toHexString(p));
		final int pn = 0x13d65; // 1 0011 1101 0110 0101

		// for much data, using a lookup table would be a way faster CRC calculation
		int crc = 0;
		for (int i = offset; i < offset + length; i++) {
			final int bite = data[i] & 0xff;
			for (int b = 8; b --> 0;) {
				final boolean bit = ((bite >> b) & 1) == 1;
				final boolean one = (crc >> 15 & 1) == 1;
				crc <<= 1;
				if (one ^ bit)
					crc ^= pn;
			}
		}
		return (~crc) & 0xffff;
	}
}

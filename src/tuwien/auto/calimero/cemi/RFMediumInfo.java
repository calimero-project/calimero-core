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

package tuwien.auto.calimero.cemi;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.cemi.CEMILDataEx.AddInfo;
import tuwien.auto.calimero.internal.JavaME;

/**
 * RF medium information, with additional data link layer information mandatory for communication
 * over RF medium.
 *
 * @author B. Malinowsky
 */
final class RFMediumInfo extends AddInfo
{
	// for .req: if dst is individual address, use RF domain address (AET is set to 1)
	// for .req: if dst is group address == 0x0000, use RF domain address (AET is set to 1)
	// for .req: if dst is group address != 0x0000, use KNX serial number (AET is set to 0)

	/**
	 * Defined values of Received Signal Strength (RSS).
	 */
	// RSS = 0x00: no measurement, 0x01: weak, 0x02: medium, 0x03: strong
	public enum RSS {
		Void("void (no measurement)"),
		Weak("weak"),
		Medium("medium"),
		Strong("strong");

		private final String v;

		RSS(final String value) { v = value; }

		@Override
		public String toString() { return v; }
	}

	private final boolean sysBcast;

	public RFMediumInfo(final byte[] info)
	{
		this(info, false);
	}

	public RFMediumInfo(final byte[] info, final boolean systemBroadcast)
	{
		super(CEMILDataEx.ADDINFO_RFMEDIUM, info);
		sysBcast = systemBroadcast;
	}

	// use for .req, .con
	public RFMediumInfo(final boolean batteryOk, final boolean transmitOnlyDevice)
	{
		this(0, 0, batteryOk, transmitOnlyDevice, new byte[6], 0);
	}

	// use for .req, .con
	public RFMediumInfo(final boolean batteryOk, final boolean transmitOnlyDevice,
		final byte[] doA, final int lfn)
	{
		this(0, 0, batteryOk, transmitOnlyDevice, doA, lfn);
	}

	// use for .ind
	public RFMediumInfo(final int rss, final int retransmitterRss, final boolean batteryOk,
		final boolean transmitOnlyDevice, final byte[] doA, final int lfn)
	{
		super(CEMILDataEx.ADDINFO_RFMEDIUM, toByteArray(rss, retransmitterRss, batteryOk,
				transmitOnlyDevice, doA, lfn));
		sysBcast = false;
	}

	// not part of the actual RF medium information structure
	public boolean isSystemBroadcast()
	{
		return sysBcast;
	}

	public RSS getRSS()
	{
		final int rfInfo = getInfo()[0] & 0xff;
		final int rss = (rfInfo >> 4) & 0x03;
		return RSS.values()[rss];
	}

	public RSS getRetransmitterRSS()
	{
		final int rfInfo = getInfo()[0] & 0xff;
		final int rss = (rfInfo >> 2) & 0x03;
		return RSS.values()[rss];
	}

	public boolean isBatteryOk()
	{
		final int rfInfo = getInfo()[0] & 0xff;
		final boolean ok = ((rfInfo >> 1) & 0x01) == 0x01;
		return ok;
	}

	public boolean isTransmitOnlyDevice()
	{
		final int rfInfo = getInfo()[0] & 0xff;
		final boolean uni = (rfInfo & 0x01) == 0x01;
		return uni;
	}

	// SN or DoA according to System Broadcast flag in cEMI control field used for open media
	// SB bit: 0 = system broadcast, 1 = broadcast
	// ??? maybe make two methods with dedicated names
	public byte[] getDoAorSN()
	{
		return DataUnitBuilder.copyOfRange(getInfo(), 1, 1 + 6);
	}

	// Link layer Frame Number (LFN)
	// valid [0..7]
	// L_Data.req : if 255 then cEMI server shall insert the value for LFN
	public int getFrameNumber()
	{
		final int lfn = getInfo()[7] & 0xff;
		return lfn;
	}

	private static byte[] toByteArray(final int rss, final int retransmitterRss,
		final boolean batteryOK, final boolean transmitOnlyDevice, final byte[] doA, final int lfn)
	{
		if (rss < 0 || rss > 3)
			throw new KNXIllegalArgumentException("RSS out of range [0..3]: " + rss);
		if (retransmitterRss < 0 || retransmitterRss > 3)
			throw new KNXIllegalArgumentException("retransmitter RSS out of range [0..3]: "
					+ retransmitterRss);

		final byte[] info = new byte[8];
		final int unidir = transmitOnlyDevice ? 0x01 : 0x00;
		info[0] = (byte) ((rss << 4) | (retransmitterRss << 2) | (batteryOK ? 0x02 : 0x0) | unidir);
		if (doA.length != 6)
			throw new KNXIllegalArgumentException("DoA/SN invalid length: 0x"
					+ DataUnitBuilder.toHex(doA, ""));
		for (int i = 0; i < doA.length; i++) {
			final byte b = doA[i];
			info[1 + i] = b;
		}
		if ((lfn < 0 || lfn > 7) && lfn != 255)
			throw new KNXIllegalArgumentException("LFN not in {0, ..., 7, 255}: " + lfn);
		info[7] = (byte) lfn;
		return info;
	}
}

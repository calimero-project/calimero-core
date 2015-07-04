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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.cemi.RFMediumInfo.RSS;

/**
 * @author B. Malinowsky
 */
public class RFLDataTest
{
	private final boolean batteryOk = true;
	private final boolean transmitOnlyDevice = false;
	private final int frameType = 0x94;
	private final int frameNumber = 4;
	private final byte[] doa = new byte[] { (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83,
		(byte) 0x84, (byte) 0x85 };
	private final IndividualAddress src = new IndividualAddress(1, 1, 22);
	private final KNXAddress dst = new GroupAddress(2, 1, 3);
	private final byte[] tpdu = new byte[] { 2, 3, 0, 12, 1, 56, 0, 0, 0, 16, 17, 18, 19, 20, 21,
		22 };

	private final byte[] serial = new byte[] { 1, 2, 3, 4, 5, 6 };

	private RFLData rf;

	private final byte[] frame = new byte[] { (byte) 0x1f, (byte) 0x44, (byte) 0xff, (byte) 0x02,
		(byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0xe4,
		(byte) 0xae, (byte) 0x94, (byte) 0x11, (byte) 0x16, (byte) 0x11, (byte) 0x03, (byte) 0xe8,
		(byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x0c, (byte) 0x01, (byte) 0x38, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0xfe, (byte) 0xfb, (byte) 0x11, (byte) 0x12,
		(byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0xf6, (byte) 0x83 };

	public RFLDataTest()
	{}

	@Before
	public void setUp() throws Exception
	{
		rf = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src, dst, tpdu);
	}

	@Test
	public final void testNewForTransmitOnlyDevice()
	{
		final RFLData f = RFLData.newForTransmitOnlyDevice(batteryOk, frameType, frameNumber,
				serial, new GroupAddress(1), tpdu);
		Assert.assertEquals(batteryOk, f.isBatteryOk());
		Assert.assertEquals(true, f.isTransmitOnlyDevice());
		Assert.assertEquals(frameType, f.getFrameType());
		Assert.assertEquals(frameNumber, f.getFrameNumber());
		Assert.assertArrayEquals(serial, f.getDoAorSN());
		Assert.assertEquals(new IndividualAddress(0, 5, 0xff), f.getSource());
		Assert.assertEquals(new GroupAddress(1), f.getDestination());
		Assert.assertArrayEquals(tpdu, f.getTpdu());
	}

	@Test
	public final void testRFLDataBooleanBooleanIntIntByteArrayIndividualAddressKNXAddressByteArray()
	{
		new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src, dst, tpdu);
	}

	@Test
	public final void testRFLDataByteArrayInt() throws KNXFormatException
	{
		RFLData f = new RFLData(frame, 0);
		Assert.assertArrayEquals(frame, f.toByteArray());

		final byte[] offsetData = new byte[100];
		final int offset = 5;
		System.arraycopy(frame, 0, offsetData, offset, frame.length);
		f = new RFLData(offsetData, offset);
		Assert.assertArrayEquals(frame, f.toByteArray());
	}

	@Test
	public final void testGetSource()
	{
		Assert.assertEquals(src, rf.getSource());
	}

	@Test
	public final void testGetDestination()
	{
		Assert.assertEquals(dst, rf.getDestination());
	}

	@Test
	public final void testGetFrameType()
	{
		Assert.assertEquals(frameType, rf.getFrameType());
	}

	@Test
	public final void testGetRss()
	{
		Assert.assertEquals(RSS.Void, rf.getRss());
	}

	@Test
	public final void testIsBatteryOk()
	{
		Assert.assertEquals(batteryOk, rf.isBatteryOk());
	}

	@Test
	public final void testIsTransmitOnlyDevice()
	{
		Assert.assertEquals(transmitOnlyDevice, rf.isTransmitOnlyDevice());
	}

	@Test
	public final void testIsSystemBroadcast()
	{
		Assert.assertEquals(true, rf.isSystemBroadcast());

		RFLData f = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, null,
				dst, tpdu);
		Assert.assertEquals(true, f.isSystemBroadcast());

		f = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src,
				new IndividualAddress(1, 1, 1), tpdu);
		Assert.assertEquals(false, f.isSystemBroadcast());

		f = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src,
				new GroupAddress(0), tpdu);
		Assert.assertEquals(false, f.isSystemBroadcast());
	}

	@Test
	public final void testGetDoAorSN()
	{
		Assert.assertArrayEquals(doa, rf.getDoAorSN());
	}

	@Test
	public final void testGetFrameNumber()
	{
		Assert.assertEquals(frameNumber, rf.getFrameNumber());
	}

	@Test
	public final void testGetTpdu()
	{
		Assert.assertArrayEquals(tpdu, rf.getTpdu());
	}

	@Test
	public final void testToByteArray()
	{
		Assert.assertArrayEquals(frame, rf.toByteArray());
	}

	@Test
	public final void testCrc()
	{
		// 01 02 03 04 05 06 07 08 has the CRC 0xFCBC
		final byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		final int correctCrc = 0xfcbc;
		final int calcCrc = RFLData.crc16(data, 0, data.length);
//		System.out.println("calculated CRC=" + calcCrc + ", correct CRC=" + correctCrc);
		Assert.assertEquals(correctCrc, calcCrc);
	}
}

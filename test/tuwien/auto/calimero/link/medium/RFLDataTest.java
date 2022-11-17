/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2022 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.cemi.RFMediumInfo.RSS;

class RFLDataTest {
	private final boolean batteryOk = true;
	private final boolean transmitOnlyDevice = false;
	private final int frameType = 0x94;
	private final int frameNumber = 4;
	private final byte[] doa = new byte[] { (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84,
		(byte) 0x85 };
	private final IndividualAddress src = new IndividualAddress(1, 1, 22);
	private final KNXAddress dst = new GroupAddress(2, 1, 3);
	private final byte[] tpdu = new byte[] { 2, 3, 0, 12, 1, 56, 0, 0, 0, 16, 17, 18, 19, 20, 21, 22 };

	private final byte[] serial = new byte[] { 1, 2, 3, 4, 5, 6 };

	private RFLData rf;

	private final byte[] frame = new byte[] { (byte) 0x1f, (byte) 0x44, (byte) 0xff, (byte) 0x02, (byte) 0x80,
		(byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0xe4, (byte) 0xae, (byte) 0x94,
		(byte) 0x11, (byte) 0x16, (byte) 0x11, (byte) 0x03, (byte) 0xe8, (byte) 0x02, (byte) 0x03, (byte) 0x00,
		(byte) 0x0c, (byte) 0x01, (byte) 0x38, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0xfe,
		(byte) 0xfb, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0xf6,
		(byte) 0x83 };

	@BeforeEach
	void setUp() throws Exception {
		rf = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src, dst, tpdu);
	}

	@Test
	void newForTransmitOnlyDevice() {
		final RFLData f = RFLData.newForTransmitOnlyDevice(batteryOk, frameType, frameNumber, serial,
				new GroupAddress(1), tpdu);
		assertEquals(batteryOk, f.isBatteryOk());
		assertEquals(true, f.isTransmitOnlyDevice());
		assertEquals(frameType, f.getFrameType());
		assertEquals(frameNumber, f.getFrameNumber());
		assertArrayEquals(serial, f.getDoAorSN());
		assertEquals(new IndividualAddress(0, 5, 0xff), f.getSource());
		assertEquals(new GroupAddress(1), f.getDestination());
		assertArrayEquals(tpdu, f.getTpdu());
	}

	@Test
	void rfLDataBooleanBooleanIntIntByteArrayIndividualAddressKNXAddressByteArray() {
		new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src, dst, tpdu);
	}

	@Test
	void rfLDataByteArrayInt() throws KNXFormatException {
		RFLData f = new RFLData(frame, 0);
		assertArrayEquals(frame, f.toByteArray());

		final byte[] offsetData = new byte[100];
		final int offset = 5;
		System.arraycopy(frame, 0, offsetData, offset, frame.length);
		f = new RFLData(offsetData, offset);
		assertArrayEquals(frame, f.toByteArray());
	}

	@Test
	void getSource() {
		assertEquals(src, rf.getSource());
	}

	@Test
	void getDestination() {
		assertEquals(dst, rf.getDestination());
	}

	@Test
	void getFrameType() {
		assertEquals(frameType, rf.getFrameType());
	}

	@Test
	void getRss() {
		assertEquals(RSS.Void, rf.getRss());
	}

	@Test
	void isBatteryOk() {
		assertEquals(batteryOk, rf.isBatteryOk());
	}

	@Test
	void isTransmitOnlyDevice() {
		assertEquals(transmitOnlyDevice, rf.isTransmitOnlyDevice());
	}

	@Test
	void isSystemBroadcast() {
		assertEquals(true, rf.isSystemBroadcast());

		RFLData f = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, null, dst, tpdu);
		assertEquals(true, f.isSystemBroadcast());

		f = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src, new IndividualAddress(1, 1, 1),
				tpdu);
		assertEquals(false, f.isSystemBroadcast());

		f = new RFLData(batteryOk, transmitOnlyDevice, frameType, frameNumber, doa, src, GroupAddress.Broadcast, tpdu);
		assertEquals(false, f.isSystemBroadcast());
	}

	@Test
	void getDoAorSN() {
		assertArrayEquals(doa, rf.getDoAorSN());
	}

	@Test
	void getFrameNumber() {
		assertEquals(frameNumber, rf.getFrameNumber());
	}

	@Test
	void getTpdu() {
		assertArrayEquals(tpdu, rf.getTpdu());
	}

	@Test
	void toByteArray() {
		assertArrayEquals(frame, rf.toByteArray());
	}

	@Test
	void crc() {
		// 01 02 03 04 05 06 07 08 has the CRC 0xFCBC
		final byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		final int correctCrc = 0xfcbc;
		final int calcCrc = RFLData.crc16(data, 0, data.length);
		assertEquals(correctCrc, calcCrc);
	}
}

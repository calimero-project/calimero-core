/*
    Calimero - A library for KNX network access
    Copyright (c) 2019, 2021 B. Malinowsky

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

package tuwien.auto.calimero.secure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tuwien.auto.calimero.DataUnitBuilder.fromHex;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.SerialNumber;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.link.AbstractLink;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.mgmt.TransportLayer;
import tuwien.auto.calimero.mgmt.TransportLayerImpl;
import tuwien.auto.calimero.secure.SecurityControl.DataSecurity;

class SecureApplicationLayerTest {

	private static final int PropertyWrite = 0x03D7;
	private static final int PropertyResponse = 0x03D6;

	private static final byte[] toolKey = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b,
		0x0c, 0x0d, 0x0e, 0x0f };

	private static final byte[] encryptedPropertyWrite = fromHex(
			"03 f1 90 00 00 00 00 00 04 6767242a2308ca76a11774214ee4cf5d94909f743d050d8fc168");
	private static final byte[] encryptedPropertyResponse = fromHex(
			"03 f1 90 00 00 00 00 00 03 706f533105503557cb2b24f1dd341b60b7e017ecd6b06849a72b");
	private static final byte[] encryptedSyncReq = fromHex(
			"43 F1 92 00 00 00 00 00 01 00 00 00 00 00 00 c1 cf 45 06 f0 9b d7 9f ab 55");
	private static final byte[] encryptedSyncRes = fromHex(
			"43 f1 93 aa aa aa aa aa a9 9c 02 3a d2 5e 14 64 70 69 3e 63 8d 5b 70 ca c4");

	private static final IndividualAddress local = new IndividualAddress(15, 15, 103);
	private static final IndividualAddress remote = new IndividualAddress(15, 15, 0);

	private KNXNetworkLink link;
	private TransportLayer transportLayer;
	private SecureApplicationLayer sal;

	private long sequenceNumberToolAccess;

	private byte[] remoteAndLocalSeq;
	private long receivedSyncRegChallenge;

	private static final int DataConnected = 0x40;
	private int tpci = DataConnected;

	@BeforeEach
	void init() throws KNXException {
		link = new AbstractLink<>(null, "test", new TPSettings()) {
			@Override
			protected void onSend(final CEMILData msg, final boolean waitForCon) {}

			@Override
			protected void onSend(final KNXAddress dst, final byte[] msg, final boolean waitForCon) {}
		};
		transportLayer = new TransportLayerImpl(link);

		final Map<GroupAddress, byte[]> groupKeys = Map.of();
		final Map<IndividualAddress, byte[]> deviceToolKeys = Map.of();
		sal = new SecureApplicationLayer(link, groupKeys, Map.of(), deviceToolKeys) {
			@Override
			long nextSequenceNumber(final boolean toolAccess) {
				if (toolAccess)
					return sequenceNumberToolAccess;
				return 5;
			};

			@Override
			protected long lastValidSequenceNumber(final boolean toolAccess, final IndividualAddress remote) {
				if (toolAccess)
					return sequenceNumberToolAccess - 1;
				return 0;
			}

			@Override
			protected byte[] toolKey(final IndividualAddress device) {
				if (device.equals(local))
					return toolKey;
				if (device.equals(remote))
					return toolKey;
				return super.toolKey(device);
			}

			@Override
			void receivedSyncResponse(final IndividualAddress remote, final boolean toolAccess,
					final byte[] plainApdu) {
				remoteAndLocalSeq = plainApdu.clone();
				super.receivedSyncResponse(remote, toolAccess, plainApdu);
			}

			@Override
			void receivedSyncRequest(final IndividualAddress source, final KNXAddress dst, final boolean toolAccess,
					final boolean sysBcast, final byte[] seq, final long challenge) {
				receivedSyncRegChallenge = challenge;
			}

			@Override
			protected int tpci(final KNXAddress dst) { return tpci; }
		};

		SecureApplicationLayer.test = true;
	}

	@AfterEach
	void cleanup() {
		link.close();
		sal.close();
	}

	private byte[] secure(final int service, final IndividualAddress src, final IndividualAddress dst, final byte[] data) {
		final var secCtrl = SecurityControl.of(DataSecurity.AuthConf, true);
		return sal.secure(service, src, dst, data, secCtrl).get();
	}

	@Test
	void encryptPropertyWrite() {
		final byte[] asdu = propertyAsdu();
		final byte[] apdu = DataUnitBuilder.createAPDU(PropertyWrite, asdu);

		assertArrayEquals(fromHex("03 D7 05 35 10 01 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F"), apdu);
		sequenceNumberToolAccess = 4;
		tpci = 0; // T_Data_Individual
		// A+C
		final byte[] secureApdu = secure(SecureApplicationLayer.SecureDataPdu, local, remote, apdu);

		assertArrayEquals(encryptedPropertyWrite, secureApdu);
	}

	@Test
	void decryptPropertyWrite() {
		assertEquals(SecureApplicationLayer.SecureService, DataUnitBuilder.getAPDUService(encryptedPropertyWrite));

		final var salData = sal.extract(local, remote, encryptedPropertyWrite);
		final var plainApdu = salData.apdu();
		assertEquals(PropertyWrite, DataUnitBuilder.getAPDUService(plainApdu));

		final var plainAsdu = DataUnitBuilder.extractASDU(plainApdu);
		final byte[] expected = propertyAsdu();
		assertArrayEquals(expected, plainAsdu);
	}

	@Test
	void encryptPropertyResponse() {
		final byte[] asdu = propertyAsdu();
		final byte[] apdu = DataUnitBuilder.createAPDU(PropertyResponse, asdu);

		assertArrayEquals(fromHex("03D605351001202122232425262728292A2B2C2D2E2F"), apdu);

		final int seq = 3;
		sequenceNumberToolAccess = seq;
		tpci = 0; // T_Data_Individual

		// A+C
		final byte[] secureApdu = secure(SecureApplicationLayer.SecureDataPdu, remote, local, apdu);

		final byte[] prefix = { 0x03, (byte) 0xf1, (byte) 0x90, 0x00, 00, 00, 00, 00, (byte) seq };
		final String encoded = "706f533105503557cb2b24f1dd341b60b7e017ecd6b06849a72b";
		final byte[] encrypted = fromHex(encoded);
		final byte[] expected = new byte[prefix.length + encrypted.length];
		System.arraycopy(prefix, 0, expected, 0, prefix.length);
		System.arraycopy(encrypted, 0, expected, prefix.length, encrypted.length);
		assertArrayEquals(expected, secureApdu);
	}

	@Test
	void decryptPropertyResponse() {
		assertEquals(SecureApplicationLayer.SecureService, DataUnitBuilder.getAPDUService(encryptedPropertyResponse));

		sequenceNumberToolAccess = 3;
		final var salData = sal.extract(remote, local, encryptedPropertyResponse);
		final var plainApdu = salData.apdu();
		assertEquals(PropertyResponse, DataUnitBuilder.getAPDUService(plainApdu));

		final var plainAsdu = DataUnitBuilder.extractASDU(plainApdu);
		final byte[] expected = propertyAsdu();
		assertArrayEquals(expected, plainAsdu);
	}

	private static byte[] propertyAsdu() {
		final int objIndex = 5;
		final int elements = 1;
		final int start = 1;
		final byte[] data = { 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E,
			0x2F };

		final int pidGroupKeyTable = 53;
		final byte[] asdu = new byte[4 + data.length];
		asdu[0] = (byte) objIndex;
		asdu[1] = (byte) pidGroupKeyTable;
		asdu[2] = (byte) ((elements << 4) | ((start >>> 8) & 0x0f));
		asdu[3] = (byte) start;
		for (int i = 0; i < data.length; ++i)
			asdu[i + 4] = data[i];

		return asdu;
	}

	@Test
	void encryptSyncRequest() {
		final long challenge = 3;
		final byte[] data = ByteBuffer.allocate(6).putShort((short) (challenge >> 32)).putInt((int) challenge).array();

		sequenceNumberToolAccess = 1;
		transportLayer.createDestination(remote, true);
		// A+C
		final byte[] secureApdu = secure(SecureApplicationLayer.SecureSyncRequest, local, remote, data);
		assertArrayEquals(encryptedSyncReq, secureApdu);
	}

	@Test
	void decryptSyncRequest() {
		assertEquals(SecureApplicationLayer.SecureService, DataUnitBuilder.getAPDUService(encryptedSyncReq));

		transportLayer.createDestination(local, true);
		final var salData = sal.extract(local, remote, encryptedSyncReq);
		assertEquals(0, salData.apdu().length);
		final long expected = 3;
		assertEquals(expected, receivedSyncRegChallenge);
	}

	@Test
	void encryptSyncResponse() {
		final long seqRemote = 4;
		final long responderSeq = 3;

		final byte[] apdu = ByteBuffer.allocate(12).putShort((short) (responderSeq >> 32)).putInt((int) responderSeq)
				.putShort((short) (seqRemote >> 32)).putInt((int) seqRemote).array();
		transportLayer.createDestination(local, true);
		sequenceNumberToolAccess = 4;
		sal.syncChallenge.set(3L);
		// A+C
		final byte[] secureApdu = secure(SecureApplicationLayer.SecureSyncResponse, remote, local, apdu);
		assertArrayEquals(encryptedSyncRes, secureApdu);
	}

	@Test
	void decryptSyncResponse() {
		assertEquals(SecureApplicationLayer.SecureService, DataUnitBuilder.getAPDUService(encryptedSyncRes));

		sal.stashSyncRequest(remote, 3);
		final var salData = sal.extract(remote, local, encryptedSyncRes);
		assertEquals(0, salData.apdu().length);
		// seq remote = 3, seq local = 4
		final byte[] expected = { 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 4 };
		assertArrayEquals(expected, remoteAndLocalSeq);
	}

	private static final byte[] p2pKey = { (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff };
	private static final SerialNumber nonExistingSerialNo = SerialNumber.of(0x010203040506L);

	@Test
	void broadcastSyncRequest() {
		final var e = assertThrows(ExecutionException.class,
				() -> sal.broadcastSyncRequest(nonExistingSerialNo, p2pKey, false, false).get());
		assertEquals(TimeoutException.class, e.getCause().getClass());
	}

	@Test
	void systemBroadcastSyncRequest() {
		sequenceNumberToolAccess = 1;
		final var e = assertThrows(ExecutionException.class,
				() -> sal.broadcastSyncRequest(nonExistingSerialNo, p2pKey, true, true).get());
		assertEquals(TimeoutException.class, e.getCause().getClass());
	}

	@Test
	void broadcastSyncRequestToolAccess() {
		sequenceNumberToolAccess = 10;
		final var e = assertThrows(ExecutionException.class,
				() -> sal.broadcastSyncRequest(nonExistingSerialNo, toolKey, true, false).get());
		assertEquals(TimeoutException.class, e.getCause().getClass());
	}
}

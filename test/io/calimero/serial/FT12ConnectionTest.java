/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tag.FT12;
import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXAckTimeoutException;
import io.calimero.KNXException;
import io.calimero.KNXTimeoutException;
import io.calimero.Priority;
import io.calimero.Util;
import io.calimero.cemi.CEMIFactory;
import io.calimero.cemi.CEMILData;

class FT12ConnectionTest {
	private static final String portID = Util.getSerialPortID();
	private FT12Connection c;
	private final BauFt12Emulator emulator = new BauFt12Emulator();

	private static final GroupAddress dst = new GroupAddress(2, 3, 7);

	private static final CEMILData cemiLDataReq = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), dst,
			new byte[] { 9, 9, 9 }, Priority.LOW);
	private static final byte[] emi2LDataReq = CEMIFactory.toEmi(cemiLDataReq);

	@AfterEach
	void tearDown() {
		if (c != null)
			c.close();
	}

	@Test
	void noAckForReset() {
		emulator.replyWithAck = false;
		assertThrows(KNXAckTimeoutException.class, () -> new FT12Connection(emulator, "emulator", false),
				"reset got ACKed");
	}

	@Nested
	@FT12
	class InitWithPhysicalConnection {
		@BeforeEach
		void init() throws KNXException, InterruptedException {
			c = new FT12Connection(portID);
		}

		@Test
		void ft12ConnectionNoSharing() throws InterruptedException {
			try {
				final FT12Connection c2 = new FT12Connection(portID);
				c2.close();
				fail("no sharing");
			}
			catch (final KNXException e) {}
			c.close();
		}

		@Test
		void ft12ConnectionStringInt() throws KNXException, InterruptedException {
			c.close();
			c = new FT12Connection(portID, 19200);
			c.close();
			try {
				final FT12Connection c2 = new FT12Connection(portID, 56000);
				c2.close();
				fail("this baud rate should not work with BCU");
			}
			catch (final KNXException e) {}
		}

		@Test
		void close() throws KNXException, InterruptedException {
			assertEquals(FT12Connection.OK, c.getState());
			c.close();
			assertEquals(FT12Connection.CLOSED, c.getState());
			c.close();
			assertEquals("", c.getPortID());
			try {
				c.send(new byte[] { 1, 2, }, true);
				fail("closed");
			}
			catch (final KNXPortClosedException e) {}
			assertEquals(FT12Connection.CLOSED, c.getState());
		}

		@Test
		void getBaudRate() throws IOException {
			assertEquals(19200, c.getBaudRate());
		}

		@Test
		void getState() throws KNXException, InterruptedException {
			assertEquals(FT12Connection.OK, c.getState());
			final byte[] switchNormal = { (byte) 0xA9, 0x1E, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, };
			c.send(switchNormal, true);
			assertEquals(FT12Connection.OK, c.getState());
			c.send(switchNormal, true);
			assertEquals(FT12Connection.OK, c.getState());

			c.send(switchNormal, false);
			assertEquals(FT12Connection.ACK_PENDING, c.getState());
			Thread.sleep(150);
			assertEquals(FT12Connection.OK, c.getState());
			c.send(new byte[] { 1, 2, }, true);
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void send() throws KNXException, InterruptedException {
			c.send(new byte[] { 1, 2, }, true);
			c.send(new byte[] { 1, 2, }, false);
			c.send(new byte[] { 1, 2, }, true);
			c.send(new byte[] { 1, 2, }, false);
			c.close();
		}
	}

	@Nested
	class InitWithEmulator {
		@BeforeEach
		void init() throws KNXException, InterruptedException {
			c = new FT12Connection(emulator, "emulator", false);
		}

		@Test
		void stateOk() {
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void stateOkAfterSendingNoLDataBlocking()
			throws KNXTimeoutException, KNXPortClosedException, InterruptedException {
			c.send(new byte[10], true);
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void stateOkAfterSendingCemiLDataBlocking()
			throws KNXTimeoutException, KNXPortClosedException, InterruptedException {
			c.send(cemiLDataReq.toByteArray(), true);
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void stateOkAfterSendingEmi2LDataBlocking()
			throws KNXTimeoutException, KNXPortClosedException, InterruptedException {
			c.send(emi2LDataReq, true);
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void noAckForSend() {
			emulator.replyWithAck = false;
			assertThrows(KNXAckTimeoutException.class, () -> c.send(new byte[2], true), "send got ACKed");
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void noConForSend() {
			emulator.replyWithCon = false;
			assertThrows(KNXTimeoutException.class, () -> c.send(cemiLDataReq.toByteArray(), true), "received .con");
			assertEquals(FT12Connection.OK, c.getState());
		}

		@Test
		void stateClosed() {
			c.close();
			assertEquals(FT12Connection.CLOSED, c.getState());
		}
	}
}

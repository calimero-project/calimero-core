/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2025, 2025 B. Malinowsky

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

package io.calimero.cemi;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CEMIFactoryTest {

	private final byte[] tpdu = new byte[] { 1, 2, 3, 4, 5 };
	private final byte[] newTpdu = { 10, 11, 12 };
	private final int invalidMsgCode = 333;

	@Test
	void createLData() {
		var ldata = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(100), new GroupAddress(101), tpdu,
				Priority.LOW);

		var created = CEMIFactory.create(CEMILData.MC_LDATA_CON, newTpdu, ldata);
		assertArrayEquals(newTpdu, created.getPayload());

		created = CEMIFactory.create(CEMILData.MC_LDATA_CON, null, ldata);
		assertEquals(CEMILData.MC_LDATA_CON, created.getMessageCode());
		assertArrayEquals(tpdu, created.getPayload());

		created = CEMIFactory.create(0, null, ldata);
		assertEquals(ldata, created);

		assertThrows(KNXIllegalArgumentException.class, () -> CEMIFactory.create(invalidMsgCode, tpdu, ldata));
		assertThrows(NullPointerException.class, () -> CEMIFactory.create(CEMILData.MC_LDATA_CON, tpdu, null));
	}

	@Test
	void createLDataEx() {
		var ldata = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(100), new GroupAddress(101), tpdu,
				Priority.LOW);

		var created = CEMIFactory.create(CEMILData.MC_LDATA_CON, newTpdu, ldata);
		assertArrayEquals(newTpdu, created.getPayload());

		created = CEMIFactory.create(CEMILData.MC_LDATA_CON, null, ldata);
		assertEquals(CEMILData.MC_LDATA_CON, created.getMessageCode());
		assertArrayEquals(tpdu, created.getPayload());

		created = CEMIFactory.create(0, null, ldata);
		assertEquals(ldata, created);

		assertThrows(KNXIllegalArgumentException.class, () -> CEMIFactory.create(invalidMsgCode, tpdu, ldata));
		assertThrows(NullPointerException.class, () -> CEMIFactory.create(CEMILData.MC_LDATA_CON, tpdu, null));
	}

	@Test
	void createBusMon() {
		var mon = CEMIBusMon.newWithStatus(34, 789, false, new byte[40]);

		var created = CEMIFactory.create(CEMIBusMon.MC_BUSMON_IND, newTpdu, mon);
		assertArrayEquals(newTpdu, created.getPayload());

		created = CEMIFactory.create(CEMIBusMon.MC_BUSMON_IND, null, mon);
		assertEquals(mon, created);

		assertThrows(KNXIllegalArgumentException.class, () -> CEMIFactory.create(invalidMsgCode, tpdu, mon));
		assertThrows(NullPointerException.class, () -> CEMIFactory.create(CEMIBusMon.MC_BUSMON_IND, tpdu, null));
	}

	@Test
	void createTData() {
		final var tdata = new CemiTData(CemiTData.ConnectedIndication, tpdu);

		var created = CEMIFactory.create(CemiTData.ConnectedRequest, newTpdu, tdata);
		assertArrayEquals(newTpdu, created.getPayload());

		created = CEMIFactory.create(CemiTData.ConnectedRequest, null, tdata);
		assertEquals(CemiTData.ConnectedRequest, created.getMessageCode());
		assertArrayEquals(tpdu, created.getPayload());

		created = CEMIFactory.create(CemiTData.ConnectedIndication, null, tdata);
		assertEquals(tdata, created);

		assertThrows(KNXIllegalArgumentException.class, () -> CEMIFactory.create(invalidMsgCode, tpdu, tdata));
		assertThrows(NullPointerException.class, () -> CEMIFactory.create(CemiTData.ConnectedRequest, tpdu, null));
	}
}

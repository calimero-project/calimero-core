/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

package io.calimero.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.Priority;
import io.calimero.cemi.CEMILData;

class LDataObjectTest {
	CEMILData frame;

	@BeforeEach
	void init() throws Exception {
		frame = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0), new GroupAddress("1/1/1"),
				new byte[] { 1, 2, 3, }, Priority.NORMAL);
	}

	@Test
	void ldataObjectCEMILData() {
		final LDataObject o = new LDataObject(frame);
		assertEquals(frame, o.getFrame());
	}

	@Test
	void getFrame() throws KNXFormatException {
		final LDataObject o = new LDataObject(frame);
		assertEquals(frame, o.getFrame());
		final CEMILData frame2 = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1/1/1"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
		o.setFrame(frame2);
		assertEquals(frame2, o.getFrame());
	}

	@Test
	void ldataObjectKey() {
		final LDataObject o = new LDataObject(frame);
		assertEquals(o.getKey(), frame.getDestination());
	}

	@Test
	void set() throws KNXFormatException {
		final LDataObject o = new LDataObject(frame);
		final CEMILData frame2 = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1/1/2"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
		boolean failed = false;
		try {
			o.setFrame(frame2);
		}
		catch (final KNXIllegalArgumentException e) {
			failed = true;
		}
		assertTrue(failed);

		final CEMILData frame3 = new CEMILData(CEMILData.MC_LDATA_IND, new IndividualAddress(0),
				new GroupAddress("1/1/1"), new byte[] { 1, 2, 3, }, Priority.NORMAL);
		failed = false;
		try {
			o.setFrame(frame3);
		}
		catch (final KNXIllegalArgumentException e) {
			failed = true;
		}
		assertFalse(failed);
	}
}

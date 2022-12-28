/*
    Calimero 2 - A library for KNX network access
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

package io.calimero.cemi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.Priority;


class CEMILDataExTest
{
	private CEMILDataEx f;
	private final IndividualAddress src = new IndividualAddress(1, 2, 3);
	private final GroupAddress dst = new GroupAddress(2, 4, 4);
	private final byte[] tpdu = new byte[] { 0, (byte) 129 };
	private final byte[] plinfo = new byte[] { 0x10, 0x20 };
	private final byte[] extts = new byte[] { (byte) 0x80, 0x2, 0x3, 0x4 };


	@BeforeEach
	void init() throws Exception
	{
		f = new CEMILDataEx(CEMILData.MC_LDATA_REQ, src, dst, tpdu, Priority.LOW);
		f.additionalInfo().add(AdditionalInfo.of(AdditionalInfo.PlMedium, plinfo));
		f.additionalInfo().add(AdditionalInfo.of(AdditionalInfo.ExtendedTimestamp, extts));
	}

	@Test
	void getStructLength()
	{
		assertEquals(11 + 2 + 2 + 2 + 4, f.getStructLength());
		f.additionalInfo().removeIf(info -> info.type() == AdditionalInfo.PlMedium);
		f.additionalInfo().removeIf(info -> info.type() == AdditionalInfo.ExtendedTimestamp);
		assertEquals(11, f.getStructLength());
	}

	@Test
	void testToString()
	{
		assertNotNull(f.toString());
		assertNotNull(new CEMILDataEx(CEMILData.MC_LDATA_REQ, src, dst, tpdu, Priority.LOW).toString());
	}

	@Test
	void setHopCount()
	{
		assertEquals(6, f.getHopCount());
		f.setHopCount(2);
		assertEquals(2, f.getHopCount());
		f.setHopCount(7);
		assertEquals(7, f.getHopCount());
		assertThrows(KNXIllegalArgumentException.class, () -> f.setHopCount(8), "out of range");
	}

	@Test
	void setPriority()
	{
		assertEquals(Priority.LOW, f.getPriority());
		f.setPriority(Priority.SYSTEM);
		assertEquals(Priority.SYSTEM, f.getPriority());
	}

	@Test
	void setBroadcast()
	{
		assertTrue(f.isDomainBroadcast());
		f.setBroadcast(false);
		assertFalse(f.isDomainBroadcast());
	}

	@Test
	void addAdditionalInfo()
	{
		assertThrows(KNXIllegalArgumentException.class, () -> AdditionalInfo.of(AdditionalInfo.PlMedium, new byte[] { 1 }), "wrong length");

		final byte[] getPL = f.getAdditionalInfo(AdditionalInfo.PlMedium);
		assertTrue(Arrays.equals(plinfo, getPL));
		f.additionalInfo().removeIf(info -> info.type() == AdditionalInfo.ExtendedTimestamp);
		f.additionalInfo().add(AdditionalInfo.of(AdditionalInfo.ExtendedTimestamp, new byte[] { 4, 4, 4, 4 }));
		final byte[] getTS = f.getAdditionalInfo(AdditionalInfo.ExtendedTimestamp);
		assertTrue(Arrays.equals(new byte[] { 4, 4, 4, 4 }, getTS));
	}

	@Test
	void getAdditionalInfoInt()
	{
		assertNull(f.getAdditionalInfo(AdditionalInfo.RfMedium));
	}

	@Test
	void isExtendedFrame()
	{
		assertFalse(f.isExtendedFrame());
		final CEMILDataEx f2 = new CEMILDataEx(CEMILData.MC_LDATA_REQ, src, dst,
				new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }, Priority.LOW);
		assertTrue(f2.isExtendedFrame());
	}

	@Test
	void testClone()
	{
		final CEMILDataEx clone = f.clone();
		assertArrayEquals(f.toByteArray(), clone.toByteArray());
		final var l = f.additionalInfo();
		final var l2 = clone.additionalInfo();
		for (int i = 0; i < l.size(); ++i) {
			assertEquals(l.get(i).type(), l2.get(i).type());
			assertNotSame(l.get(i).info(), l2.get(i).info());
		}
	}
}

/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.datapoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.GroupAddress;
import io.calimero.Util;
import io.calimero.xml.KNXMLException;
import io.calimero.xml.XmlInputFactory;
import io.calimero.xml.XmlOutputFactory;
import io.calimero.xml.XmlReader;
import io.calimero.xml.XmlWriter;


class StateDPTest
{
	private static final GroupAddress ga = new GroupAddress(3, 2, 1);
	private static final String filename = "stateDP.xml";
	private static final String dpFile = Util.getTargetPath() + filename;

	private List<GroupAddress> inv;
	private List<GroupAddress> upd;


	@BeforeEach
	void init() {
		inv = new ArrayList<>();
		inv.add(new GroupAddress(1, 1, 1));
		inv.add(new GroupAddress(2, 2, 2));
		inv.add(new GroupAddress(3, 3, 3));
		upd = new ArrayList<>();
		upd.add(new GroupAddress(4, 4, 4));
		upd.add(new GroupAddress(5, 5, 5));
	}

	@Test
	void testToString()
	{
		final Datapoint dp = new StateDP(ga, "test");
		assertTrue(dp.toString().contains("test"));
		assertTrue(dp.toString().contains("state DP"));
		assertTrue(dp.toString().contains(ga.toString()));
	}

	@Test
	void stateDPGroupAddressString()
	{
		final Datapoint dp2 = new StateDP(ga, "test");
		assertTrue(dp2.isStateBased());
	}

	@Test
	void stateDPGroupAddressStringIntString()
	{
		final Datapoint dp = new StateDP(ga, "test", 1, "1.001");
		assertEquals(ga, dp.getMainAddress());
		assertEquals("test", dp.getName());
		assertTrue(dp.isStateBased());
		assertEquals("1.001", dp.getDPT());
	}

	@Test
	void stateDPGroupAddressStringCollectionCollection()
	{
		final StateDP dp = new StateDP(ga, "test", inv, upd);
		assertEquals(ga, dp.getMainAddress());
		assertEquals("test", dp.getName());
		assertTrue(dp.isStateBased());

		Collection<GroupAddress> c = dp.getAddresses(false);
		assertEquals(3, c.size());
		assertTrue(c.contains(new GroupAddress(1, 1, 1)));
		assertTrue(c.contains(new GroupAddress(2, 2, 2)));
		assertTrue(c.contains(new GroupAddress(3, 3, 3)));
		try {
			c.add(ga);
			fail("collection should be unmodifiable");
		}
		catch (final UnsupportedOperationException e) {}

		c = dp.getAddresses(true);
		assertEquals(2, c.size());
		assertTrue(c.contains(new GroupAddress(4, 4, 4)));
		assertTrue(c.contains(new GroupAddress(5, 5, 5)));
		try {
			c.add(ga);
			fail("collection should be unmodifiable");
		}
		catch (final UnsupportedOperationException e) {}
	}

	@Test
	void stateDPXmlReader() throws KNXMLException
	{
		final XmlWriter w = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		StateDP dp = new StateDP(ga, "testSave2", inv, upd);
		dp.setExpirationTimeout(15);
		dp.save(w);
		w.close();
		final XmlReader r = XmlInputFactory.newInstance().createXMLReader(dpFile);
		dp = new StateDP(r);
		r.close();
		assertEquals(ga, dp.getMainAddress());
		assertEquals("testSave2", dp.getName());
		assertTrue(dp.isStateBased());
		assertNotNull(dp.dptId());
		assertEquals(15, dp.getExpirationTimeout());
		assertEquals(upd, new ArrayList<>(dp.getAddresses(true)));
		assertEquals(inv, new ArrayList<>(dp.getAddresses(false)));
	}

	@Test
	void create() throws KNXMLException
	{
		final XmlReader r = XmlInputFactory.newInstance().createXMLReader(Util.getPath() + filename);
		assertInstanceOf(StateDP.class, Datapoint.create(r));
		r.close();
	}

	@Test
	void setExpirationTimeout()
	{
		final StateDP dp = new StateDP(ga, "test");
		assertEquals(0, dp.getExpirationTimeout());
		dp.setExpirationTimeout(1000);
		assertEquals(1000, dp.getExpirationTimeout());
	}

	@Test
	void add()
	{
		final StateDP dp = new StateDP(ga, "name1", inv, upd);
		for (final GroupAddress a : upd) {
			dp.addUpdatingAddress(a);
		}
		assertEquals(upd.size(), dp.getAddresses(true).size());
		for (final GroupAddress a : inv) {
			dp.addInvalidatingAddress(a);
		}
		assertEquals(inv.size(), dp.getAddresses(false).size());
		dp.addInvalidatingAddress(new GroupAddress(5, 5, 5));
		assertEquals(inv.size() + 1, dp.getAddresses(false).size());
		assertTrue(dp.isInvalidating(new GroupAddress(5, 5, 5)));
	}

	@Test
	void removeAddress()
	{
		final StateDP dp = new StateDP(ga, "name1", inv, upd);
		assertTrue(dp.isInvalidating(new GroupAddress(1, 1, 1)));
		dp.removeAddress(new GroupAddress(1, 1, 1));
		assertFalse(dp.isInvalidating(new GroupAddress(1, 1, 1)));
		assertTrue(dp.isUpdating(new GroupAddress(4, 4, 4)));
		dp.removeAddress(new GroupAddress(4, 4, 4));
		assertFalse(dp.isUpdating(new GroupAddress(4, 4, 4)));
	}

	@Test
	void getAddresses()
	{
		final StateDP dp = new StateDP(ga, "test", inv, upd);
		assertNotNull(dp.getAddresses(true));
		assertNotNull(dp.getAddresses(false));
		assertEquals(2, dp.getAddresses(true).size());
		assertEquals(3, dp.getAddresses(false).size());
	}

	@Test
	void isInvalidating()
	{
		final StateDP dp = new StateDP(ga, "test");
		dp.addUpdatingAddress(new GroupAddress(2, 2, 2));
		assertFalse(dp.isInvalidating(new GroupAddress(2, 2, 2)));
		dp.addInvalidatingAddress(new GroupAddress(2, 2, 2));
		assertTrue(dp.isInvalidating(new GroupAddress(2, 2, 2)));
	}

	@Test
	void isUpdating()
	{
		final StateDP dp = new StateDP(ga, "test");
		dp.addUpdatingAddress(new GroupAddress(2, 2, 2));
		assertTrue(dp.isUpdating(new GroupAddress(2, 2, 2)));
		dp.addInvalidatingAddress(new GroupAddress(1, 1, 1));
		assertFalse(dp.isUpdating(new GroupAddress(1, 1, 1)));
	}

	@Test
	void equals() {
		final StateDP dp1 = new StateDP(ga, "test");
		final StateDP dp2 = new StateDP(ga, "test", inv, upd);
		assertNotEquals(dp1, dp2);

		final StateDP dup = new StateDP(ga, "test", inv, upd);
		assertEquals(dp2, dup);
		dup.removeAddress(new GroupAddress(1, 1, 1));
		assertNotEquals(dp2, dup);

		final StateDP dp3 = new StateDP(ga, "test");
		dp3.setExpirationTimeout(1000);

		assertNotEquals(dp1, dp3);
		dp3.setExpirationTimeout(0);
		assertEquals(dp1, dp3);
	}
}

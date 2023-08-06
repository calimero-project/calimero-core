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

package io.calimero.datapoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.calimero.GroupAddress;
import io.calimero.Util;
import io.calimero.xml.KNXMLException;
import io.calimero.xml.XmlInputFactory;
import io.calimero.xml.XmlOutputFactory;
import io.calimero.xml.XmlReader;
import io.calimero.xml.XmlWriter;

class CommandDPTest {
	private static final GroupAddress ga = new GroupAddress(3, 2, 1);
	private static final String filename = "commandDP.xml";
	private static final String dpFile = Util.getTargetPath() + filename;

	@Test
	void testToString() {
		final Datapoint dp = new CommandDP(ga, "test");
		assertTrue(dp.toString().contains("test"));
		assertTrue(dp.toString().contains("command DP"));
		assertTrue(dp.toString().contains(ga.toString()));
	}

	@Test
	void commandDPGroupAddressString() {
		final Datapoint dp = new CommandDP(ga, "test");
		assertEquals(ga, dp.getMainAddress());
		assertEquals("test", dp.getName());
		assertFalse(dp.isStateBased());
	}

	@Test
	void commandDPGroupAddressStringIntString() {
		final Datapoint dp = new CommandDP(ga, "test", 1, "1.001");
		assertEquals(ga, dp.getMainAddress());
		assertEquals("test", dp.getName());
		assertFalse(dp.isStateBased());
		assertEquals(1, dp.getMainNumber());
		assertEquals("1.001", dp.getDPT());
	}

	@Test
	void commandDPXmlReader() throws KNXMLException {
		Datapoint dp = new CommandDP(ga, "testSave", 4, "4.001");
		final XmlWriter w = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		dp.save(w);
		w.close();

		final XmlReader r = XmlInputFactory.newInstance().createXMLReader(dpFile);
		dp = new CommandDP(r);
		r.close();
		assertEquals(ga, dp.getMainAddress());
		assertEquals("testSave", dp.getName());
		assertFalse(dp.isStateBased());
		assertEquals(4, dp.getMainNumber());
		assertEquals("4.001", dp.getDPT());
	}

	@Test
	void create() throws KNXMLException {
		final XmlReader r = XmlInputFactory.newInstance().createXMLReader(Util.getPath() + filename);
		assertTrue(Datapoint.create(r) instanceof CommandDP);
		r.close();
	}
}

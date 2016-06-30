/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

package tuwien.auto.calimero.datapoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XmlInputFactory;
import tuwien.auto.calimero.xml.XmlOutputFactory;
import tuwien.auto.calimero.xml.XmlReader;
import tuwien.auto.calimero.xml.XmlWriter;

/**
 * @author B. Malinowsky
 */
public class DatapointMapTest
{
	private static final String dpFile = Util.getTargetPath() + "datapointMap.xml";

	private DatapointModel<Datapoint> m;
	private final GroupAddress ga1 = new GroupAddress(1, 1, 1);
	private final GroupAddress ga2 = new GroupAddress(2, 2, 2);
	private final GroupAddress ga3 = new GroupAddress(3, 3, 3);
	private final Datapoint dp1 = new StateDP(ga1, "test1");
	private final Datapoint dp2 = new CommandDP(ga2, "test2");
	private final Datapoint dp3 = new StateDP(ga3, "test3");

	@BeforeEach
	protected void init() throws Exception
	{
		m = new DatapointMap<>();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#DatapointMap(
	 * java.util.Collection)}.
	 */
	@Test
	public final void testDatapointMapCollection()
	{
		final List<Datapoint> l = new ArrayList<>();
		l.add(dp1);
		l.add(dp2);
		l.add(dp3);
		final DatapointModel<Datapoint> dpm = new DatapointMap<>(l);
		assertTrue(dpm.contains(ga1));
		assertTrue(dpm.contains(ga2));
		assertTrue(dpm.contains(ga3));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#add(
	 * tuwien.auto.calimero.datapoint.Datapoint)}.
	 */
	@Test
	public final void testAdd()
	{
		assertFalse(m.contains(ga1));
		assertFalse(m.contains(dp1));
		m.add(dp1);
		assertTrue(m.contains(ga1));
		assertTrue(m.contains(dp1));
		assertEquals(dp1, m.get(ga1));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#remove(
	 * tuwien.auto.calimero.datapoint.Datapoint)}.
	 */
	@Test
	public final void testRemove()
	{
		m.add(dp1);
		assertTrue(m.contains(ga1));
		m.remove(dp1);
		assertNull(m.get(ga1));
		assertFalse(m.contains(dp1));
		assertFalse(m.contains(ga1));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#get(
	 * tuwien.auto.calimero.GroupAddress)}.
	 */
	@Test
	public final void testGet()
	{
		assertNull(m.get(ga3));
		m.add(dp3);
		assertEquals(dp3, m.get(ga3));
		assertNull(m.get(ga2));
		m.removeAll();
		assertNull(m.get(ga3));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#removeAll()}.
	 */
	@Test
	public final void testRemoveAll()
	{
		final List<Datapoint> l = new ArrayList<>();
		l.add(dp1);
		l.add(dp2);
		l.add(dp3);
		final DatapointModel<Datapoint> dpm = new DatapointMap<>(l);
		dpm.removeAll();
		assertFalse(dpm.contains(ga1));
		assertFalse(dpm.contains(ga2));
		assertFalse(dpm.contains(ga3));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#getDatapoints()}.
	 */
	@Test
	public final void testGetDatapoints()
	{
		Collection<Datapoint> c = ((DatapointMap<Datapoint>) m).getDatapoints();
		assertEquals(0, c.size());
		m.add(dp2);
		c = ((DatapointMap<Datapoint>) m).getDatapoints();
		assertEquals(1, c.size());
		assertTrue(c.contains(dp2));

		try {
			c.add(dp3);
			fail("unmodifiable");
		}
		catch (final UnsupportedOperationException e) {}
		m.add(dp1);
		m.add(dp3);
		assertEquals(3, c.size());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#load(
	 * tuwien.auto.calimero.xml.XmlReader)}.
	 *
	 * @throws KNXMLException
	 */
	@Test
	@Disabled
	public final void testLoad() throws KNXMLException
	{
		final XmlWriter w = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		m.add(dp1);
		m.add(dp2);
		m.add(dp3);
		m.save(w);
		w.close();
		final XmlReader r = XmlInputFactory.newInstance().createXMLReader(dpFile);
		try {
			m.load(r);
			fail("datapoints already in map");
		}
		catch (final KNXMLException e) {}
		r.close();

		m.removeAll();
		assertEquals(0, ((DatapointMap<Datapoint>) m).getDatapoints().size());
		final XmlReader r2 = XmlInputFactory.newInstance().createXMLReader(dpFile);
		m.load(r2);
		r2.close();
		assertEquals(3, ((DatapointMap<Datapoint>) m).getDatapoints().size());
		assertTrue(m.contains(dp1));
		assertTrue(m.contains(dp2));
		assertTrue(m.contains(dp3));

		// save empty file
		final XmlWriter w2 = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		new DatapointMap<StateDP>().save(w2);
		w2.close();
		// load empty file
		final XmlReader r3 = XmlInputFactory.newInstance().createXMLReader(dpFile);
		final DatapointMap<StateDP> dpm = new DatapointMap<>();
		dpm.load(r3);
		r3.close();
		assertEquals(0, dpm.getDatapoints().size());

		// load empty file into nonempty map
		final XmlReader r4 = XmlInputFactory.newInstance().createXMLReader(dpFile);
		m.load(r4);
		r4.close();
		assertEquals(3, ((DatapointMap<Datapoint>) m).getDatapoints().size());

		// ensure state-based DPs only
		final XmlWriter w5 = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		m.removeAll();
		m.add(dp1);
		m.add(dp2); // command-based!
		m.add(dp3);
		m.save(w5);
		w5.close();
		final XmlReader r5 = XmlInputFactory.newInstance().createXMLReader(dpFile);
		try {
			new DatapointMap<StateDP>().load(r5);
			fail("loaded command DP into state-based DP map");
		}
		catch (final KNXMLException expected) {}
		r5.close();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.datapoint.DatapointMap#save(
	 * tuwien.auto.calimero.xml.XmlWriter)}.
	 *
	 * @throws KNXMLException
	 */
	@Test
	public final void testSave() throws KNXMLException
	{
		final XmlWriter w = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		m.save(w);
		w.close();
		final XmlWriter w2 = XmlOutputFactory.newInstance().createXMLWriter(dpFile);
		m.add(dp1);
		m.add(dp2);
		m.add(dp3);
		m.save(w2);
		w2.close();
	}
}

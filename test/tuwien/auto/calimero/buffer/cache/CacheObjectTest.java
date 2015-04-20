/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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

package tuwien.auto.calimero.buffer.cache;

import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * @author B. Malinowsky
 */
public class CacheObjectTest extends TestCase
{
	private final Object key = new Object();
	private final Object value = new Object();

	/**
	 * @param name name of test case
	 */
	public CacheObjectTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.buffer.cache.CacheObject#CacheObject
	 * (java.lang.Object, java.lang.Object)}.
	 */
	public void testCacheObject()
	{
		CacheObject o = null;
		try {
			o = new CacheObject(null, null);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertNull(o);

		try {
			o = new CacheObject("CacheObject", null);
		}
		catch (final KNXIllegalArgumentException e) {}
		assertNull(o);

		o = new CacheObject(key, value);
		assertEquals(key, o.getKey());
		assertEquals(value, o.getValue());

		final String skey = "CacheObject";
		final List<Object> vvalue = new Vector<>();
		o = new CacheObject(skey, vvalue);
		assertEquals(skey, o.getKey());
		assertEquals(vvalue, o.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.cache.CacheObject#getTimestamp()}.
	 */
	public void testGetTimestamp()
	{
		final long time = System.currentTimeMillis();
		final CacheObject o = new CacheObject(key, value);
		assertTrue(o.getTimestamp() != 0);
		assertTrue("wrong timestamp", o.getTimestamp() >= time
			&& o.getTimestamp() <= time + 2);
	}

}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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

package tuwien.auto.calimero.log;

import junit.framework.TestCase;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class LogManagerTest extends TestCase
{
	private static final String file = Util.getPath() + "test-manager-log-writer.log";
	private static final String file2 = Util.getPath() + "test-manager-log-writer 2.log";
	private static final String file3 = Util.getPath() + "test-manager-log-writer 3.log";

	private LogManager m;

	/**
	 * @param name name for test case
	 */
	public LogManagerTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		m = LogManager.getManager();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
//		final String[] all = m.getAllLogServices();
//		for (int i = 0; i < all.length; ++i)
//			m.removeLogService(all[i]);
//		final LogWriter[] w = m.getAllGlobalWriters();
//		for (int i = 0; i < w.length; ++i)
//			m.removeWriter("", w[i]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogManager#getManager()}.
	 */
	public void testGetManager()
	{
		final LogManager m2 = LogManager.getManager();
		assertEquals(m2, m);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogManager#getLogService(java.lang.String)}.
	 */
	public void testGetLogger()
	{
		final LogService l1 = m.getLogService("test-logger");
		final LogService l2 = m.getLogService("test-logger");
		final LogService l3 = m.getLogService("test-logger 2");
		assertEquals(l1, l2);
		assertNotSame(l1, l3);
	}
}

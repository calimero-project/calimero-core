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
*/

package tuwien.auto.calimero.log;

import java.io.IOException;
import java.io.Writer;

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
	protected void setUp() throws Exception
	{
		m = LogManager.getManager();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		final String[] all = m.getAllLogServices();
		for (int i = 0; i < all.length; ++i)
			m.removeLogService(all[i]);
		final LogWriter[] w = m.getAllGlobalWriters();
		for (int i = 0; i < w.length; ++i)
			m.removeWriter("", w[i]);
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

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogManager#getAllLogServices()}.
	 */
	public void testGetAllLogger()
	{
		m.getLogService("test-logger");
		m.getLogService("test-logger");
		m.getLogService("test-logger 2");
		assertEquals(2, m.getAllLogServices().length);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogManager#removeLogService(java.lang.String)}.
	 */
	public void testRemoveLogger()
	{
		final LogService l1 = m.getLogService("test-logger");
		/* LogService l2 = */m.getLogService("test-logger");
		final LogService l3 = m.getLogService("test-logger 2");
		m.removeLogService("test-logger");
		assertTrue(m.hasLogService("test-logger 2"));
		assertFalse(m.hasLogService("test-logger"));
		assertEquals(1, m.getAllLogServices().length);

		final LogService l4 = m.getLogService("test-logger 2");
		assertEquals(l3, l4);
		final LogService l5 = m.getLogService("test-logger");
		assertNotSame(l1, l5);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogManager#addWriter
	 * (java.lang.String, tuwien.auto.calimero.log.LogWriter)}.
	 * 
	 * @throws IOException
	 * @throws KNXLogException
	 */
	public void testAddWriter() throws IOException, KNXLogException
	{
		final LogService l1 = m.getLogService("test-logger");
		final LogService l2 = m.getLogService("test-logger 2");
		final LogWriter w1 = new LogFileWriter(LogLevel.ALL, file, false, 0, true);
		m.addWriter(l1.getName(), w1);
		final LogWriter w2 = new LogFileWriter(LogLevel.INFO, file2, false, 0, true);
		m.addWriter(l2.getName(), w2);
		l1.warn("warn msg logger 1");
		assertEquals(1, LogServiceTest.readLines(file).length);
		assertEquals(0, LogServiceTest.readLines(file2).length);
		l2.warn("warn msg logger 2");
		assertEquals(1, LogServiceTest.readLines(file).length);
		assertEquals(1, LogServiceTest.readLines(file2).length);
		l2.trace("wrong trace msg logger 2");
		assertEquals(1, LogServiceTest.readLines(file).length);
		assertEquals(1, LogServiceTest.readLines(file2).length);
		// set w1 global
		m.addWriter("", w1);
		l2.warn("warn msg logger 2");
		assertEquals(2, LogServiceTest.readLines(file).length);
		assertEquals(2, LogServiceTest.readLines(file2).length);
		l1.warn("warn msg logger 1");
		assertEquals(4, LogServiceTest.readLines(file).length);
		assertEquals(2, LogServiceTest.readLines(file2).length);
		w1.close();
		w2.close();

	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogManager#removeWriter
	 * (java.lang.String, tuwien.auto.calimero.log.LogWriter)}.
	 * 
	 * @throws IOException
	 * @throws KNXLogException
	 */
	public void testRemoveWriter() throws IOException, KNXLogException
	{
		assertEquals(0, m.getAllGlobalWriters().length);

		final LogService l1 = m.getLogService("test-logger");
		final LogService l2 = m.getLogService("test-logger 2");
		final LogWriter w1 = new LogFileWriter(LogLevel.ALL, file, false, 0, true);
		m.addWriter(l1.getName(), w1);
		final LogWriter w2 = new LogFileWriter(LogLevel.INFO, file2, false, 0, true);
		m.addWriter(l2.getName(), w2);
		m.removeWriter("", w1);
		l1.info("info msg");
		assertEquals(1, LogServiceTest.readLines(file).length);
		m.removeWriter("", w1);
		l1.info("info msg");
		assertEquals(2, LogServiceTest.readLines(file).length);
		m.addWriter("", w1);
		assertEquals(1, m.getAllGlobalWriters().length);
		l1.info("info msg");
		assertEquals(4, LogServiceTest.readLines(file).length);
		l2.info("info msg");
		assertEquals(5, LogServiceTest.readLines(file).length);
		assertEquals(1, LogServiceTest.readLines(file2).length);
		m.removeWriter("", w1);
		assertEquals(0, m.getAllGlobalWriters().length);
		l1.info("info msg");
		assertEquals(6, LogServiceTest.readLines(file).length);
		m.removeWriter(l1.getName(), w1);
		l1.info("info msg");
		assertEquals(6, LogServiceTest.readLines(file).length);
		m.removeWriter(l2.getName(), w2);
		l2.info("info msg");
		assertEquals(1, LogServiceTest.readLines(file2).length);

		final LogWriter w3 = new LogFileWriter(file3, false, true);
		m.addWriter("", w3);
		final LogService l3 = m.getLogService("test-logger 3");
		l3.trace("trace msg");
		assertEquals(1, LogServiceTest.readLines(file3).length);
		l1.info("info msg");
		assertEquals(2, LogServiceTest.readLines(file3).length);
		l3.removeWriter(w3);
		l3.trace("trace msg");
		assertEquals(2, LogServiceTest.readLines(file3).length);
		m.removeWriter("", w3);
		l1.info("info msg");
		assertEquals(2, LogServiceTest.readLines(file3).length);
		assertEquals(0, m.getAllGlobalWriters().length);
		w1.close();
		w2.close();
		w3.close();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogManager#hasLogService(String)}.
	 */
	public void testHasLogger()
	{
		final String logger1 = "test-logger 1";
		final String logger2 = "test-logger 2";
		final String logger3 = "test-logger 3";
		assertFalse(m.hasLogService(logger1));
		assertFalse(m.hasLogService(logger2));
		m.getLogService(logger1);
		assertTrue(m.hasLogService(logger1));
		assertFalse(m.hasLogService(logger2));
		m.getLogService(logger2);
		assertTrue(m.hasLogService(logger1));
		assertTrue(m.hasLogService(logger2));
		m.getLogService(logger3);
		assertTrue(m.hasLogService(logger1));
		assertTrue(m.hasLogService(logger2));
		assertTrue(m.hasLogService(logger3));
		m.removeLogService(logger1);
		assertFalse(m.hasLogService(logger1));
		assertTrue(m.hasLogService(logger2));
		assertTrue(m.hasLogService(logger3));
	}
	
	// subclass so we have the writer instance
	private class TestWriter extends LogFileWriter{
		Writer w;
		
		public TestWriter(final LogLevel level, final String file, final boolean append, final int maxSize,
			final boolean autoFlush) throws KNXLogException
		{
			super(level, file, append, maxSize, autoFlush);
		}
		
		protected void setOutput(final Writer w)
		{
			super.setOutput(w);
			this.w = w;
		}
	}
	
	public void testShutdown() throws KNXLogException
	{
		// add global writer
		final TestWriter w1 = new TestWriter(LogLevel.ALL, file, false, 0, true);
		m.addWriter(null, w1);
		
		final TestWriter w2 = new TestWriter(LogLevel.ALL, file2, false, 0, true);
		final LogService s = m.getLogService("test-logger 1");
		s.addWriter(w2);
		// remove the global writer from this service
		s.removeWriter(w1);
		
		m.shutdown(true);
		try {
			w1.w.write('c');
			fail("writer should be closed");
		}
		catch (final IOException e) {
			// ok
		}
		assertEquals(0, m.getAllGlobalWriters().length);
	}
}

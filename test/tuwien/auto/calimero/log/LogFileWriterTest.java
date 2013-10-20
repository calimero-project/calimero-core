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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class LogFileWriterTest extends TestCase
{
	private static final String test = Util.getPath() + "test.log";
	private static final String exists = Util.getPath() + "exists.log";
	private static final String close = Util.getPath() + "close.log";
	// on MacOS, this is actually not invalid
	private static final String invalid = Util.getPath() + "{!<\"~?*^',invalid.log";
	private static final String wrongDir = Util.getPath() + "dirNotExists/invalid.log";

	private LogFileWriter w, w2;
	private final LogLevel all = LogLevel.ALL;
	private final LogLevel err = LogLevel.ERROR;
	private final LogLevel info = LogLevel.INFO;

	private final String logService = "my LogService";

	/**
	 * @param name name of test case
	 */
	public LogFileWriterTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		w = new LogFileWriter(test, false);
		w2 = new LogFileWriter(err, test, false);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		w.close();
		w2.close();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogFileWriter#write(String, LogLevel, String)}.
	 */
	public void testWriteLevelString()
	{
		w.write(logService, info, info.toString());
		w.write(logService, all, all.toString());
		w2.write(logService, info, info.toString());
		w2.write(logService, all, all.toString());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogFileWriter#write(String, LogLevel, String, Throwable)}
	 * .
	 */
	public void testWriteLevelStringThrowable()
	{
		final Exception e = new Exception("exception msg");
		w.write(logService, info, info.toString(), e);
		w.write(logService, all, all.toString(), e);
		w2.write(logService, info, info.toString(), e);
		w2.write(logService, all, all.toString(), e);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogFileWriter#close()}.
	 * 
	 * @throws KNXLogException
	 * @throws IOException
	 */
	public void testClose() throws KNXLogException, IOException
	{
		final LogWriter l = new LogFileWriter(close, false);
		l.write(logService, all, "close after this");
		l.close();
		l.close();
		l.write(logService, all, "should not be in log");
		final int lines = readLines(close);
		assertEquals(1, lines);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogFileWriter#LogFileWriter(java.lang.String, boolean)}
	 * .
	 * 
	 * @throws IOException
	 */
	public void testLogFileWriterStringBoolean() throws IOException
	{
		LogWriter l = null;
		try {
			l = new LogFileWriter(exists, false);
			l.close();
		}
		catch (final KNXLogException e) {}
		assertNotNull(l);
		l = null;
		// dont run this, as its a valid name on MacOS
//		try {
//			l = new LogFileWriter(invalid, false);
//			l.close();
//		}
//		catch (final KNXLogException e) {}
		assertNull(l);
		l = null;
		try {
			l = new LogFileWriter(wrongDir, true);
			l.close();
		}
		catch (final KNXLogException e) {}
		assertNull(l);
		l = null;
		try {
			l = new LogFileWriter(exists, false);
			assertEquals(0, new File(exists).length());
			l.write(logService, all, "1 line");
			l.close();
		}
		catch (final KNXLogException e) {
			fail("should not fail here");
		}
		assertNotNull(l);

		l = null;
		try {
			l = new LogFileWriter(exists, true);
			assertEquals(1, readLines(exists));
			l.close();
		}
		catch (final KNXLogException e) {
			fail("should not fail here");
		}
		assertNotNull(l);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogFileWriter#LogFileWriter(tuwien.auto.calimero.log.LogLevel, java.lang.String, boolean)}
	 * .
	 */
	public void testLogFileWriterLevelStringBoolean()
	{
		LogFileWriter l = null;
		try {
			l = new LogFileWriter(all, exists, true);
			assertEquals(all, l.getLogLevel());
			l.close();
		}
		catch (final KNXLogException e) {
			fail("should not fail here");
		}
		assertNotNull(l);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogFileWriter#LogFileWriter(tuwien.auto.calimero.log.LogLevel, java.lang.String, boolean, int)}
	 * .
	 */
	public void testLogFileWriterLevelStringBooleanInt()
	{
		LogFileWriter l = null;
		try {
			l = new LogFileWriter(all, exists, false, -1);
			assertEquals(0, l.getMaxSize());
			l.close();
		}
		catch (final KNXLogException e) {}
		assertNotNull(l);
		l = null;
		try {
			l = new LogFileWriter(all, exists, false, 0);
			assertEquals(0, l.getMaxSize());
			l.close();
		}
		catch (final KNXLogException e) {}
		assertNotNull(l);
		l = null;
		try {
			l = new LogFileWriter(all, exists, false, 10);
			assertEquals(10, l.getMaxSize());
			l.close();
		}
		catch (final KNXLogException e) {}
		assertNotNull(l);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogFileWriter#getFileName()}.
	 */
	public void testGetFileName()
	{
		LogFileWriter l = null;
		try {
			l = new LogFileWriter(all, exists, false, 10);
			assertEquals(exists, l.getFileName());
			l.close();
			assertEquals("", l.getFileName());
		}
		catch (final KNXLogException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogFileWriter#setMaxSize(int)}.
	 */
	public void testSetMaxSize()
	{
		LogFileWriter l = null;
		try {
			l = new LogFileWriter(exists, true);
			l.setMaxSize(10000);
			assertEquals(10000, l.getMaxSize());
			l.close();
		}
		catch (final KNXLogException e) {
			fail("should not fail here");
		}
		assertNotNull(l);
	}

	/**
	 * Tests the file size of the file written by the log file writer.
	 * 
	 * @throws KNXLogException
	 */
	public void testSize() throws KNXLogException
	{
		LogFileWriter w3;
		w3 = new LogFileWriter(test, false, true)
		{
			protected String formatOutput(final String logService, final LogLevel l, final String msg,
				final Throwable t)
			{
				return msg;
			}
		};

		final String s = "This is a test output string";
		int len = s.getBytes().length;
		w3.setMaxSize(len);
		w3.write(logService, LogLevel.INFO, s);
		int fileLen = (int) new File(test).length();
		assertTrue(fileLen >= len && fileLen <= len + 4);
		final String s2 = "a bit more";
		w3.write(logService, LogLevel.INFO, s2);
		len = s2.getBytes().length;
		fileLen = (int) new File(test).length();
		assertTrue(fileLen >= len && fileLen <= len + 4);

		w3 = new LogFileWriter(test, false, true)
		{
			protected String formatOutput(final String logService, final LogLevel l,
				final String msg, final Throwable t)
			{
				return msg;
			}
		};

		final char[] buf = new char[10000];
		Arrays.fill(buf, 'a');
		final String str = new String(buf);
		w3.setMaxSize((str.getBytes().length + 2) * 2);
		w3.write(logService, LogLevel.ALL, str);
		len = str.getBytes().length;
		fileLen = (int) new File(test).length();
		assertTrue(fileLen >= len && fileLen <= len + 4);
		w3.write(logService, LogLevel.ALL, str);
		fileLen = (int) new File(test).length();
		assertTrue(fileLen >= 2 * len && fileLen <= 2 * (len + 4));
		// now a new file is created and written into
		w3.write(logService, LogLevel.ALL, s2);
		len = s2.getBytes().length;
		fileLen = (int) new File(test).length();
		assertTrue(fileLen >= len && fileLen <= len + 4);
	}

	private int readLines(final String file) throws IOException
	{
		final BufferedReader r = new BufferedReader(new FileReader(new File(file)));
		int lines = 0;
		try {
			while (r.readLine() != null)
				++lines;
		}
		catch (final IOException e) {
			fail("reading back log file failed");
		}
		r.close();
		return lines;
	}

}

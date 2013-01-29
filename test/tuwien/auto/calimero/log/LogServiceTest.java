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
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class LogServiceTest extends TestCase
{
	private static final String file = Util.getPath() + "test-log-writer.log";
	private LogService ls;
	private LogWriter w;
	private final String logService = "my LogService";

	/**
	 * @param name name of test case
	 */
	public LogServiceTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		ls = LogManager.getManager().getLogService("test-log");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		ls = null;
		if (w != null)
			w.close();
	}

	/**
	 * Test method for tuwien.auto.calimero.log.LogService.
	 */
	public void testLogService()
	{
		final LogService s =
			LogManager.getManager().getLogService(getClass().toString());
		assertEquals(getClass().toString(), s.getName());
		System.out.println("Name of LogService: " + s.getName());
		s.info("");
		s.warn(null);
		final LogService s2 = LogManager.getManager().getLogService("x");
		assertEquals("x", s2.getName());
		s2.info("");
		s2.warn(null);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogService#addWriter(tuwien.auto.calimero.log.LogWriter)}.
	 * 
	 * @throws IOException
	 * @throws KNXLogException
	 */
	public void testAddWriter() throws IOException, KNXLogException
	{
		w = new LogFileWriter(file, false, true);
		ls.addWriter(w);
		final String s = "test string";
		ls.warn(s);
		assertEquals(true, readLines(file)[0].indexOf(s) > 0);
		ls.removeWriter(w);
		ls.warn(s);
		assertEquals(1, readLines(file).length);
		w.close();
		w = new LogFileWriter(LogLevel.ERROR, file, false, 0, true);
		ls.addWriter(w);
		ls.warn("this string is not allowed");
		assertEquals(0, readLines(file).length);
		ls.error("this string is allowed");
		assertEquals(1, readLines(file).length);
		ls.fatal("this string is allowed");
		assertEquals(2, readLines(file).length);
		ls.removeWriter(w);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogService#removeWriter(tuwien.auto.calimero.log.LogWriter)}.
	 * 
	 * @throws IOException
	 * @throws KNXLogException
	 */
	public void testRemoveWriter() throws IOException, KNXLogException
	{
		w = new LogFileWriter(file, false, true);
		// don't close log writer afterwards, because we set it to System.out
		ls.addWriter(new LogStreamWriter(System.out));
		ls.addWriter(w);
		ls.info("this message is ok");
		assertEquals(1, readLines(file).length);
		ls.removeAllWriters(false);
		ls.info("this message should not appear");
		assertEquals(1, readLines(file).length);

		w.write(logService, LogLevel.INFO, "using write() of LogWriter");
		assertEquals(2, readLines(file).length);

		ls.addWriter(w);
		ls.info("this message is ok");
		assertEquals(3, readLines(file).length);
		ls.removeAllWriters(true);
		ls.info("this message should not appear");
		assertEquals(3, readLines(file).length);

		w.write(logService, LogLevel.INFO,
			"this message should not appear - using write() of LogWriter");
		assertEquals(3, readLines(file).length);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogService#getLogLevel()}.
	 */
	public void testGetLogLevel()
	{
		assertNotNull(ls.getLogLevel());
		ls.setLogLevel(LogLevel.TRACE);
		assertEquals(LogLevel.TRACE, ls.getLogLevel());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogService#log(LogLevel, String, Throwable)}, with
	 * no exception object used.
	 * 
	 * @throws IOException
	 * @throws KNXLogException
	 */
	public void testLogLevelString() throws IOException, KNXLogException
	{
		w = new LogFileWriter(file, false, true);
		ls.addWriter(w);
		ls.setLogLevel(LogLevel.TRACE);
		ls.log(LogLevel.ALL, LogLevel.ALL.toString(), null);
		assertEquals(0, readLines(file).length);

		ls.setLogLevel(LogLevel.ALL);
		ls.log(LogLevel.ALL, LogLevel.ALL.toString(), null);
		assertEquals(1, readLines(file).length);

		ls.setLogLevel(LogLevel.OFF);
		ls.log(LogLevel.OFF, LogLevel.OFF.toString(), null);
		assertEquals(1, readLines(file).length);

		ls.setLogLevel(LogLevel.OFF);
		ls.log(LogLevel.ALL, LogLevel.ALL.toString(), null);
		assertEquals(1, readLines(file).length);

		ls.setLogLevel(LogLevel.TRACE);
		ls.log(LogLevel.INFO, LogLevel.INFO.toString(), null);
		assertEquals(2, readLines(file).length);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogService#log(tuwien.auto.calimero.log.LogLevel, java.lang.String, java.lang.Throwable)}.
	 * 
	 * @throws IOException
	 * @throws KNXLogException
	 */
	public void testLogLevelStringThrowable() throws IOException, KNXLogException
	{
		w = new LogFileWriter(file, false, true);
		ls.addWriter(w);
		ls.setLogLevel(LogLevel.TRACE);
		ls.log(LogLevel.INFO, "this is a log string", new Exception(
			"this is an exception string"));
		String[] buf = readLines(file);
		assertEquals(1, buf.length);
		assertEquals(true, buf[0].indexOf("exception") > 0);
		// don't close log writer afterwards because we set it to System.out
		final LogWriter out = new LogStreamWriter(System.out);
		ls.addWriter(out);
		ls.log(LogLevel.INFO, "this is another log string", new Exception(
			"this is an exception string"));
		assertEquals(2, (buf = readLines(file)).length);
		assertEquals(true, buf[1].indexOf("exception") > 0);

		ls.log(LogLevel.INFO, null, new Exception("this is an exception string"));
		ls.log(LogLevel.INFO, null, null);
	}

	static String[] readLines(final String file) throws IOException
	{
		// this is necessary to let the log service thread write its data out
		try {
			Thread.sleep(500);
		}
		catch (final InterruptedException e1) {
			e1.printStackTrace();
		}
		final BufferedReader r = new BufferedReader(new FileReader(new File(file)));
		String s = null;
		final List v = new Vector();
		try {
			while ((s = r.readLine()) != null)
				v.add(s);
		}
		catch (final IOException e) {
			fail("reading back log file failed");
		}
		r.close();
		return (String[]) v.toArray(new String[v.size()]);
	}

}

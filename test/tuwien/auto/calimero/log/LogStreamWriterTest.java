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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class LogStreamWriterTest extends TestCase
{
	private static final String file = Util.getPath() + "stream.log";
	private static final String file2 = Util.getPath() + "stream2.log";

	private LogWriter w, w2;
	private final String logService = "my LogService";

	private OutputStream stream;

	private class MyErrorHandler extends ErrorHandler
	{
		public synchronized void error(final LogWriter source, final String msg,
			final Exception e)
		{
			super.error(source, msg, e);
		}
	}

	/**
	 * @param name name for test case
	 */
	public LogStreamWriterTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		w = new LogStreamWriter(LogLevel.ALL, stream = new FileOutputStream(file), true);
		// no autoflush
		w2 = new LogStreamWriter(LogLevel.WARN, new FileOutputStream(file2), false);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		w.close();
		w = null;
		w2.close();
		w2 = null;
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogStreamWriter#LogStreamWriter(LogLevel, OutputStream, boolean, boolean)}
	 * .
	 * 
	 * @throws FileNotFoundException
	 */
	public void testLogStreamWriterLogLevelOutputStreamBooleanBoolean()
		throws FileNotFoundException
	{
		final OutputStream str = new FileOutputStream(file);
		final LogWriter w3 = new LogStreamWriter(LogLevel.ALL, str, true, false);
		w3.close();
		try {
			str.write(1);
		}
		catch (final IOException e) {
			fail("we were closed");
		}
		finally {
			try {
				str.close();
			}
			catch (final IOException e) {}
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogStreamWriter#write(String, LogLevel, String)}.
	 * 
	 * @throws IOException
	 */
	public void testWriteLevelString() throws IOException
	{
		w.setLogLevel(LogLevel.INFO);
		w.write(logService, LogLevel.INFO, "info msg");
		String[] buf = readLines(file);
		assertEquals(1, buf.length);
		w.write(logService, LogLevel.TRACE, "this string should not appear here");
		buf = readLines(file);
		assertEquals(1, buf.length);
		w.setLogLevel(LogLevel.OFF);
		w.write(logService, LogLevel.OFF, "this is a OFF msg");
		buf = readLines(file);
		assertEquals(1, buf.length);
		w.write(logService, LogLevel.ERROR, "error: this is a ERROR msg at loglevel OFF");
		buf = readLines(file);
		assertEquals(1, buf.length);

		w2.write(logService, LogLevel.INFO, "this string should not appear here");
		w2.write(logService, LogLevel.WARN, "this string is not autoflushed");
		buf = readLines(file2);
		assertEquals(0, buf.length);
		w2.close();
		buf = readLines(file2);
		assertEquals(1, buf.length);

		// write a bunch of logs out to force a flush due to full buffer
		w2 = new LogStreamWriter(LogLevel.WARN, new FileOutputStream(file2), false);
		for (int i = 0; i < 1000; ++i)
			w2.write(logService, LogLevel.ERROR,
				"this is a standard log message with error information");
		buf = readLines(file2);
		assertTrue(buf.length > 0);
		System.out
			.println("LogStreamWriter with no autoflush, buffer full, lines written to disk: "
				+ buf.length);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogStreamWriter#write (String, LogLevel, String, Throwable)}
	 * .
	 * 
	 * @throws IOException
	 */
	public void testWriteLevelStringThrowable() throws IOException
	{
		w.write(logService, LogLevel.INFO, "info msg", new Exception("exception string"));
		final String[] buf = readLines(file);
		assertEquals(1, buf.length);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogStreamWriter#close()}.
	 * 
	 * @throws IOException
	 */
	public void testClose() throws IOException
	{
		w.close();
		w.close();
		w.write(logService, LogLevel.FATAL, "error: stream should be closed");
		final String[] buf = readLines(file);
		assertEquals(0, buf.length);
	}

	/**
	 * Tests the log error handler.
	 * 
	 * @throws IOException
	 */
	public void testErrorHandler() throws IOException
	{
		System.out.println("*** error output on next lines is intentional ***");
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException e) {}
		LogWriter.setErrorHandler(new MyErrorHandler());
		stream.close();
		w.write(logService, LogLevel.FATAL, "error: stream should be closed");
		w.write(logService, LogLevel.FATAL, "error: stream should be closed");
		w.write(logService, LogLevel.FATAL, "error: stream should be closed");
	}

	/**
	 * @throws IOException
	 */
	private String[] readLines(final String file) throws IOException
	{
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

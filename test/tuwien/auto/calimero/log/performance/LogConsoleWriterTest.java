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

package tuwien.auto.calimero.log.performance;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import performance.base.PerfTestCase;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogStreamWriter;
import tuwien.auto.calimero.log.LogWriter;

/**
 * @author B. Malinowsky
 */
public class LogConsoleWriterTest extends PerfTestCase
{
	private static int iterations = 500;
	private static List<LogConsoleWriterTest> results = new ArrayList<>();
	private LogWriter w;
	private LogWriter w2;
	private String s;

	private static class LogConsoleWriter extends LogStreamWriter
	{
		LogConsoleWriter()
		{
			setOutput(new OutputStreamWriter(System.out));
		}

		@Override
		public void close()
		{}
	};

	private static class LogConsoleWriter2 extends LogStreamWriter
	{
		LogConsoleWriter2()
		{
			setOutput(new BufferedWriter(new OutputStreamWriter(System.out), 150));
		}

		@Override
		public void close()
		{}
	};

	/**
	 * @param name name of test case
	 */
	public LogConsoleWriterTest(final String name)
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
		setNormalize(iterations);
		final char[] buf = new char[2 * 26];
		for (char c = 'a'; c <= 'z'; ++c)
			buf[c - 'a'] = c;
		for (char c = 'A'; c <= 'Z'; ++c)
			buf[c - 'A' + 26] = c;
		s = new String(buf);
		w = new LogConsoleWriter();
		w2 = new LogConsoleWriter2();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		results.add(this);
		for (final Iterator<LogConsoleWriterTest> i = results.iterator(); i.hasNext();) {
			final PerfTestCase test = i.next();
			test.printResults();
		}
	}

	public void testUnbufferedSystemOut()
	{
		for (int i = 0; i < iterations; ++i) {
			w.write("test service", LogLevel.INFO, s);
		}
		w.flush();
	}

	public void testBufferedSystemOut()
	{
		for (int i = 0; i < iterations; ++i) {
			w2.write("test service", LogLevel.INFO, s);
		}
		w2.flush();
	}
}

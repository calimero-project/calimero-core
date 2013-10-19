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

package tuwien.auto.calimero.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import junit.framework.TestCase;

/**
 * @author B. Malinowsky
 */
public class LogNetWriterTest extends TestCase
{
	private class Receiver extends Thread
	{
		private boolean quit;
		private ServerSocket s;
		private Socket r;

		synchronized void quit()
		{
			quit = true;
			try {
				if (r != null)
					r.close();
				s.close();
			}
			catch (final IOException e) {}
		}

		public void run()
		{
			try {
				s = new ServerSocket(port);
				synchronized (this) {
					notifyAll();
				}
				while (!quit) {
					try {
						r = s.accept();
						final BufferedReader in =
							new BufferedReader(new InputStreamReader(r.getInputStream()));
						String str = in.readLine();
						for (; str != null; str = in.readLine())
							System.out.println(str);
						in.close();
					}
					finally {
						if (r != null)
							r.close();
					}
				}
			}
			catch (final IOException e) {}
			finally {
				try {
					if (s != null)
						s.close();
				}
				catch (final IOException e) {}
				synchronized (this) {
					if (quit)
						notifyAll();
				}
			}
			System.out.println("Receiver - quit");
		}
	}

	private Receiver r;
	private final int port = 1444;
	private final LogLevel all = LogLevel.ALL;
	private final LogLevel info = LogLevel.INFO;
	private final String logService = "my LogService";

	/**
	 * @param name name of test case
	 */
	public LogNetWriterTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		r = new Receiver();
		synchronized (r) {
			r.start();
			r.wait();
		}
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		synchronized (r) {
			r.quit();
			r.wait();
			r = null;
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.log.LogNetWriter#close()}.
	 */
	public void testClose()
	{
		LogNetWriter w = null;
		try {
			w = new LogNetWriter("localhost", port);
			w.close();
			w.close();
			w.write(logService, all, "this msg should fail");
			assertEquals("", w.getHostAddress());
			assertEquals("", w.getHostName());
			assertEquals(0, w.getPort());
		}
		catch (final KNXLogException e) {
			fail("no receiver");
		}
		assertNotNull(w);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogNetWriter#LogNetWriter(java.lang.String, int)}.
	 * 
	 * @throws UnknownHostException
	 */
	public void testLogNetWriterStringInt() throws UnknownHostException
	{
		LogNetWriter w = null;
		try {
			w = new LogNetWriter("localhost", 0);
			w.write(logService, all, "this msg should fail");
			w.close();
		}
		catch (final KNXLogException e) {}
		assertNull(w);

		try {
			w = new LogNetWriter("", 3200);
			w.write(logService, all, "this msg should fail");
			w.close();
		}
		catch (final KNXLogException e) {}
		assertNull(w);

		try {
			w = new LogNetWriter("localhost", port);
			assertEquals(InetAddress.getByName("localhost").getHostAddress(), w
				.getHostAddress());
			assertEquals(InetAddress.getByName("localhost").getCanonicalHostName(), w
				.getHostName());
			assertEquals(port, w.getPort());
			w.write(logService, all, "success");
			w.close();
		}
		catch (final KNXLogException e) {
			fail("no receiver");
		}
		assertNotNull(w);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.log.LogNetWriter#LogNetWriter(tuwien.auto.calimero.log.LogLevel, java.lang.String, int)}.
	 */
	public void testLogNetWriterLevelStringInt()
	{
		LogNetWriter w = null;
		try {
			w = new LogNetWriter(info, "localhost", port);
			assertEquals(info, w.getLogLevel());
			w.close();
		}
		catch (final KNXLogException e) {
			fail("no receiver");
		}
		assertNotNull(w);
	}
}

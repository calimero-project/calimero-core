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

package tuwien.auto.calimero.serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tag.FT12;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.Util;

/**
 * Test case for FT12Connection.
 *
 * @author B. Malinowsky
 */
@FT12
public class FT12ConnectionTest
{
	private static int usePort = Util.getSerialPort();
	private static String portID;
	private FT12Connection c;

	@AfterEach
	void tearDown() throws Exception
	{
		if (c != null)
			c.close();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#FT12Connection(int)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testFT12ConnectionInt() throws KNXException
	{
		c = new FT12Connection(usePort);
		portID = c.getPortID();
		try {
			final FT12Connection c2 = new FT12Connection(usePort);
			c2.close();
			fail("no sharing");
		}
		catch (final KNXException e) {}
		try {
			final FT12Connection c3 = new FT12Connection(2);
			c3.close();
		}
		catch (final KNXException e) {}
		c.close();
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#FT12Connection(java.lang.String, int)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testFT12ConnectionStringInt() throws KNXException
	{
		c = new FT12Connection(portID, 19200);
		c.close();
		try {
			final FT12Connection c2 = new FT12Connection(portID, 56000);
			c2.close();
			fail("this baud rate should not work with BCU");
		}
		catch (final KNXException e) {}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#getPortIdentifiers()}.
	 */
	@Test
	public final void testGetPortIdentifiers()
	{
		assertNotNull(FT12Connection.getPortIdentifiers());
		System.out.println(Arrays.asList(FT12Connection.getPortIdentifiers()));
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#close()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testClose() throws KNXException, InterruptedException
	{
		c = new FT12Connection(usePort);
		assertEquals(FT12Connection.OK, c.getState());
		c.close();
		assertEquals(FT12Connection.CLOSED, c.getState());
		c.close();
		assertEquals("", c.getPortID());
		try {
			c.send(new byte[] { 1, 2, }, true);
			fail("closed");
		}
		catch (final KNXPortClosedException e) {}
		assertEquals(FT12Connection.CLOSED, c.getState());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#getBaudRate()}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testGetSetBaudRate() throws KNXException
	{
		c = new FT12Connection(usePort);
		assertEquals(19200, c.getBaudRate());
		c.setBaudrate(9600);
		assertEquals(9600, c.getBaudRate());
		c.setBaudrate(0);
		assertFalse(0 == c.getBaudRate());
		c.close();
		c.setBaudrate(9600);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#getState()}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testGetState() throws KNXException, InterruptedException
	{
		c = new FT12Connection(usePort);
		assertEquals(FT12Connection.OK, c.getState());
		final byte[] switchNormal = { (byte) 0xA9, 0x1E, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, };
		c.send(switchNormal, true);
		assertEquals(FT12Connection.OK, c.getState());
		c.send(switchNormal, true);
		assertEquals(FT12Connection.OK, c.getState());

		c.send(switchNormal, false);
		assertEquals(FT12Connection.ACK_PENDING, c.getState());
		Thread.sleep(150);
		assertEquals(FT12Connection.OK, c.getState());
		c.send(new byte[] { 1, 2, }, true);
		assertEquals(FT12Connection.OK, c.getState());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.serial.FT12Connection#send(byte[], boolean)}.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSend() throws KNXException, InterruptedException
	{
		c = new FT12Connection(usePort);
		c.send(new byte[] { 1, 2, }, true);
		c.send(new byte[] { 1, 2, }, false);
		c.send(new byte[] { 1, 2, }, true);
		c.send(new byte[] { 1, 2, }, false);
		c.close();
	}
}

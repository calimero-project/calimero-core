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

package tuwien.auto.calimero.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.FT12;
import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.TPSettings;

/**
 * Test for KNXNetworkLinkFT12.
 *
 * @author B. Malinowsky
 */
@FT12
public class KNXNetworkLinkFT12Test
{
	private KNXNetworkLink lnk;
	private NLListenerImpl nll;
	private CEMILData frame;
	private CEMILData frame2;
	private CEMILData frame3;

	private final class NLListenerImpl implements NetworkLinkListener
	{
		volatile CEMILData ind;
		volatile CEMILData con;
		volatile boolean closed;

		@Override
		public void indication(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(lnk, e.getSource());
			final CEMILData f = (CEMILData) e.getFrame();
			ind = f;
			assertEquals(CEMILData.MC_LDATA_IND, ind.getMessageCode());
			System.out.println("indication");
			Debug.printLData(ind);
		}

		@Override
		public void confirmation(final FrameEvent e)
		{
			assertNotNull(e);
			assertEquals(lnk, e.getSource());
			final CEMILData f = (CEMILData) e.getFrame();
			con = f;
			assertEquals(CEMILData.MC_LDATA_CON, f.getMessageCode());
			assertTrue(f.isPositiveConfirmation());
			System.out.println("confirmation");
			Debug.printLData(f);
		}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			assertNotNull(e);
			assertEquals(lnk, e.getSource());
			if (closed)
				fail("already closed");
			closed = true;
		}
	}

	@BeforeEach
	void setUp() throws Exception
	{
		try {
			// prevents access problems with a just previously closed port
			Thread.sleep(50);
			lnk = new KNXNetworkLinkFT12(Util.getSerialPort(), TPSettings.TP1);
		}
		catch (final Exception e) {
			Util.tearDownLogging();
			throw e;
		}
		nll = new NLListenerImpl();
		lnk.addLinkListener(nll);

		frame = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 1) }, Priority.LOW);
		frame2 = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 1),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.URGENT, true, 3);
		frame3 = new CEMILData(CEMILData.MC_LDATA_REQ, new IndividualAddress(0), new GroupAddress(0, 0, 3),
				new byte[] { 0, (byte) (0x80 | 0) }, Priority.NORMAL);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if (lnk != null)
			lnk.close();
	}

	/**
	 * Test method for
	 * {@link KNXNetworkLinkFT12#KNXNetworkLinkFT12(String, tuwien.auto.calimero.link.medium.KNXMediumSettings)}.
	 *
	 * @throws KNXException
	 */
	@Test
	public final void testKNXNetworkLinkFT12StringKNXMediumSettings() throws KNXException
	{
		lnk.close();
		lnk = new KNXNetworkLinkFT12(Util.getSerialPortID(), TPSettings.TP1);
		lnk.close();
	}

	/**
	 * Test method for
	 * {@link KNXNetworkLinkFT12#KNXNetworkLinkFT12(int, tuwien.auto.calimero.link.medium.KNXMediumSettings)}.
	 */
	@Test
	public final void testKNXNetworkLinkFT12IntKNXMediumSettings()
	{
		try {
			lnk = new KNXNetworkLinkFT12(1055, TPSettings.TP1);
			fail("should fail");
		}
		catch (final KNXException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#setKNXMedium(tuwien.auto.calimero.link.medium.KNXMediumSettings)}.
	 */
	@Test
	public final void testSetKNXMedium()
	{
		try {
			lnk.setKNXMedium(new PLSettings());
			fail("different medium");
		}
		catch (final KNXIllegalArgumentException e) {}
		final class TPSettingsSubClass extends TPSettings
		{
			TPSettingsSubClass()
			{
				super();
			}
		}
		// replace basetype with subtype
		lnk.setKNXMedium(new TPSettingsSubClass());
		// replace subtype with its supertype
		lnk.setKNXMedium(new TPSettings());

		lnk.setKNXMedium(new TPSettings(new IndividualAddress(200)));
		assertEquals(200, lnk.getKNXMedium().getDeviceAddress().getRawAddress());
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#getKNXMedium()}.
	 */
	@Test
	public final void testGetKNXMedium()
	{
		assertTrue(lnk.getKNXMedium() instanceof TPSettings);
		assertEquals(0, lnk.getKNXMedium().getDeviceAddress().getRawAddress());
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#addLinkListener(tuwien.auto.calimero.link.NetworkLinkListener)}.
	 */
	@Test
	public final void testAddLinkListener()
	{
		lnk.addLinkListener(nll);
		lnk.addLinkListener(nll);
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#removeLinkListener(tuwien.auto.calimero.link.NetworkLinkListener)}.
	 */
	@Test
	public final void testRemoveLinkListener()
	{
		lnk.removeLinkListener(nll);
		lnk.removeLinkListener(nll);
		// should do nothing
		lnk.removeLinkListener(nll);
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#getHopCount()}.
	 */
	@Test
	public final void testGetHopCount()
	{
		assertEquals(6, lnk.getHopCount());
		lnk.setHopCount(7);
		assertEquals(7, lnk.getHopCount());
		try {
			lnk.setHopCount(-1);
			fail("negative hop count");
		}
		catch (final KNXIllegalArgumentException e) {}
		try {
			lnk.setHopCount(8);
			fail("hop count too big");
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#sendRequest(KNXAddress, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 * @throws InterruptedException on interrupted thread
	 */
	@Test
	public final void testSendRequest() throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		doSend(new byte[] { 0, (byte) (0x80 | 1) });
		doSend(new byte[] { 0, (byte) (0x80 | 0) });
		doSend(new byte[] { 0, (byte) (0x80 | 1) });
		doSend(new byte[] { 0, (byte) (0x80 | 0) });

		// send an extended PL frame
		try {
			lnk.sendRequestWait(new GroupAddress(0, 0, 1), Priority.LOW,
					new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) (0x80 | 0) });
		}
		catch (final KNXIllegalArgumentException e) {}
	}

	private void doSend(final byte[] nsdu) throws KNXLinkClosedException, InterruptedException, KNXTimeoutException
	{
		nll.con = null;
		lnk.sendRequest(new GroupAddress(0, 0, 1), Priority.LOW, nsdu);
		Thread.sleep(200);
		assertNotNull(nll.con);
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#sendRequestWait(KNXAddress, tuwien.auto.calimero.Priority, byte[])}.
	 *
	 * @throws KNXTimeoutException
	 * @throws KNXLinkClosedException
	 */
	@Test
	public final void testSendRequestWait() throws KNXLinkClosedException, KNXTimeoutException
	{
		doSendWait(new byte[] { 0, (byte) (0x80 | 1) });
		doSendWait(new byte[] { 0, (byte) (0x80 | 0) });
		doSendWait(new byte[] { 0, (byte) (0x80 | 1) });
		doSendWait(new byte[] { 0, (byte) (0x80 | 0) });
	}

	private void doSendWait(final byte[] nsdu) throws KNXLinkClosedException, KNXTimeoutException
	{
		nll.con = null;
		lnk.sendRequestWait(new GroupAddress(0, 0, 1), Priority.LOW, nsdu);
		// even in router mode, we still get tunnel ind., so always wait
		try {
			Thread.sleep(200);
		}
		catch (final InterruptedException e) {}
		assertNotNull(nll.con);
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#send(tuwien.auto.calimero.cemi.CEMILData, boolean)}.
	 *
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testSend() throws KNXTimeoutException, KNXLinkClosedException
	{
		nll.con = null;
		lnk.send(frame2, false);
		try {
			Thread.sleep(150);
		}
		catch (final InterruptedException e) {}
		assertNotNull(nll.con);

		lnk.send(frame3, false);
		try {
			Thread.sleep(150);
		}
		catch (final InterruptedException e) {}
		lnk.send(frame3, false);
		try {
			Thread.sleep(150);
		}
		catch (final InterruptedException e) {}
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#getName()}.
	 */
	@Test
	public final void testGetName()
	{
		String n = lnk.getName();
		assertTrue(n.indexOf(Util.getSerialPortID()) > -1, Util.getSerialPortID());
		assertTrue(n.indexOf("link") > -1);
		lnk.close();
		n = lnk.getName();
		assertNotNull(n);
		assertTrue(n.indexOf("link") > -1);
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#isOpen()}.
	 */
	@Test
	public final void testIsOpen()
	{
		assertTrue(lnk.isOpen());
		lnk.close();
		assertFalse(lnk.isOpen());
	}

	/**
	 * Test method for {@link KNXNetworkLinkFT12#close()}.
	 *
	 * @throws InterruptedException on interrupted thread
	 * @throws KNXTimeoutException
	 */
	@Test
	public final void testClose() throws InterruptedException, KNXTimeoutException
	{
		System.out.println(lnk.toString());
		assertTrue(lnk.isOpen());
		lnk.close();
		System.out.println(lnk.toString());
		// time for link event notifier
		Thread.sleep(50);
		assertTrue(nll.closed);
		assertFalse(lnk.isOpen());
		lnk.close();
		try {
			lnk.send(frame, false);
			fail("we are closed");
		}
		catch (final KNXLinkClosedException e) {}
		try {
			lnk.send(frame, false);
			fail("we are closed");
		}
		catch (final KNXLinkClosedException e) {}
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

import java.util.Arrays;
import java.util.EnumSet;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAckTimeoutException;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.serial.FT12Connection;
import tuwien.auto.calimero.serial.KNXPortClosedException;
import tuwien.auto.calimero.serial.usb.HidReport;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.KnxTunnelEmi;
import tuwien.auto.calimero.serial.usb.UsbConnection;

final class BcuSwitcher
{
	// EMI1.x has PEI types 12, 16 or 20
	// EMI2.0 has PEI-type 10

	// EMI 1

	// Memory locations

	/**
	 * Bit field (bit set is active): Parity Bit (even) | DM | UE | serial PEI IF | app layer |
	 * Transport layer | Link layer | Prog mode
	 */
	static final int AddrSystemState = 0x60;

	// Ext Busmon mode to obtain 2 byte domain address: switch at address 0x0101 bit 3 to 0
	// Domain address shall be in range [0x01 .. 0xFF], 0x0 is system bcast
	static final int AddrBaseConfig = 0x0101;
	// PL domain address, allocates 2 bytes
	static final int AddrDomainAddress = 0x0102;

	static final int AddrExpectedPeiType = 0x0109;
	// start address of address table
	// address table realization type 1  (cf Vol 3/5/1), allocates 233 bytes
	static final int AddrStartAddressTable = 0x0116;

	// individual address, allocates 2 bytes
	static final int AddrIndividualAddress = 0x0117;

	// for system state
	enum OperationMode {
		Busmonitor(0x90),
		LinkLayer(0x12),
		TransportLayer(0x96),
		ApplicationLayer(0x1E),
		Reset(0xC0);

		OperationMode(final int mode) { this.mode = mode; };

		final int mode;

		static OperationMode of(final int mode)
		{
			for (final OperationMode v : EnumSet.allOf(OperationMode.class))
				if (v.mode == mode)
					return v;
			throw new KNXIllegalArgumentException("invalid operation mode 0x"
					+ Integer.toHexString(mode));
		}
	}

	// EMI 1 message codes

	private static final int getValue_req = 0x4c;
	private static final int getValue_con = 0x4b;
	private static final int setValue_req = 0x46;

	static boolean isEmi1GetValue(final int messageCode) {
		return messageCode == getValue_con;
	}

	private static final int frameOffsetData = 4;

	private static final int responseTimeout = 1000;
	private byte[] response;

	private final UsbConnection c;
	private final Logger logger;

	BcuSwitcher(final UsbConnection c, final Logger l)
	{
		this.c = c;
		logger = l;
		conn = null; // XXX
	}

	enum BcuMode {
		LinkLayer,
		Busmonitor,
		ExtBusmonitor
	}

	void enter(final BcuMode mode) throws KNXFormatException, KNXPortClosedException,
		KNXTimeoutException, InterruptedException
	{
		final KNXListener l = new KNXListener() {
			@Override
			public void frameReceived(final FrameEvent e) { setResponse(e.getFrameBytes()); }

			@Override
			public void connectionClosed(final CloseEvent e) {}
		};
		c.addConnectionListener(l);
		try {
			byte[] data = read(createGetValue(AddrExpectedPeiType, 1));
			logger.info("PEI type {}", data[0] & 0xff);
			data = read(createGetValue(AddrStartAddressTable, 1));
			logger.debug("Address Table location {}", DataUnitBuilder.toHex(data, ""));
			data = read(createGetValue(AddrSystemState, 1));
			logger.debug("Current operation mode {}", OperationMode.of(data[0] & 0xff));
			// set PEI type 1: ensure that the application will not be started
			writeVerify(AddrExpectedPeiType, new byte[] { 1 });
			// power line: set extended busmonitor to transmit domain address in .ind
			setExtBusmon(mode == BcuMode.ExtBusmonitor);
			// set active operation mode, link layer or busmonitor
			final OperationMode set = mode == BcuMode.LinkLayer ? OperationMode.LinkLayer : OperationMode.Busmonitor;
			writeVerify(AddrSystemState, new byte[] { (byte) set.mode });
			// set address table to 0
			writeVerify(AddrStartAddressTable, new byte[] { 0 });
			data = read(createGetValue(AddrIndividualAddress, 2));
			logger.info("KNX individual address " + new IndividualAddress(data));
		}
		finally {
			c.removeConnectionListener(l);
		}
	}

	// TODO reset actually takes a while, enforce a wait?
	void reset() throws KNXPortClosedException, KNXTimeoutException, InterruptedException
	{
		write(createSetValue(AddrSystemState, new byte[] { (byte) OperationMode.Reset.mode }));
	}

	static String print(final byte[] frame)
	{
		final StringBuilder sb = new StringBuilder();
		switch (frame[0] & 0xff) {
		case getValue_req:
			sb.append("PC_Get_Value.req");
			break;
		case getValue_con:
			sb.append("PC_Get_Value.con");
			break;
		case setValue_req:
			sb.append("PC_Set_Value.req");
			break;
		default:
			sb.append("unknown msg code");
		}
		sb.append(", length ").append(frame[1] & 0xff);
		final int address = ((frame[2] & 0xff) << 8) | (frame[3] & 0xff);
		sb.append(", address 0x").append(Integer.toHexString(address));

		sb.append(", data = ");
		for (int i = 4; i < frame.length; i++)
			sb.append(String.format("%02x", frame[i] & 0xff));
		return sb.toString();
	}

	private static byte[] createSetValue(final int address, final byte[] data)
	{
		// msg-code | length | address | data
		if (data.length > 15)
			throw new KNXIllegalArgumentException("data length exceeds maximum of 15 bytes");
		final byte[] frame = new byte[frameOffsetData + data.length];
		frame[0] = setValue_req;
		frame[1] = (byte) data.length;
		frame[2] = (byte) (address >> 8);
		frame[3] = (byte) address;
		for (int i = 0; i < data.length; i++)
			frame[i + frameOffsetData] = data[i];
		return frame;
	}

	private static byte[] createGetValue(final int address, final int length)
	{
		// msg-code | length | address
		final byte[] frame = new byte[frameOffsetData];
		frame[0] = getValue_req;
		frame[1] = (byte) length;
		frame[2] = (byte) (address >> 8);
		frame[3] = (byte) address;
		return frame;
	}

	private static byte[] dataOfGetValueCon(final byte[] frame) throws KNXFormatException
	{
		if (frame.length < frameOffsetData)
			throw new KNXFormatException("frame too short for Get-Value.con", frame.length);
		final int mc = frame[0] & 0xff;
		if (mc != getValue_con)
			throw new KNXFormatException("no Get-Value.con message", mc);
		final int length = frame[1] & 0xff;
		if (length + frameOffsetData != frame.length)
			throw new KNXFormatException("invalid length for frame size " + frame.length, length);
		final byte[] data = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = frame[frameOffsetData + i];
		}
		return data;
	}

	private void setExtBusmon(final boolean ext) throws KNXPortClosedException,
		KNXTimeoutException, KNXFormatException, InterruptedException
	{
		final byte[] data = read(createGetValue(AddrBaseConfig, 1));
		logger.debug("Base configuration flags {}", Integer.toBinaryString(data[0] & 0xff));
		int config = data[0] & 0xff;
		if (ext)
			config &= ~(1 << 3);
		else
			config |= (1 << 3);

		writeVerify(AddrBaseConfig, new byte[] { (byte) config });
	}

	private byte[] read(final byte[] frame) throws KNXPortClosedException, KNXTimeoutException,
		KNXFormatException, InterruptedException
	{
		write(frame);
		return dataOfGetValueCon(waitForResponse());
	}

	private static final long txInterframeSpacing = 30;
	private long tsLastTx;

	private void write(final byte[] frame) throws KNXPortClosedException, KNXTimeoutException,
		InterruptedException
	{
		final long now = System.currentTimeMillis();
		final long wait = txInterframeSpacing - now + tsLastTx;
		if (wait > 0) {
			System.out.println("enforce transmission interframe spacing, wait " + wait + " ms");
			Thread.sleep(wait);
		}
		tsLastTx = now;
		c.send(HidReport.create(KnxTunnelEmi.Emi1, frame).get(0), true);
	}

	private boolean writeVerify(final int address, final byte[] data)
		throws KNXPortClosedException, KNXTimeoutException, KNXFormatException,
		InterruptedException
	{
		write(createSetValue(address, data));
		final byte[] read = read(createGetValue(address, data.length));
		final boolean equal = Arrays.equals(data, read);
		if (!equal)
			logger.error("verify write failed for address " + Integer.toHexString(address) + ": "
					+ DataUnitBuilder.toHex(data, "") + " vs " + DataUnitBuilder.toHex(read, ""));
		return equal;
	}

	private synchronized byte[] waitForResponse() throws KNXTimeoutException, InterruptedException
	{
		long remaining = responseTimeout;
		final long end = System.currentTimeMillis() + remaining;

		while (remaining > 0) {
			if (response != null) {
				final byte[] r = response;
				response = null;
//				System.out.println("bcu switcher time remaining " + (remaining) + " ms");
				return r;
			}
			wait(remaining);
			remaining = end - System.currentTimeMillis();
		}
		throw new KNXTimeoutException("expected service confirmation msg code 0x"
				+ Integer.toHexString(getValue_con));
	}

	private synchronized void setResponse(final byte[] frame)
	{
		final int msgCode = frame[0] & 0xff;
		if (msgCode == getValue_con) {
			response = frame;
			notify();
		}
	}

	//
	// EMI 2 stuff
	//
	// TODO align methods with EMI 1

	// EMI 2 PEI switch
	private static final int peiSwitch_req = 0xA9;

	private final FT12Connection conn;

	BcuSwitcher(final FT12Connection conn)
	{
		this.conn = conn;
		c = null; // XXX
		logger = null;
	}

	void normalMode() throws KNXAckTimeoutException, KNXPortClosedException, InterruptedException
	{
		final byte[] switchNormal = { (byte) peiSwitch_req, 0x1E, 0x12, 0x34, 0x56, 0x78,
			(byte) 0x9A, };
		conn.send(switchNormal, true);
	}

	void linkLayerMode() throws KNXException
	{
		final byte[] switchLinkLayer = { (byte) peiSwitch_req, 0x00, 0x18, 0x34, 0x56, 0x78, 0x0A, };
		try {
			conn.send(switchLinkLayer, true);
		}
		catch (final InterruptedException e) {
			conn.close();
			Thread.currentThread().interrupt();
			throw new KNXLinkClosedException(e.getMessage());
		}
		catch (final KNXAckTimeoutException e) {
			conn.close();
			throw e;
		}
	}

	void enterBusmonitor() throws KNXAckTimeoutException, KNXPortClosedException,
		KNXLinkClosedException
	{
		try {
			final byte[] switchBusmon = { (byte) peiSwitch_req, (byte) 0x90, 0x18, 0x34, 0x56,
				0x78, 0x0A, };
			conn.send(switchBusmon, true);
		}
		catch (final InterruptedException e) {
			conn.close();
			Thread.currentThread().interrupt();
			throw new KNXLinkClosedException(e.getMessage());
		}
		catch (final KNXAckTimeoutException e) {
			conn.close();
			throw e;
		}
	}

	void leaveBusmonitor() throws KNXAckTimeoutException, KNXPortClosedException,
		InterruptedException
	{
		normalMode();
	}
}

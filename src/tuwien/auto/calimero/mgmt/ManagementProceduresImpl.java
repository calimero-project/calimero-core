/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2012 B. Malinowsky

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

package tuwien.auto.calimero.mgmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * An implementation of {@link ManagementProcedures}.
 * <p>
 * In general, invoked procedures should not be run concurrently on the same remote endpoint. As
 * precaution, this implementation will guard against such behavior by executing procedures
 * synchronized on the used management client instance if considered necessary ( {@ManagementClient
 * }). Note that this, although, is not sufficient to guarantee non-concurrent
 * execution of procedures on the same remote endpoint in general.
 * 
 * @author B. Malinowsky
 */
public class ManagementProceduresImpl implements ManagementProcedures
{
	private static final int DEVICE_OBJECT_INDEX = 0;

	// device memory address for programming mode
	private static final int memAddrProgMode = 0x60;
	private static final int defaultApduLength = 15;
	// transport layer connection disconnect timeout [ms]
	private static final int disconnectTimeout = 6000;

	// procedures should synchronize on mc (this also prevents unwanted modifications of
	// procedure timeouts set in mc)
	private final ManagementClient mc;
	private final boolean detachMgmtClient;
	private final TransportLayer tl;
	
	private final LogService logger = LogManager.getManager().getLogService("MgmtProc");

	private static final class TLListener implements TransportListener
	{
		private final Set devices;
		private final boolean routers;

		private TLListener(final Set scanResult, final boolean scanRouters)
		{
			devices = scanResult;
			routers = scanRouters;
		}

		public void broadcast(final FrameEvent e)
		{}

		public void dataConnected(final FrameEvent e)
		{}

		public void dataIndividual(final FrameEvent e)
		{}

		public void disconnected(final Destination d)
		{
			if (d.getDisconnectedBy() != Destination.REMOTE_ENDPOINT)
				return;
			final IndividualAddress addr = d.getAddress();
			if (routers && addr.getDevice() == 0)
				devices.add(addr);
			else if (!routers && addr.getDevice() != 0)
				devices.add(addr);
		}

		public void group(final FrameEvent e)
		{}

		public void detached(final DetachEvent e)
		{}

		public void linkClosed(final CloseEvent e)
		{}
	};

	/**
	 * Creates a new management procedures instance, using the supplied KNX network link.
	 * <p>
	 * 
	 * @param link the KNX network link, with link in open state
	 * @throws KNXLinkClosedException on closed {@link KNXNetworkLink}
	 */
	public ManagementProceduresImpl(final KNXNetworkLink link)
		throws KNXLinkClosedException
	{
		tl = new TransportLayerImpl(link);
		mc = new ManagementClientImpl(link, tl);
		detachMgmtClient = true;
	}

	/**
	 * Creates a new management procedures instance, using the supplied management client
	 * for application layer services.
	 * <p>
	 * 
	 * @param mgmtClient the management client, with a network link attached and in open
	 *        state
	 * @param transportLayer
	 */
	protected ManagementProceduresImpl(final ManagementClient mgmtClient,
		final TransportLayer transportLayer)
	{
		mc = mgmtClient;
		if (!mc.isOpen())
			throw new KNXIllegalStateException("management client not in open state");
		tl = transportLayer;
		detachMgmtClient = false;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#readAddress()
	 */
	public IndividualAddress[] readAddress() throws KNXException, InterruptedException
	{
		synchronized (mc) {
			final int oldTimeout = mc.getResponseTimeout();
			mc.setResponseTimeout(3);
			try {
				return mc.readAddress(false);
			}
			catch (final KNXTimeoutException e) {
				// Note: we differ from mgmt client behavior, which throws on no responses
				return new IndividualAddress[0];
			}
			finally {
				mc.setResponseTimeout(oldTimeout);
			}
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures
	 * #writeAddress(tuwien.auto.calimero.IndividualAddress)
	 */
	public boolean writeAddress(final IndividualAddress newAddress) throws KNXException,
		InterruptedException
	{
		final Destination dst = mc.createDestination(newAddress, true);
		boolean exists = false;
		try {
			mc.readDeviceDesc(dst, 0);
			exists = true;
		}
		catch (final KNXDisconnectException e) {
			// remote endpoint exists, but might not support CO mode, we proceed
		}
		catch (final KNXTimeoutException e) {
			// no remote endpoint answered, we proceed
		}
		finally {
			dst.destroy();
		}

		boolean setAddr = false;
		synchronized (mc) {
			final int oldTimeout = mc.getResponseTimeout();
			final Destination verify = mc.createDestination(newAddress, true);
			try {
				mc.setResponseTimeout(1);
				int count = 0;
				while (count != 1) {
					try {
						final IndividualAddress[] list = mc.readAddress(false);
						count = list.length;
						if (count == 1 && !list[0].equals(newAddress))
							setAddr = true;
					}
					catch (final KNXException e) {
						// a device with newAddress exists but is not in programming mode,
						// bail out
						if (exists) {
							logger.warn("device exists but is not in programming mode, " +
									"cancel writing address");
							return false;
						}
					}
					logger.info("KNX devices in programming mode: " + count);
				}
				if (!setAddr)
					return false;
				mc.writeAddress(newAddress);
				// if this throws, either programming failed, or its
				// probably some network configuration issue
				mc.readDeviceDesc(verify, 0);
				mc.restart(verify);
			}
			finally {
				verify.destroy();
				mc.setResponseTimeout(oldTimeout);
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#resetAddress()
	 */
	public void resetAddress() throws KNXException, InterruptedException
	{
		final IndividualAddress def = new IndividualAddress(0xffff);
		final Destination dst = mc.createDestination(def, true);
		try {
			while (true) {
				mc.writeAddress(def);
				mc.restart(dst);
				try {
					mc.readAddress(true);
				}
				catch (final KNXTimeoutException e) {
					// no more responses, we're done
					break;
				}
			}
		}
		finally {
			dst.destroy();
		}
	}
	
	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures
	 * #isAddressOccupied(tuwien.auto.calimero.IndividualAddress)
	 */
	public boolean isAddressOccupied(final IndividualAddress devAddr)
		throws KNXException, InterruptedException
	{
		final Destination dst = mc.createDestination(devAddr, true);
		try {
			mc.readDeviceDesc(dst, 0);
		}
		catch (final KNXTimeoutException e) {
			return false;
		}
		catch (final KNXDisconnectException e) {
			// remote disconnect: device with that address exists but does not support CO mode
			if (e.getDestination().getDisconnectedBy() != Destination.REMOTE_ENDPOINT)
				return false;
		}
		finally {
			dst.destroy();
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#readAddress(byte[])
	 */
	public IndividualAddress readAddress(final byte[] serialNo) throws KNXException,
		InterruptedException
	{
		return mc.readAddress(serialNo);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures
	 * #writeAddress(byte[], tuwien.auto.calimero.IndividualAddress)
	 */
	public boolean writeAddress(final byte[] serialNo, final IndividualAddress newAddress)
		throws KNXException, InterruptedException
	{
		mc.writeAddress(serialNo, newAddress);
		final IndividualAddress chk = mc.readAddress(serialNo);
		final boolean equals = chk.equals(newAddress);
		final String s = " on device [s/n] " + DataUnitBuilder.toHex(serialNo, "");
		if (equals)
			logger.info("wrote device address " + newAddress + s);
		else
			logger.error("write device address reported " + chk + ", not " + newAddress + s);
		return equals;
		// old code threw generic exception (with no return value),
		// don't think that's the way to go
		//if (!equals)
		//	throw new KNXException("Device address is " + chk + " instead of "
		//		+ newAddress);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#scanNetworkRouters()
	 */
	public IndividualAddress[] scanNetworkRouters() throws KNXTimeoutException,
		KNXLinkClosedException, InterruptedException
	{
		final List addresses = new ArrayList();
		for (int address = 0x0000; address <= 0xFF00; address += 0x0100)
			addresses.add(new IndividualAddress(address));
		return scanAddresses(addresses, true);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#scanNetworkDevices(int, int)
	 */
	public IndividualAddress[] scanNetworkDevices(final int area, final int line)
		throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		if (area < 0 || area > 0xf)
			throw new KNXIllegalArgumentException("area out of range [0..0xf]");
		if (line < 0 || line > 0xf)
			throw new KNXIllegalArgumentException("line out of range [0..0xf]");

		final List addresses = new ArrayList();
		for (int device = 0; device <= 0xff; ++device) {
			final IndividualAddress remote = new IndividualAddress(area, line, device);
			addresses.add(remote);
		}
		return scanAddresses(addresses, false);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#scanSerialNumbers(int)
	 */
	public List scanSerialNumbers(final int medium) throws KNXException, InterruptedException
	{
		final Destination dst = mc.createDestination(new IndividualAddress(0, medium, 0xff), true);
		synchronized (mc) {
			final int oldTimeout = mc.getResponseTimeout();
			try {
				mc.setResponseTimeout(7);
				return ((ManagementClientImpl) mc).readProperty2(dst, 0,
						PropertyAccess.PID.SERIAL_NUMBER, 1, 1);
			}
			catch (final KNXTimeoutException e) {
				// no response is fine
			}
			finally {
				mc.setResponseTimeout(oldTimeout);
				dst.destroy();
			}
		}
		return Collections.EMPTY_LIST;
	}
	
	/*
	 * (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#setProgrammingMode
	 * (tuwien.auto.calimero.IndividualAddress, boolean)
	 */
	public void setProgrammingMode(final IndividualAddress device,
		final boolean programming) throws KNXException, InterruptedException
	{
		// ??? there also exists a KNX property for this, might query that property first

		// conn.oriented
		final Destination d = tl.createDestination(device, true);
		// read from memory where device keeps programming mode
		final byte[] mem = mc.readMemory(d, memAddrProgMode, 1);
		// store lowest 7 bits
		int set = mem[0] & 0x7f;
		// programming mode is at bit 0
		set = programming ? set | 0x1 : set & 0xfe;
		// update to even parity at bit 7 (MSB)
		set = isOddParity(set) ? set | 0x80 : set;
		mem[0] = (byte) set;
		mc.writeMemory(d, memAddrProgMode, mem);
	}

	/*
	 * (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#writeMemory
	 * (tuwien.auto.calimero.IndividualAddress, long, byte[], boolean, boolean)
	 */
	public void writeMemory(final IndividualAddress device, final long startAddress,
		final byte[] data, final boolean verifyWrite, final boolean verifyByServer)
		throws KNXException, InterruptedException
	{
		if (startAddress < 0 || startAddress > 0xffffffffL)
			throw new KNXIllegalArgumentException("start address is no 32 Bit address");

		// either verifyWrite or verifyByServer, not both applicable
		if (verifyWrite && verifyByServer)
			throw new KNXIllegalArgumentException(
				"verify write and verify by server not both applicable");

		final Destination d = tl.createDestination(device, true, false, verifyByServer);
		// if automatic server verification is requested, turn verify flag on
		if (verifyByServer) {
			// reading description checks whether property exists
			/* final byte[] desc = */mc.readPropertyDesc(d, DEVICE_OBJECT_INDEX,
				PropertyAccess.PID.DEVICE_CONTROL, 0);
			final byte[] ctrl = mc.readProperty(d, DEVICE_OBJECT_INDEX,
				PropertyAccess.PID.DEVICE_CONTROL, 1, 1);
			// bit 2 is set for automatic management server verification (responds on
			// memory write with a response containing the written data)
			ctrl[0] |= 0x04;
			mc.writeProperty(d, DEVICE_OBJECT_INDEX, PropertyAccess.PID.DEVICE_CONTROL,
				1, 1, ctrl);
		}

		// create structure to write
		/*
		 Byte structure of data transmitted in ASDUs:
		 | 0    | 1     | 2 3 4 5            | 6 7 8 9      |10 11 12 13 |14..31|
		 | code | flags | dataBlockStartAddr | startAddress | endAddress | data |
		 | 0x20 | ...   | ...                | ...          | ...        | ...  |
		*/
		final int dataBlockStartAddress = 0;
		// int flags = dataBlockStartAddress == 0 ? 0x01 : 0;
		int flags = 0x01;
		flags |= verifyWrite ? 0x02 : 0;
		final long endAddress = startAddress + data.length;

		final int offset = 14;
		final byte[] write = new byte[offset + data.length];
		write[0] = 0x20;
		write[1] = (byte) flags;
		setAddress(write, 2, dataBlockStartAddress);
		setAddress(write, 6, startAddress);
		setAddress(write, 10, endAddress);
		for (int i = 0; i < data.length; ++i)
			write[offset + i] = data[i];

		// write memory in chunks matching the maximum asdu length of the device
		final int asduLength = readMaxAsduLength(d);
		for (int i = 0; i < write.length; i += asduLength) {
			final byte[] range = DataUnitBuilder.copyOfRange(write, i, i + asduLength);

			// on server verification, our mgmt client will already do the response
			// value comparison
			mc.writeMemory(d, (int) startAddress + i, range);

			// on manual write verification, we explicitly read back memory
			if (verifyWrite) {
				final byte[] read = mc.readMemory(d, (int) startAddress + i, range.length);
				if (!Arrays.equals(read, range))
					throw new KNXRemoteException("verify failed (memory data differs)");
			}
		}
	}

	public byte[] readMemory(final IndividualAddress device, final long startAddress,
		final int bytes) throws KNXException, InterruptedException
	{
		if (startAddress < 0 || startAddress > 0xffffffffL)
			throw new KNXIllegalArgumentException("start address is no 32 Bit address");
		if (bytes < 0)
			throw new KNXIllegalArgumentException("bytes to read require a positive number");
		// sanity check, at least emit a warning
		// if (bytes > 4096)
		// logger.warn("reading over 4K of device memory "
		// + "(hope you know what you are doing)");

		final Destination d = tl.createDestination(device, true);

		final byte[] read = new byte[bytes];
		final int asduLength = readMaxAsduLength(d);
		for (int i = 0; i < read.length; i += asduLength) {
			final int size = i + asduLength <= read.length ? asduLength : read.length - i;
			final byte[] range = mc.readMemory(d, (int) startAddress + i, size);
			System.arraycopy(range, 0, read, i, range.length);
		}
		return read;
	}

	/*
	 * (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementProcedures#detach()
	 */
	public void detach()
	{
		if (detachMgmtClient)
			mc.detach();
	}

	private IndividualAddress[] scanAddresses(final List addresses, final boolean routers)
		throws KNXTimeoutException, KNXLinkClosedException, InterruptedException
	{
		final Set devices = new HashSet();
		final TransportListener tll = new TLListener(devices, routers);
		tl.addTransportListener(tll);

		final List destinations = new ArrayList();
		try {
			for (final Iterator i = addresses.iterator(); i.hasNext();) {
				final IndividualAddress remote = (IndividualAddress) i.next();
				final Destination d = tl.createDestination(remote, true);
				destinations.add(d);
				tl.connect(d);
				// increased from 100 (the default) to minimize chance of overflow over FT1.2
				Thread.sleep(115);
			}
			waitFor(disconnectTimeout);
		}
		finally {
			tl.removeTransportListener(tll);
			for (final Iterator i = destinations.iterator(); i.hasNext();) {
				final Destination d = (Destination) i.next();
				tl.destroyDestination(d);
			}
		}
		final IndividualAddress[] array = (IndividualAddress[]) devices
				.toArray(new IndividualAddress[devices.size()]);
		return array;
	}

	private int readMaxAsduLength(final Destination d) throws InterruptedException
	{
		// asdu is 3 bytes shorter than apdu
		try {
			final byte[] data = mc.readProperty(d, DEVICE_OBJECT_INDEX,
				PropertyAccess.PID.MAX_APDULENGTH, 1, 1);
			return toUnsigned(data) - 3;
		}
		catch (final KNXException e) {
			return defaultApduLength - 3;
		}
	}

	private void waitFor(final int ms) throws InterruptedException
	{
		Thread.sleep(ms);
	}

	private static int toUnsigned(final byte[] data)
	{
		return ((data[0] & 0xff) << 8) | (data[1] & 0xff);
	}

	private static boolean isOddParity(final int bite)
	{
		int parity = bite ^ (bite >> 4);
		parity ^= parity >> 2;
		parity ^= parity >> 1;
		return (parity & 0x1) != 0;
	}

	private static void setAddress(final byte[] data, final int from, final long address)
	{
		for (int i = 0; i < 4; ++i)
			data[from + i] = (byte) (address >> (3 - i) & 0xff);
	}
}

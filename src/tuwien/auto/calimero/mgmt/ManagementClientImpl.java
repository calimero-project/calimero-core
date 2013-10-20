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

package tuwien.auto.calimero.mgmt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXInvalidResponseException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Implementation of management client.
 * <p>
 * Uses {@link TransportLayer} internally for communication. <br>
 * All management service methods invoked after a detach of the network link are allowed
 * to throw {@link KNXIllegalStateException}.
 * 
 * @author B. Malinowsky
 */
public class ManagementClientImpl implements ManagementClient
{
	private static final int ADC_READ = 0x0180;
	private static final int ADC_RESPONSE = 0x01C0;

	private static final int AUTHORIZE_READ = 0x03D1;
	private static final int AUTHORIZE_RESPONSE = 0x03D2;

	private static final int DOA_WRITE = 0x3E0;
	private static final int DOA_READ = 0x3E1;
	private static final int DOA_RESPONSE = 0x3E2;
	private static final int DOA_SELECTIVE_READ = 0x3E3;

	private static final int IND_ADDR_READ = 0x0100;
	private static final int IND_ADDR_RESPONSE = 0x0140;
	private static final int IND_ADDR_WRITE = 0xC0;

	private static final int IND_ADDR_SN_READ = 0x03DC;
	private static final int IND_ADDR_SN_RESPONSE = 0x03DD;
	private static final int IND_ADDR_SN_WRITE = 0x03DE;

	private static final int DEVICE_DESC_READ = 0x300;
	private static final int DEVICE_DESC_RESPONSE = 0x340;

	private static final int KEY_WRITE = 0x03D3;
	private static final int KEY_RESPONSE = 0x03D4;

	private static final int MEMORY_READ = 0x0200;
	private static final int MEMORY_RESPONSE = 0x0240;
	private static final int MEMORY_WRITE = 0x0280;

	private static final int PROPERTY_DESC_READ = 0x03D8;
	private static final int PROPERTY_DESC_RESPONSE = 0x03D9;

	private static final int PROPERTY_READ = 0x03D5;
	private static final int PROPERTY_RESPONSE = 0x03D6;
	private static final int PROPERTY_WRITE = 0x03D7;

	// serves as both req and res
	private static final int RESTART = 0x0380;

	private class TLListener implements TransportListener
	{
		TLListener()
		{}

		public void broadcast(final FrameEvent e)
		{
			checkResponse(e);
		}

		public void dataConnected(final FrameEvent e)
		{
			checkResponse(e);
		}

		public void dataIndividual(final FrameEvent e)
		{
			checkResponse(e);
		}

		public void disconnected(final Destination d)
		{}

		public void group(final FrameEvent e)
		{}

		public void detached(final DetachEvent e)
		{}

		public void linkClosed(final CloseEvent e)
		{
			logger.info("attached link was closed");
		}

		private void checkResponse(final FrameEvent e)
		{
			if (svcResponse != 0) {
				final byte[] tpdu = e.getFrame().getPayload();
				//logger.trace("check response: " + e.getFrame());
				if (DataUnitBuilder.getAPDUService(tpdu) == svcResponse)
					synchronized (indications) {
						indications.add(e);
						indications.notify();
					}
			}
		}
	};

	private final TransportLayer tl;
	private final TLListener tlListener = new TLListener();
	private volatile Priority priority = Priority.LOW;
	private volatile int responseTimeout = 5000; // [ms]
	private final List indications = new LinkedList();
	private volatile int svcResponse;
	private volatile boolean detached;
	private final LogService logger;

	/**
	 * Creates a new management client attached to the supplied KNX network link.
	 * <p>
	 * The log service used by this management client is named "MC " +
	 * <code>link.getName()</code>.
	 * 
	 * @param link network link used for communication with a KNX network
	 * @throws KNXLinkClosedException if the network link is closed
	 */
	public ManagementClientImpl(final KNXNetworkLink link) throws KNXLinkClosedException
	{
		this(link, new TransportLayerImpl(link));
	}

	ManagementClientImpl(final KNXNetworkLink link, final TransportLayer transportLayer)
	{
		tl = transportLayer;
		tl.addTransportListener(tlListener);
		logger = LogManager.getManager().getLogService("MC " + link.getName());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#setResponseTimeout(int)
	 */
	public void setResponseTimeout(final int timeout)
	{
		if (timeout <= 0)
			throw new KNXIllegalArgumentException("timeout not > 0");
		responseTimeout = timeout * 1000;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#getResponseTimeout()
	 */
	public int getResponseTimeout()
	{
		return responseTimeout / 1000;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#setPriority
	 * (tuwien.auto.calimero.Priority)
	 */
	public void setPriority(final Priority p)
	{
		priority = p;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#getPriority()
	 */
	public Priority getPriority()
	{
		return priority;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#createDestination
	 * (tuwien.auto.calimero.IndividualAddress, boolean)
	 */
	public Destination createDestination(final IndividualAddress remote,
		final boolean connectionOriented)
	{
		return tl.createDestination(remote, connectionOriented);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#createDestination
	 * (tuwien.auto.calimero.IndividualAddress, boolean, boolean, boolean)
	 */
	public Destination createDestination(final IndividualAddress remote,
		final boolean connectionOriented, final boolean keepAlive,
		final boolean verifyMode)
	{
		return tl.createDestination(remote, connectionOriented, keepAlive, verifyMode);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#writeAddress
	 * (tuwien.auto.calimero.IndividualAddress)
	 */
	public void writeAddress(final IndividualAddress newAddress)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		tl.broadcast(false, Priority.SYSTEM,
				DataUnitBuilder.createAPDU(IND_ADDR_WRITE, newAddress.toByteArray()));
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readAddress(boolean)
	 */
	public synchronized IndividualAddress[] readAddress(final boolean oneAddressOnly)
		throws KNXTimeoutException, KNXRemoteException, KNXLinkClosedException,
		InterruptedException
	{
		final List l = new ArrayList();
		try {
			svcResponse = IND_ADDR_RESPONSE;
			tl.broadcast(false, Priority.SYSTEM,
					DataUnitBuilder.createCompactAPDU(IND_ADDR_READ, null));
			long wait = responseTimeout;
			final long end = System.currentTimeMillis() + wait;
			while (wait > 0) {
				l.add(new IndividualAddress(waitForResponse(null, 0, 0, wait)));
				if (oneAddressOnly)
					break;
				wait = end - System.currentTimeMillis();
			}
		}
		catch (final KNXTimeoutException e) {
			if (l.isEmpty())
				throw e;
		}
		finally {
			svcResponse = 0;
		}
		return (IndividualAddress[]) l.toArray(new IndividualAddress[l.size()]);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#writeAddress
	 * (byte[], tuwien.auto.calimero.IndividualAddress)
	 */
	public void writeAddress(final byte[] serialNo, final IndividualAddress newAddress)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		if (serialNo.length != 6)
			throw new KNXIllegalArgumentException("length of serial number not 6 bytes");
		final byte[] asdu = new byte[12];
		for (int i = 0; i < 6; ++i)
			asdu[i] = serialNo[i];
		asdu[6] = (byte) (newAddress.getRawAddress() >>> 8);
		asdu[7] = (byte) newAddress.getRawAddress();
		tl.broadcast(false, Priority.SYSTEM, DataUnitBuilder.createAPDU(IND_ADDR_SN_WRITE, asdu));
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readAddress(byte[])
	 */
	public synchronized IndividualAddress readAddress(final byte[] serialNo)
		throws KNXTimeoutException, KNXRemoteException, KNXLinkClosedException,
		InterruptedException
	{
		if (serialNo.length != 6)
			throw new KNXIllegalArgumentException("length of serial number not 6 bytes");
		try {
			svcResponse = IND_ADDR_SN_RESPONSE;
			tl.broadcast(false, Priority.SYSTEM,
					DataUnitBuilder.createAPDU(IND_ADDR_SN_READ, serialNo));
			return new IndividualAddress(waitForResponse(null, 10, 10, responseTimeout));
		}
		finally {
			svcResponse = 0;
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#writeDomainAddress(byte[])
	 */
	public void writeDomainAddress(final byte[] domain) throws KNXTimeoutException,
		KNXLinkClosedException
	{
		if (domain.length != 2 && domain.length != 6)
			throw new KNXIllegalArgumentException("invalid length of domain address");
		tl.broadcast(true, priority, DataUnitBuilder.createAPDU(DOA_WRITE, domain));
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readDomainAddress(boolean)
	 */
	public synchronized List readDomainAddress(final boolean oneDomainOnly)
		throws KNXLinkClosedException, KNXInvalidResponseException, KNXTimeoutException,
		InterruptedException
	{
		// we allow 6 bytes ASDU for RF domains
		return makeDOAs(readBroadcast(priority, DataUnitBuilder.createCompactAPDU(DOA_READ, null),
				DOA_RESPONSE, 6, 6, oneDomainOnly));
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readDomainAddress
	 * (byte[], tuwien.auto.calimero.IndividualAddress, int)
	 */
	public List readDomainAddress(final byte[] domain, final IndividualAddress start,
		final int range) throws KNXInvalidResponseException, KNXLinkClosedException,
		KNXTimeoutException, InterruptedException
	{
		if (domain.length != 2)
			throw new KNXIllegalArgumentException("length of domain address not 2 bytes");
		if (range < 0 || range > 255)
			throw new KNXIllegalArgumentException("range out of range [0..255]");
		final byte[] addr = start.toByteArray();
		return makeDOAs(readBroadcast(priority,
				DataUnitBuilder.createAPDU(DOA_SELECTIVE_READ, new byte[] { domain[0], domain[1],
					addr[0], addr[1], (byte) range }), DOA_RESPONSE, 2, 2, false));
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readDeviceDesc
	 * (tuwien.auto.calimero.mgmt.Destination, int)
	 */
	public byte[] readDeviceDesc(final Destination dst, final int descType)
		throws KNXInvalidResponseException, KNXDisconnectException, KNXTimeoutException,
		KNXLinkClosedException, InterruptedException
	{
		if (descType < 0 || descType > 63)
			throw new KNXIllegalArgumentException("descriptor type out of range [0..63]");
		final byte[] apdu = sendWait2(dst, priority, DataUnitBuilder.createCompactAPDU(
				DEVICE_DESC_READ, new byte[] { (byte) descType }), DEVICE_DESC_RESPONSE, 2, 14);
		final byte[] dd = new byte[apdu.length - 2];
		for (int i = 0; i < apdu.length - 2; ++i)
			dd[i] = apdu[2 + i];
		return dd;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#restart
	 * (tuwien.auto.calimero.mgmt.Destination)
	 */
	public void restart(final Destination dst) throws KNXTimeoutException, KNXLinkClosedException
	{
		try {
			restart(true, dst, 0, 0);
		}
		catch (final KNXRemoteException ignore) { }
		catch (final KNXDisconnectException ignore) { }
		catch (final InterruptedException ignore) { }
	}

	public int restart(final Destination dst, final int eraseCode, final int channel)
		throws KNXTimeoutException, KNXRemoteException, KNXLinkClosedException,
		KNXDisconnectException, InterruptedException
	{
		return restart(false, dst, eraseCode, channel);
	}
	
	// for erase codes 1,3,4 the channel should be 0
	private int restart(final boolean basic, final Destination dst, final int eraseCode,
		final int channel) throws KNXTimeoutException, KNXRemoteException, KNXLinkClosedException,
		KNXDisconnectException, InterruptedException
	{
		int time = 0;
		if (basic) {
			send(dst, priority, DataUnitBuilder.createCompactAPDU(RESTART, null), 0);
		}
		else {
			final byte[] sdu = new byte[] { 0x01, (byte) eraseCode, (byte) channel, };
			final byte[] send = DataUnitBuilder.createCompactAPDU(RESTART, sdu);
			final byte[] apdu = sendWait2(dst, priority, send, RESTART, 3, 3);
			// check we get a restart response
			if ((apdu[1] & 0x32) == 0)
				throw new KNXInvalidResponseException("restart response bit not set");
			// defined error codes: 0,1,2,3
			final String[] codes = new String[] { "Success", "Access Denied",
				"Unsupported Erase Code", "Invalid Channel Number", "Unknown Error" };
			final int error = Math.min(apdu[2] & 0xff, 4) ;
			if (error > 0)
				throw new KNXRemoteException("master reset: " + codes[error]);
			time = ((apdu[3] & 0xff) << 8) | (apdu[4] & 0xff);
		}
		
		if (dst.isConnectionOriented()) {
			// a remote endpoint is allowed to not send a TL disconnect before restart, but
			// a TL disconnect timeout shall not be treated as protocol error
			final Object lock = new Object();
			final TransportListener l = new TLListener()
			{
				public void disconnected(final Destination d)
				{
					if (d.equals(dst))
						synchronized (lock) {
							lock.notify();
						}
				};
			};
			tl.addTransportListener(l);
			try {
				synchronized (lock) {
					while (dst.getState() != Destination.DISCONNECTED)
						lock.wait();
				}
			}
			finally {
				tl.removeTransportListener(l);
			}
			// always force a disconnect from our side
			tl.disconnect(dst);
		}
		return time;
	}
	
	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int)
	 */
	public byte[] readProperty(final Destination dst, final int objIndex, final int propertyId,
		final int start, final int elements) throws KNXTimeoutException, KNXRemoteException,
		KNXDisconnectException, KNXLinkClosedException, InterruptedException
	{
		final List l = readProperty(dst, objIndex, propertyId, start, elements, true);
		if (l.isEmpty())
			throw new KNXTimeoutException("timeout waiting for property response");
		return (byte[]) l.get(0);
	}

	// as readProperty, but collects all responses until response timeout is reached
	List readProperty2(final Destination dst, final int objIndex, final int propertyId,
		final int start, final int elements) throws KNXTimeoutException, KNXRemoteException,
		KNXDisconnectException, KNXLinkClosedException, InterruptedException
	{
		return readProperty(dst, objIndex, propertyId, start, elements, false);
	}
	
	private List readProperty(final Destination dst, final int objIndex, final int propertyId,
		final int start, final int elements, final boolean oneResponseOnly)
		throws KNXTimeoutException, KNXRemoteException, KNXDisconnectException,
		KNXLinkClosedException, InterruptedException
	{
		if (objIndex < 0 || objIndex > 255 || propertyId < 0 || propertyId > 255
			|| start < 0 || start > 0xFFF || elements < 0 || elements > 15)
			throw new KNXIllegalArgumentException("argument value out of range");
		final byte[] asdu = new byte[4];
		asdu[0] = (byte) objIndex;
		asdu[1] = (byte) propertyId;
		asdu[2] = (byte) ((elements << 4) | ((start >>> 8) & 0xF));
		asdu[3] = (byte) start;
		
		final List responses;
		synchronized (this) {
			try {
				send(dst, priority, DataUnitBuilder.createAPDU(PROPERTY_READ, asdu),
						PROPERTY_RESPONSE);
				// if we are waiting for several responses, we pass no from address to accept
				// messages from any sender
				responses = waitForResponses(oneResponseOnly ? dst.getAddress() : null, priority,
						4, 14, oneResponseOnly);
			}
			finally {
				svcResponse = 0;
			}
		}
		final List ret = new ArrayList();
		for (final Iterator i = responses.iterator(); i.hasNext();) {
			final byte[] apdu = (byte[]) i.next();
			try {
				ret.add(extractPropertyElements(apdu, elements));
			}
			catch (final KNXRemoteException e) {
				logger.warn("skip invalid property read response "
						+ DataUnitBuilder.toHex(apdu, ""), e);
			}
		}
		return ret;
	}
	
	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#writeProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int, byte[])
	 */
	public void writeProperty(final Destination dst, final int objIndex,
		final int propertyId, final int start, final int elements, final byte[] data)
		throws KNXTimeoutException, KNXRemoteException, KNXDisconnectException,
		KNXLinkClosedException, InterruptedException
	{
		if (objIndex < 0 || objIndex > 255 || propertyId < 0 || propertyId > 255 || start < 0
				|| start > 0xFFF || data.length == 0 || elements < 0 || elements > 15)
			throw new KNXIllegalArgumentException("argument value out of range");
		final byte[] asdu = new byte[4 + data.length];
		asdu[0] = (byte) objIndex;
		asdu[1] = (byte) propertyId;
		asdu[2] = (byte) ((elements << 4) | ((start >>> 8) & 0xF));
		asdu[3] = (byte) start;
		for (int i = 0; i < data.length; ++i)
			asdu[4 + i] = data[i];
		final byte[] send = DataUnitBuilder.createAPDU(PROPERTY_WRITE, asdu);
		final byte[] apdu = sendWait2(dst, priority, send, PROPERTY_RESPONSE, 4, 14);
		// if number of elements is 0, remote app had problems
		final int elems = (apdu[4] & 0xFF) >> 4;
		if (elems == 0)
			throw new KNXRemoteException("property write failed/forbidden");
		if (elems != elements)
			throw new KNXInvalidResponseException("number of elements differ");
		if (data.length != apdu.length - 6)
			throw new KNXInvalidResponseException("data lengths differ, bytes: "
				+ data.length + " written, " + (apdu.length - 6) + " response");
		// explicitly read back written properties
		for (int i = 4; i < asdu.length; ++i)
			if (apdu[2 + i] != asdu[i])
				throw new KNXRemoteException("read back failed (erroneous property data)");
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readPropertyDesc
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int)
	 */
	public byte[] readPropertyDesc(final Destination dst, final int objIndex,
		final int propertyId, final int propIndex) throws KNXTimeoutException,
		KNXRemoteException, KNXDisconnectException, KNXLinkClosedException,
		InterruptedException
	{
		if (objIndex < 0 || objIndex > 255 || propertyId < 0 || propertyId > 255
			|| propIndex < 0 || propIndex > 255)
			throw new KNXIllegalArgumentException("argument value out of range");
		final byte[] send = DataUnitBuilder.createAPDU(PROPERTY_DESC_READ, new byte[] {
			(byte) objIndex, (byte) propertyId, (byte) (propertyId == 0 ? propIndex : 0) });
		final byte[] apdu = sendWait2(dst, priority, send, PROPERTY_DESC_RESPONSE, 7, 7);
		// max_nr_elem field is a 4bit exponent + 12bit unsigned
		// on problem this field is 0
		if (apdu[6] == 0 && apdu[7] == 0)
			throw new KNXRemoteException("got no property description (object non-existant?)");
		return new byte[] { apdu[2], apdu[3], apdu[4], apdu[5], apdu[6], apdu[7], apdu[8] };
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readADC
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)
	 */
	public int readADC(final Destination dst, final int channel, final int repeat)
		throws KNXTimeoutException, KNXDisconnectException, KNXRemoteException,
		KNXLinkClosedException, InterruptedException
	{
		if (channel < 0 || channel > 63 || repeat < 0 || repeat > 255)
			throw new KNXIllegalArgumentException("ADC arguments out of range");
		if (dst.isConnectionOriented())
			tl.connect(dst);
		else
			logger.error("doing read ADC in connectionless mode, " + dst.toString());
		final byte[] apdu = sendWait(dst, priority, DataUnitBuilder.createCompactAPDU(ADC_READ,
				new byte[] { (byte) channel, (byte) repeat }), ADC_RESPONSE, 3, 3);
		if (apdu[2] == 0)
			throw new KNXRemoteException("error reading value of A/D converter");
		return ((apdu[3] & 0xff) << 8) | apdu[4] & 0xff;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#readMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)
	 */
	public byte[] readMemory(final Destination dst, final int startAddr, final int bytes)
		throws KNXTimeoutException, KNXDisconnectException, KNXRemoteException,
		KNXLinkClosedException, InterruptedException
	{
		if (startAddr < 0 || startAddr > 0xFFFF || bytes < 1 || bytes > 63)
			throw new KNXIllegalArgumentException("argument value out of range");
		if (dst.isConnectionOriented())
			tl.connect(dst);
		else
			logger.error("doing read memory in connectionless mode, " + dst.toString());
		final byte[] apdu = sendWait(dst, priority,
			DataUnitBuilder.createCompactAPDU(MEMORY_READ, new byte[] { (byte) bytes,
				(byte) (startAddr >>> 8), (byte) startAddr }), MEMORY_RESPONSE, 2, 65);
		int no = apdu[1] & 0x3F;
		if (no == 0)
			throw new KNXRemoteException("could not read memory from 0x"
					+ Integer.toHexString(startAddr));
		final byte[] mem = new byte[no];
		while (--no >= 0)
			mem[no] = apdu[4 + no];
		return mem;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#writeMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, byte[])
	 */
	public void writeMemory(final Destination dst, final int startAddr, final byte[] data)
		throws KNXDisconnectException, KNXTimeoutException, KNXRemoteException,
		KNXLinkClosedException, InterruptedException
	{
		if (startAddr < 0 || startAddr > 0xFFFF || data.length == 0 || data.length > 63)
			throw new KNXIllegalArgumentException("argument value out of range");
		final byte[] asdu = new byte[data.length + 3];
		asdu[0] = (byte) data.length;
		asdu[1] = (byte) (startAddr >> 8);
		asdu[2] = (byte) startAddr;
		for (int i = 0; i < data.length; ++i)
			asdu[3 + i] = data[i];
		if (dst.isConnectionOriented())
			tl.connect(dst);
		else
			logger.error("doing write memory in connectionless mode, " + dst.toString());
		final byte[] send = DataUnitBuilder.createCompactAPDU(MEMORY_WRITE, asdu);
		if (dst.isVerifyMode()) {
			// explicitly read back data
			final byte[] apdu = sendWait(dst, priority, send, MEMORY_RESPONSE, 2, 65);
			if ((apdu[1] & 0x3f) == 0)
				throw new KNXRemoteException("remote app. could not write memory");
			if (apdu.length - 4 != data.length)
				throw new KNXInvalidResponseException("number of memory bytes differ");
			for (int i = 4; i < apdu.length; ++i)
				if (apdu[i] != asdu[i - 1])
					throw new KNXRemoteException("verify failed (erroneous memory data)");
		}
		else
			tl.sendData(dst, priority, send);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#authorize
	 * (tuwien.auto.calimero.mgmt.Destination, byte[])
	 */
	public int authorize(final Destination dst, final byte[] key)
		throws KNXDisconnectException, KNXTimeoutException, KNXRemoteException,
		KNXLinkClosedException, InterruptedException
	{
		if (key.length != 4)
			throw new KNXIllegalArgumentException("length of authorize key not 4 bytes");
		if (dst.isConnectionOriented())
			tl.connect(dst);
		else
			logger.error("doing authorize in connectionless mode, " + dst.toString());
		final byte[] asdu = new byte[] { 0, key[0], key[1], key[2], key[3] };
		final byte[] apdu = sendWait(dst, priority,
			DataUnitBuilder.createAPDU(AUTHORIZE_READ, asdu), AUTHORIZE_RESPONSE, 1, 1);
		final int level = apdu[2] & 0xff;
		if (level > 15)
			throw new KNXInvalidResponseException("authorization level out of range [0..15]");
		return level;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#writeKey
	 * (tuwien.auto.calimero.mgmt.Destination, int, byte[])
	 */
	public void writeKey(final Destination dst, final int level, final byte[] key)
		throws KNXTimeoutException, KNXDisconnectException, KNXRemoteException,
		KNXLinkClosedException, InterruptedException
	{
		// level 255 is free access
		if (level < 0 || level > 254 || key.length != 4)
			throw new KNXIllegalArgumentException("level out of range or key length not 4 bytes");
		if (dst.isConnectionOriented())
			tl.connect(dst);
		else
			logger.error("doing write key in connectionless mode, " + dst.toString());
		final byte[] apdu = sendWait(dst, priority,
			DataUnitBuilder.createAPDU(KEY_WRITE, new byte[] { (byte) level, key[0],
				key[1], key[2], key[3] }), KEY_RESPONSE, 1, 1);
		if ((apdu[1] & 0xFF) == 0xFF)
			throw new KNXRemoteException("access denied: current access level > write level");
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#isOpen()
	 */
	public boolean isOpen()
	{
		return !detached;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.ManagementClient#detach()
	 */
	public KNXNetworkLink detach()
	{
		final KNXNetworkLink lnk = tl.detach();
		if (lnk != null) {
			logger.info("detached from " + lnk.getName());
			LogManager.getManager().removeLogService(logger.getName());
		}
		detached = true;
		return lnk;
	}

	// helper which sets the expected svc response, and sends in CO or CL mode
	private void send(final Destination d, final Priority p, final byte[] apdu, final int response)
		throws KNXTimeoutException, KNXDisconnectException, KNXLinkClosedException
	{
		svcResponse = response;
		if (d.isConnectionOriented()) {
			tl.connect(d);
			tl.sendData(d, p, apdu);
		}
		else
			tl.sendData(d.getAddress(), p, apdu);
	}
	
	private synchronized byte[] sendWait(final Destination d, final Priority p,
		final byte[] apdu, final int response, final int minAsduLen, final int maxAsduLen)
		throws KNXDisconnectException, KNXTimeoutException, KNXInvalidResponseException,
		KNXLinkClosedException, InterruptedException
	{
		try {
			svcResponse = response;
			tl.sendData(d, p, apdu);
			return waitForResponse(d.getAddress(), minAsduLen, maxAsduLen, responseTimeout);
		}
		finally {
			svcResponse = 0;
		}
	}

	private synchronized byte[] sendWait2(final Destination d, final Priority p,
		final byte[] apdu, final int response, final int minAsduLen, final int maxAsduLen)
		throws KNXDisconnectException, KNXTimeoutException, KNXInvalidResponseException,
		KNXLinkClosedException, InterruptedException
	{
		try {
			send(d, p, apdu, response);
			return waitForResponse(d.getAddress(), minAsduLen, maxAsduLen, responseTimeout);
		}
		finally {
			svcResponse = 0;
		}
	}

	// timeout in milliseconds
	// min + max ASDU len are *not* including any field that contains ACPI
	private byte[] waitForResponse(final IndividualAddress from, final int minAsduLen,
		final int maxAsduLen, final long timeout) throws KNXInvalidResponseException,
		KNXTimeoutException, InterruptedException
	{
		long remaining = timeout;
		final long end = System.currentTimeMillis() + remaining;
		synchronized (indications) {
			while (remaining > 0) {
				while (indications.size() > 0) {
					final CEMI frame = ((FrameEvent) indications.remove(0)).getFrame();
					final byte[] apdu = frame.getPayload();
					if (svcResponse != DataUnitBuilder.getAPDUService(apdu))
						continue;
					// broadcasts set parameter from to null; we then accept every sender address
					if (from != null) {
						if (!((CEMILData) frame).getSource().equals(from))
							continue;
					}
					if (apdu.length < minAsduLen + 2 || apdu.length > maxAsduLen + 2) {
						final String s = "invalid ASDU response length " + (apdu.length - 2)
								+ " bytes, expected " + minAsduLen + " to " + maxAsduLen;
						logger.error("received response with " + s);
						throw new KNXInvalidResponseException(s);
					}
					if (svcResponse == IND_ADDR_RESPONSE || svcResponse == IND_ADDR_SN_RESPONSE)
						return ((CEMILData) frame).getSource().toByteArray();
					indications.clear();
					return apdu;
				}
				indications.wait(remaining);
				remaining = end - System.currentTimeMillis();
			}
		}
		throw new KNXTimeoutException("timeout occurred while waiting for data response");
	}

	private List waitForResponses(final IndividualAddress from, final Priority p,
		final int minAsduLen, final int maxAsduLen, final boolean oneOnly)
		throws KNXLinkClosedException, KNXInvalidResponseException, InterruptedException
	{
		final List l = new ArrayList();
		try {
			long wait = responseTimeout;
			final long end = System.currentTimeMillis() + wait;
			while (wait > 0) {
				l.add(waitForResponse(from, minAsduLen, maxAsduLen, wait));
				if (oneOnly)
					break;
				wait = end - System.currentTimeMillis();
			}
		}
		catch (final KNXTimeoutException e) {}
		return l;
	}
	
	private synchronized List readBroadcast(final Priority p, final byte[] apdu,
		final int response, final int minAsduLen, final int maxAsduLen, final boolean oneOnly)
		throws KNXLinkClosedException, KNXInvalidResponseException, KNXTimeoutException,
		InterruptedException
	{
		try {
			svcResponse = response;
			tl.broadcast(true, p, apdu);
			final List l = waitForResponses(null, p, minAsduLen, maxAsduLen, oneOnly);
			if (l.isEmpty())
				throw new KNXTimeoutException("timeout waiting for responses");
			return l;
		}
		finally {
			svcResponse = 0;
		}
	}

	// cut domain addresses out of APDUs
	private static List makeDOAs(final List l)
	{
		for (int i = 0; i < l.size(); ++i) {
			final byte[] pdu = (byte[]) l.get(i);
			l.set(i, DataUnitBuilder.copyOfRange(pdu, 2, pdu.length));
		}
		return l;
	}
	
	// returns property read.res element values
	private static byte[] extractPropertyElements(final byte[] apdu, final int elements)
		throws KNXRemoteException
	{
		// check if number of elements is 0, indicates access problem
		final int number = (apdu[4] & 0xFF) >>> 4;
		if (number == 0)
			throw new KNXRemoteException("property access failed/forbidden");
		if (number != elements)
			throw new KNXInvalidResponseException("number of elements differ");
		final byte[] prop = new byte[apdu.length - 6];
		for (int i = 0; i < prop.length; ++i)
			prop[i] = apdu[i + 6];
		return prop;
	}
}

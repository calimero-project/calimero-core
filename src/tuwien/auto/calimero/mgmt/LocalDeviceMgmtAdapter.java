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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXInvalidResponseException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.knxnetip.KNXConnectionClosedException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt;

/**
 * Property adapter for KNXnet/IP local device management.
 * <p>
 * This adapter is based on a {@link KNXnetIPDevMgmt} connection.<br>
 * The object instance used is always the first one, i.e., object instance 1.
 * 
 * @author B. Malinowsky
 */
public class LocalDeviceMgmtAdapter implements PropertyAdapter
{
	private static final class Pair
	{
		final int oindex;
		final int otype;

		Pair(final int objIndex, final int objType)
		{
			oindex = objIndex;
			otype = objType;
		}
	}

	private final class KNXListenerImpl implements KNXListener
	{
		KNXListenerImpl()
		{}

		public void frameReceived(final FrameEvent e)
		{
			frames.add(e.getFrame());
		}

		public void connectionClosed(final CloseEvent e)
		{
			if (listener == null)
				return;
			final int initiator = serverReset ? CloseEvent.SERVER_REQUEST : e.getInitiator();
			listener.adapterClosed(new CloseEvent(LocalDeviceMgmtAdapter.this, initiator,
					serverReset ? "server reset" : e.getReason()));
		}
	}

	private static final int DEVICE_OBJECT = 0;

	private final KNXnetIPConnection conn;
	private final PropertyAdapterListener listener;
	private final List frames = Collections.synchronizedList(new LinkedList());

	private final List interfaceObjects = new ArrayList();
	private final boolean checkRW;

	private volatile boolean serverReset;
	private volatile boolean closed;
	
	// little helpers to reduce a property scan to an acceptable level
	private int lastObjIndex;
	private int lastPropIndex;
	private int lastPid;

	/**
	 * Creates a new property adapter for local device management.
	 * <p>
	 * The server to do management for is specified with the server control endpoint.
	 * <p>
	 * A note on write enabled / read only properties:<br>
	 * The check whether a property is read only or write enabled, is done by issuing a
	 * write request for that property. Due to the memory layout, write cycles of a memory
	 * location and similar, this might not always be desired. To enable or skip this
	 * check, the <code>queryWriteEnable</code> option has to be set appropriately.
	 * Currently, the write enabled check is only of interest when getting a property
	 * description {@link #getDescription(int, int, int)}.
	 * 
	 * @param localEP the local endpoint of the connection, use <code>null</code> for
	 *        assigning the default local host and an unused (ephemeral) port
	 * @param serverCtrlEP the remote server control endpoint used for connect request
	 * @param useNAT <code>true</code> to use a network address translation aware
	 *        communication mechanism, <code>false</code> to use the default way
	 * @param l property adapter listener to get notified about adapter events, use
	 *        <code>null</code> for no listener
	 * @param queryWriteEnable <code>true</code> to check whether a property is write
	 *        enabled or read only, <code>false</code> to skip the check
	 * @throws KNXException on failure establishing local device management connection or
	 *         failure while initializing the property adapter
	 * @throws InterruptedException on interrupted thread while initializing the adapter
	 */
	public LocalDeviceMgmtAdapter(final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNAT,
		final PropertyAdapterListener l, final boolean queryWriteEnable)
		throws KNXException, InterruptedException
	{
		InetSocketAddress local = localEP;
		if (local == null)
			try {
				local = new InetSocketAddress(InetAddress.getLocalHost(), 0);
			}
			catch (final UnknownHostException e) {
				throw new KNXException("no local host available");
			}
		conn = new KNXnetIPDevMgmt(local, serverCtrlEP, useNAT);
		conn.addConnectionListener(new KNXListenerImpl());
		listener = l;
		checkRW = queryWriteEnable;
		resetLastDescription();
		try {
			initInterfaceObjects();
		}
		catch (final KNXException e) {
			conn.close();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAdapter#setProperty
	 * (int, int, int, int, byte[])
	 */
	public void setProperty(final int objIndex, final int pid, final int start, final int elements,
		final byte[] data) throws KNXTimeoutException, KNXRemoteException,
		KNXConnectionClosedException
	{
		if (closed)
			throw new KNXIllegalStateException("adapter closed");
		conn.send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, getObjectType(objIndex), 1, pid,
				start, elements, data), KNXnetIPConnection.WAIT_FOR_CON);
		findFrame(CEMIDevMgmt.MC_PROPWRITE_CON);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAdapter#getProperty(int, int, int, int)
	 */
	public byte[] getProperty(final int objIndex, final int pid, final int start, final int elements)
		throws KNXTimeoutException, KNXRemoteException, KNXConnectionClosedException
	{
		if (closed)
			throw new KNXIllegalStateException("adapter closed");
		conn.send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, getObjectType(objIndex),
			1, pid, start, elements), KNXnetIPConnection.WAIT_FOR_CON);
		return findFrame(CEMIDevMgmt.MC_PROPREAD_CON);
	}
		
	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAdapter#getDescription(int, int, int)
	 */
	public byte[] getDescription(final int objIndex, final int pid, final int propIndex)
		throws KNXTimeoutException, KNXConnectionClosedException, KNXRemoteException
	{
		// imitate property description:
		// oindex: PropertyAccess.PID.IO_LIST
		// pindex: supplied, conditional
		// pid: supplied, conditional
		// pdt: -
		// elements: current number of elements
		// rLevel: - (not existent)
		// wLevel: - (not existent)
		// read-only: only with write emulation

		if (pid != 0 || objIndex != lastObjIndex || propIndex < lastPropIndex)
			resetLastDescription();
		// get current number of elements
		// since we can't query by index, emulate with PID if necessary
		byte[] ret = null;
		int p = pid;
		if (p == 0) {
			p = lastPid;
			int i = lastPropIndex;
			while (p < 0xff && i < propIndex)
				try {
					ret = getProperty(objIndex, ++p, 0, 1);
					++i;
				}
				catch (final KNXRemoteException ignore) {}
			if (i != propIndex) {
				resetLastDescription();
				throw new KNXRemoteException("can't deduce property index in object");
			}
			setLastDescription(objIndex, i, p);
		}
		else
			ret = getProperty(objIndex, p, 0, 1);

		int writeEnabled = 0;
		if (checkRW) {
			// check out if write enabled or read only
			try {
				setProperty(objIndex, p, 0, 1, ret);
				writeEnabled = 0x80;
			}
			catch (final KNXRemoteException e) {}
		}
		// ??? should we return current number of elements for max.element
		// int elements = 0;
		// for (int i = 0; i < ret.length; ++i)
		// 	elements = elements << 8 | ret[i] & 0xff;
		return new byte[] { (byte) objIndex, (byte) p, (byte) propIndex, (byte) writeEnabled, 0, 0,
			0 };
	}

	/**
	 * {@inheritDoc} The name for this adapter starts with "local DM " + KNXnet/IP server
	 * control endpoint, allowing easier distinction of adapter types.
	 */
	public String getName()
	{
		return "local DM " + conn.getRemoteAddress();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAdapter#isOpen()
	 */
	public boolean isOpen()
	{
		return !closed;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAdapter#close()
	 */
	public void close()
	{
		if (!closed) {
			closed = true;
			conn.close();
		}
	}

	private byte[] findFrame(final int messageCode) throws KNXRemoteException,
		KNXConnectionClosedException
	{
		while (frames.size() > 0) {
			final CEMIDevMgmt frame = (CEMIDevMgmt) frames.remove(0);
			final int mc = frame.getMessageCode();
			if (mc == CEMIDevMgmt.MC_RESET_IND) {
				serverReset = true;
				close();
				throw new KNXConnectionClosedException("reset by server");
			}
			else if (mc == messageCode) {
				if (frame.isNegativeResponse())
					throw new KNXRemoteException("L-DM negative response, "
							+ frame.getErrorMessage());
				return frame.getPayload();
			}
		}
		throw new KNXInvalidResponseException("confirmation expected");
	}

	private int getObjectType(final int objIndex) throws KNXRemoteException
	{
		for (final Iterator i = interfaceObjects.iterator(); i.hasNext();) {
			final Pair p = (Pair) i.next();
			if (p.oindex == objIndex)
				return p.otype;
		}
		throw new KNXRemoteException("object not listed");
	}

	private void initInterfaceObjects() throws KNXException
	{
		conn.send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, DEVICE_OBJECT, 1,
			PropertyAccess.PID.IO_LIST, 0, 1), KNXnetIPConnection.WAIT_FOR_CON);
		int objects = 0;
		try {
			final byte[] ret = findFrame(CEMIDevMgmt.MC_PROPREAD_CON);
			objects = (ret[0] & 0xff) << 8 | ret[1] & 0xff;
		}
		catch (final KNXRemoteException e) {
			// device only has device- and cEMI server-object
			interfaceObjects.add(new Pair(0, DEVICE_OBJECT));
			interfaceObjects.add(new Pair(1, 8));
			return;
		}
		conn.send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, DEVICE_OBJECT, 1,
			PropertyAccess.PID.IO_LIST, 1, objects), KNXnetIPConnection.WAIT_FOR_CON);
		final byte[] ret = findFrame(CEMIDevMgmt.MC_PROPREAD_CON);
		int lastObjType = -1;
		for (int i = 0; i < objects; ++i) {
			final int objType = (ret[2 * i] & 0xff) << 8 | ret[2 * i + 1] & 0xff;
			if (objType != lastObjType)
				interfaceObjects.add(new Pair(i, objType));
			lastObjType = objType;
		}
	}
	
	private void resetLastDescription()
	{
		lastObjIndex = -1;
		lastPropIndex = -1;
		lastPid = 0;
	}

	private void setLastDescription(final int oi, final int pi, final int pid)
	{
		lastObjIndex = oi;
		lastPropIndex = pi;
		lastPid = pid;
	}
}

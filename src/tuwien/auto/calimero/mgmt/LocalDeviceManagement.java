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

package tuwien.auto.calimero.mgmt;

import static tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode.WaitForCon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;

abstract class LocalDeviceManagement implements PropertyAdapter
{
	protected static final class Pair
	{
		final int oindex;
		final int otype;

		Pair(final int objIndex, final int objType)
		{
			oindex = objIndex;
			otype = objType;
		}
	}

	protected final class KNXListenerImpl implements KNXListener
	{
		@Override
		public void frameReceived(final FrameEvent e)
		{
			if (e.getFrame() instanceof CEMIDevMgmt)
				frames.add(e.getFrame());
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			if (listener == null)
				return;
			final int initiator = serverReset ? CloseEvent.SERVER_REQUEST : e.getInitiator();
			listener.adapterClosed(new CloseEvent(LocalDeviceManagement.this, initiator,
					serverReset ? "server reset" : e.getReason()));
		}
	}

	private static final int DEVICE_OBJECT = 0;

	protected final AutoCloseable c;

	private final PropertyAdapterListener listener;
	private final List<CEMI> frames = Collections.synchronizedList(new LinkedList<>());
	private volatile boolean serverReset;
	private final List<Pair> interfaceObjects = new ArrayList<>();

	private final boolean checkRW;
	private volatile boolean closed;

	private int lastObjIndex;
	private int lastPropIndex;
	private int lastPid;

	LocalDeviceManagement(final AutoCloseable connection, final PropertyAdapterListener l,
		final boolean queryWriteEnable)
	{
		c = connection;
		listener = l;
		checkRW = queryWriteEnable;
	}

	void init() throws KNXException, InterruptedException
	{
		resetLastDescription();
		try {
			initInterfaceObjects();
		}
		catch (final KNXException e) {
			try {
				c.close();
			}
			catch (final Exception ignore) {}
			throw e;
		}
	}

	@Override
	public void setProperty(final int objIndex, final int pid, final int start, final int elements,
		final byte[] data) throws KNXException, InterruptedException
	{
		if (closed)
			throw new KNXIllegalStateException("adapter closed");
		final int objectType = getObjectType(objIndex);
		final int objectInstance = getObjectInstance(objIndex, objectType);
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, objectType, objectInstance, pid, start,
				elements, data), WaitForCon);
		findFrame(CEMIDevMgmt.MC_PROPWRITE_CON);
	}

	@Override
	public byte[] getProperty(final int objIndex, final int pid, final int start, final int elements)
		throws KNXException, InterruptedException
	{
		if (closed)
			throw new KNXIllegalStateException("adapter closed");
		final int objectType = getObjectType(objIndex);
		final int objectInstance = getObjectInstance(objIndex, objectType);
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, objectType, objectInstance, pid, start,
				elements), WaitForCon);
		return findFrame(CEMIDevMgmt.MC_PROPREAD_CON);
	}

	@Override
	public byte[] getDescription(final int objIndex, final int pid, final int propIndex)
		throws KNXException, InterruptedException
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

	@Override
	public boolean isOpen()
	{
		return !closed;
	}

	@Override
	public void close()
	{
		if (!closed) {
			closed = true;
			try {
				c.close();
			}
			catch (final Exception ignore) {}
		}
	}

	protected abstract void send(final CEMIDevMgmt frame, final Object mode) throws KNXException, InterruptedException;

	protected byte[] findFrame(final int messageCode) throws KNXRemoteException
	{
		while (frames.size() > 0) {
			final CEMIDevMgmt frame = (CEMIDevMgmt) frames.remove(0);
			final int mc = frame.getMessageCode();
			if (mc == CEMIDevMgmt.MC_RESET_IND) {
				serverReset = true;
				close();
				throw new KNXRemoteException("reset by server");
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

	protected int getObjectType(final int objIndex) throws KNXRemoteException
	{
		for (final Iterator<Pair> i = interfaceObjects.iterator(); i.hasNext();) {
			final Pair p = i.next();
			if (p.oindex == objIndex)
				return p.otype;
		}
		throw new KNXRemoteException("object not listed");
	}

	private int getObjectInstance(final int objIndex, final int objectType)
	{
		int instance = 0;
		for (int i = 0; i <= objIndex; i++) {
			final Pair p = interfaceObjects.get(i);
			if (p.otype == objectType)
				instance++;
		}
		return instance;
	}

	protected void initInterfaceObjects() throws KNXException, InterruptedException
	{
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, DEVICE_OBJECT, 1,
				PropertyAccess.PID.IO_LIST, 0, 1), WaitForCon);
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
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, DEVICE_OBJECT, 1,
				PropertyAccess.PID.IO_LIST, 1, objects), WaitForCon);
		final byte[] ret = findFrame(CEMIDevMgmt.MC_PROPREAD_CON);
		for (int i = 0; i < objects; ++i) {
			final int objType = (ret[2 * i] & 0xff) << 8 | ret[2 * i + 1] & 0xff;
			interfaceObjects.add(new Pair(i, objType));
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

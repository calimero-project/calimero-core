/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2015, 2025 B. Malinowsky

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

package io.calimero.mgmt;

import static io.calimero.cemi.CEMIDevMgmt.MC_PROPREAD_REQ;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import io.calimero.CloseEvent;
import io.calimero.Connection;
import io.calimero.Connection.BlockingMode;
import io.calimero.FrameEvent;
import io.calimero.KNXException;
import io.calimero.KNXInvalidResponseException;
import io.calimero.KNXListener;
import io.calimero.KNXRemoteException;
import io.calimero.ReturnCode;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIDevMgmt;
import io.calimero.mgmt.PropertyAccess.PID;

abstract class LocalDeviceManagement<T> implements PropertyAdapter
{
	protected final class KNXListenerImpl implements KNXListener
	{
		@Override
		public void frameReceived(final FrameEvent e)
		{
			if (e.getFrame() instanceof CEMIDevMgmt) {
				synchronized (frames) {
					frames.add(e.getFrame());
				}
			}
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			closed = true;
			final int initiator = serverReset ? CloseEvent.SERVER_REQUEST : e.getInitiator();
			adapterClosed.accept(new CloseEvent(LocalDeviceManagement.this, initiator,
					serverReset ? "server reset" : e.getReason()));
		}
	}

	private static final int DEVICE_OBJECT = 0;

	protected final Connection<T> c;
	private final Consumer<CloseEvent> adapterClosed;

	private final Deque<CEMI> frames = new ArrayDeque<>();
	private volatile boolean serverReset;
	private final List<Integer> interfaceObjects = new ArrayList<>();

	private final boolean checkRW;
	private volatile boolean closed;

	private int lastObjIndex;
	private int lastPropIndex;
	private int lastPid;

	LocalDeviceManagement(final Connection<T> connection, final Consumer<CloseEvent> adapterClosed,
		final boolean queryWriteEnable)
	{
		c = connection;
		this.adapterClosed = adapterClosed;
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

	/**
	 * {@return unmodifiable list of interface objects in ascending order, containing the object type of each interface
	 *         object}
	 */
	public final List<Integer> interfaceObjects() { return List.copyOf(interfaceObjects); }

	@Override
	public void setProperty(final int objIndex, final int pid, final int start, final int elements,
		final byte... data) throws KNXException, InterruptedException
	{
		if (closed)
			throw new IllegalStateException("adapter closed");
		final int objectType = getObjectType(objIndex);
		final int objectInstance = getObjectInstance(objIndex, objectType);
		final CEMIDevMgmt req = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, objectType, objectInstance, pid, start,
				elements, data);
		send(req, BlockingMode.Confirmation);
		findFrame(CEMIDevMgmt.MC_PROPWRITE_CON, req);
	}

	@Override
	public byte[] getProperty(final int objIndex, final int pid, final int start, final int elements)
		throws KNXException, InterruptedException
	{
		if (closed)
			throw new IllegalStateException("adapter closed");
		final int objectType = getObjectType(objIndex);
		final int objectInstance = getObjectInstance(objIndex, objectType);
		final var req = new CEMIDevMgmt(MC_PROPREAD_REQ, objectType, objectInstance, pid, start, elements);
		send(req, BlockingMode.Confirmation);
		return findFrame(CEMIDevMgmt.MC_PROPREAD_CON, req);
	}

	@Override
	public byte[] getDescription(final int objIndex, final int pid, final int propIndex)
		throws KNXException, InterruptedException
	{
		// imitate property description:
		// oindex: PropertyAccess.PID.IO_LIST
		// pid: supplied, conditional
		// pindex: supplied, conditional
		// read-only: only with write emulation
		// pdt: -
		// elements (2 bytes): current number of elements
		// rLevel: - (not existent)
		// wLevel: - (not existent)

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
		return new byte[] { (byte) objIndex, (byte) p, (byte) propIndex, (byte) writeEnabled, 0, 0, 0 };
	}

	public void callFunctionProperty(final int objIndex, final int pid, final int serviceId, final byte... serviceInfo)
			throws KNXException, InterruptedException {
		if (closed)
			throw new IllegalStateException("adapter closed");
		final int objectType = getObjectType(objIndex);
		final int objectInstance = getObjectInstance(objIndex, objectType);
		callFunctionProperty(objectType, objectInstance, pid, serviceId, serviceInfo);
	}

	@Override
	public void callFunctionProperty(final int objectType, final int objectInstance, final int pid, final int serviceId,
			final byte... serviceInfo) throws KNXException, InterruptedException {
		if (closed)
			throw new IllegalStateException("adapter closed");
		final var req = CEMIDevMgmt.newFunctionPropertyService(CEMIDevMgmt.MC_FUNCPROP_CMD_REQ, objectType,
				objectInstance, pid, ReturnCode.Success, serviceId, serviceInfo);
		send(req, BlockingMode.Confirmation);
		findFrame(CEMIDevMgmt.MC_FUNCPROP_CON, req);
	}

	public byte[] getFunctionPropertyState(final int objIndex, final int pid, final int serviceId,
			final byte... serviceInfo) throws KNXException, InterruptedException {
		if (closed)
			throw new IllegalStateException("adapter closed");
		final int objectType = getObjectType(objIndex);
		final int objectInstance = getObjectInstance(objIndex, objectType);
		return getFunctionPropertyState(objectType, objectInstance, pid, serviceId, serviceInfo);
	}

	@Override
	public byte[] getFunctionPropertyState(final int objectType, final int objectInstance, final int pid,
			final int serviceId, final byte... serviceInfo) throws KNXException, InterruptedException {
		if (closed)
			throw new IllegalStateException("adapter closed");
		final var req = CEMIDevMgmt.newFunctionPropertyService(CEMIDevMgmt.MC_FUNCPROP_READ_REQ, objectType,
				objectInstance, pid, ReturnCode.Success, serviceId, serviceInfo);
		send(req, BlockingMode.Confirmation);
		return findFrame(CEMIDevMgmt.MC_FUNCPROP_CON, req);
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

	protected abstract void send(CEMIDevMgmt frame, BlockingMode mode) throws KNXException, InterruptedException;

	@SuppressWarnings("unused")
	protected byte[] findFrame(final int messageCode, final CEMIDevMgmt request)
		throws KNXRemoteException, InterruptedException {
		while (true) {
			final CEMIDevMgmt frame;
			synchronized (frames) {
				if (frames.isEmpty())
					throw new KNXInvalidResponseException(
							String.format("confirmation expected (msg code 0x%x)", messageCode));
				frame = (CEMIDevMgmt) frames.remove();
			}
			final int mc = frame.getMessageCode();
			if (mc == CEMIDevMgmt.MC_RESET_IND) {
				serverReset = true;
				close();
				throw new KNXRemoteException("received reset indication from server, connection closed");
			}
			else if (mc == messageCode && frame.getObjectType() == request.getObjectType()
					&& frame.getObjectInstance() == request.getObjectInstance()
					&& frame.getPID() == request.getPID()) {
				if (frame.isNegativeResponse())
					throw new KNXRemoteException(frame.getErrorMessage());
				if (frame.getElementCount() == request.getElementCount())
					return frame.getPayload();
			}
		}
	}

	protected int getObjectType(final int objIndex) throws KNXRemoteException
	{
		if (objIndex < interfaceObjects.size())
			return interfaceObjects.get(objIndex);
		throw new KNXRemoteException("interface object with index " + objIndex + " not listed");
	}

	private int getObjectInstance(final int objIndex, final int objectType)
	{
		int instance = 0;
		for (int i = 0; i <= objIndex; i++) {
			if (interfaceObjects.get(i) == objectType)
				instance++;
		}
		return instance;
	}

	protected void initInterfaceObjects() throws KNXException, InterruptedException
	{
		final var req = new CEMIDevMgmt(MC_PROPREAD_REQ, DEVICE_OBJECT, 1, PID.IO_LIST, 0, 1);
		send(req, BlockingMode.Confirmation);
		final int objects;
		try {
			final byte[] ret = findFrame(CEMIDevMgmt.MC_PROPREAD_CON, req);
			objects = (ret[0] & 0xff) << 8 | (ret[1] & 0xff);
		}
		catch (final KNXRemoteException e) {
			// device only has device- and cEMI server-object
			interfaceObjects.add(DEVICE_OBJECT);
			interfaceObjects.add(8);
			return;
		}
		final int stride = 15;
		for (int startIndex = 1; startIndex <= objects; startIndex += stride) {
			final int elements = Math.min(stride, objects - startIndex + 1);
			final var req2 = new CEMIDevMgmt(MC_PROPREAD_REQ, DEVICE_OBJECT, 1, PID.IO_LIST, startIndex, elements);
			send(req2, BlockingMode.Confirmation);
			final byte[] ret = findFrame(CEMIDevMgmt.MC_PROPREAD_CON, req2);
			for (int i = 0; i < elements; ++i) {
				final int objType = (ret[2 * i] & 0xff) << 8 | (ret[2 * i + 1] & 0xff);
				interfaceObjects.add(objType);
			}
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

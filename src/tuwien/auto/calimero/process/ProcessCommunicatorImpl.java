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

package tuwien.auto.calimero.process;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.DPTXlatorString;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.log.LogService;

/**
 * This implementation of the process communicator uses in any case the DPT translators
 * {@link DPTXlatorBoolean}, {@link DPTXlator3BitControlled}, {@link DPTXlator8BitUnsigned},
 * {@link DPTXlator2ByteFloat}, {@link DPTXlator4ByteFloat}, {@link DPTXlatorString}. Other
 * translator types are loaded through {@link TranslatorTypes}.
 *
 * @author B. Malinowsky
 */
public class ProcessCommunicatorImpl implements ProcessCommunicator
{
	private final class NLListener implements NetworkLinkListener
	{
		@Override
		public void indication(final FrameEvent e)
		{
			final CEMILData f = (CEMILData) e.getFrame();
			final byte[] apdu = f.getPayload();
			// can't be a process communication indication if too short
			if (apdu.length < 2)
				return;
			final int svc = DataUnitBuilder.getAPDUService(apdu);
			// Note: even if this is a read response we have waited for,
			// we nevertheless notify the listeners about it (we do *not* discard it)
			if (svc == GROUP_RESPONSE) {
				synchronized (indications) {
					if (indications.replace((GroupAddress) f.getDestination(), e) != null)
						indications.notifyAll();
				}
			}
			try {
				// notify listeners
				if (svc == GROUP_READ)
					fireGroupReadWrite(f, new byte[0], svc, false);
				else if (svc == GROUP_RESPONSE || svc == GROUP_WRITE)
					fireGroupReadWrite(f, DataUnitBuilder.extractASDU(apdu), svc, apdu.length <= 2);
			}
			catch (final RuntimeException rte) {
				logger.error("on group indication from {}", f.getDestination(), rte);
			}
		}

		private void fireGroupReadWrite(final CEMILData f, final byte[] asdu, final int svc, final boolean optimized)
		{
			final ProcessEvent e = new ProcessEvent(ProcessCommunicatorImpl.this, f.getSource(),
					(GroupAddress) f.getDestination(), svc, asdu, optimized);
			final Consumer<? super ProcessListener> c;
			if (svc == GROUP_READ)
				c = l -> l.groupReadRequest(e);
			else if (svc == GROUP_RESPONSE)
				c = l -> l.groupReadResponse(e);
			else
				c = l -> l.groupWrite(e);
			listeners.fire(c);
		}

		@Override
		public void confirmation(final FrameEvent e)
		{}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			logger.info("attached link was closed");
			detach();
		}
	}

	private static final int GROUP_READ = 0x00;
	private static final int GROUP_RESPONSE = 0x40;
	private static final int GROUP_WRITE = 0x80;

	private final KNXNetworkLink lnk;
	private final NetworkLinkListener lnkListener = new NLListener();
	private final EventListeners<ProcessListener> listeners;

	private final Map<GroupAddress, FrameEvent> indications = new HashMap<>();
	private static final FrameEvent NoResponse = new FrameEvent(ProcessCommunicatorImpl.class, (CEMI) null);
	private final Map<GroupAddress, AtomicInteger> readers = new HashMap<>();

	private volatile Priority priority = Priority.LOW;
	// maximum wait time in seconds for a response message
	private volatile int responseTimeout = 10;
	private volatile boolean detached;
	private final Logger logger;

	/**
	 * Creates a new process communicator attached to the supplied KNX network link.
	 * <p>
	 * The log service used by this process communicator is named "process " +
	 * <code>link.getName()</code>.
	 *
	 * @param link network link used for communication with a KNX network
	 * @throws KNXLinkClosedException if the network link is closed
	 */
	public ProcessCommunicatorImpl(final KNXNetworkLink link) throws KNXLinkClosedException
	{
		if (!link.isOpen())
			throw new KNXLinkClosedException(
					"cannot initialize process communication using closed link " + link.getName());
		logger = LogService.getLogger("calimero.process.process " + link.getName());
		lnk = link;
		listeners = new EventListeners<>(logger);
		lnk.addLinkListener(lnkListener);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#setResponseTimeout(int)
	 */
	@Override
	public void setResponseTimeout(final int timeout)
	{
		if (timeout <= 0)
			throw new KNXIllegalArgumentException("timeout <= 0");
		responseTimeout = timeout;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#getResponseTimeout()
	 */
	@Override
	public int getResponseTimeout()
	{
		return responseTimeout;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#setPriority
	 * (tuwien.auto.calimero.Priority)
	 */
	@Override
	public void setPriority(final Priority p)
	{
		priority = p;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#getPriority()
	 */
	@Override
	public Priority getPriority()
	{
		return priority;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#addProcessListener
	 * (tuwien.auto.calimero.process.ProcessListener)
	 */
	@Override
	public void addProcessListener(final ProcessListener l)
	{
		listeners.add(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#removeProcessListener
	 * (tuwien.auto.calimero.process.ProcessListener)
	 */
	@Override
	public void removeProcessListener(final ProcessListener l)
	{
		listeners.remove(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#readBool
	 * (tuwien.auto.calimero.GroupAddress)
	 */
	@Override
	public boolean readBool(final GroupAddress dst) throws KNXTimeoutException, KNXRemoteException,
		KNXLinkClosedException, KNXFormatException, InterruptedException
	{
		final byte[] apdu = readFromGroup(dst, priority, 0, 0);
		final DPTXlatorBoolean t = new DPTXlatorBoolean(DPTXlatorBoolean.DPT_BOOL);
		extractGroupASDU(apdu, t);
		return t.getValueBoolean();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#write
	 * (tuwien.auto.calimero.GroupAddress, boolean)
	 */
	@Override
	public void write(final GroupAddress dst, final boolean value)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		try {
			final DPTXlatorBoolean t = new DPTXlatorBoolean(DPTXlatorBoolean.DPT_BOOL);
			t.setValue(value);
			write(dst, priority, t);
		}
		catch (final KNXFormatException ignore) {}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#readUnsigned
	 * (tuwien.auto.calimero.GroupAddress, java.lang.String)
	 */
	@Override
	public int readUnsigned(final GroupAddress dst, final String scale) throws KNXTimeoutException,
		KNXRemoteException, KNXLinkClosedException, KNXFormatException, InterruptedException
	{
		final byte[] apdu = readFromGroup(dst, priority, 1, 1);
		final DPTXlator8BitUnsigned t = new DPTXlator8BitUnsigned(scale);
		extractGroupASDU(apdu, t);
		return t.getValueUnsigned();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#write
	 * (tuwien.auto.calimero.GroupAddress, int, java.lang.String)
	 */
	@Override
	public void write(final GroupAddress dst, final int value, final String scale)
		throws KNXTimeoutException, KNXFormatException, KNXLinkClosedException
	{
		final DPTXlator8BitUnsigned t = new DPTXlator8BitUnsigned(scale);
		t.setValue(value);
		write(dst, priority, t);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#readControl
	 * (tuwien.auto.calimero.GroupAddress)
	 */
	@Override
	public int readControl(final GroupAddress dst) throws KNXTimeoutException, KNXRemoteException,
		KNXLinkClosedException, KNXFormatException, InterruptedException
	{
		final byte[] apdu = readFromGroup(dst, priority, 0, 0);
		final DPTXlator3BitControlled t = new DPTXlator3BitControlled(
				DPTXlator3BitControlled.DPT_CONTROL_DIMMING);
		extractGroupASDU(apdu, t);
		return t.getValueSigned();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#write
	 * (tuwien.auto.calimero.GroupAddress, boolean, byte)
	 */
	@Override
	public void write(final GroupAddress dst, final boolean control, final int stepcode)
		throws KNXTimeoutException, KNXFormatException, KNXLinkClosedException
	{
		final DPTXlator3BitControlled t = new DPTXlator3BitControlled(
				DPTXlator3BitControlled.DPT_CONTROL_DIMMING);
		t.setValue(control, stepcode);
		write(dst, priority, t);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicationBase#write
	 * (tuwien.auto.calimero.GroupAddress, float, boolean)
	 */
	@Override
	public void write(final GroupAddress dst, final float value, final boolean use4ByteFloat)
		throws KNXTimeoutException, KNXFormatException, KNXLinkClosedException
	{
		if (use4ByteFloat) {
			final DPTXlator4ByteFloat t = new DPTXlator4ByteFloat(
					DPTXlator4ByteFloat.DPT_TEMPERATURE_DIFFERENCE);
			t.setValue(value);
			write(dst, priority, t);
		}
		else {
			final DPTXlator2ByteFloat t = new DPTXlator2ByteFloat(
					DPTXlator2ByteFloat.DPT_RAIN_AMOUNT);
			t.setValue(value);
			write(dst, priority, t);
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#readFloat
	 * (tuwien.auto.calimero.GroupAddress, boolean)
	 */
	@Override
	public double readFloat(final GroupAddress dst, final boolean is4ByteFloat)
		throws KNXTimeoutException, KNXRemoteException, KNXLinkClosedException, KNXFormatException,
		InterruptedException
	{
		final DPTXlator t = is4ByteFloat
				? new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_TEMPERATURE_DIFFERENCE)
				: new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_RAIN_AMOUNT);
		final int size = is4ByteFloat ? 4 : 2;
		final byte[] apdu = readFromGroup(dst, priority, size, size);
		extractGroupASDU(apdu, t);
		return t.getNumericValue();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#readString
	 * (tuwien.auto.calimero.GroupAddress)
	 */
	@Override
	public String readString(final GroupAddress dst) throws KNXTimeoutException, KNXRemoteException,
		KNXLinkClosedException, KNXFormatException, InterruptedException
	{
		final byte[] apdu = readFromGroup(dst, priority, 0, 14);
		final DPTXlatorString t = new DPTXlatorString(DPTXlatorString.DPT_STRING_8859_1);
		extractGroupASDU(apdu, t);
		return t.getValue();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#write
	 * (tuwien.auto.calimero.GroupAddress, java.lang.String)
	 */
	@Override
	public void write(final GroupAddress dst, final String value)
		throws KNXTimeoutException, KNXFormatException, KNXLinkClosedException
	{
		final DPTXlatorString t = new DPTXlatorString(DPTXlatorString.DPT_STRING_8859_1);
		t.setValue(value);
		write(dst, priority, t);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#write
	 * (tuwien.auto.calimero.GroupAddress, tuwien.auto.calimero.dptxlator.DPTXlator)
	 */
	@Override
	public void write(final GroupAddress dst, final DPTXlator value)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		write(dst, priority, value);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If <code>dp</code> has no {@link DPT} set, this method returns a hexadecimal representation
	 * of the ASDU.
	 */
	@Override
	public String read(final Datapoint dp) throws KNXException, InterruptedException
	{
		final byte[] apdu = readFromGroup(dp.getMainAddress(), dp.getPriority(), 0, 14);
		if (dp.getDPT() == null)
			return DataUnitBuilder.toHex(DataUnitBuilder.extractASDU(apdu), " ");
		final DPTXlator t = TranslatorTypes.createTranslator(dp.getMainNumber(), dp.getDPT());
		extractGroupASDU(apdu, t);
		return t.getValue();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#write
	 * (tuwien.auto.calimero.datapoint.Datapoint, java.lang.String)
	 */
	@Override
	public void write(final Datapoint dp, final String value) throws KNXException
	{
		final DPTXlator t = TranslatorTypes.createTranslator(dp.getMainNumber(), dp.getDPT());
		t.setValue(value);
		write(dp.getMainAddress(), dp.getPriority(), t);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.process.ProcessCommunicator#detach()
	 */
	@Override
	public KNXNetworkLink detach()
	{
		// if we synchronize on method we would take into account
		// a worst case blocking of response timeout seconds
		synchronized (lnkListener) {
			// wait of response time seconds
			if (detached)
				return null;
			detached = true;
		}
		lnk.removeLinkListener(lnkListener);
		fireDetached();
		logger.info("detached from " + lnk.getName());
		return lnk;
	}

	private void write(final GroupAddress dst, final Priority p, final DPTXlator t)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		if (detached)
			throw new KNXIllegalStateException("process communicator detached");
		lnk.sendRequestWait(dst, p, createGroupAPDU(GROUP_WRITE, t));
		if (logger.isTraceEnabled())
			logger.trace("group write to " + dst + " succeeded");
	}

	private byte[] readFromGroup(final GroupAddress dst, final Priority p,
		final int minASDULen, final int maxASDULen) throws KNXTimeoutException,
			KNXInvalidResponseException, KNXLinkClosedException, InterruptedException
	{
		if (detached)
			throw new KNXIllegalStateException("process communicator detached");
		try {
			synchronized (indications) {
				readers.computeIfAbsent(dst, v -> new AtomicInteger()).incrementAndGet();
				indications.putIfAbsent(dst, NoResponse);
			}

			lnk.sendRequestWait(dst, p, DataUnitBuilder.createLengthOptimizedAPDU(GROUP_READ, null));
			logger.trace("sent group read request to {}", dst);
			return waitForResponse(dst, minASDULen + 2, maxASDULen + 2);
		}
		finally {
			synchronized (indications) {
				final boolean none = readers.get(dst).decrementAndGet() == 0;
				readers.compute(dst, (k, v) -> none ? null : v);
				indications.compute(dst, (k, v) -> none ? null : v);
			}
		}
	}

	private byte[] waitForResponse(final GroupAddress from, final int minAPDU, final int maxAPDU)
		throws KNXInvalidResponseException, KNXTimeoutException, InterruptedException
	{
		long remaining = responseTimeout * 1000;
		final long end = System.currentTimeMillis() + remaining;
		synchronized (indications) {
			while (remaining > 0) {
				final FrameEvent e = indications.get(from);
				if (e == NoResponse) {
					indications.wait(remaining);
					remaining = end - System.currentTimeMillis();
				}
				else {
					final byte[] d = e.getFrame().getPayload();
					final int len = d.length;
					// validate length of response we're waiting for
					if (len >= minAPDU && len <= maxAPDU)
						return d;

					final String s = "APDU response length " + len + " bytes, expected " + minAPDU + " to " + maxAPDU;
					logger.error("received group read response from {} with {}", from, s);
					throw new KNXInvalidResponseException(s);
				}
			}
		}
		logger.info("timeout waiting for group read response from {}", from);
		throw new KNXTimeoutException("timeout waiting for group read response from " + from);
	}

	private void fireDetached()
	{
		final DetachEvent e = new DetachEvent(this);
		listeners.fire(l -> l.detached(e));
	}

	// createGroupAPDU and extractGroupASDU helper would actually better fit
	// into to DataUnitBuilder, but moved here to avoid DPT dependencies

	/**
	 * Creates a group service application layer protocol data unit containing all items of a DPT
	 * translator.
	 * <p>
	 * The transport layer bits in the first byte (TL / AL control field) are set 0. The maximum
	 * length used for the ASDU is not checked.<br>
	 * For DPTs occupying &lt;= 6 bits in length the optimized (compact) group write / response
	 * format layout is used.
	 *
	 * @param service application layer group service code
	 * @param t DPT translator with items to put into ASDU
	 * @return group APDU as byte array
	 */
	private static byte[] createGroupAPDU(final int service, final DPTXlator t)
	{
		// check for group read
		if (service == 0x00)
			return new byte[2];
		// only group response and group write are allowed
		if (service != 0x40 && service != 0x80)
			throw new KNXIllegalArgumentException("not an APDU group service");
		// determine if data starts at byte offset 1 (optimized) or 2 (default)
		final int offset = t.getItems() == 1 && t.getTypeSize() == 0 ? 1 : 2;
		final byte[] buf = new byte[t.getItems() * Math.max(1, t.getTypeSize()) + offset];
		buf[0] = (byte) (service >> 8);
		buf[1] = (byte) service;
		return t.getData(buf, offset);
	}

	/**
	 * Extracts the service data unit of an application layer protocol data unit into a DPT
	 * translator.
	 * <p>
	 * The whole service data unit is taken as data for translation. If the length of the supplied
	 * <code>apdu</code> is 2, a length-optimized (compact) group APDU format layout is assumed.<br>
	 * On return of this method, the supplied translator contains the DPT items from the ASDU.
	 *
	 * @param apdu application layer protocol data unit, 2 &lt;= apdu.length
	 * @param t the DPT translator to fill with the ASDU
	 */
	private static void extractGroupASDU(final byte[] apdu, final DPTXlator t)
	{
		if (apdu.length < 2)
			throw new KNXIllegalArgumentException("minimum APDU length is 2 bytes");
		t.setData(apdu, apdu.length == 2 ? 1 : 2);
	}
}

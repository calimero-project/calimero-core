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

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.cemi.CEMILDataEx;
import tuwien.auto.calimero.cemi.RFMediumInfo;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.PLSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogService;

/**
 * Provides an abstract KNX network link implementation, independent of the actual communication
 * protocol and medium access. The link supports EMI1/EMI2/cEMI format. Subtypes extend this link by
 * specifying the protocol, e.g., KNXnet/IP, or providing a specific implementation of medium
 * access, e.g., via a serial port driver. In most cases, it is sufficient for a subtype to provide
 * an implementation of {@link #onSend(CEMILData, boolean)} or
 * {@link #onSend(KNXAddress, byte[], boolean)}, as well as {@link #onClose()}. If the communication
 * protocol message format differs from the supported EMI 1/2 or cEMI, a subtype needs to override
 * {@link #onReceive(FrameEvent)}. For receiving and dispatching frames from the specified protocol,
 * a subtype uses the KNXListener {@link AbstractLink#notifier} as connection listener.
 * <p>
 * In general, once a link has been closed, it is not available for further link communication and
 * cannot be reopened. For many use cases, a {@link Connector} is useful for creating KNX network
 * links.
 *
 * @author B. Malinowsky
 * @see KNXNetworkLinkIP
 * @see KNXNetworkLinkFT12
 * @see KNXNetworkLinkUsb
 * @see Connector
 */
public abstract class AbstractLink implements KNXNetworkLink
{
	/** Logger for this link instance. */
	protected final Logger logger;

	/**
	 * Listener and notifier of KNX link events, add as connection listener to the underlying
	 * protocol during initialization.
	 */
	protected final EventNotifier<NetworkLinkListener> notifier;

	/** The message format to generate: cEMI or EMI1/EMI2. */
	protected boolean cEMI = true;

	/**
	 * With cEMI format, use {@link #onSend(KNXAddress, byte[], boolean)} if set <code>true</code>,
	 * use {@link #onSend(CEMILData, boolean)} if set <code>false</code>.
	 */
	protected boolean sendCEmiAsByteArray;

	private final String name;
	private volatile boolean closed;
	private volatile int hopCount = 6;
	private KNXMediumSettings medium;

	final AutoCloseable conn;

	private final class LinkNotifier extends EventNotifier<NetworkLinkListener>
	{
		LinkNotifier()
		{
			super(AbstractLink.this, AbstractLink.this.logger);
		}

		@Override
		public void frameReceived(final FrameEvent e)
		{
			try {
				// with EMI1 frames, there is the possibility we receive some left-over Get-Value responses
				// from BCU switching during link setup, silently discard them
				final byte[] emi1 = e.getFrameBytes();
				if (emi1 != null && BcuSwitcher.isEmi1GetValue(emi1[0] & 0xff))
					return;

				final CEMI cemi = onReceive(e);
				if (cemi instanceof CEMIDevMgmt) {
					// XXX check .con correctly (required for setting cEMI link layer mode)
					final int mc = cemi.getMessageCode();
					if (mc == CEMIDevMgmt.MC_PROPWRITE_CON) {
						final CEMIDevMgmt f = (CEMIDevMgmt) cemi;
						if (f.isNegativeResponse())
							logger.error("L-DM negative response, " + f.getErrorMessage());
					}
				}
				// from this point on, we are only dealing with L_Data
				if (!(cemi instanceof CEMILData))
					return;
				final CEMILData f = (CEMILData) cemi;
				final int mc = f.getMessageCode();
				if (mc == CEMILData.MC_LDATA_IND) {
					addEvent(l -> l.indication(new FrameEvent(source, f)));
					logger.debug("indication {}", f.toString());
				}
				else if (mc == CEMILData.MC_LDATA_CON) {
					addEvent(l -> l.confirmation(new FrameEvent(source, f)));
					logger.debug("confirmation of {}", f.getDestination());
				}
				else
					logger.warn("unspecified frame event - ignored, msg code = 0x" + Integer.toHexString(mc));
			}
			catch (final KNXFormatException | RuntimeException ex) {
				logger.warn("received unspecified frame {}", DataUnitBuilder.toHex(e.getFrameBytes(), ""), ex);
			}
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			((AbstractLink) source).closed = true;
			super.connectionClosed(e);
			logger.info("link closed");
		}
	};

	/**
	 * @param connection if not <code>null</code>, the link object will close this resource as last
	 *        action before returning from {@link #close()}, relinquishing any underlying resources
	 * @param name the link name
	 * @param settings medium settings of the accessed KNX network
	 */
	protected AbstractLink(final AutoCloseable connection, final String name,
		final KNXMediumSettings settings)
	{
		conn = connection;
		this.name = name;
		logger = LogService.getLogger("calimero.link." + getName());
		notifier = new LinkNotifier();
		setKNXMedium(settings);
		notifier.start();
	}

	/**
	 * This constructor does not start the event notifier.
	 *
	 * @param name the link name
	 * @param settings medium settings of the accessed KNX network
	 */
	protected AbstractLink(final String name, final KNXMediumSettings settings)
	{
		conn = null;
		this.name = name;
		logger = LogService.getLogger("calimero.link." + getName());
		notifier = new LinkNotifier();
		setKNXMedium(settings);
	}

	@Override
	public final synchronized void setKNXMedium(final KNXMediumSettings settings)
	{
		if (settings == null)
			throw new KNXIllegalArgumentException("medium settings are mandatory");
		if (medium != null && !settings.getClass().isAssignableFrom(medium.getClass())
				&& !medium.getClass().isAssignableFrom(settings.getClass()))
			throw new KNXIllegalArgumentException("medium differs");
		medium = settings;
	}

	@Override
	public final synchronized KNXMediumSettings getKNXMedium()
	{
		return medium;
	}

	@Override
	public void addLinkListener(final NetworkLinkListener l)
	{
		notifier.addListener(l);
	}

	@Override
	public void removeLinkListener(final NetworkLinkListener l)
	{
		notifier.removeListener(l);
	}

	@Override
	public final void setHopCount(final int count)
	{
		if (count < 0 || count > 7)
			throw new KNXIllegalArgumentException("hop count out of range [0..7]");
		hopCount = count;
		logger.info("hop count set to " + count);
	}

	@Override
	public final int getHopCount()
	{
		return hopCount;
	}

	@Override
	public void sendRequest(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		send(CEMILData.MC_LDATA_REQ, dst, p, nsdu, false);
	}

	@Override
	public void sendRequestWait(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		send(CEMILData.MC_LDATA_REQ, dst, p, nsdu, true);
	}

	@Override
	public void send(final CEMILData msg, final boolean waitForCon)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		if (closed)
			throw new KNXLinkClosedException("link closed");
		if (cEMI && !sendCEmiAsByteArray) {
			final CEMILData adjusted = adjustMsgType(msg);
			addMediumInfo(adjusted);
			onSend(adjusted, waitForCon);
			return;
		}
		onSend(msg.getDestination(), createEmi(msg), waitForCon);
	}

	@Override
	public final String getName()
	{
		return name;
	}

	@Override
	public final boolean isOpen()
	{
		return !closed;
	}

	@Override
	public final void close()
	{
		synchronized (this) {
			if (closed)
				return;
			closed = true;
		}
		onClose();
		notifier.quit();
		try {
			if (conn != null)
				conn.close();
		}
		catch (final Exception ignore) {}
	}

	@Override
	public String toString()
	{
		return "link" + (closed ? " (closed) " : " ") + getName() + " " + medium + ", hopcount " + hopCount;
	}

	/**
	 * Prepares the message in the required EMI format, using the supplied message parameters, and
	 * calls {@link #onSend(CEMILData, boolean)} and {@link #onSend(KNXAddress, byte[], boolean)}.
	 *
	 * @param mc message code
	 * @param dst KNX destination address
	 * @param p message priority
	 * @param nsdu NSDU
	 * @param waitForCon <code>true</code> to wait for a link layer confirmation response,
	 *        <code>false</code> to not wait for the confirmation
	 * @throws KNXTimeoutException on a timeout during send or while waiting for the confirmation
	 * @throws KNXLinkClosedException if the link is closed
	 */
	protected void send(final int mc, final KNXAddress dst, final Priority p, final byte[] nsdu,
		final boolean waitForCon) throws KNXTimeoutException, KNXLinkClosedException
	{
		if (closed)
			throw new KNXLinkClosedException("link closed");
		if (cEMI && !sendCEmiAsByteArray) {
			final CEMILData f = cEMI(mc, dst, p, nsdu);
			onSend(f, waitForCon);
			return;
		}
		onSend(dst, createEmi(mc, dst, p, nsdu), waitForCon);
	}

	/**
	 * Implement with the connection/medium-specific protocol send primitive. Sends the message
	 * supplied as byte array over the link. In case a L-Data confirmation is requested, the message
	 * send is only successful if the corresponding confirmation is received.
	 *
	 * @param dst for logging purposes only: the KNX destination address, or <code>null</code>
	 * @param msg the message to send
	 * @param waitForCon <code>true</code> to wait for a link layer confirmation response,
	 *        <code>false</code> to not wait for the confirmation
	 * @throws KNXTimeoutException on a timeout during send or while waiting for the confirmation
	 * @throws KNXLinkClosedException if the link is closed
	 */
	protected abstract void onSend(final KNXAddress dst, final byte[] msg, final boolean waitForCon)
		throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Implement with the connection/medium-specific protocol send primitive. Sends the message
	 * supplied in cEMI L-Data format over the link. In case a L-Data confirmation is requested, the
	 * message send is only successful if the corresponding confirmation is received.
	 *
	 * @param msg the message to send
	 * @param waitForCon <code>true</code>, if the communication protocol should block and wait for
	 *        the link layer confirmation (L-Data.con), executing all required retransmission
	 *        attempts and timeout validations, <code>false</code> if no confirmation is requested
	 * @throws KNXTimeoutException on a timeout during send or while waiting for the confirmation
	 * @throws KNXLinkClosedException if the link is closed
	 */
	protected abstract void onSend(CEMILData msg, boolean waitForCon)
		throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Returns a cEMI representation, e.g., cEMI L-Data, using the received frame event for EMI and
	 * cEMI formats. Override this method if the used message format does not conform to the
	 * supported EMI 1/2 or cEMI format.
	 *
	 * @param e the received frame event
	 * @return the constructed cEMI message, e.g., cEMI L-Data
	 * @throws KNXFormatException on unsupported frame formats, or errors in the frame format
	 */
	protected CEMI onReceive(final FrameEvent e) throws KNXFormatException
	{
		final CEMI f = e.getFrame();
		return f != null ? f : CEMIFactory.createFromEMI(e.getFrameBytes());
	}

	/**
	 * Invoked on {@link #close()} to execute additional close sequences of the communication
	 * protocol, or releasing link-specific resources.
	 */
	protected void onClose() {}

	private CEMILData adjustMsgType(final CEMILData msg)
	{
		final boolean srcOk = msg.getSource().getRawAddress() != 0;
		// just return if we don't need to adjust source address and don't need LDataEx
		if ((srcOk || medium.getDeviceAddress().getRawAddress() == 0)
				&& (medium instanceof TPSettings || msg instanceof CEMILDataEx))
			return msg;
		return CEMIFactory.create(srcOk ? null : medium.getDeviceAddress(), null, msg, true);
	}

	private void addMediumInfo(final CEMILData msg)
	{
		String s = "";
		if (medium instanceof PLSettings) {
			final CEMILDataEx f = (CEMILDataEx) msg;
			if (f.getAdditionalInfo(CEMILDataEx.ADDINFO_PLMEDIUM) == null)
				f.addAdditionalInfo(CEMILDataEx.ADDINFO_PLMEDIUM, ((PLSettings) medium).getDomainAddress());
		}
		else if (medium.getMedium() == KNXMediumSettings.MEDIUM_RF) {
			final CEMILDataEx f = (CEMILDataEx) msg;
			if (f.getAdditionalInfo(CEMILDataEx.ADDINFO_RFMEDIUM) == null) {
				final RFSettings rf = (RFSettings) medium;
				final byte[] sn = f.isDomainBroadcast() ? rf.getDomainAddress() : rf.getSerialNumber();
				final byte[] ai = new RFMediumInfo(true, rf.isUnidirectional(), sn, 255).getInfo();
				f.addAdditionalInfo(CEMILDataEx.ADDINFO_RFMEDIUM, ai);
				s = f.isDomainBroadcast() ? "(using domain address)" : "(using device SN)";
			}
		}
		else
			return;
		logger.trace("add cEMI additional info for {} {}", medium.getMediumString(), s);
	}

	// Creates the target EMI format using L-Data message parameters
	private byte[] createEmi(final int mc, final KNXAddress dst, final Priority p, final byte[] nsdu)
	{
		if (cEMI)
			return cEMI(mc, dst, p, nsdu).toByteArray();

		final boolean repeat = true;
		final boolean ackRequest = false;
		final boolean posCon = true;
		return CEMIFactory.toEmi(mc, dst, p, repeat, ackRequest, posCon, hopCount, nsdu);
	}

	// Creates the target EMI format using a cEMI L-Data message
	private byte[] createEmi(final CEMILData f)
	{
		if (cEMI) {
			final CEMILData adjusted = adjustMsgType(f);
			addMediumInfo(adjusted);
			return adjusted.toByteArray();
		}
		return CEMIFactory.toEmi(f);
	}

	private CEMILData cEMI(final int mc, final KNXAddress dst, final Priority p, final byte[] nsdu)
	{
		final IndividualAddress src = medium.getDeviceAddress();
		// use default address 0 in system broadcast
		final KNXAddress d = dst == null ? new GroupAddress(0) : dst;
		final boolean repeat = mc == CEMILData.MC_LDATA_IND ? false : true;
		final boolean tp = medium.getMedium() == KNXMediumSettings.MEDIUM_TP1;
		if (nsdu.length <= 16 && tp)
			return new CEMILData(mc, src, d, nsdu, p, repeat, hopCount);

		final boolean pl = medium.getMedium() == KNXMediumSettings.MEDIUM_PL110;
		// TODO allow domain bcast for RF, currently we send all RF L_Data as system broadcast
		final boolean domainBcast = tp ? true : pl && dst != null ? true : false;
		final CEMILDataEx f = new CEMILDataEx(mc, src, d, nsdu, p, repeat, domainBcast, false, hopCount);
		addMediumInfo(f);
		return f;
	}
}

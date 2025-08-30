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

package io.calimero.link;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;

import io.calimero.CloseEvent;
import io.calimero.FrameEvent;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIBusMon;
import io.calimero.cemi.CEMIFactory;
import io.calimero.link.medium.KNXMediumSettings;
import io.calimero.link.medium.PLSettings;
import io.calimero.link.medium.RawFrame;
import io.calimero.link.medium.RawFrameFactory;
import io.calimero.log.LogService;

/**
 * Provides an abstract KNX network monitor implementation, independent of the actual communication
 * protocol and medium access. The monitor supports EMI1/EMI2/cEMI format. Subtypes extend the
 * abstract monitor by specifying the protocol, e.g., KNXnet/IP, or providing a specific
 * implementation of medium access, e.g., via a serial port driver. In most cases, it is sufficient
 * for a subtype to provide an implementation of {@link #onClose()}. For receiving and dispatching
 * frames from the specified protocol, a subtype uses the KNXListener {@link AbstractLink#notifier}
 * as connection listener.
 * <p>
 * In general, once a monitor has been closed, it is not available for further communication and
 * cannot be reopened.
 *
 * @author B. Malinowsky
 */
public abstract class AbstractMonitor<T extends AutoCloseable> implements KNXNetworkMonitor
{
	/** Implements KNXListener to listen to the monitor connection. */
	protected final EventNotifier<LinkListener> notifier;
	protected final Logger logger;

	final T conn;

	volatile boolean wrappedByConnector;
	private volatile boolean closed;
	private final KNXMediumSettings medium;
	private final String name;

	private static final class MonitorNotifier extends EventNotifier<LinkListener>
	{
		volatile boolean decode;
		private boolean extBusmon;

		MonitorNotifier(final AbstractMonitor<?> source, final Logger logger, final boolean extBusmon)
		{
			super(source, logger);
			this.extBusmon = extBusmon;
		}

		@Override
		public void frameReceived(final FrameEvent e)
		{
			try {
				final CEMI frame = e.getFrame();

				final CEMIBusMon mon;
				if (frame == null)
					mon = (CEMIBusMon) CEMIFactory.fromEmiBusmon(e.getFrameBytes());
				else if (frame instanceof final CEMIBusMon busMon)
					mon = busMon;
				else {
					logger.log(WARNING, "received unsupported frame type with msg code 0x"
							+ Integer.toHexString(frame.getMessageCode()));
					return;
				}
				logger.log(TRACE, "{0}", mon);
				final AbstractMonitor<?> netmon = (AbstractMonitor<?>) source;
				MonitorFrameEvent mfe = new MonitorFrameEvent(netmon, mon);
				if (decode) {
					try {
						final RawFrame rf = RawFrameFactory.create(netmon.medium.getMedium(),
								mon.getPayload(), 0, extBusmon);
						mfe = new MonitorFrameEvent(netmon, mon, rf);
					}
					catch (final KNXFormatException ex) {
						logger.log(WARNING, "decoding raw frame", ex);
						mfe = new MonitorFrameEvent(netmon, mon, ex);
						// workaround for PL, BCU might not have switched to ext. busmonitor
						if (extBusmon) {
							extBusmon = false;
							logger.log(WARNING, "disable extended busmonitor mode, maybe this helps");
						}
					}
				}
				final var event = mfe;
				addEvent(l -> l.indication(event));
			}
			catch (KNXFormatException | RuntimeException ex) {
				logger.log(WARNING, "unspecified frame event - ignored", ex);
			}
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			final var monitor = (AbstractMonitor<?>) source;
			monitor.closed = true;
			logger.log(DEBUG, "monitor closed by {0} ({1})", initiator(e.getInitiator()), e.getReason());
			if (monitor.wrappedByConnector) {
				getListeners().listeners().stream().filter(Connector.Link.class::isInstance)
						.forEach(l -> l.linkClosed(e));
 				quit();
				return;
			}
			super.connectionClosed(e);
		}

		private static String initiator(int initiator) {
			return switch (initiator) {
				case CloseEvent.USER_REQUEST -> "link owner";
				case CloseEvent.SERVER_REQUEST -> "server";
				case CloseEvent.CLIENT_REQUEST -> "client";
				case CloseEvent.INTERNAL -> "internal event";
				default -> throw new IllegalStateException("unexpected initiator: " + initiator);
			};
		}
	}

	protected AbstractMonitor(final T conn, final String name, final KNXMediumSettings settings) {
		this.conn = conn;
		this.name = name;
		logger = LogService.getLogger("io.calimero.link." + getName());

		if (settings == null)
			throw new KNXIllegalArgumentException("medium settings are mandatory");
		medium = settings;
		if (medium instanceof PLSettings)
			logger.log(INFO, "power-line medium, assuming BCU has extended busmonitor enabled");

		notifier = new MonitorNotifier(this, logger, settings instanceof PLSettings);
	}

	@Override
	public final KNXMediumSettings getKNXMedium()
	{
		return medium;
	}

	@Override
	public void addMonitorListener(final LinkListener l) { notifier.addListener(l); }

	@Override
	public void removeMonitorListener(final LinkListener l)
	{
		notifier.removeListener(l);
	}

	@Override
	public final void setDecodeRawFrames(final boolean decode)
	{
		final var monitorNotifier = (MonitorNotifier) notifier;
		if (monitorNotifier.decode != decode) {
			monitorNotifier.decode = decode;
			logger.log(INFO, (decode ? "enable" : "disable") + " decoding of raw frames");
		}
	}

	@Override
	public final String getName()
	{
		return name;
	}

	@Override
	public boolean isOpen()
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
		try {
			leaveBusmonitor();
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		onClose();
		try {
			if (conn != null)
				conn.close();
		}
		catch (final Exception ignore) {}
		notifier.quit();
	}

	@Override
	public String toString()
	{
		return "monitor " + getName() + " " + medium.getMediumString() + " medium"
				+ (((MonitorNotifier) notifier).decode ? ", raw frame decoding" : "") + (closed ? " (closed)" : "");
	}

	/**
	 * Invoked on {@link #close()} to execute additional close sequences of the communication
	 * protocol, or releasing link-specific resources.
	 */
	protected void onClose() {}

	/**
	 * Invoked on {@link #close()} to allow the communication protocol to leave busmonitor mode.
	 *
	 * @throws InterruptedException on interrupted thread
	 */
	@SuppressWarnings("unused")
	protected void leaveBusmonitor() throws InterruptedException {}

	void dispatchCustomEvent(final Object event) {
		notifier.dispatchCustomEvent(event);
	}
}

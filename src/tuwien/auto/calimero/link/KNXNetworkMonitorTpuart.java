/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015 B. Malinowsky

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

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.RawFrameFactory;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.serial.TpuartConnection;

/**
 * Implementation of the KNX network monitor link for monitoring a TP1 network, using a
 * {@link TpuartConnection}. Once a monitor has been closed, it is not available for further
 * communication, i.e., it can't be reopened.
 *
 * @author B. Malinowsky
 */
public class KNXNetworkMonitorTpuart implements KNXNetworkMonitor
{
	private static final class MonitorNotifier extends EventNotifier<LinkListener>
	{
		volatile boolean decode;

		MonitorNotifier(final Object source, final Logger logger)
		{
			super(source, logger);
		}

		@Override
		public void frameReceived(final FrameEvent e)
		{
			try {
				final CEMI msg = e.getFrame();
				final byte[] frame = e.getFrameBytes();
				final CEMIBusMon mon;
				if (msg == null)
					mon = (CEMIBusMon) CEMIFactory.create(frame, 0, frame.length);
				else if (msg instanceof CEMIBusMon)
					mon = (CEMIBusMon) msg;
				else
					return;

				logger.trace("received monitor indication");
				final KNXNetworkMonitorTpuart netmon = (KNXNetworkMonitorTpuart) source;
				MonitorFrameEvent mfe = new MonitorFrameEvent(netmon, mon);
				if (decode) {
					try {
						mfe = new MonitorFrameEvent(netmon, mon, RawFrameFactory
								.create(netmon.medium.getMedium(), mon.getPayload(), 0, false));
					}
					catch (final KNXFormatException ex) {
						logger.error("decoding raw frame", ex);
						mfe = new MonitorFrameEvent(netmon, mon, ex);
					}
				}
				addEvent(new Indication(mfe));
			}
			catch (final KNXFormatException | RuntimeException ex) {
				logger.warn("unspecified frame event - ignored", ex);
			}
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			((KNXNetworkMonitorTpuart) source).closed = true;
			super.connectionClosed(e);
			logger.info("monitor closed");
			LogService.removeLogger(logger);
		}
	}

	private volatile boolean closed;
	private final TpuartConnection conn;
	private final KNXMediumSettings medium = TPSettings.TP1;
	private final MonitorNotifier notifier;

	private final String name;
	private final Logger logger;

	/**
	 * Creates a new network monitor using a {@link TpuartConnection} for accessing the KNX network.
	 * The port identifier specifies the serial port for communication. These identifiers are
	 * usually device and platform specific.
	 *
	 * @param portId identifier of the serial communication port
	 * @param decodeRawFrames <code>true</code> to decode the received raw frame on medium for
	 *        notification (supplying the exception if decoding failed), <code>false</code> to
	 *        provide only the cEMI bus monitor indication in the notification
	 * @throws KNXException on error establishing the TP-UART connection
	 */
	public KNXNetworkMonitorTpuart(final String portId, final boolean decodeRawFrames)
		throws KNXException
	{
		conn = new TpuartConnection(portId, Collections.emptyList());
		enterBusmonitor();
		name = "monitor tpuart:" + portId;
		logger = LogService.getLogger(getName());

		notifier = new MonitorNotifier(this, logger);
		setDecodeRawFrames(decodeRawFrames);
		conn.addConnectionListener(notifier);
		notifier.start();
		logger.info("in busmonitor mode - ready to receive");
	}

	@Override
	public void setKNXMedium(final KNXMediumSettings settings)
	{
		logger.info("changing the KNX medium settings for a TP-UART network monitor is useless");
	}

	@Override
	public KNXMediumSettings getKNXMedium()
	{
		return medium;
	}

	@Override
	public void addMonitorListener(final LinkListener l)
	{
		notifier.addListener(l);
	}

	@Override
	public void removeMonitorListener(final LinkListener l)
	{
		notifier.removeListener(l);
	}

	@Override
	public void setDecodeRawFrames(final boolean decode)
	{
		notifier.decode = decode;
		logger.info((decode ? "enable" : "disable") + " decoding of raw frames");
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isOpen()
	{
		return !closed;
	}

	@Override
	public void close()
	{
		synchronized (this) {
			if (closed)
				return;
			closed = true;
		}
		leaveBusmonitor();
		conn.close();
		notifier.quit();
	}

	@Override
	public String toString()
	{
		return getName() + (closed ? " (closed), " : ", ") + medium.getMediumString() + " medium"
				+ (notifier.decode ? ", raw frame decoding" : "");
	}

	private void enterBusmonitor() throws KNXLinkClosedException
	{
		try {
			conn.activateBusmonitor();
		}
		catch (final IOException e) {
			conn.close();
			throw new KNXLinkClosedException("on activating TP-UART busmonitor", e);
		}
	}

	private void leaveBusmonitor()
	{
		// nothing to do, TP-UART is reset anyway on close or establishing new connection
	}
}

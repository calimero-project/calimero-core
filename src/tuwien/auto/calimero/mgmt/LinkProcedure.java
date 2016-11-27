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

import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.BeginConnection;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.ChannelFunctionActuator;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.ChannelFunctionSensor;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.ChannelParamResponse;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.EnterConfigMode;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.LinkResponse;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.None;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.QuitConfigMode;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.ResetInstallation;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.SetChannelParam;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.SetDeleteLink;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.StartLink;
import static tuwien.auto.calimero.mgmt.LinkProcedure.Action.StopLink;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.link.KNXLinkClosedException;

/**
 * Pushbutton-mode (PB-mode) link procedure.
 */
public final class LinkProcedure implements Runnable
{
	// the command code is Action::ordinal()
	enum Action {
		None,
		EnterConfigMode, // Actuator to all
		StartLink, // Sensor to all
		ChannelFunctionActuator, // Actuator to sensor
		ChannelFunctionSensor, // Sensor to actuator
		SetChannelParam, // Actuator to sensor
		ChannelParamResponse, // Sensor to actuator
		BeginConnection, // Actuator to sensor
		SetDeleteLink, // Sensor to actuator
		LinkResponse, // Actuator to sensor
		StopLink, // Sensor to all
		QuitConfigMode, // Actuator to all
		ResetInstallation, // To all, n/a on RF medium
		Features, // Actuator to sensor, sensor to actuator; KNX RF Multi only, action comes after StartLink
	};

	private static final EnumSet<Action> actuator = EnumSet.of(EnterConfigMode, ChannelFunctionActuator,
			SetChannelParam, BeginConnection, LinkResponse, QuitConfigMode);
	private static final EnumSet<Action> sensor = EnumSet.of(StartLink, ChannelFunctionSensor, ChannelParamResponse,
			SetDeleteLink, StopLink);

	private static final int deviceObjectType = 0;
	private static final int pidConfigLink = 59;

	// NYI for the Features response from sensor to actuator, a timeout should not abort the procedure
	private static final long timeout = 3000; // [ms]

	private static final Logger logger = LoggerFactory.getLogger("calimero.mgmt.LinkProcedure");

	private final boolean isActuator;
	// execute reset installation procedure
	private final boolean reset;

	private Action state = None;

	private final ManagementClient mgmt;
	private final IndividualAddress self;
	private final IndividualAddress remote;

	// StartLink
	private final int mfId;
	private final boolean unidir;
	// for actuator: how many group objects to expect, i.e., SetDeleteLink (and send the corresponding LinkResponse)
	private int expectedGroupObjects;

	// false: no additional frames, true: parameter frames are sent after DD2
	private static final boolean paramIndicator = false;
	private static final int subFunction = 0;

	// ChannelFunctionActuator
	private final int channelCode; // 13 bits

	// StartLink

	// SetDeleteLink
	// sub function = { 0, 1, 2 }
	// TODO allow sub function 1 to add scene number support for at least scenes 1 - 8 (encoded as 0 - 7)
	private final int subFunc = 0;
	// Key is either connection code or scene number (subFunc = 1); AckSlotNumber for KNX RF Multi devices (subFunc = 2)
	private final Map<Integer, GroupAddress> groupObjects;
	private int activeConnectionCode;

	// LinkResponse Status Flags
	// Bits 0-1, 2 bit status - one of:
	public static final int LinkAdded = 0;
	public static final int UseExistingAddress = 1;
	public static final int LinkDeleted = 2;
	public static final int LinkNotAdded = 3;
	// Bit 2, error indication (setting error bit will stop link procedure)
	public static final int Error = 4;

	// StopLink
	private boolean abort;
	private boolean noChannel; // also used for QuitConfigMode
	private boolean timerExpired; // also used for QuitConfigMode

	// QuitConfigMode
	private boolean wrongService;

	// the default link change notification does nothing and always succeeds
	private BiFunction<Integer, Map<Integer, GroupAddress>, Integer> linkFunction = (f, g) -> LinkAdded;
	// set by the link function, but only relevant for the actuator returning its status used for link response
	private volatile int linkResponseStatus;

	/**
	 * Creates the link procedure for an actuator. Running the link procedure ({@link #run()}) should be based on the
	 * actuator decision to enter configuration mode.
	 *
	 * @param mgmt management client
	 * @param self the device address of the actuator
	 * @param d sensor destination, or <code>null</code> for broadcast communication mode
	 * @param channelCode E-mode channel code
	 * @return the link procedure initialized for the actuator
	 */
	public static LinkProcedure forActuator(final ManagementClient mgmt, final IndividualAddress self,
		final Destination d, final int channelCode)
	{
		return new LinkProcedure(true, false, mgmt, self, d, false, 0, channelCode, new HashMap<>());
	}

	/**
	 * Creates the link procedure for a sensor, assuming that an actuator notification EnterConfigMode has been
	 * received. Running the procedure ({@link #run()}) should be based on a preceding user action, e.g., pressing a
	 * pushbutton, to send "start link" (the link procedure starts with sending the "start link" event).
	 *
	 * @param mgmt management client
	 * @param self the device address of the sensor
	 * @param d actuator destination, or <code>null</code> for broadcast communication mode
	 * @param unidirectional is this a unidirectional sensor device
	 * @param manufacturerId sensor device manufacturer ID
	 * @param groupObjects the group objects to link or unlink, as mapping of connection code to group address
	 * @return the link procedure initialized for the sensor
	 */
	public static LinkProcedure forSensor(final ManagementClient mgmt, final IndividualAddress self,
		final Destination d, final boolean unidirectional, final int manufacturerId,
		final Map<Integer, GroupAddress> groupObjects)
	{
		return new LinkProcedure(false, false, mgmt, self, d, unidirectional, manufacturerId, 0, groupObjects);
	}

	/**
	 * Creates a link procedure to reset all devices of an installation to factory setup; don't use with RF devices!
	 *
	 * @param mgmt management client
	 * @return link procedure to reset an installation
	 */
	public static LinkProcedure resetInstallation(final ManagementClient mgmt)
	{
		return new LinkProcedure(false, true, mgmt, new IndividualAddress(0), null, false, 0, 0,
				Collections.emptyMap());
	}

	/**
	 * Examines the supplied <code>asdu</code>, to determine whether an actuator (which sent <code>asdu</code>) entered
	 * configuration mode. This method is useful for a sensor device to decide if its link procedure should be started.
	 *
	 * @param asdu the ASDU of a network parameter write service
	 * @return <code>true</code> if the sender of that ASDU entered configuration mode, <code>false</code> otherwise
	 */
	public static boolean isEnterConfigMode(final byte[] asdu)
	{
		final int iot = (asdu[0] & 0xff << 8) | asdu[1] & 0xff;
		final int pid = asdu[2] & 0xff;
		if (iot == deviceObjectType && pid == pidConfigLink) {
			final int command = (asdu[3] & 0xff) >> 4;
			final Action action = Action.values()[command];
			return action == EnterConfigMode;
		}
		return false;
	}

	private LinkProcedure(final boolean isActuator, final boolean reset, final ManagementClient mgmt,
		final IndividualAddress self, final Destination d, final boolean unidir, final int mfId, final int channelCode,
		final Map<Integer, GroupAddress> groupObjects)
	{
		this.isActuator = isActuator;
		this.reset = reset;
		this.mgmt = mgmt;
		this.self = self;
		remote = d != null ? d.getAddress() : null;

		this.unidir = unidir;
		this.mfId = mfId;
		this.channelCode = channelCode;
		this.groupObjects = Collections.synchronizedMap(groupObjects);
	}

	/**
	 * Implementing the link function:<br>
	 * Actuator link function behavior: if the link status (as sent as part of the link response) corresponds to
	 * {@link #UseExistingAddress}, then the group objects map has to be updated to contain that existing address. The
	 * link procedure's link response will send that address back to the sensor, and not the one that was received with
	 * the command to set/delete the link.
	 * <p>
	 * Sensor link function behavior: return code of link function is currently not used
	 *
	 * @param linkFunction link function taking the flags received with the command, and a map of connection code (or
	 *        scene number) to group address
	 */
	public void setLinkFunction(final BiFunction<Integer, Map<Integer, GroupAddress>, Integer> linkFunction)
	{
		this.linkFunction = linkFunction;
	}

	@Override
	public void run()
	{
		final Consumer<FrameEvent> onEvent = this::receivedManagementService;
		try {
			if (reset) {
				write(EnterConfigMode);
				write(ResetInstallation);
				write(QuitConfigMode);
				return;
			}

			logger.info("starting link procedure for {} {}->{}", isActuator ? "actuator" : "sensor", self, remote);
			((ManagementClientImpl) mgmt).addEventListener(onEvent);
			int groupObject = 0;
			final Iterator<Integer> connectionCodes = groupObjects.keySet().iterator();
			final Action[] commands = (isActuator ? actuator : sensor).toArray(new Action[0]);
			for (int i = 0; i < commands.length;) {
				final Action action = commands[i];
				if (action == SetDeleteLink && connectionCodes.hasNext())
					activeConnectionCode = connectionCodes.next();
				write(action);
				if (action == QuitConfigMode)
					return;

				Action next = Action.values()[action.ordinal() + 1];
				if (action == LinkResponse && ++groupObject < expectedGroupObjects)
					next = SetDeleteLink;
				else if (action == SetDeleteLink && connectionCodes.hasNext())
					;
				else
					++i;
				waitFor(next);
			}
		}
		catch (KNXLinkClosedException | KNXTimeoutException | RuntimeException e) {
			stopLink(e);
		}
		catch (final InterruptedException e) {
			abort = true;
			stopLink(e);
			Thread.currentThread().interrupt();
		}
		finally {
			((ManagementClientImpl) mgmt).removeEventListener(onEvent);
			logger.info("finished link procedure for {} {}->{}", isActuator ? "actuator" : "sensor", self, remote);
		}
	}

	/**
	 * @param action link procedure event
	 */
	private void onCommand(final Action action)
	{
		logger.debug("on {}", action);
	}

	private byte[] create(final Action action)
	{
		final int command = action.ordinal() << 4;
		final byte[] value = new byte[] { (byte) command, 0, 0, 0 };

		int flags = 0;
		switch (action) {
		// commented actions are all dealt by default
//		case EnterConfigMode:
//		case BeginConnection:
//		case QuitConfigMode:
//		case ResetInstallation:
//			return value;
		case StartLink:
			flags = (unidir ? 0x08 : 0) | (paramIndicator ? 0x04 : 0) | subFunction;
			value[1] = (byte) (mfId >> 8);
			value[2] = (byte) mfId;
			value[3] = (byte) groupObjects.size();
			break;
		case ChannelFunctionActuator:
		case ChannelFunctionSensor:
			value[1] = (byte) (channelCode >> 8);
			value[2] = (byte) channelCode;
			break;
		case SetDeleteLink:
		case LinkResponse:
			flags = (action == Action.SetDeleteLink) ? subFunc : linkResponseStatus;
			final GroupAddress ga = groupObjects.get(activeConnectionCode);
			final byte[] addr = ga.toByteArray();
			value[1] = (byte) activeConnectionCode; // or scene number
			value[2] = addr[0];
			value[3] = addr[1];
			logger.info("create {}: connection code {} ==> {}", action, activeConnectionCode, ga);
			break;
		case StopLink:
			flags = (abort ? Error : 0) | (noChannel ? 0x02 : 0) | (timerExpired ? 0x01 : 0);
			break;
		case QuitConfigMode:
			flags = timerExpired ? 1 : noChannel ? 2 : wrongService ? 3 : 0;
			break;
		default:
		}

		value[0] |= flags;
		return value;
	}

	private void write(final Action action) throws KNXLinkClosedException, KNXTimeoutException
	{
		final byte[] value = create(action);
		logger.info("send {}", action);
		synchronized (this) {
			mgmt.writeNetworkParameter(remote, deviceObjectType, pidConfigLink, value);
			state = action;
		}
		onCommand(action);
	}

	private void receivedManagementService(final FrameEvent e)
	{
		final byte[] apdu = e.getFrame().getPayload();
		final int svc = DataUnitBuilder.getAPDUService(apdu);
		if (svc == ManagementClientImpl.NetworkParamWrite) {
			if (((CEMILData) e.getFrame()).getSource().equals(self)) {
				logger.debug("received management service sent by us ({}) -- ignore", self);
				return;
			}
			final byte[] asdu = DataUnitBuilder.extractASDU(apdu);
			final int iot = (asdu[0] & 0xff << 8) | asdu[1] & 0xff;
			final int pid = asdu[2] & 0xff;
//			logger.trace("network parameter write IOT {} PID {}", iot, pid);
			if (iot == deviceObjectType && pid == pidConfigLink) {
				final int command = (asdu[3] & 0xff) >> 4;
				final Action action = Action.values()[command];
				parseAction(action, asdu);
				synchronized (this) {
					state = action;
					notifyAll();
				}
			}
		}
	}

	// NYI standard wants us to check reserved/0x00 fields for 0x00, otherwise discard the message
	private void parseAction(final Action action, final byte[] asdu)
	{
		// content of current state in link procedure starts at asdu[3] (command & flags)
		final int flags = asdu[3] & 0x0f;
		switch (action) {
		case StartLink:
			final int code = (asdu[4] & 0xff) << 8 | asdu[5] & 0xff;
			final int objects = asdu[6] & 0xff;
			final boolean unidirectional = (flags & 0x08) == 0x08;
			final boolean params = (flags & 0x04) == 0x04;
			final int subfunc = flags & 0x03;
			logger.debug("received {}: unidir {}, params {}, subfunc {}, manufacturer code {}, "
					+ "group objects to link: {}", action, unidirectional, params, subfunc, code, objects);
			expectedGroupObjects = Math.max(1, objects);
			break;
		case ChannelFunctionActuator:
		case ChannelFunctionSensor:
			final int channel = (asdu[4] & 0xff) << 8 | asdu[5] & 0xff;
			logger.debug("received {}: E-mode channel code {}", action, channel);
			break;
		case SetDeleteLink:
		case LinkResponse:
			final int cc = asdu[4] & 0xff;
			final GroupAddress ga = new GroupAddress((asdu[5] & 0xff) << 8 | asdu[6] & 0xff);
			groupObjects.put(cc, ga);
			logger.info("received {}: flags {}, connection code {} ==> {}", action, flags, cc, ga);

			if (action == SetDeleteLink)
				activeConnectionCode = cc;
			else {
				if (activeConnectionCode != cc)
					logger.error("link response connection code {} does not match {}", cc, activeConnectionCode);
				if ((flags & Error) == Error) {
					abort = true;
					// NYI stop link procedure by notifying run method
				}
			}
//			final Map<Integer, GroupAddress> link = new HashMap<>();
//			link.put(activeConnectionCode, groupObjects.get(activeConnectionCode));
			linkResponseStatus = linkFunction.apply(flags, groupObjects);
			break;
		case StopLink:
		case QuitConfigMode:
			final int status = asdu[3] & 0x07;
			logger.debug("received {}: status {}", action, status);
			break;
		default:
		}
	}

	private void stopLink(final Exception e)
	{
		logger.error("stop link procedure with {}", remote, e);
		try {
			if (state.ordinal() >= EnterConfigMode.ordinal())
				write(Action.StopLink);
		}
		catch (KNXLinkClosedException | KNXTimeoutException expected) {}
	}

	private synchronized void waitFor(final Action next) throws InterruptedException, KNXTimeoutException
	{
		logger.trace("wait for command {}", next);
		final long start = System.currentTimeMillis();
		long remaining = timeout;
		while (remaining > 0) {
			if (state == next) {
				onCommand(next);
				return;
			}
			wait(remaining);
			remaining = remaining - (System.currentTimeMillis() - start);
		}
		timerExpired = true;
		throw new KNXTimeoutException("pairing timeout waiting for " + next);
	}
}

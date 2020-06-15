/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019, 2020 B. Malinowsky

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

package tuwien.auto.calimero.baos;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Implementation of the ObjectServer protocol service data structure, based on BAOS Binary Protocol v2.1.
 */
public class BaosService {

	enum ErrorCode {
		NoError,
		InternalError,
		NoElementFound,
		BufferTooSmall,
		ItemNotWriteable,
		ServiceNotSupported,
		BadServiceParameter,
		BadIdentifier,
		BadCommandOrValue,
		BadLength,
		MessageInconsistent,
		ObjectServerBusy;


		public static ErrorCode of(final int code) { return ErrorCode.values()[code]; }


		public int code() { return ordinal(); }

		@Override
		public String toString() { return message(); }

		private String message() { return name().replaceAll("(\\p{Lower})\\B([A-Z])", "$1 $2").toLowerCase(); }
	}

	public enum Property {
		Invalid(true, 0),
		HardwareType(true, 6),
		HardwareVersion(true, 1),
		FirmwareVersion(true, 1),
		Manufacturer(true, 2),
		ManufacturerApp(true, 2),
		ApplicationId(true, 2),
		ApplicationVersion(true, 2),
		SerialNumber(true, 6),
		TimeSinceReset(true, 4),
		ConnectionState(true, 1),
		MaxBufferSize(false, 2),
		LengthOfDescriptionString(true, 2),
		Baudrate(false, 1),
		CurrentBufferSize(true, 2),
		ProgrammingMode(false, 1),
		ProtocolVersionBinary(true, 1),
		IndicationSending(false, 1),

		// from here on optional properties, supported only in some devices
		ProtocolVersionWeb(true, 1),
		ProtocolVersionRest(true, 1),
		IndividualAddress(true, 2),
		MacAddress(true, 6),
		TunnelingEnabled(true, 1),
		BaosBinaryEnabled(true, 1),
		BaosWebEnabled(true, 1),
		BaosRestEnabled(true, 1),
		HttpFileEnabled(true, 1),
		SearchRequestEnabled(true, 1),
		IsStructured(true, 1), // ??? DatabaseIsStructured
		MaxManagementClients(true, 1),
		ConnectedManagementClients(true, 1),
		MaxTunnelingClients(true, 1),
		ConnectedTunnelingClients(true, 1),
		MaxBaosUdpClients(true, 1),
		ConnectedBaosUdpClients(true, 1),
		MaxBaosTcpClients(true, 1),
		ConnectedBaosTcpClients(true, 1),
		DeviceFriendlyName(true, 30),
		MaxDatapoints(true, 2),
		ConfiguredDatapoints(true, 2),
		MaxParameterBytes(true, 2),
		DownloadCounter(true, 2),
		IPAssignment(true, 1), // ??? which: configured, current?
		IPAddress(true, 4),
		SubnetMask(true, 4),
		DefaultGateway(true, 4),
		TimeSinceResetUnit(true, 1),
		SystemTime(true, -1),
		SystemTimezoneOffset(true, 1),
		MenuEnabled(true, 1),
		EnableSuspend(false, 1);


		public static Property of(final int id) { return Property.values()[id]; }


		private final boolean readOnly;

		private Property(final boolean readOnly, final int size) { this.readOnly = readOnly; }

		public boolean readOnly() { return readOnly; }

		public int id() { return ordinal(); }
	}

	enum DatapointCommand {
		NoCommand,
		SetValue,
		SendValueOnBus,
		SetValueAndSendOnBus,
		ReadValueViaBus,
		ClearDatapointTransmissionState;

		public static DatapointCommand of(final int command) { return DatapointCommand.values()[command]; }
	};

	enum HistoryCommand {
		None,
		Clear,
		Start,
		ClearAndStart,
		Stop,
		StopAndClear
	};

	public static final class Timer {
		public static Timer delete(final int timerId) { return new Timer(timerId, 0, new byte[0], new byte[0], ""); }

		public static Timer oneShot(final int timerId, final ZonedDateTime dateTime, final byte[] job,
				final String description) {
			final var triggerParams = ByteBuffer.allocate(4).putInt((int) dateTime.toEpochSecond()).array();
			return new Timer(timerId, 1, triggerParams, job, description);
		}

		public static Timer interval(final int timerId, final ZonedDateTime start, final ZonedDateTime end,
				final Duration interval, final byte[] job, final String description) {
			if (end.isBefore(start))
				throw new KNXIllegalArgumentException("end " + end + " is before start " + start);

			final var triggerParams = ByteBuffer.allocate(14);
			// date/time range
			triggerParams.putInt((int) start.toEpochSecond()).putInt((int) end.toEpochSecond());
			// interval parts
			final short weeks = (short) (interval.toDays() / 7);
			final byte days = (byte) (interval.toDays() % 7);
			final byte hours = (byte) interval.toHoursPart();
			final byte minutes = (byte) interval.toMinutesPart();
			final byte seconds = (byte) interval.toSecondsPart();
			triggerParams.putShort(weeks).put(days).put(hours).put(minutes).put(seconds);

			return new Timer(timerId, 2, triggerParams.array(), job, description);
		}

		public static Timer from(final ByteBuffer buf) throws KNXFormatException {
			if (buf.remaining() < TimerSize)
				throw new KNXFormatException("");

			final int id = buf.getShort() & 0xffff;
			final var trigger = buf.get() & 0xff;

			final var triggerParams = getWithLengthPrefix(buf);

			final int job = buf.get() & 0xff;
			if (job != JobSetDatapointValue)
				throw new KNXFormatException("");
			final var jobParams = getWithLengthPrefix(buf);

			final var desc = getWithLengthPrefix(buf);
			final String description = new String(desc, StandardCharsets.UTF_8);

			return new Timer(id, trigger, triggerParams, jobParams, description);
		}


		private static final int TimerSize = 7;

		private static final int JobSetDatapointValue = 1;

		private final int id;
		private final int trigger;
		private final byte[] triggerParams;
		private static final int job = JobSetDatapointValue;
		private final byte[] jobParams;
		private final String desc;

		private Timer(final int id, final int trigger, final byte[] triggerParams, final byte[] job,
				final String description) {
			this.id = id;

			this.trigger = trigger;
			if (trigger > 2)
				throw new KNXIllegalArgumentException("unsupported timer trigger " + trigger);
			this.triggerParams = triggerParams.clone();

			this.jobParams = job.clone();
			this.desc = description;
		}

		public final int id() { return id; }

		public final String description() { return desc; }

		public byte[] toByteArray() {
			final int capacity = 4 + triggerParams.length + 2 + jobParams.length + 2 + desc.length(); // XXX utf8?
			final ByteBuffer buf = ByteBuffer.allocate(capacity);
			// trigger
			buf.putShort((short) id).put((byte) trigger).put((byte) triggerParams.length).put(triggerParams);
			// job
			buf.put((byte) job).put((byte) jobParams.length).put(jobParams);
			// description
			// TODO check supported object server charset, ASCII / latin1 only?
			final var descBytes = desc.getBytes(StandardCharsets.UTF_8);
			buf.putShort((short) descBytes.length).put(descBytes);

			return buf.array();
		}

		@Override
		public String toString() {
			final String action = new String[] { "delete", "one-shot", "interval" }[trigger];
			String s = action + " timer " + id;
			s += (desc.length() > 0 ? " (" + desc + ")" : "");
			if (jobParams.length > 4) {
				final int dp = (jobParams[0] & 0xff) << 8 | jobParams[1] & 0xff;
				final String jobInfo = ", set datapoint " + dp + ", command " + (jobParams[2] & 0xff) + ": "
						+ DataUnitBuilder.toHex(Arrays.copyOfRange(jobParams, 4, jobParams.length), " ");
				s += jobInfo;
			}
			return s;
		}
	}

//	private static final int ProtocolV1 = 1;
//	private static final int ProtocolV2 = 2;

	//
	// supported object server services
	//

	private static final int MainService = 0xf0;

	// sub services

	private static final int GetServerItem              = 0x01;
	private static final int SetServerItem              = 0x02;
	private static final int GetDatapointDescription    = 0x03;
	private static final int GetDescriptionString       = 0x04; // ??? add Datapoint
	private static final int GetDatapointValue          = 0x05;
	private static final int SetDatapointValue          = 0x06;
	private static final int GetParameterByte           = 0x07;
	private static final int SetDatapointHistoryCommand = 0x08;
	private static final int GetDatapointHistoryState   = 0x09;
	private static final int GetDatapointHistory        = 0x0a;
	private static final int GetTimer                   = 0x0b;
	private static final int SetTimer                   = 0x0c;

	private static final int DatapointValueIndication	= 0xC1;
	private static final int ServerItemIndication		= 0xC2;

	private static String subServiceString(final int subService) {
		switch (subService) {
		case GetServerItem             : return "GetServerItem";
		case SetServerItem             : return "SetServerItem";
		case GetDatapointDescription   : return "GetDatapointDescription";
		case GetDescriptionString      : return "GetDescriptionString";
		case GetDatapointValue         : return "GetDatapointValue";
		case SetDatapointValue         : return "SetDatapointValue";
		case GetParameterByte          : return "GetParameterByte";
		case SetDatapointHistoryCommand: return "SetDatapointHistoryComma";
		case GetDatapointHistoryState  : return "GetDatapointHistoryState";
		case GetDatapointHistory       : return "GetDatapointHistory";
		case GetTimer                  : return "GetTimer";
		case SetTimer                  : return "SetTimer";
		case DatapointValueIndication  : return "DatapointValueIndication";
		case ServerItemIndication	   : return "ServerItemIndication";
		default: return "" + subService;
		}
	}

	// service code bit for the corresponding service response
	private static final int ResponseFlag = 0x80;

	private static final int MinimumFrameSize = 6;

	private final int subService;
	private final int start;
	private final int items;

	private final byte[] data;
	private final Map<Integer, byte[]> dataById;


	private static boolean isSupportedService(final int subService) {
		if (subService == DatapointValueIndication || subService == ServerItemIndication)
			return true;
		final int svc = subService & ~ResponseFlag;
		if (svc == 0 || svc > SetTimer)
			return false;
		return true;
	}


	public static BaosService getServerItem(final Property startItem, final int items) {
		return new BaosService(GetServerItem, startItem.id(), items);
	}

	public static BaosService setServerItem(final Map<Property, byte[]> items) {
		final var map = items.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().id(), Entry::getValue));
		return new BaosService(SetServerItem, map);
	}

	public static BaosService getDatapointDescription(final int startDatapointId, final int datapoints) {
		return new BaosService(GetDatapointDescription, startDatapointId, datapoints);
	}

	public static BaosService getDatapointDescriptionString(final int startDatapointId, final int datapoints) {
		return new BaosService(GetDescriptionString, startDatapointId, datapoints);
	}

	public static BaosService getDatapointValue(final int startDatapointId, final int datapoints, final int filter) {
		return new BaosService(GetDatapointValue, startDatapointId, datapoints, (byte) filter);
	}

	public static BaosService setDatapointValue(final Map<Integer, byte[]> items) {
		return new BaosService(SetDatapointValue, items);
	}

	public static BaosService getParameter(final int parameterStartIndex, final int bytes) {
		return new BaosService(GetParameterByte, parameterStartIndex, bytes);
	}

	public static BaosService setDatapointHistoryCommand(final int startDatapoint, final int datapoints,
			final HistoryCommand command) {
		return new BaosService(SetDatapointHistoryCommand, startDatapoint, datapoints, (byte) command.ordinal());
	}

	public static BaosService getDatapointHistoryState(final int startDatapoint, final int datapoints) {
		return new BaosService(GetDatapointHistoryState, startDatapoint, datapoints);
	}

	public static BaosService getDatapointHistory(final int startDatapoint, final int datapoints, final Instant start,
			final Instant end) {
		final byte[] range = { 0, 0, 0, 0 }; // TODO serialize timestamps
		return new BaosService(GetDatapointHistory, startDatapoint, datapoints, range);
	}

	public static BaosService getTimer(final int startTimer, final int timers) {
		return new BaosService(GetTimer, startTimer, timers);
	}

	// TODO do timers have to be ordered in the frame (specifically, for sequences of delete/set timer)
	public static BaosService setTimer(final Timer... timers) {
		if (timers.length == 0)
			throw new IllegalArgumentException("no timer supplied");
		return new BaosService(SetTimer, timers[0].id(), timers.length, timerByteArray(timers));
	}

	static BaosService errorResponse(final int subService, final int startItem, final ErrorCode error) {
		return new BaosService(subService, startItem, 0, (byte) error.code());
	}

	public static BaosService from(final ByteBuffer data) throws KNXFormatException {
		final int size = data.remaining();
		if (size < MinimumFrameSize)
			throw new KNXFormatException("");
		if ((data.get() & 0xff) != MainService)
			throw new KNXFormatException("");
		final int subService = data.get() & 0xff;
		if (!isSupportedService(subService))
			throw new KNXFormatException("unsupported service", subService);

		final int start = data.getShort() & 0xffff;
		final int items = data.getShort() & 0xffff;
		// check error response
		if (size == (MinimumFrameSize + 1) && items == 0) {
			final var error = ErrorCode.of(data.get() & 0xff);
			return errorResponse(subService, start, error);
		}

		if (subService == GetParameterByte) {
			if (MinimumFrameSize + items > size)
				throw new KNXFormatException("");
			final byte[] bytes = new byte[items];
			data.get(bytes);
			return new BaosService(subService, start, items, bytes);
		}
		return new BaosService(subService, start, items, data);
	}

	private BaosService(final int service, final int startItem, final int items, final byte... additionalData) {
		subService = service;
		start = startItem;
		this.items = items;
		this.data = additionalData;
		dataById = Map.of();
	}

	private BaosService(final int service, final Map<Integer, byte[]> dataById) {
		subService = service;
		start = Collections.min(dataById.keySet());
		this.items = dataById.size();
		this.data = new byte[0];
		this.dataById = dataById;
	}

	private BaosService(final int service, final int start, final int items, final ByteBuffer buf)
			throws KNXFormatException {
		subService = service;
		this.start = start;
		this.items = items;
		this.data = new byte[0];
		this.dataById = parseItems(buf);
	}

	public final int subService() { return subService; }

	public byte[] toByteArray() {

		final int collectionSize = dataById.values().stream().mapToInt(data -> 3 + data.length).sum();
		final int capacity = 6 + data.length + collectionSize;
		final var frame = ByteBuffer.allocate(capacity);

		frame.put((byte) MainService).put((byte) subService).putShort((short) start).putShort((short) items);
		frame.put(data);
		dataById.keySet().stream().sorted().forEach(id -> {
			final var d = dataById.get(id);
			frame.putShort((short) (int) id).put((byte) d.length).put(d);
		});

		return frame.array();
	}

	@Override
	public String toString() {
		final String response = (subService & ResponseFlag) != 0 ? ".res" : "";
		final String svc = subServiceString(subService & ~ResponseFlag);
		if (items == 0)
			return svc + response + " item " + start + " (" + ErrorCode.of(data[0] & 0xff) + ")";

		final var s = svc + response + " start " + start + " items " + items;
		if (data.length > 0 || !dataById.isEmpty()) {
			return s + ", " + DataUnitBuilder.toHex(data, " ") + dataById.entrySet().stream()
					.map(entry -> ((subService & 0x7f) == GetServerItem ? Property.of(entry.getKey()) : entry.getKey())
							+ "=" + DataUnitBuilder.toHex(entry.getValue(), ""))
					.collect(Collectors.joining(", "));
		}
		return s;
	}

	private Map<Integer, byte[]> parseItems(final ByteBuffer buf) throws KNXFormatException {
		final var dataById = new HashMap<Integer, byte[]>();
		for (int item = 0; item < items; item++) {
			final int remaining = buf.remaining();
			if (remaining < 4)
				throw new KNXFormatException("");

			// get the timer beast out of the way
			if (subService == GetTimer || subService == SetTimer) {
				final var timer = Timer.from(buf);
				dataById.put(timer.id(), timer.toByteArray());
				continue;
			}

			final int id;
			// description response does not provide id per item
			if (subService == GetDescriptionString)
				id = start + item; // ??? is id always incrementing by 1 or not
			else
				id = buf.getShort() & 0xffff;

			if (subService == GetDatapointValue || subService == DatapointValueIndication || subService == GetDatapointHistoryState) {
				final int dpState = buf.get() & 0xff;
			}
			if (subService == SetDatapointValue) {
				final var command = DatapointCommand.of(buf.get() & 0xff);
			}

			final int length;
			// datapoint description does not provide length
			if (subService == GetDatapointDescription)
				length = 3;
			else if (subService == GetDatapointHistoryState)
				length = 4; // 4 byte field containing number of history items
			else if (subService == GetDatapointHistory) {
				final long timestamp = buf.getInt() & 0xffffffffL;
				length = buf.get() & 0xff;
			}
			else {
				// TODO use getWithLengthPrefix
				length = buf.get() & 0xff;
				if (remaining < 3 + length)
					throw new KNXFormatException("");
			}

			final byte[] data = new byte[length];
			buf.get(data);
			dataById.put(id, data);
		}
		if (buf.remaining() > 0)
			throw new KNXFormatException("");
		if (dataById.size() != items)
			throw new KNXFormatException("");

		return dataById;
	}

	private static byte[] getWithLengthPrefix(final ByteBuffer buf) throws KNXFormatException {
		final int length = buf.get() & 0xff;
		if (buf.remaining() < length)
			throw new KNXFormatException("");

		final var data = new byte[length];
		buf.get(data);
		return data;
	}

	private static byte[] timerByteArray(final Timer... timers) {
		final var os = new ByteArrayOutputStream();
		for (final var timer : timers)
			os.writeBytes(timer.toByteArray());
		return os.toByteArray();
	}
}

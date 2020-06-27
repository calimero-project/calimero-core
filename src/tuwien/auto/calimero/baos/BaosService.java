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

import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Implementation of the ObjectServer protocol service data structure, based on BAOS Binary Protocol v2.1.
 */
public final class BaosService {

	public enum ErrorCode {
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
		MaxBufferSize(true, 2),
		LengthOfDescriptionString(true, 2),
		Baudrate(false, 1),
		CurrentBufferSize(false, 2),
		ProgrammingMode(false, 1),
		ProtocolVersionBinary(true, 1),
		IndicationSending(false, 1),

		// from here on optional properties, supported only in some devices
		ProtocolVersionWeb(true, 1),
		ProtocolVersionRest(true, 1),
		IndividualAddress(false, 2),
		MacAddress(true, 6),
		TunnelingEnabled(false, 1),
		BaosBinaryEnabled(false, 1),
		BaosWebEnabled(false, 1),
		BaosRestEnabled(false, 1),
		HttpFileEnabled(false, 1),
		SearchRequestEnabled(false, 1),
		StructuredDatabase(true, 1),
		MaxManagementClients(true, 1),
		ConnectedManagementClients(true, 1),
		MaxTunnelingClients(true, 1),
		ConnectedTunnelingClients(true, 1),
		MaxBaosUdpClients(true, 1),
		ConnectedBaosUdpClients(true, 1),
		MaxBaosTcpClients(true, 1),
		ConnectedBaosTcpClients(true, 1),
		DeviceFriendlyName(false, 30),
		MaxDatapoints(true, 2),
		ConfiguredDatapoints(true, 2),
		MaxParameterBytes(true, 2),
		DownloadCounter(true, 2),
		IpAssignment(false, 1), // ??? which: configured, current?
		IpAddress(false, 4),
		SubnetMask(false, 4),
		DefaultGateway(false, 4),
		TimeSinceResetUnit(false, 1),
		SystemTime(false, -1),
		SystemTimezoneOffset(false, 1),
		MenuEnabled(false, 1),
		EnableSuspend(false, 1);


		public static Property of(final int id) { return Property.values()[id]; }


		private final boolean readOnly;

		Property(final boolean readOnly, final int size) { this.readOnly = readOnly; }

		public int id() { return ordinal(); }

		public String friendlyName() { return name().replaceAll("([A-Z])", " $1").trim(); }

		public boolean readOnly() { return readOnly; }
	}

	public enum ValueFilter {
		All, ValidOnly, UpdatedOnly;

		public static ValueFilter of(final int id) { return values()[id]; }
	}

	public enum DatapointCommand {
		NoCommand,
		SetValue,
		SendValueOnBus,
		SetValueAndSendOnBus,
		ReadValueViaBus,
		ClearTransmissionState;

		public static DatapointCommand of(final int command) { return DatapointCommand.values()[command]; }
	};

	public enum HistoryCommand {
		None,
		Clear,
		Start,
		ClearStart,
		Stop,
		StopClear;

		public static HistoryCommand of(final String command) {
			for (final var v : values())
				if (v.name().equalsIgnoreCase(command))
					return v;
			throw new KNXIllegalArgumentException("invalid history command '" + command + "'");
		}
	};

	public static final class Item<T> {
		public static Item<Property> property(final Property p, final byte[] data) {
			return new Item<>(p.id(), p, data);
		}

		public static Item<DatapointCommand> datapoint(final int dpId, final DatapointCommand cmd, final byte[] data) {
			return new Item<>(dpId, cmd, data);
		}

		private final int id;
		private final T info;
		private final byte[] data;

		private Item(final int id, final T info, final byte[] data) {
			this.id = id;
			this.info = info;
			this.data = data.clone();
		}

		public int id() { return id; }

		public T info() { return info; }

		public byte[] data() { return data.clone(); }

		int size() {
			if (info instanceof Timer)
				return data.length;
			if (info instanceof Instant)
				return 2 + 4 + 1 + data.length;
			if (info instanceof DatapointCommand)
				return 2 + 1 + 1 + data.length;
			return 2 + 1 + data.length;
		}

		byte[] toByteArray() {
			if (info instanceof Timer)
				return data;
			final var buf = allocate(size()).putShort((short) id);
			if (info instanceof Instant)
				buf.putInt((int) ((Instant) info).getEpochSecond());
			else if (info instanceof DatapointCommand)
				buf.put((byte) ((DatapointCommand) info).ordinal());
			return buf.put((byte) data.length).put(data).array();
		}
	}

	public static final class Timer {
		public static Timer delete(final int timerId) { return new Timer(timerId, 0, new byte[0], new byte[0], ""); }

		public static Timer oneShot(final int timerId, final ZonedDateTime dateTime, final byte[] job,
				final String description) {
			final var triggerParams = allocate(4).putInt((int) dateTime.toEpochSecond()).array();
			return new Timer(timerId, 1, triggerParams, job, description);
		}

		public static Timer interval(final int timerId, final ZonedDateTime start, final ZonedDateTime end,
				final Duration interval, final byte[] job, final String description) {
			if (end.isBefore(start))
				throw new KNXIllegalArgumentException("end " + end + " is before start " + start);

			final var triggerParams = allocate(14);
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
			ensureMinSize(0, TimerSize, buf.remaining());

			final int id = buf.getShort() & 0xffff;
			final var trigger = buf.get() & 0xff;

			final var triggerParams = getWithLengthPrefix(buf);

			final int job = buf.get() & 0xff;
			if (job != JobSetDatapointValue)
				throw new KNXFormatException("unsupported timer job type", job);
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

		public int id() { return id; }

		public String description() { return desc; }

		public byte[] toByteArray() {
			final int capacity = 4 + triggerParams.length + 2 + jobParams.length + 2 + desc.length(); // XXX utf8?
			final ByteBuffer buf = allocate(capacity);
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

	public static final int GetServerItem                 = 0x01;
	public static final int SetServerItem                 = 0x02;
	public static final int GetDatapointDescription       = 0x03;
	public static final int GetDatapointDescriptionString = 0x04;
	public static final int GetDatapointValue             = 0x05;
	public static final int SetDatapointValue             = 0x06;
	public static final int GetParameterByte              = 0x07;
	public static final int SetDatapointHistoryCommand    = 0x08;
	public static final int GetDatapointHistoryState      = 0x09;
	public static final int GetDatapointHistory           = 0x0a;
	public static final int GetTimer                      = 0x0b;
	public static final int SetTimer                      = 0x0c;

	public static final int DatapointValueIndication     = 0xC1;
	public static final int ServerItemIndication         = 0xC2;

	private static String subServiceString(final int subService) {
		switch (subService & ~ResponseFlag) {
		case GetServerItem             : return "GetServerItem";
		case SetServerItem             : return "SetServerItem";
		case GetDatapointDescription   : return "GetDatapointDescription";
		case GetDatapointDescriptionString: return "GetDescriptionString";
		case GetDatapointValue         : return "GetDatapointValue";
		case SetDatapointValue         : return "SetDatapointValue";
		case GetParameterByte          : return "GetParameterByte";
		case SetDatapointHistoryCommand: return "SetDatapointHistoryCommand";
		case GetDatapointHistoryState  : return "GetDatapointHistoryState";
		case GetDatapointHistory       : return "GetDatapointHistory";
		case GetTimer                  : return "GetTimer";
		case SetTimer                  : return "SetTimer";
		case DatapointValueIndication  : return "DatapointValueIndication";
		case ServerItemIndication      : return "ServerItemIndication";
		default: return "" + subService;
		}
	}

	// service code bit for the corresponding service response
	private static final int ResponseFlag = 0x80;

	private static final int MinimumFrameSize = 6;

	private final int subService;
	private final int start;
	private final int count;

	private final byte[] data;
	private final List<Item<?>> items;


	public static BaosService getServerItem(final Property startItem, final int items) {
		return new BaosService(GetServerItem, startItem.id(), items);
	}

	@SafeVarargs
	public static BaosService setServerItem(final Item<Property>... items) {
		return new BaosService(SetServerItem, items);
	}

	public static BaosService getDatapointDescription(final int startDatapointId, final int datapoints) {
		return new BaosService(GetDatapointDescription, startDatapointId, datapoints);
	}

	public static BaosService getDatapointDescriptionString(final int startDatapointId, final int datapoints) {
		return new BaosService(GetDatapointDescriptionString, startDatapointId, datapoints);
	}

	public static BaosService getDatapointValue(final int startDatapointId, final int datapoints,
			final ValueFilter filter) {
		return new BaosService(GetDatapointValue, startDatapointId, datapoints, (byte) filter.ordinal());
	}

	@SafeVarargs
	public static BaosService setDatapointValue(final Item<DatapointCommand>... items) {
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
		ensureMinSize(0, MinimumFrameSize, size);
		final int mainService = data.get() & 0xff;
		if (mainService != MainService)
			throw new KNXFormatException("no BAOS service", mainService);
		final int i = data.get() & 0xff;
		final int subService = extractSubService(i);
		final boolean response = (i & 0xc0) == 0x80;

		final int start = data.getShort() & 0xffff;
		final int items = data.getShort() & 0xffff;
		// check error response
		if (response && size == (MinimumFrameSize + 1) && items == 0) {
			final var error = ErrorCode.of(data.get() & 0xff);
			return errorResponse(subService, start, error);
		}

		if (subService == GetParameterByte) {
			ensureMinSize(GetParameterByte, MinimumFrameSize + items, size);
			final byte[] bytes = new byte[items];
			data.get(bytes);
			return new BaosService(subService, start, items, bytes);
		}
		return new BaosService(subService, start, items, data);
	}

	private BaosService(final int service, final int startItem, final int items, final byte... additionalData) {
		subService = service;
		start = startItem;
		this.count = items;
		this.data = additionalData;
		this.items = List.of();
	}

	private BaosService(final int service, final Item<?>... items) {
		if (items.length == 0)
			throw new KNXIllegalArgumentException("no item supplied");
		subService = service;
		start = items[0].id();
		this.count = items.length;
		this.data = new byte[0];
		this.items = List.of(items);
	}

	private BaosService(final int service, final int start, final int items, final ByteBuffer buf)
			throws KNXFormatException {
		subService = service;
		this.start = start;
		this.count = items;
		this.data = new byte[0];
		this.items = List.copyOf(parseItems(buf));
	}

	public int subService() { return subService; }

	public ErrorCode error() { return count == 0 ? ErrorCode.of(data[0]) : ErrorCode.NoError; }

	public List<Item<?>> items() { return items; }

	public byte[] toByteArray() {
		final int collectionSize = items.stream().mapToInt(Item::size).sum();
		final int capacity = 6 + data.length + collectionSize;
		final var frame = allocate(capacity);

		frame.put((byte) MainService).put((byte) subService).putShort((short) start).putShort((short) count);
		frame.put(data);
		items.stream().map(Item::toByteArray).forEach(frame::put);

		return frame.array();
	}

	@Override
	public String toString() {
		final String response = (subService & ResponseFlag) != 0 ? ".res" : "";
		final String svc = subServiceString(subService);
		if (count == 0)
			return svc + response + " item " + start + " (" + ErrorCode.of(data[0] & 0xff) + ")";

		final var s = svc + response + " start " + start + " items " + count;
		if (data.length > 0 || !items.isEmpty()) {
			return s + ", " + DataUnitBuilder.toHex(data, " ") + items.stream()
					.map(item -> ((subService & 0x7f) == GetServerItem ? Property.of(item.id()) : item.id())
							+ "=" + DataUnitBuilder.toHex(item.data, ""))
					.collect(Collectors.joining(", "));
		}
		return s;
	}

	private List<Item<?>> parseItems(final ByteBuffer buf) throws KNXFormatException {
		final var list = new ArrayList<Item<?>>();
		for (int item = 0; item < count; item++) {
			final int remaining = buf.remaining();
			ensureMinSize(subService, 4, remaining);

			// get the timer beast out of the way
			if (subService == GetTimer || subService == SetTimer) {
				final var timer = Timer.from(buf);
				list.add(timer.id(), new Item<>(timer.id(), timer, timer.toByteArray()));
				continue;
			}

			final int id;
			// description response does not provide id per item
			if (subService == GetDatapointDescriptionString)
				id = start + item; // ??? is id always incrementing by 1 or not
			else
				id = buf.getShort() & 0xffff;

			Object info = null;
			if (subService == GetDatapointValue || subService == DatapointValueIndication
					|| subService == GetDatapointHistoryState) {
				final int dpState = buf.get() & 0xff;
				info = dpState;
			}
			else if (subService == SetDatapointValue) {
				final var command = DatapointCommand.of(buf.get() & 0xff);
				info = command;
			}
			else if (subService == GetServerItem || subService == SetServerItem)
				info = Property.of(id);

			// datapoint description and history state don't have length field
			if (subService == GetDatapointDescription || subService == GetDatapointHistoryState) {
				final int length = subService == GetDatapointDescription ? 3 : 4;
				final byte[] data = new byte[length];
				buf.get(data);
				list.add(new Item<>(id, info, data));
			}
			else {
				if (subService == GetDatapointHistory) { // parse extra timestamp field
					final long timestamp = buf.getInt() & 0xffff_ffffL;
					info = Instant.ofEpochSecond(timestamp);
				}

				final byte[] data = getWithLengthPrefix(buf);
				list.add(new Item<>(id, info, data));
			}

		}
		if (buf.remaining() > 0)
			throw new KNXFormatException(
					format("%s invalid structure, %d leftover bytes", subServiceString(subService), buf.remaining()));
		if (list.size() != count)
			throw new KNXFormatException(
					format("%s expected %d items, got %d", subServiceString(subService), count, list.size()));

		return list;
	}

	private static byte[] getWithLengthPrefix(final ByteBuffer buf) throws KNXFormatException {
		final int length = buf.get() & 0xff;
		ensureMinSize(0, length, buf.remaining());

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

	private static void ensureMinSize(final int subService, final int expected, final int actual)
			throws KNXFormatException {
		if (expected > actual) {
			final String svc = subService != 0 ? subServiceString(subService) : "BAOS";
			throw new KNXFormatException(
					format("invalid %s structure, remaining length %d < %d (expected)", svc, actual, expected));
		}
	}

	private static int extractSubService(final int subService) throws KNXFormatException {
		if (subService == DatapointValueIndication || subService == ServerItemIndication)
			return subService;
		final int svc = subService & ~ResponseFlag;
		if (svc == 0 || svc > SetTimer)
			throw new KNXFormatException("unsupported BAOS service", subService);
		return svc;
	}
}

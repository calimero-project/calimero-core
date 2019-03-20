/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019 B. Malinowsky

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

package tuwien.auto.calimero;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified return codes for KNX services and functions, providing a return code, friendly name, and description.
 * <p>
 * Note, that several older KNX services and functions do not use these return codes.
 */
public final class ReturnCode {
	static Map<Integer, ReturnCode> values = Collections.synchronizedMap(new HashMap<>());

	// Generic positive return codes

	public static final ReturnCode Success = new ReturnCode(0x00, "success",
			"service, function or command executed sucessfully");
	public static final ReturnCode SuccessWithCrc = new ReturnCode(0x01, "success with crc",
			"positive message confirmation, CRC over original data");

	// Generic negative return codes

	public static final ReturnCode MemoryError = new ReturnCode(0xf1, "memory error",
			"memory cannot be accessed or only with fault(s)");
	public static final ReturnCode InvalidCommand = new ReturnCode(0xf2, "invalid command",
			"server does not support the requested command"); // ets: also non-existing or protected resource
	public static final ReturnCode ImpossibleCommand = new ReturnCode(0xf3, "impossible command",
			"command cannot be executed because a dependency is not fulfilled");
	public static final ReturnCode ExceedsMaxApduLength = new ReturnCode(0xf4, "exceeds max apdu length",
			"data will not fit into a frame supported by this server"); // received data cannot be evaluated due to reception capabilities
	public static final ReturnCode DataOverflow = new ReturnCode(0xf5, "data overflow",
			"attempt to write data beyond what is reserved for the addressed resource");
	public static final ReturnCode OutOfMinRange = new ReturnCode(0xf6, "out of min range",
			"write value below minimum supported value"); // also DataMin
	public static final ReturnCode OutOfMaxRange = new ReturnCode(0xf7, "out of max range",
			"write value exceeds maximum supported value"); // also DataMax
	public static final ReturnCode DataVoid = new ReturnCode(0xf8, "data void", "request contains invalid data");
	public static final ReturnCode TemporarilyNotAvailable = new ReturnCode(0xf9, "temporarily not available",
			"data access not possible at this time");
	public static final ReturnCode AccessWriteOnly = new ReturnCode(0xfa, "access write-only",
			"read access to write-only resource");
	public static final ReturnCode AccessReadOnly = new ReturnCode(0xfb, "access read-only",
			"write access to read-only resource");
	public static final ReturnCode AccessDenied = new ReturnCode(0xfc, "access denied",
			"access to recource is not allowed because of authorization/security");
	public static final ReturnCode AddressVoid = new ReturnCode(0xfd, "address void", "resource is not present"); // address does not exist
	public static final ReturnCode DataTypeConflict = new ReturnCode(0xfe, "data type conflict",
			"write access with wrong datatype (datapoint length)");
	public static final ReturnCode Error = new ReturnCode(0xff, "error", "service, function or command failed");

	private final int code;
	private final String name;
	private final String description;

	private ReturnCode(final int code, final String name, final String description) {
		this.code = code;
		this.name = name;
		this.description = description;
		values.put(code, this);
	}

	public static ReturnCode of(final int code) {
		if (code < 0 || code > 0xff)
			throw new KNXIllegalArgumentException("return code " + code + " out of range [0..0xff]");
		final ReturnCode rc = values.get(code);
		return rc != null ? rc : new ReturnCode(code, "0x" + Integer.toHexString(code), "unknown return code");
	}

	public int code() { return code; }

	public String friendly() { return name; }

	public String description() { return description; }

	@Override
	public String toString() { return name; }
}

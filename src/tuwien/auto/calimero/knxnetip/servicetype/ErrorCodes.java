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

package tuwien.auto.calimero.knxnetip.servicetype;

/**
 * Contains KNXnet/IP error codes for all supported service types.
 * <p>
 *
 * @author B. Malinowsky
 */
public final class ErrorCodes
{
	// common error codes

	/**
	 * Error code: Operation successful.
	 */
	public static final int NO_ERROR = 0x00;

	/**
	 * Error code: The requested type of host protocol is not supported by the device.
	 */
	public static final int HOST_PROTOCOL_TYPE = 0x01;

	/**
	 * Error code: The requested protocol version is not supported by the device.
	 */
	public static final int VERSION_NOT_SUPPORTED = 0x02;

	/**
	 * Error code: The received sequence number is out of order.
	 */
	public static final int SEQUENCE_NUMBER = 0x04;

	// connect response error codes

	/**
	 * Error code: The server does not support the requested connection type.
	 */
	public static final int CONNECTION_TYPE = 0x22;

	/**
	 * Error code: The server does not support the requested connection options.
	 */
	public static final int CONNECTION_OPTION = 0x23;

	/**
	 * Error code: The server could not accept a new connection, maximum reached.
	 */
	public static final int NO_MORE_CONNECTIONS = 0x24;

	/**
	 * Error code: The requested tunneling layer is not supported by the server.
	 */
	public static final int TUNNELING_LAYER = 0x29;

	// connection state response error codes

	/**
	 * Error code: The server device could not find an active data connection with the
	 * specified ID.
	 */
	public static final int CONNECTION_ID = 0x21;

	/**
	 * Error code: The server detected an error concerning the data connection with the
	 * specified ID.
	 */
	public static final int DATA_CONNECTION = 0x26;

	/**
	 * Error code: The server detected an error concerning the KNX subsystem connection
	 * with the specified ID.
	 */
	public static final int KNX_CONNECTION = 0x27;

	private ErrorCodes()
	{}

	/**
	 * Returns a brief description message for an error code.
	 * <p>
	 * For now, only common error codes are translated.<br>
	 * On unknown error code, the string "error code 0x&lt;code&gt;" is returned.
	 *
	 * @param code error code to lookup the description
	 * @return string representation of error code
	 */
	public static String getErrorMessage(final int code)
	{
		switch (code) {
		case NO_ERROR:
			return "success";
		case HOST_PROTOCOL_TYPE:
			return "host protocol type not supported";
		case VERSION_NOT_SUPPORTED:
			return "protocol version not supported";
		case SEQUENCE_NUMBER:
			return "sequence number out of order";
		case CONNECTION_ID:
			return "no active data connection with that ID";
		case NO_MORE_CONNECTIONS:
			return "could not accept new connection (maximum reached)";
		default:
			return "error code 0x" + Integer.toHexString(code);
		}
	}
}

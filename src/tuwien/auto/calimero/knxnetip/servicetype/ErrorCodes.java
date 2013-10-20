/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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
	 * <p>
	 */
	public static final int NO_ERROR = 0x00;

	/**
	 * Error code: The requested type of host protocol is not supported by the device.
	 * <p>
	 */
	public static final int HOST_PROTOCOL_TYPE = 0x01;

	/**
	 * Error code: The requested protocol version is not supported by the device.
	 * <p>
	 */
	public static final int VERSION_NOT_SUPPORTED = 0x02;

	/**
	 * Error code: The received sequence number is out of order.
	 * <p>
	 */
	public static final int SEQUENCE_NUMBER = 0x04;

	// connect response error codes

	/**
	 * Error code: The server does not support the requested connection type.
	 * <p>
	 */
	public static final int CONNECTION_TYPE = 0x22;

	/**
	 * Error code: The server does not support the requested connection options.
	 * <p>
	 */
	public static final int CONNECTION_OPTION = 0x23;

	/**
	 * Error code: The server could not accept a new connection, maximum reached.
	 * <p>
	 */
	public static final int NO_MORE_CONNECTIONS = 0x24;

	/**
	 * Error code: The requested tunneling layer is not supported by the server.
	 * <p>
	 */
	public static final int TUNNELING_LAYER = 0x29;

	// connection state response error codes

	/**
	 * Error code: The server device could not find an active data connection with the
	 * specified ID.
	 * <p>
	 */
	public static final int CONNECTION_ID = 0x21;

	/**
	 * Error code: The server detected an error concerning the data connection with the
	 * specified ID.
	 * <p>
	 */
	public static final int DATA_CONNECTION = 0x26;

	/**
	 * Error code: The server detected an error concerning the KNX subsystem connection
	 * with the specified ID.
	 * <p>
	 */
	public static final int KNX_CONNECTION = 0x27;

	private ErrorCodes()
	{}

	/**
	 * Returns a brief description message for an error code.
	 * <p>
	 * For now, only common error codes are translated.<br>
	 * On unknown error code, the string "unknown error code" is returned.
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
		default:
			return "error code 0x" + Integer.toHexString(code);
		}
	}
}

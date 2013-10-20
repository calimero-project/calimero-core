/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2012 B. Malinowsky

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

package tuwien.auto.calimero.process;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXInvalidResponseException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;

/**
 * Provides process communication with a KNX network.
 * <p>
 * The process communicator uses application layer group services for communication. Its
 * interface uses high level interaction based on Java data types and blocking read/write
 * functionality.
 * 
 * @author B. Malinowsky
 */
public interface ProcessCommunicator extends ProcessCommunicationBase
{
	// Implementation note: responding to read indications is done the same way as
	// all the writing stuff just with different service, so no magic here...

	/**
	 * Sets the response timeout to wait for a KNX response message to arrive to complete
	 * a message exchange.
	 * <p>
	 * 
	 * @param timeout time in seconds, <code>timeout > 0</code>
	 */
	void setResponseTimeout(int timeout);

	/**
	 * Returns the response timeout used when waiting for a KNX response message to
	 * arrive.
	 * <p>
	 * 
	 * @return timeout in seconds
	 */
	int getResponseTimeout();

	/**
	 * Reads a boolean datapoint value from a group destination.
	 * <p>
	 * 
	 * @param dst group destination to read from
	 * @return the read value of type boolean
	 * @throws KNXTimeoutException on a timeout during send or no read response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	boolean readBool(GroupAddress dst) throws KNXException, InterruptedException;

	/**
	 * Reads an unsigned 8 bit datapoint value from a group destination.
	 * <p>
	 * The predefined scaling format constants are equal to DPT identifiers of the 8 Bit
	 * DPT translator, any other suiting IDs of that type might be specified as well.
	 * 
	 * @param dst group destination to read from
	 * @param scale scaling of the read value before return, one of {@link #SCALING},
	 *        {@link #UNSCALED}, {@link #ANGLE}
	 * @return the read value of type unsigned byte
	 * @throws KNXTimeoutException on a timeout during send or no read response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	int readUnsigned(GroupAddress dst, String scale) throws KNXException, InterruptedException;

	/**
	 * Reads a 3 Bit controlled datapoint value from a group destination.
	 * <p>
	 * The returned value is either positive or negative according to the read control
	 * information. For control bit orientation, the DPT <b>Dimming</b> (DPT ID 3.007) is
	 * used (i.e., control bit type <b>Step</b>). A control value of "decrease" results in
	 * a negative value return, a control value of "increase" results in a positive value
	 * return. The possible value range is -7 (decrease 7) to +7 (increase 7).
	 * 
	 * @param dst group destination to read from
	 * @return the read value of type 3 Bit controlled
	 * @throws KNXTimeoutException on a timeout during send or no read response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	int readControl(GroupAddress dst) throws KNXException, InterruptedException;

	// Temp. keep the following method as forwarder, since i don't know how freq. it is used.
	// Remove at 2.1/2.2
	/**
	 * Deprecated (> v2.0.5), do not longer use; replaced by the 3 arg write.
	 * <p>
	 * Reads a 2-byte KNX float datapoint value from a group destination.
	 * <p>
	 * 
	 * @param dst group destination to read from
	 * @return the read value of type float
	 * @throws KNXTimeoutException on a timeout during send or no read response was received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	float readFloat(GroupAddress dst) throws KNXException, InterruptedException;


	/**
	 * Reads a float datapoint value from a group destination.
	 * <p>
	 * 
	 * @param dst group destination to read from
	 * @param is4ByteFloat specifies the datapoint floating point type the datapoint is encoded
	 *        with: either a 2-byte KNX float of DPT main number 9 (<code>false</code>), or a 4-byte
	 *        float of DPT main number 14 (<code>true</code>)
	 * @return the read value of type float
	 * @throws KNXTimeoutException on a timeout during send or no read response was received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	float readFloat(GroupAddress dst, boolean is4ByteFloat) throws KNXException,
		InterruptedException;

	/**
	 * Reads a string datapoint value from a group destination.
	 * <p>
	 * The supported character set covers at least ISO-8859-1 (Latin 1), with an allowed
	 * string length of 14 characters.
	 * 
	 * @param dst group destination to read from
	 * @return the read value of type string
	 * @throws KNXTimeoutException on a timeout during send or no read response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	String readString(GroupAddress dst) throws KNXException, InterruptedException;

	/**
	 * Reads a datapoint value from a group destination.
	 * <p>
	 * The used KNX message priority is according the supplied datapoint priority.
	 * 
	 * @param dp the datapoint for read
	 * @return the read value in textual representation according the datapoint its type
	 * @throws KNXTimeoutException on a timeout during send or no read response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException if no appropriate DPT translator for the datapoint type is
	 *         available
	 * @throws InterruptedException on interrupt during read
	 */
	String read(Datapoint dp) throws KNXException, InterruptedException;

	/**
	 * Detaches the network link from this process communicator.
	 * <p>
	 * If no network link is attached, no action is performed.
	 * <p>
	 * Note that a detach does not trigger a close of the used network link.
	 * 
	 * @return the formerly attached KNX network link, or <code>null</code> if already
	 *         detached
	 */
	KNXNetworkLink detach();
}

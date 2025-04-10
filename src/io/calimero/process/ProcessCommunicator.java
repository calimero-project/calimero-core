/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.process;

import java.time.Duration;

import io.calimero.GroupAddress;
import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXInvalidResponseException;
import io.calimero.KNXTimeoutException;
import io.calimero.datapoint.Datapoint;
import io.calimero.dptxlator.DPT;
import io.calimero.link.KNXLinkClosedException;
import io.calimero.link.KNXNetworkLink;

/**
 * Provides process communication with a KNX network.
 * <p>
 * The process communicator uses application layer group services for communication. Its
 * interface uses high level interaction based on Java data types and blocking read/write
 * functionality.
 *
 * @author B. Malinowsky
 */
public interface ProcessCommunicator extends ProcessCommunication
{
	// Implementation note: responding to read indications is done the same way as
	// all the writing stuff just with different service, so no magic here...

	/**
	 * Returns the response timeout used when waiting for a KNX response message to arrive.
	 *
	 * @return response timeout
	 */
	Duration responseTimeout();

	/**
	 * Sets the response timeout to wait for a KNX response message to arrive to complete a message exchange.
	 *
	 * @param timeout {@code timeout > 0}
	 */
	void responseTimeout(Duration timeout);

	/**
	 * Reads a boolean datapoint value from a group destination.
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

	/**
	 * Reads a floating point datapoint value from a group destination.
	 *
	 * @param dst group destination to read from
	 * @return the read floating point value
	 * @throws KNXTimeoutException on a timeout during send or no read response was received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation problem of the response data
	 * @throws KNXException on other read problems
	 * @throws InterruptedException on interrupt during read
	 */
	double readFloat(GroupAddress dst) throws KNXException, InterruptedException;

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
	 * The used KNX message priority is according the supplied datapoint priority.<br>
	 * If no {@link DPT} is set for {@code dp}, a non-translated representation of the ASDU is
	 * returned, e.g., a hexadecimal value string.
	 *
	 * @param dp the datapoint to read
	 * @return the read datapoint value in textual representation (translated using the DPT)
	 * @throws KNXTimeoutException on a timeout during send or no read response was received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation error of the response data
	 * @throws KNXException if no appropriate DPT translator for the datapoint type is available
	 * @throws InterruptedException on interrupt during read
	 */
	String read(Datapoint dp) throws KNXException, InterruptedException;

	/**
	 * Reads a numeric datapoint value from a group destination.
	 * <p>
	 * This method is applicable for any datapoint where the DPT translation fits into a {@code double} or
	 * {@code long} data type. If no numeric representation is defined for {@code dp}, any returned value or
	 * exception is implementation-specific. For example, an implementation might parse the ASDU into a value of type
	 * {@code long}.<br>
	 * The used KNX message priority is according the supplied datapoint priority.
	 *
	 * @param dp the datapoint to read
	 * @return the read datapoint value in numerical representation (translated using the DPT)
	 * @throws KNXTimeoutException on a timeout during send or no read response was received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXFormatException on translation error of the response data
	 * @throws KNXException if no appropriate DPT translator for the datapoint type is available
	 * @throws InterruptedException on interrupt during read
	 */
	double readNumeric(Datapoint dp) throws KNXException, InterruptedException;

	/**
	 * Detaches the network link from this process communicator.
	 * <p>
	 * If no network link is attached, no action is performed.
	 * <p>
	 * Note that a detach does not trigger a close of the used network link.
	 *
	 * @return the formerly attached KNX network link, or {@code null} if already
	 *         detached
	 */
	@Override
	KNXNetworkLink detach();
}

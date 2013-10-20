/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2012 B. Malinowsky

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
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;

/**
 * Process communication interface for writing to a KNX network.
 * <p>
 * 
 * @author B. Malinowsky
 */
public interface ProcessCommunicationBase
{
	/**
	 * Represents "on" of datapoint type <b>Switch</b> (DPT ID 1.001), value =
	 * {@value #BOOL_ON}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_ON = true;

	/**
	 * Represents "off" of datapoint type <b>Switch</b> (DPT ID 1.001), value =
	 * {@value #BOOL_OFF}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_OFF = false;

	/**
	 * Represents "up" of datapoint type <b>Up/Down</b> (DPT ID 1.008), value =
	 * {@value #BOOL_UP}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_UP = false;

	/**
	 * Represents "down" of datapoint type <b>Up/Down</b> (DPT ID 1.008), value =
	 * {@value #BOOL_DOWN}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_DOWN = true;

	/**
	 * Represents "start" of datapoint type <b>Start</b> (DPT ID 1.010), value =
	 * {@value #BOOL_START}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_START = true;

	/**
	 * Represents "stop" of datapoint type <b>Start</b> (DPT ID 1.010), value =
	 * {@value #BOOL_STOP}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_STOP = false;

	/**
	 * Represents "increase" of datapoint type <b>Step</b> (DPT ID 1.007), value =
	 * {@value #BOOL_INCREASE}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_INCREASE = true;

	/**
	 * Represents "decrease" of datapoint type <b>Step</b> (DPT ID 1.007), value =
	 * {@value #BOOL_DECREASE}.
	 * <p>
	 * 
	 * @see #write(GroupAddress, boolean)
	 * @see #write(GroupAddress, boolean, int)
	 */
	boolean BOOL_DECREASE = false;

	/**
	 * Represents the scaling format of datapoint type <b>Scaling</b> (DPT ID 5.001).
	 * <p>
	 * This format scales the 8 Bit unsigned value range from 0 to 100.
	 * 
	 * @see #write(GroupAddress, int, String)
	 */
	String SCALING = "5.001";

	/**
	 * Represents the unscaled format, no scaling is used (like in datapoint types
	 * <b>Unsigned count</b> (DPT ID 5.010) or <b>Decimal factor</b> (DPT ID 5.005) ).
	 * <p>
	 * 
	 * @see #write(GroupAddress, int, String)
	 */
	String UNSCALED = "5.010";

	/**
	 * Represents the scaling format of datapoint type <b>Angle</b> (DPT ID 5.003).
	 * <p>
	 * This format scales the 8 Bit unsigned value range from 0 to 360.
	 * 
	 * @see #write(GroupAddress, int, String)
	 */
	String ANGLE = "5.003";
	
	
	
	/**
	 * Sets the KNX message priority for KNX messages to send.
	 * <p>
	 * 
	 * @param p new priority to use
	 */
	void setPriority(Priority p);

	/**
	 * Returns the currently used KNX message priority for KNX messages.
	 * <p>
	 * 
	 * @return message Priority
	 */
	Priority getPriority();

	/**
	 * Adds the specified event listener <code>l</code> to receive events from this
	 * process communicator.
	 * <p>
	 * If <code>l</code> was already added as listener, no action is performed.
	 * 
	 * @param l the listener to add
	 */
	void addProcessListener(ProcessListener l);

	/**
	 * Removes the specified event listener <code>l</code>, so it does no longer
	 * receive events from this process communicator.
	 * <p>
	 * If <code>l</code> was not added in the first place, no action is performed.
	 * 
	 * @param l the listener to remove
	 */
	void removeProcessListener(ProcessListener l);

	/**
	 * Writes a boolean datapoint value to a group destination.
	 * <p>
	 * 
	 * @param dst group destination to write to
	 * @param value boolean value to write, consider the predefined BOOL_* constants (e.g.
	 *        {@link #BOOL_ON})
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void write(GroupAddress dst, boolean value) throws KNXTimeoutException,
		KNXLinkClosedException;

	/**
	 * Writes a 8 bit unsigned datapoint value to a group destination.
	 * <p>
	 * The predefined scaling format constants are equal to DPT identifiers of the 8 Bit
	 * DPT translator, any other suiting IDs of that type might be specified as well.
	 * 
	 * @param dst group destination to write to
	 * @param value unsigned scaled value to write, 0 &lt;= value &lt;= scale format
	 *        specific upper value
	 * @param scale scaling of the read value before return, one of {@link #SCALING},
	 *        {@link #UNSCALED}, {@link #ANGLE}
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXFormatException on translation problem of the supplied datapoint value
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write problems
	 */
	void write(GroupAddress dst, int value, String scale) throws KNXException;

	/**
	 * Writes a 3 bit controlled datapoint value to a group destination.
	 * <p>
	 * 
	 * @param dst group destination to write to
	 * @param control control information, one of the predefined BOOL_* constants of DPT
	 *        <b>Step</b> and DPT <b>Up/Down</b>
	 * @param stepcode stepcode value, 0 &lt;= value &lt;= 7
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXFormatException on translation problem of the supplied datapoint value
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write problems
	 */
	void write(GroupAddress dst, boolean control, int stepcode) throws KNXException;

	// Temp. keep the following method as forwarder, since i don't know how freq. it is used.
	// Remove at 2.1/2.2
	/**
	 * Deprecated (> v2.0.5), do not longer use; replaced by the 3 arg write.
	 * <p>
	 * Writes a 2-byte KNX float datapoint value to a group destination.
	 * <p>
	 * 
	 * @param dst group destination to write to
	 * @param value float value to write
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXFormatException on translation problem of the supplied datapoint value
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write problems
	 */
	void write(GroupAddress dst, float value) throws KNXException;

	/**
	 * Writes a float datapoint value to a group destination.
	 * <p>
	 * The supplied float value is written according to the specified float datapoint type.
	 * 
	 * @param dst group destination to write to
	 * @param value float value to write
	 * @param use4ByteFloat specifies the float type of the datapoint; either writes a 2-byte KNX
	 *        float of DPT main number 9 (<code>false</code>), or a 4-byte float of DPT main number
	 *        14 (<code>true</code>)
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXFormatException on translation problem of the supplied datapoint value
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write problems
	 */
	void write(GroupAddress dst, float value, boolean use4ByteFloat) throws KNXException;
	
	/**
	 * Writes a string datapoint value to a group destination.
	 * <p>
	 * The supported character set covers at least ISO-8859-1 (Latin 1), with an allowed
	 * string length of 14 characters.
	 * 
	 * @param dst group destination to write to
	 * @param value string value to write
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXFormatException on translation problem of the supplied datapoint value
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write problems
	 */
	void write(GroupAddress dst, String value) throws KNXException;

	/**
	 * Writes a datapoint value to a group destination.
	 * <p>
	 * The used KNX message priority is according the supplied datapoint priority.
	 * 
	 * @param dp the datapoint for write
	 * @param value datapoint value in textual representation according the datapoint its
	 *        type
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXFormatException on translation problem of the supplied datapoint value
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException if no appropriate DPT translator for the datapoint type is
	 *         available
	 */
	void write(Datapoint dp, String value) throws KNXException;

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

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

package tuwien.auto.calimero.knxnetip;

import java.net.InetSocketAddress;

import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXTimeoutException;

/**
 * Interface for working with KNX networks over an IP network connection.
 * <p>
 * The data exchange (send and receive) is done through {@link CEMI} messages.
 * Asynchronous or incoming events are relayed to the registered event listeners.
 * <p>
 * Point-to-point logical connections:<br>
 * Implementations with connectionless protocols, like UDP, shall provide heartbeat
 * monitoring as defined by the KNX specification to check the connection state.
 * Connection state messages are sent regularly, every 60 seconds, to the connected
 * server. If the message is not responded to within a timeout of 10 seconds, it is
 * repeated 3 times, and on no response the connection will be terminated.
 * <p>
 * Log information by this connection is provided using the log service with the name
 * obtained from {@link KNXnetIPConnection#getName()}.
 * 
 * @author B. Malinowsky
 * @see KNXnetIPTunnel
 * @see KNXnetIPDevMgmt
 * @see KNXnetIPRouting
 * @see KNXListener
 * @see CEMI
 */
public interface KNXnetIPConnection
{
	/**
	 * Identifier for KNXnet/IP protocol version 1.0.
	 * <p>
	 */
	// same as in KNXnetIPHeader type
	int KNXNETIP_VERSION_10 = 0x10;

	/**
	 * KNXnet/IP default transport layer port number (port {@value #DEFAULT_PORT}) used
	 * for a communication endpoint (besides, this is the fixed port number used for
	 * discovery and routing).
	 * <p>
	 */
	int DEFAULT_PORT = 3671;

	/**
	 * State of communication: in idle state, no error, ready to send.
	 * <p>
	 */
	int OK = 0;

	/**
	 * State of communication: in closed state, no send possible.
	 * <p>
	 */
	int CLOSED = 1;

	/**
	 * Type for blocking mode used in
	 * {@link KNXnetIPConnection#send(CEMI,
	 * tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode)}.
	 * <p>
	 */
	class BlockingMode
	{
		private final String mode;

		BlockingMode(final String mode)
		{
			this.mode = mode;
		}

		/**
		 * Returns a textual representation of this blocking mode.
		 * <p>
		 */
		public String toString()
		{
			return mode;
		}
	}

	/**
	 * Send mode without any blocking for a response.
	 * <p>
	 */
	BlockingMode NONBLOCKING = new BlockingMode("non-blocking");

	/**
	 * Send mode with waiting for service acknowledgment response.
	 * <p>
	 */
	BlockingMode WAIT_FOR_ACK = new BlockingMode("wait for ack");

	/**
	 * Send mode with waiting for cEMI confirmation response.
	 */
	BlockingMode WAIT_FOR_CON = new BlockingMode("wait for cEMI.con");

	/**
	 * Adds the specified event listener <code>l</code> to receive events from this
	 * connection.
	 * <p>
	 * If <code>l</code> was already added as listener, no action is performed.
	 * <p>
	 * Note: the method {@link KNXListener#frameReceived(tuwien.auto.calimero.FrameEvent)}
	 * of an added listener will be invoked by the KNXnet/IP receiver, and not in the
	 * context of the calling thread. Any lengthy processing tasks have to be avoided
	 * during the notification, and should be moved to dedicated own worker thread.
	 * Otherwise subsequent listener invocations will suffer from time delays since the
	 * receiver can not move on.
	 * 
	 * @param l the listener to add
	 */
	void addConnectionListener(KNXListener l);

	/**
	 * Removes the specified event listener <code>l</code>, so it does no longer receive
	 * events from this connection.
	 * <p>
	 * If <code>l</code> was not added in the first place, no action is performed.
	 * 
	 * @param l the listener to remove
	 */
	void removeConnectionListener(KNXListener l);

	/**
	 * Sends a cEMI frame to the remote server communicating with this endpoint.
	 * <p>
	 * The particular subtype of the cEMI frame expected might differ according to the
	 * implementation of the KNXnet/IP connection.<br>
	 * In blocking mode, all necessary retransmissions of sent frames will be done
	 * automatically according to the protocol specification (e.g. in case of timeout). <br>
	 * If a communication failure occurs on the local socket, {@link #close()} is called.
	 * <br>
	 * In blocking send mode, on successfully receiving a response, all listeners are
	 * guaranteed to get notified before this method returns, with the communication state
	 * (see {@link #getState()}) reset to {@link #OK} after the notifying is done, so to
	 * prevent another send call from a listener.
	 * 
	 * @param frame cEMI message to send
	 * @param mode specifies the behavior in regard to response messages, this parameter
	 *        will be ignored by protocols in case no response is expected at all;<br>
	 *        supply one of the {@link BlockingMode} constants, with following blocking
	 *        behavior in increasing order<br> {@link #NONBLOCKING}<br> {@link #WAIT_FOR_ACK}<br>
	 *        {@link #WAIT_FOR_CON}<br>
	 * @throws KNXTimeoutException in a blocking <code>mode</code> if a timeout regarding
	 *         a response message was encountered
	 * @throws KNXConnectionClosedException if no communication was established in the
	 *         first place or communication was closed
	 * @throws KNXIllegalStateException if the send is not permitted by the protocol
	 */
	void send(CEMI frame, BlockingMode mode) throws KNXTimeoutException,
		KNXConnectionClosedException;

	/**
	 * Returns the address (endpoint) this connection endpoint is communicating to.
	 * <p>
	 * The address returned is equal to the one used to establish the communication (e.g.,
	 * the control endpoint address), although internally, different addresses might be
	 * used.<br>
	 * If no communication is established, the unspecified (wildcard) address with port
	 * number 0 is returned.
	 * 
	 * @return IP address/host and port as {@link InetSocketAddress}
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * Returns information about the current KNXnet/IP communication state.
	 * 
	 * @return state enumeration
	 */
	int getState();

	/**
	 * Returns the name of this connection, a brief textual representation to identify a
	 * KNXnet/IP connection.
	 * <p>
	 * The name has to be unique at least for connections with different IP addresses for
	 * the remote control endpoint.<br>
	 * The returned name is used by this connection for the name of its log service.
	 * 
	 * @return name for this connection as string
	 */
	String getName();

	/**
	 * Ends communication with the remote server/client as specified by the used protocol.
	 * <p>
	 * All registered event listeners get notified. The close event is the last event the
	 * listeners receive. <br>
	 * If this connection endpoint is already closed, no action is performed.
	 */
	void close();
}

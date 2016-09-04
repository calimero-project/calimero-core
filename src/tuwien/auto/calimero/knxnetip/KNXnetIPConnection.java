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

package tuwien.auto.calimero.knxnetip;

import java.net.InetSocketAddress;

import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMI;

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
 * Log information of this connection is provided using a logger in the namespace "calimero.knxnetip" and the name
 * obtained from {@link KNXnetIPConnection#getName()}.
 *
 * @author B. Malinowsky
 * @see KNXnetIPTunnel
 * @see KNXnetIPDevMgmt
 * @see KNXnetIPRouting
 * @see KNXListener
 * @see CEMI
 */
public interface KNXnetIPConnection extends AutoCloseable
{
	/**
	 * Identifier for KNXnet/IP protocol version 1.0.
	 */
	// same as in KNXnetIPHeader type
	int KNXNETIP_VERSION_10 = 0x10;

	/**
	 * KNXnet/IP default transport layer port number (port {@value #DEFAULT_PORT}) used
	 * for a communication endpoint (besides, this is the fixed port number used for
	 * discovery and routing).
	 */
	int DEFAULT_PORT = 3671;

	/**
	 * State of communication: in idle state, no error, ready to send.
	 */
	int OK = 0;

	/**
	 * State of communication: in closed state, no send possible.
	 */
	int CLOSED = 1;

	/**
	 * Blocking mode used in {@link KNXnetIPConnection#send(CEMI, KNXnetIPConnection.BlockingMode)}.
	 */
	enum BlockingMode {
		/**
		 * Send mode without any blocking for a response.
		 */
		NonBlocking,

		/**
		 * Send mode with waiting for service acknowledgment response.
		 */
		WaitForAck,

		/**
		 * Send mode with waiting for cEMI confirmation response.
		 */
		WaitForCon
	}


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
	 *        behavior in increasing order: {@link BlockingMode#NonBlocking}, {@link BlockingMode#WaitForAck},
	 *        {@link BlockingMode#WaitForCon}
	 * @throws KNXTimeoutException in a blocking <code>mode</code> if a timeout regarding
	 *         a response message was encountered
	 * @throws KNXConnectionClosedException if no communication was established in the
	 *         first place or communication was closed
	 * @throws InterruptedException on thread interrupt
	 * @throws KNXIllegalStateException if the send is not permitted by the protocol
	 */
	void send(CEMI frame, BlockingMode mode)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException;

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
	@Override
	void close();
}

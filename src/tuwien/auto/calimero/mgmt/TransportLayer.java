/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

package tuwien.auto.calimero.mgmt;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.log.LogService;

/**
 * Transport layer providing OSI layer 4 (connection oriented) communication.
 * <p>
 * A transport layer (TL) communicates using a {@link KNXNetworkLink}. On creation of a
 * new transport layer object, a network link is attached to the layer to be used for
 * communication with the KNX network. This attached link is not owned by the TL and will
 * never get closed by it. If the TL object is not needed anymore, this is indicated by
 * detaching the link from the TL.
 *
 * @author B. Malinowsky
 */
public interface TransportLayer
{
	/**
	 * Creates a new destination using the remote KNX address for doing transport layer
	 * communication.
	 * <p>
	 * The destination is owned and maintained by this transport layer, it is also
	 * responsible for doing all layer 4 communication with that destination. The returned
	 * destination connection state for a new destination is disconnected.<br>
	 * If a destination with the remote address is already available, behavior is
	 * implementation dependent.
	 *
	 * @param remote destination KNX individual address
	 * @param connectionOriented <code>true</code> for connection oriented mode,
	 *        <code>false</code> for connectionless mode
	 * @return destination representing the logical connection
	 * @see Destination
	 */
	Destination createDestination(IndividualAddress remote, boolean connectionOriented);

	/**
	 * Creates a new destination using the remote KNX address and connection settings for
	 * doing transport layer communication.
	 * <p>
	 * The destination is owned and maintained by this transport layer, it is also
	 * responsible for doing all layer 4 communication with that destination. The returned
	 * destination connection state for a new destination is disconnected.<br>
	 * If a destination with the remote address is already available, behavior is
	 * implementation dependent.
	 *
	 * @param remote destination KNX individual address
	 * @param connectionOriented <code>true</code> for connection oriented mode,
	 *        <code>false</code> for connectionless mode
	 * @param keepAlive <code>true</code> to prevent a timing out of the logical
	 *        connection in connection oriented mode, <code>false</code> to use default
	 *        connection timeout
	 * @param verifyMode <code>true</code> to indicate the destination has verify mode
	 *        enabled, <code>false</code> otherwise
	 * @return destination representing the logical connection
	 * @see Destination
	 */
	Destination createDestination(IndividualAddress remote, boolean connectionOriented,
		boolean keepAlive, boolean verifyMode);

	/**
	 * Destroys the given destination and removes it from being maintained by this
	 * transport layer.
	 * <p>
	 * All necessary steps (like disconnecting) are done according to the transport layer
	 * protocol before destroying the destination.<br>
	 * The transport layer does not own the destination any longer.<br>
	 * If the destination is not owned by this transport layer, no action is performed.
	 *
	 * @param d destination to destroy
	 */
	void destroyDestination(Destination d);

	/**
	 * Adds the specified event listener <code>l</code> to receive events from this
	 * transport layer.
	 * <p>
	 * If <code>l</code> was already added as listener, no action is performed.
	 *
	 * @param l the listener to add
	 */
	void addTransportListener(TransportListener l);

	/**
	 * Removes the specified event listener <code>l</code>, so it does no longer
	 * receive events from this transport layer.
	 * <p>
	 * If <code>l</code> was not added in the first place, no action is performed.
	 *
	 * @param l the listener to remove
	 */
	void removeTransportListener(TransportListener l);

	/**
	 * Connects to the destination.
	 * <p>
	 * If the supplied destination is not configured in connection oriented mode or is not
	 * in disconnected state, no action is performed.<br>
	 * On return of this method, the destination is in connected state.
	 *
	 * @param d destination to connect
	 * @throws KNXTimeoutException on timeout during connect
	 * @throws KNXLinkClosedException if sending on a closed KNX network link
	 */
	void connect(Destination d) throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Disconnects from the destination.
	 * <p>
	 * If the supplied destination is in connected state, it will be in disconnected state
	 * on return of this method.<br>
	 * If the destination is already in disconnected state or destroyed, no action is
	 * performed.
	 *
	 * @param d destination to disconnect from
	 * @throws KNXLinkClosedException if sending on a closed KNX network link
	 */
	void disconnect(Destination d) throws KNXLinkClosedException;

	/**
	 * Sends data to the given destination using connection oriented mode.
	 * <p>
	 * A destination has to be set in connected state before sending. All necessary
	 * timeouts are observed and send repetition are done according to the transport layer
	 * protocol. The method blocks for the corresponding layer 4 acknowledge before
	 * returning. If no acknowledgment is received, the destination is disconnected.
	 *
	 * @param d send destination in the KNX network
	 * @param p KNX message priority
	 * @param tsdu transport layer service data unit to send
	 * @throws KNXDisconnectException if destination state is disconnected or a disconnect
	 *         occurs during send
	 * @throws KNXLinkClosedException if sending on a closed KNX network link
	 */
	void sendData(Destination d, Priority p, byte[] tsdu) throws KNXDisconnectException,
		KNXLinkClosedException;

	/**
	 * Sends data to the given KNX address using connectionless mode.
	 * <p>
	 * Depending on the supplied type of KNX address, sending on the KNX network is
	 * unicast or multicast.
	 *
	 * @param addr send destination in the KNX network, address of type
	 *        {@link IndividualAddress} for unicast, {@link GroupAddress} for multicast
	 * @param p KNX message priority
	 * @param tsdu transport layer service data unit to send
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if sending on a closed KNX network link
	 */
	void sendData(KNXAddress addr, Priority p, byte[] tsdu) throws KNXTimeoutException,
		KNXLinkClosedException;

	/**
	 * Broadcasts data in the KNX network.
	 * <p>
	 * The broadcast mode for KNX messages also depends on the used KNX medium, and might
	 * differ from the mode specified here.
	 *
	 * @param system <code>true</code> for system broadcast, <code>false</code> for
	 *        default (domain) broadcast
	 * @param p KNX message priority
	 * @param tsdu transport layer service data unit to send
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if sending on a closed KNX network link
	 */
	void broadcast(boolean system, Priority p, byte[] tsdu) throws KNXTimeoutException,
		KNXLinkClosedException;

	/**
	 * Returns the name of this transport layer, a short textual representation to
	 * identify this layer.
	 * <p>
	 * The name should at least be unique for transport layers attached to different
	 * links.<br>
	 * The returned name is used by this layer as the name of its log service. Supply
	 * {@link #getName()} for {@link LogService#getLogger(String)} for example to get
	 * the associated log service.
	 * <p>
	 * By default, the name starts with "TL " + the name of the attached network link.
	 * After detach of the transport layer the name might get reset to some default name.
	 *
	 * @return transport layer name as string
	 */
	String getName();

	/**
	 * Detaches the network link from this transport layer and terminates all layer 4
	 * communication.
	 * <p>
	 * All owned destinations will get destroyed (equally to
	 * {@link #destroyDestination(Destination)}), other transport layer resources are
	 * freed and all registered event listeners get notified with
	 * {@link TransportListener#detached(tuwien.auto.calimero.DetachEvent)}.<br>
	 * If no network link is attached, no action is performed.
	 * <p>
	 * Note that a detach does not trigger a close of the used network link.
	 *
	 * @return the formerly attached KNX network link, or <code>null</code> if already
	 *         detached
	 */
	KNXNetworkLink detach();
}

/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

package io.calimero.knxnetip;

import static java.lang.System.Logger.Level.ERROR;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.calimero.CloseEvent;
import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.KNXInvalidResponseException;
import io.calimero.KNXRemoteException;
import io.calimero.KNXTimeoutException;
import io.calimero.ServiceType;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIDevMgmt;
import io.calimero.cemi.CemiTData;
import io.calimero.knxnetip.servicetype.ErrorCodes;
import io.calimero.knxnetip.servicetype.KNXnetIPHeader;
import io.calimero.knxnetip.servicetype.PacketHelper;
import io.calimero.knxnetip.servicetype.ServiceAck;
import io.calimero.knxnetip.servicetype.ServiceRequest;
import io.calimero.knxnetip.util.CRI;
import io.calimero.log.LogService;

/**
 * KNXnet/IP connection for KNX local device management, communication on OSI layer 4 is done using UDP, TCP, or
 * Unix domain sockets (UDS).
 *
 * @author B. Malinowsky
 */
public class KNXnetIPDevMgmt extends ClientConnection
{
	/**
	 * Connection type used to configure a KNXnet/IP device.
	 */
	public static final int DEVICE_MGMT_CONNECTION = 0x03;

	// client SHALL wait 10 seconds for a device config response from server
	private static final int CONFIGURATION_REQ_TIMEOUT = 10;

	static final CRI cri = CRI.createRequest(DEVICE_MGMT_CONNECTION);

	/**
	 * Creates a new unsecured KNXnet/IP device management connection over UDP to a remote device.
	 *
	 * @param localEP the local endpoint to use for communication channel
	 * @param serverCtrlEP the remote server control endpoint used for connect request
	 * @param useNAT {@code true} to use a NAT (Network Address Translation) aware communication mechanism,
	 *        {@code false} to use the default way
	 * @throws KNXException on socket communication error
	 * @throws KNXTimeoutException on no connect response before connect timeout
	 * @throws KNXRemoteException if response indicates an error condition at the server concerning the request
	 * @throws KNXInvalidResponseException if connect response is in wrong format
	 * @throws InterruptedException on interrupted thread while creating the management connection
	 */
	public KNXnetIPDevMgmt(final InetSocketAddress localEP, final InetSocketAddress serverCtrlEP, final boolean useNAT)
		throws KNXException, InterruptedException
	{
		this(serverCtrlEP);
		connect(localEP, serverCtrlEP, cri, useNAT);
	}

	/**
	 * Creates a new unsecured KNXnet/IP device management connection over TCP to a remote IP device.
	 *
	 * @param connection a TCP connection to the KNX IP device (if the connection state is not connected, link setup will
	 *        establish the connection); closing the link will not close the TCP connection
	 * @throws KNXException on socket communication error
	 * @throws KNXTimeoutException on no connect response before connect timeout
	 * @throws KNXRemoteException if response indicates an error condition at the remote endpoint concerning the request
	 * @throws KNXInvalidResponseException if connect response is in wrong format
	 * @throws InterruptedException on interrupted thread while creating the management connection
	 */
	public KNXnetIPDevMgmt(final StreamConnection connection) throws KNXException, InterruptedException {
		super(KNXnetIPHeader.DEVICE_CONFIGURATION_REQ, KNXnetIPHeader.DEVICE_CONFIGURATION_ACK, 4,
				CONFIGURATION_REQ_TIMEOUT, connection);
		connect(connection, cri);
	}

	KNXnetIPDevMgmt(final InetSocketAddress serverCtrlEP) {
		super(KNXnetIPHeader.DEVICE_CONFIGURATION_REQ, KNXnetIPHeader.DEVICE_CONFIGURATION_ACK, 4,
				CONFIGURATION_REQ_TIMEOUT);
		ctrlEndpt = serverCtrlEP;
		logger = LogService.getLogger("io.calimero.knxnetip." + name());
	}

	/**
	 * Sends a cEMI device management frame to the remote server communicating with this endpoint.
	 *
	 * @param frame cEMI device management message of type {@link CEMIDevMgmt} or {@link CemiTData} to send
	 */
	@Override
	public void send(final CEMI frame, final BlockingMode mode)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
	{
		if (!(frame instanceof CEMIDevMgmt) && !(frame instanceof CemiTData))
			throw new KNXIllegalArgumentException("unsupported cEMI type");
		super.send(frame, mode);
	}

	@Override
	public String name()
	{
		return "KNXnet/IP DevMgmt " + super.name();
	}

	@Override
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset,
		final InetAddress src, final int port) throws KNXFormatException, IOException
	{
		if (super.handleServiceType(h, data, offset, src, port))
			return true;
		final int svc = h.getServiceType();
		if (svc != serviceRequest)
			return false;

		final ServiceRequest<ServiceType> req = ServiceRequest.from(h, data, offset);
		if (!checkChannelId(req.getChannelID(), "request"))
			return true;

		final int status = h.getVersion() == KNXNETIP_VERSION_10 ? ErrorCodes.NO_ERROR
				: ErrorCodes.VERSION_NOT_SUPPORTED;
		if (!stream) {
			final int seq = req.getSequenceNumber();
			if (seq == getSeqRcv()) {
				final byte[] buf = PacketHelper.toPacket(new ServiceAck(serviceAck, channelId, seq, status));
				send(buf, dataEndpt);
				incSeqRcv();
			}
			else
				logger.log(Level.WARNING, "received dev.mgmt request with rcv-seq {0}, expected {1} - ignored", seq, getSeqRcv());
		}

		if (status == ErrorCodes.VERSION_NOT_SUPPORTED) {
			close(CloseEvent.INTERNAL, "protocol version changed", ERROR, null);
			return true;
		}
		final CEMI cemi = req.service();
		final int mc = cemi.getMessageCode();
		if (mc == CEMIDevMgmt.MC_PROPINFO_IND || mc == CEMIDevMgmt.MC_RESET_IND)
			fireFrameReceived(cemi);
		else if (mc == CEMIDevMgmt.MC_PROPREAD_CON || mc == CEMIDevMgmt.MC_PROPWRITE_CON
				|| mc == CEMIDevMgmt.MC_FUNCPROP_CON || mc == CemiTData.ConnectedIndication
				|| mc == CemiTData.IndividualIndication) {
			// invariant: notify listener before return from blocking send
			fireFrameReceived(cemi);
			setStateNotify(OK);
		}

		return true;
	}
}

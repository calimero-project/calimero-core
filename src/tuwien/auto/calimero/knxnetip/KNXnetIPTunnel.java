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

import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.BusMonitorLayer;
import static tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer.RawLayer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXIllegalStateException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.cemi.CEMILDataEx;
import tuwien.auto.calimero.cemi.CEMILDataEx.AddInfo;
import tuwien.auto.calimero.knxnetip.servicetype.ErrorCodes;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceAck;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest;
import tuwien.auto.calimero.knxnetip.util.TunnelCRI;
import tuwien.auto.calimero.log.LogService.LogLevel;

/**
 * KNXnet/IP connection for KNX tunneling.
 * <p>
 * The tunneling protocol specifies a point-to-point exchange of KNX frames over an IP
 * network connection between two KNXnet/IP devices - client and server.<br>
 * Up to now, only the client side is implemented.<br>
 * The communication on OSI layer 4 is done using UDP.<br>
 *
 * @author B. Malinowsky
 */
public class KNXnetIPTunnel extends ClientConnection
{
	/**
	 * Connection type used to tunnel between two KNXnet/IP devices (client / server).
	 */
	public static final int TUNNEL_CONNECTION = 0x04;


	public enum TunnelingLayer {
		/**
		 * Tunneling on busmonitor layer, establishes a busmonitor tunnel to the KNX network.
		 */
		BusMonitorLayer(0x80),
		/**
		 * Tunneling on link layer, establishes a link layer tunnel to the KNX network.
		 */
		LinkLayer(0x02),
		/**
		 * Tunneling on raw layer, establishes a raw tunnel to the KNX network.
		 */
		RawLayer(0x04);

		private final int code;

		public static TunnelingLayer from(final int layer)
		{
			for (final TunnelingLayer v : TunnelingLayer.values())
				if (layer == v.code)
					return v;
			throw new KNXIllegalArgumentException("unspecified tunneling layer + 0x" + Integer.toHexString(layer));
		}

		TunnelingLayer(final int code) {
			this.code = code;
		}

		public final int getCode() {
			return code;
		}
	}

	// client SHALL wait 1 second for acknowledgment response to a
	// tunneling request from server
	private static final int TUNNELING_REQ_TIMEOUT = 1;

	private final TunnelingLayer layer;

	/**
	 * Creates a new KNXnet/IP tunneling connection to a remote server.
	 * <p>
	 * Establishing a raw tunneling layer (
	 * {@link tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer#RawLayer}) is not
	 * supported yet.<br>
	 *
	 * @param knxLayer KNX tunneling layer (e.g. {@link TunnelingLayer#LinkLayer})
	 * @param localEP specifies the local endpoint with the socket address to be used by the tunnel
	 * @param serverCtrlEP control endpoint of the server to establish connection to
	 * @param useNAT <code>true</code> to use a NAT (network address translation) aware
	 *        communication mechanism, <code>false</code> to use the default way
	 * @throws KNXException on socket communication error
	 * @throws KNXTimeoutException on no connect response before connect timeout
	 * @throws KNXRemoteException if response indicates an error condition at the server concerning
	 *         the request
	 * @throws KNXInvalidResponseException if connect response is in wrong format
	 * @throws InterruptedException on interrupted thread while creating tunneling connection
	 */
	public KNXnetIPTunnel(final TunnelingLayer knxLayer, final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNAT) throws KNXException,
		InterruptedException
	{
		super(KNXnetIPHeader.TUNNELING_REQ, KNXnetIPHeader.TUNNELING_ACK, 2, TUNNELING_REQ_TIMEOUT);
		if (Objects.isNull(knxLayer))
			throw new NullPointerException("Tunneling Layer");
		if (knxLayer == RawLayer)
			throw new KNXIllegalArgumentException("Raw tunnel to KNX network not supported: " + knxLayer);
		layer = knxLayer;
		connect(localEP, serverCtrlEP, new TunnelCRI(knxLayer), useNAT);
	}

	/**
	 * Sends a cEMI frame to the remote server communicating with this endpoint.
	 * <p>
	 * Sending in busmonitor mode is not permitted.<br>
	 *
	 * @param frame cEMI message to send, the expected cEMI type is according to the used
	 *        tunneling layer
	 */
	@Override
	public void send(final CEMI frame, final BlockingMode mode)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
	{
		if (layer == BusMonitorLayer)
			throw new KNXIllegalStateException("send not permitted in busmonitor mode");
		if (!(frame instanceof CEMILData))
			throw new KNXIllegalArgumentException("unsupported cEMI type " + frame.getClass());
		super.send(frame, mode);
	}

	@Override
	public String getName()
	{
		return "KNXnet/IP Tunneling " + super.getName();
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

		final ServiceRequest req = getServiceRequest(h, data, offset);
		if (!checkChannelId(req.getChannelID(), "request"))
			return true;

		final int seq = req.getSequenceNumber();
		final boolean expected = seq == getSeqRcv();
		final boolean repeated = ((seq + 1) & 0xFF) == getSeqRcv();

		// send KNXnet/IP service ack
		if (expected || repeated) {
			final int status = h.getVersion() == KNXNETIP_VERSION_10 ? ErrorCodes.NO_ERROR
					: ErrorCodes.VERSION_NOT_SUPPORTED;
			final byte[] buf = PacketHelper.toPacket(new ServiceAck(serviceAck, channelId, seq, status));
			final DatagramPacket p = new DatagramPacket(buf, buf.length, dataEndpt.getAddress(), dataEndpt.getPort());
			socket.send(p);
			if (status == ErrorCodes.VERSION_NOT_SUPPORTED) {
				close(CloseEvent.INTERNAL, "protocol version changed", LogLevel.ERROR, null);
				return true;
			}
		}
		else {
			logger.warn("tunneling request with invalid rcv-seq {}, expected {}", seq, getSeqRcv());
			return true;
		}

		// ignore repeated tunneling requests
		if (repeated) {
			logger.debug("skip tunneling request with rcv-seq {} (already received)", seq);
			return true;
		}

		// further process all expected tunneling requests
		incSeqRcv();
		final CEMI cemi = req.getCEMI();
		// leave if we are working with an empty (broken) service request
		if (cemi == null)
			return true;

		final int mc = cemi.getMessageCode();
		if (mc == CEMILData.MC_LDATA_IND || mc == CEMIBusMon.MC_BUSMON_IND) {
			logger.trace("received request seq {} (channel {}) cEMI {}", req.getSequenceNumber(), channelId,
					DataUnitBuilder.toHex(cemi.toByteArray(), " "));
			fireFrameReceived(cemi);
		}
		else if (mc == CEMILData.MC_LDATA_CON) {
			// invariant: notify listener before return from blocking send
			logger.debug("received request seq {} (channel {}) cEMI L-Data.con {}->{}", req.getSequenceNumber(),
					channelId, ((CEMILData) cemi).getSource(), ((CEMILData) cemi).getDestination());
			// TODO move notification to after we know it's a valid .con (we should keep it out of the lock, though)
			fireFrameReceived(cemi);

			synchronized (lock) {
				final CEMILData ldata = (CEMILData) keepForCon;
				if (ldata != null && internalState == CEMI_CON_PENDING) {
					// check if address was set by server
					final boolean emptySrc = ldata.getSource().getRawAddress() == 0;
					final List<Integer> types = additionalInfoTypesOf(ldata);
					final byte[] sent = unifyLData(ldata, emptySrc, types);
					final byte[] recv = unifyLData(cemi, emptySrc, types);
					if (Arrays.equals(recv, sent)) {
						keepForCon = null;
						setStateNotify(OK);
					}
					else {
						// we could get a .con with its hop count already decremented by 1 (eibd does that)
						// decrement hop count of sent for comparison
						final int sendCount = ldata.getHopCount() - 1;
						sent[3] = (byte) ((sent[3] & (0x8f)) | (sendCount << 4));
						if (Arrays.equals(recv, sent)) {
							keepForCon = null;
							setStateNotify(OK);
							logger.info("received L_Data.con with hop count decremented by 1 (sent {}, got {})",
									sendCount + 1, sendCount);
						}
					}
				}
			}
		}
		else if (mc == CEMILData.MC_LDATA_REQ)
			logger.warn("received L-Data request - ignored");

		return true;
	}

	private List<Integer> additionalInfoTypesOf(final CEMILData ldata)
	{
		if (ldata instanceof CEMILDataEx) {
			final CEMILDataEx ext = (CEMILDataEx) ldata;
			final List<AddInfo> info = ext.getAdditionalInfo();
			return info.stream().map(AddInfo::getType).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	// types parameter: a workaround introduced for the Gira server, which sometimes adds non-standard
	// additional info. types provides the list of add.info types we want to keep, everything else is removed
	private byte[] unifyLData(final CEMI ldata, final boolean emptySrc, final List<Integer> types)
	{
		final byte[] data;
		if (ldata instanceof CEMILDataEx) {
			final CEMILDataEx ext = ((CEMILDataEx) ldata);
			// remove all add.infos that are not in the types list
			ext.getAdditionalInfo().forEach(info -> {
				if (!types.contains(info.getType())) {
					logger.warn("remove L-Data additional info type {}: {}", info.getType(),
							DataUnitBuilder.toHex(info.getInfo(), ""));
					ext.removeAdditionalInfo(info.getType());
				}
			});
		}
		data = ldata.toByteArray();
		// set msg code field 0
		data[0] = 0;
		// set ctrl1 field 0
		data[1 + data[1] + 1] = 0;
		// conditionally set source address 0
		if (emptySrc) {
			data[1 + data[1] + 3] = 0;
			data[1 + data[1] + 4] = 0;
		}
		return data;
	}
}

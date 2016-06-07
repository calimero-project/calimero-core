/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

package tuwien.auto.calimero.serial.usb;

import java.io.ByteArrayOutputStream;
import java.util.EnumSet;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * A transfer protocol header is used only in a HID report start packet.
 *
 * @author B. Malinowsky
 */
public final class TransferProtocolHeader
{
	/*
	  Transfer protocol header structure

	          -----------------------------------------------------------------------
	  Field   | Protocol | Header | Body length | Protocol | EMI/Ser- | Manufacturer |
	          | Version  | Length |             |    ID    | vice ID  |    Code      |
	  		  -----------------------------------------------------------------------
	  Size    |     1     |   1   |      2      |     1    |  1       |      2       |
	*/

	/** Protocol IDs, only KNX Tunnel and Feature Service is supported. */
	public enum Protocol {
		KnxTunnel(1), BusAccessServerFeature(0x0f);

		final int id;

		Protocol(final int id) { this.id = id; }
		int id() { return id; }
	}

	//
	// EMI/Service identifiers
	//

	// escape for future extension (reserved)
//	private static final int ESCAPE = 0xff;

	@FunctionalInterface
	interface ServiceId {
		int id();
	}

	/** EMI IDs for KNX Tunnel (for other protocol IDs the coding is not defined). */
	public enum KnxTunnelEmi implements ServiceId {
		Emi1, Emi2, CEmi;

		@Override
		public int id() { return ordinal() + 1; }
	}

	/** Service IDs for Bus Access Server device feature service. */
	public enum BusAccessServerService implements ServiceId {
		Get, Response, Set, Info;

		@Override
		public int id() { return ordinal() + 1; }
	}

	private static final int version = 0x0;
	private static final int headerSize = 8;

	private final int length;
	private final Protocol protocol;
	// depending on protocol ID, the device feature service ID or EMI ID
	private final ServiceId svc;
	// set 0x0 for KNX link layer tunnel; for frames not fully compliant to the used protocol ID,
	// the manufacturer's KNX member ID is used
	private final int manufacturer;

	public TransferProtocolHeader(final int bodyLength, final Protocol protocol,
		final ServiceId service)
	{
		if (bodyLength < 0 || bodyLength > 0xffff)
			throw new KNXIllegalArgumentException("body length not in range [0..0xffff]: "
					+ bodyLength);
		length = bodyLength;
		this.protocol = protocol;
		svc = service;
		manufacturer = 0x0;
	}

	public TransferProtocolHeader(final byte[] frame, final int offset) throws KNXFormatException
	{
		if (frame.length - offset < headerSize)
			throw new KNXFormatException("frame to short to fit transfer protocol header");
		int i = offset;
		final int ver = frame[i++] & 0xff;
		if (ver != version)
			throw new KNXFormatException("invalid transfer protocol header version " + ver + " != "
					+ version);
		final int size = frame[i++] & 0xff;
		if (size != headerSize)
			throw new KNXFormatException("unsupported transfer protocol header size " + size + " != "
					+ headerSize);
		final int lhi = (frame[i++] & 0xff) << 8;
		length = lhi | frame[i++] & 0xff;
		final int p = frame[i++] & 0xff;
		final int id = frame[i++] & 0xff;

		final EnumSet<? extends ServiceId> set;
		if (p == Protocol.KnxTunnel.id()) {
			protocol = Protocol.KnxTunnel;
			set = EnumSet.allOf(KnxTunnelEmi.class);
		}
		else if (p == Protocol.BusAccessServerFeature.id()) {
			protocol = Protocol.BusAccessServerFeature;
			set = EnumSet.allOf(BusAccessServerService.class);
		}
		else
			throw new KNXFormatException("unsupported protocol ID", p);
		set.removeIf((s) -> s.id() != id);
		if (set.isEmpty())
			throw new KNXFormatException("unsupported service/EMI ID", id);
		svc = set.iterator().next();

		final int mhi = (frame[i++] & 0xff) << 8;
		manufacturer = mhi | frame[i++] & 0xff;
	}

	/**
	 * @return the protocol version
	 */
	public int getVersion()
	{
		return version;
	}

	/**
	 * @return the data length
	 */
	public int getBodyLength()
	{
		return length;
	}

	/**
	 * @return the protocol ID
	 */
	public Protocol getProtocol()
	{
		return protocol;
	}

	/**
	 * @return the service or EMI ID
	 */
	public ServiceId getService()
	{
		return svc;
	}

	int getStructLength()
	{
		return headerSize;
	}

	byte[] toByteArray(final ByteArrayOutputStream os)
	{
		os.write(version);
		os.write(headerSize);
		os.write(length >>> 8);
		os.write(length);
		os.write(getProtocol().id());
		os.write(getService().id());
		os.write(manufacturer >>> 8);
		os.write(manufacturer);
		return os.toByteArray();
	}
}

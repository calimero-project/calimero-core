/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019, 2021 B. Malinowsky

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

package tuwien.auto.calimero.baos;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.KnxRuntimeException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.link.AbstractLink;
import tuwien.auto.calimero.link.EventNotifier;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkFT12;
import tuwien.auto.calimero.link.KNXNetworkLinkUsb;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

public final class BaosLinkAdapter implements BaosLink {
	private static final VarHandle NOTIFIER;
	private static final MethodHandle REGISTER;
	private static final MethodHandle BAOS_MODE;
	private static final MethodHandle ON_SEND;
	static {
		try {
			final var privateLookup = MethodHandles.privateLookupIn(AbstractLink.class, MethodHandles.lookup());
			NOTIFIER = privateLookup.findVarHandle(AbstractLink.class, "notifier", EventNotifier.class);
			REGISTER = privateLookup.findVirtual(EventNotifier.class, "registerEventType",
					MethodType.methodType(void.class, Class.class));

			final var baosModeType = MethodType.methodType(void.class, boolean.class);
			BAOS_MODE = privateLookup.findVirtual(AbstractLink.class, "baosMode", baosModeType);

			final var onSendType = MethodType.methodType(void.class, List.of(KNXAddress.class, byte[].class, boolean.class));
			ON_SEND = privateLookup.findVirtual(AbstractLink.class, "onSend", onSendType);
		}
		catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final KNXNetworkLink link;

	/**
	 * Returns an adapter for subsequent communication with a BAOS-cabable server.
	 * If BAOS mode is not required anymore, it is possible to keep the underlying link open (and switched back to
	 * link-layer mode) by calling {@link BaosLinkAdapter#detach()}.
	 *
	 * @param link the network link to put into BAOS mode, supported are USB and FT1.2 network links
	 * @return link adapter for BAOS communication
	 * @see KNXNetworkLinkUsb
	 * @see KNXNetworkLinkFT12
	 */
	public static BaosLinkAdapter asBaosLink(final KNXNetworkLink link) { return new BaosLinkAdapter(link); }


	BaosLinkAdapter(final KNXNetworkLink link) {
		this.link = link;
		try {
			final var notifier = NOTIFIER.get(link);
			REGISTER.invoke(notifier, BaosService.class);
		}
		catch (final Throwable e) {
			throw new KnxRuntimeException("adding custom BAOS event", e);
		}
		baosMode(true);
	}

	@Override
	public void setKNXMedium(final KNXMediumSettings settings) { link.setKNXMedium(settings); }

	@Override
	public KNXMediumSettings getKNXMedium() { return link.getKNXMedium(); }

	@Override
	public void addLinkListener(final NetworkLinkListener l) { link.addLinkListener(l); }

	@Override
	public void removeLinkListener(final NetworkLinkListener l) { link.removeLinkListener(l); }

	@Override
	public void setHopCount(final int count) { link.setHopCount(count); }

	@Override
	public int getHopCount() { return link.getHopCount(); }

	@Override
	public void send(final BaosService service) throws KNXTimeoutException, KNXLinkClosedException {
		final var none = new IndividualAddress(0);
		try {
			ON_SEND.invoke(link, none, service.toByteArray(), true);
		}
		catch (KNXTimeoutException | KNXLinkClosedException e) {
			throw e;
		}
		catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendRequest(final KNXAddress dst, final Priority p, final byte[] nsdu)
			throws KNXTimeoutException, KNXLinkClosedException {
		link.sendRequest(dst, p, nsdu);
	}

	@Override
	public void sendRequestWait(final KNXAddress dst, final Priority p, final byte[] nsdu)
			throws KNXTimeoutException, KNXLinkClosedException {
		link.sendRequestWait(dst, p, nsdu);
	}

	@Override
	public void send(final CEMILData msg, final boolean waitForCon)
			throws KNXTimeoutException, KNXLinkClosedException {
		link.send(msg, waitForCon);
	}

	@Override
	public String getName() { return link.getName(); }

	@Override
	public boolean isOpen() { return link.isOpen(); }

	public KNXNetworkLink detach() {
		baosMode(false);
		return link;
	}

	@Override
	public void close() { link.close(); }

	private void baosMode(final boolean enable) {
		try {
			BAOS_MODE.invoke(link, enable);
		}
		catch (final Throwable e) {
			throw new KnxRuntimeException("switching BAOS mode", e);
		}
	}
}

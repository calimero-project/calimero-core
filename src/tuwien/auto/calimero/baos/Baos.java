/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019, 2020 B. Malinowsky

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.KnxRuntimeException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.link.AbstractLink;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

/**
 * Implementation of the ObjectServer protocol, based on BAOS Binary Protocol v2.1.
 */
public final class Baos {

	public static final class KnxBaosLink implements KNXNetworkLink {
		static final MethodHandle CUSTOM_EVENTS;
		static final MethodHandle BAOS_MODE;
		static final MethodHandle ON_SEND;
		static {
			try {
				final var privateLookup = MethodHandles.privateLookupIn(AbstractLink.class, MethodHandles.lookup());
				CUSTOM_EVENTS = privateLookup.findGetter(AbstractLink.class, "customEvents", Map.class);

				final var baosModeType = MethodType.methodType(void.class);
				BAOS_MODE = privateLookup.findVirtual(AbstractLink.class, "baosMode", baosModeType);

				final var onSendType = MethodType.methodType(void.class, List.of(KNXAddress.class, byte[].class, boolean.class));
				ON_SEND = privateLookup.findVirtual(AbstractLink.class, "onSend", onSendType);
			}
			catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private final KNXNetworkLink link;

		KnxBaosLink(final KNXNetworkLink link) {
			this.link = link;
			try {
				final var map = (Map<Class<?>, Set<MethodHandle>>) CUSTOM_EVENTS.invoke(link);
				map.put(BaosService.class, ConcurrentHashMap.newKeySet());
				BAOS_MODE.invoke(link);
			}
			catch (final Throwable e) {
				new KnxRuntimeException("adding custom BAOS event", e);
			}
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

		@Override
		public void close() { link.close(); }
	}

	public static KnxBaosLink asBaosLink(final KNXNetworkLink link) { return new KnxBaosLink(link); }
}

/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2026 B. Malinowsky

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

package io.calimero.mgmt;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import io.calimero.FrameEvent;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.Priority;
import io.calimero.cemi.CEMILData;
import io.calimero.link.KNXNetworkLink;
import io.calimero.link.NetworkLinkListener;
import io.calimero.link.medium.KNXMediumSettings;
import io.calimero.link.medium.TPSettings;
import io.calimero.mgmt.ManagementClientImpl.SupportedServiceGroup;

class SupportedFeaturesTest {
	private static final IndividualAddress local = new IndividualAddress(1, 1, 250);
	private static final IndividualAddress device = new IndividualAddress(1, 1, 2);
	private static final byte[] memory = { 1, 2, 3, 4 };

	// Answers like a device whose Features Supported property (0|89) is not 10 bytes long,
	// so feature discovery ends with no supported features.
	private static final class DeviceLink implements KNXNetworkLink {
		private final List<NetworkLinkListener> listeners = new CopyOnWriteArrayList<>();
		private final TPSettings medium = new TPSettings(local);
		private volatile boolean open = true;

		@Override
		public KNXMediumSettings getKNXMedium() { return medium; }

		@Override
		public void addLinkListener(final NetworkLinkListener l) { listeners.add(l); }

		@Override
		public void removeLinkListener(final NetworkLinkListener l) { listeners.remove(l); }

		@Override
		public void setHopCount(final int count) {}

		@Override
		public int getHopCount() { return 6; }

		@Override
		public void sendRequest(final KNXAddress dst, final Priority p, final byte... nsdu) { respond(dst, nsdu); }

		@Override
		public void sendRequestWait(final KNXAddress dst, final Priority p, final byte... nsdu) { respond(dst, nsdu); }

		@Override
		public void send(final CEMILData msg, final boolean waitForCon) { respond(msg.getDestination(), msg.getPayload()); }

		@Override
		public String getName() { return "device link"; }

		@Override
		public boolean isOpen() { return open; }

		@Override
		public void close() { open = false; }

		private void respond(final KNXAddress dst, final byte[] tpdu) {
			if (!device.equals(dst) || tpdu.length < 2)
				return;
			final int apci = (tpdu[0] & 0x03) << 8 | tpdu[1] & 0xff;
			final byte[] reply;
			if (apci == 0x3d5) // A_PropertyValue_Read: 2 bytes of data instead of 10
				reply = new byte[] { 0x03, (byte) 0xd6, tpdu[2], tpdu[3], tpdu[4], tpdu[5], 0, 0 };
			else if ((apci & 0x3c0) == 0x200) { // A_Memory_Read
				final int n = apci & 0x3f;
				reply = new byte[4 + n];
				reply[0] = 0x02;
				reply[1] = (byte) (0x40 | n);
				reply[2] = tpdu[2];
				reply[3] = tpdu[3];
				System.arraycopy(memory, 0, reply, 4, n);
			}
			else
				return;
			final var frame = new CEMILData(CEMILData.MC_LDATA_IND, device, local, reply, Priority.LOW);
			CompletableFuture.delayedExecutor(10, MILLISECONDS)
					.execute(() -> listeners.forEach(l -> l.indication(new FrameEvent(this, frame))));
		}
	}

	@Test
	void readMemoryFromDeviceWithoutSupportedFeatures() throws Exception {
		try (var link = new DeviceLink();
			 var mc = new ManagementClientImpl(link);
			 var dst = mc.createDestination(device, false)) {
			assertArrayEquals(memory, mc.readMemory(dst, 0x4000, memory.length));
			assertEquals(Optional.of(EnumSet.noneOf(SupportedServiceGroup.class)), dst.supportedFeatures());
		}
	}
}

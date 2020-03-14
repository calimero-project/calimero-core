/*
    Calimero - A library for KNX network access
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

package tuwien.auto.calimero.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Keyring;

public final class Security {

	private static Map<IndividualAddress, byte[]> deviceToolKeys = new ConcurrentHashMap<>();
	private static Map<GroupAddress, byte[]> groupKeys = new ConcurrentHashMap<>();
	private static Map<GroupAddress, Set<IndividualAddress>> groupSenders = new ConcurrentHashMap<>();

	private Security() {}

	public static void useKeyring(final Keyring keyring, final char[] password) {
		final var devices = keyring.devices();
		devices.forEach((addr, device) -> deviceToolKeys.put(addr, keyring.decryptKey(device.toolKey(), password)));

		keyring.groups().forEach((addr, key) -> groupKeys.put(addr, keyring.decryptKey(key, password)));

		final var interfaces = keyring.interfaces().values();
		final var sendersByGroupStream = interfaces.stream().flatMap(List::stream).map(Keyring.Interface::groups)
				.map(Map::entrySet).flatMap(Set::stream);
		sendersByGroupStream
				.forEach(entry -> groupSenders.merge(entry.getKey(), concurrentSetOf(entry.getValue()), (a, b) -> {
					a.addAll(b);
					return a;
				}));
	}

	private static <T> Set<T> concurrentSetOf(final Set<T> t) {
		final var set = ConcurrentHashMap.<T> newKeySet();
		set.addAll(t);
		return set;
	}

	public static Map<IndividualAddress, byte[]> deviceToolKeys() {
		return deviceToolKeys;
	}

	public static Map<GroupAddress, byte[]> groupKeys() {
		return groupKeys;
	}

	public static Map<GroupAddress, Set<IndividualAddress>> groupSenders() {
		return groupSenders;
	}
}

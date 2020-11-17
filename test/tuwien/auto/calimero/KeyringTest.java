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

package tuwien.auto.calimero;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tuwien.auto.calimero.DataUnitBuilder.fromHex;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.Test;

import tuwien.auto.calimero.Keyring.Interface;
import tuwien.auto.calimero.xml.KNXMLException;

class KeyringTest {

	private final String keyringUri = "test/resources/KeyringTest.knxkeys";
	private final IndividualAddress host = new IndividualAddress(1, 1, 0);
	private final char[] keyringPwd = "pwd".toCharArray();

	@Test
	void loadValidKeyringConstructor() {
		final var keyring = new Keyring(keyringUri, keyringPwd);
		keyring.load();
	}

	@Test
	void loadValidKeyring() {
		final var keyring = Keyring.load(keyringUri);
		final var interfaces = keyring.interfaces();

		assertEquals(8, interfaces.get(host).size());
	}

	@Test
	void decryptBackboneKey() throws UnknownHostException {
		final var keyring = Keyring.load(keyringUri);
		final var config = keyring.configuration();

		final byte[] backboneKey = (byte[]) config.get(InetAddress.getByName("224.0.23.12"));
		final byte[] decrypted = fromHex("96F034FCCF510760CBD63DA0F70D4A9D");
		assertArrayEquals(decrypted, keyring.decryptKey(backboneKey, keyringPwd));
	}

	@Test
	void allEncryptedKeysHaveExpectedLength() throws UnknownHostException {
		final var keyring = Keyring.load(keyringUri);
		final var config = keyring.configuration();

		final byte[] backboneKey = (byte[]) config.get(InetAddress.getByName("224.0.23.12"));
		assertEquals(16, backboneKey.length);

		for (final var device : keyring.devices().values())
			assertEquals(16, device.toolKey().get().length);

		final byte[] groupKey = keyring.groups().get(new GroupAddress(1, 1, 1));
		assertEquals(16, groupKey.length);
	}

	@Test
	void allEncryptedPasswordsHaveExpectedLength() {
		final var keyring = Keyring.load(keyringUri);

		for (final var device : keyring.devices().values()) {
			device.password().ifPresent(password ->  assertEquals(32, password.length));
			device.authentication().ifPresent(auth -> assertEquals(32, auth.length));
		}

		final var interfaces = keyring.interfaces().get(host);
		for (final var iface : interfaces) {
			iface.password().ifPresent(password ->  assertEquals(32, password.length));
			iface.authentication().ifPresent(auth -> assertEquals(32, auth.length));
		}
	}

	@Test
	void decryptInterfacePasswords() {
		final var keyring = Keyring.load(keyringUri);

		assertInterfacePasswords(keyring, 1);
		assertInterfacePasswords(keyring, 2);
		assertInterfacePasswords(keyring, 3);
		assertInterfacePasswords(keyring, 4);
	}

	private void assertInterfacePasswords(final Keyring keyring, final int user) {
		final var interfaces = keyring.interfaces().get(host);
		for (final var iface : interfaces) {
			if (iface.address().getDevice() == user) {
				final byte[] pwd = iface.password().get();
				assertEquals(32, pwd.length);
				assertArrayEquals(("user" + user).toCharArray(), keyring.decryptPassword(pwd, keyringPwd));

				final byte[] authCode = iface.authentication().get();
				assertEquals(32, authCode.length);
				assertArrayEquals("dev".toCharArray(), keyring.decryptPassword(authCode, keyringPwd));
				return;
			}
		}
		fail("no user " + user);
	}

	@Test
	void decryptToolKey() {
		final var keyring = Keyring.load(keyringUri);

		final var device = keyring.devices().get(host);
		final byte[] toolkey = fromHex("AEAC47C4653ED0B25249B4AB3F474479");
		assertArrayEquals(toolkey, keyring.decryptKey(device.toolKey().get(), keyringPwd));
	}

	@Test
	void decryptDevicePasswords() {
		final var keyring = Keyring.load(keyringUri);

		final var device = keyring.devices().get(host);
		assertArrayEquals("dev".toCharArray(), keyring.decryptPassword(device.authentication().get(), keyringPwd));
		assertArrayEquals("router1".toCharArray(), keyring.decryptPassword(device.password().get(), keyringPwd));
	}

	@Test
	void decryptGroupKey() {
		final var keyring = Keyring.load(keyringUri);

		final List<Interface> ifaces = keyring.interfaces().get(host);
		final Interface iface = ifaces.get(0);
		final GroupAddress group = new GroupAddress(1, 1, 1);
		assertTrue(iface.groups().containsKey(group));

		final byte[] groupAddrKey = fromHex("E14343050F4377E3159B90AFE0228216");
		final byte[] encrypted = keyring.groups().get(group);
		assertArrayEquals(groupAddrKey, keyring.decryptKey(encrypted, keyringPwd));
	}

	@Test
	void keyringWithValidSignature() {
		final var keyring = Keyring.load(keyringUri);
		assertTrue(keyring.verifySignature(keyringPwd));
	}

	@Test
	void keyringWithWrongSignature() {
		assertThrows(KnxSecureException.class,
				() -> new Keyring("test/resources/WrongSignature.knxkeys", keyringPwd).load());

		final var keyring = Keyring.load("test/resources/WrongSignature.knxkeys");
		assertFalse(keyring.verifySignature(keyringPwd));
	}

	@Test
	void keyringWithWrongFilenameExtension() {
		assertThrows(KNXIllegalArgumentException.class,
				() -> Keyring.load("test/resources/KeyringTest.wrongExtension"));
	}

	@Test
	void keyringWithoutKeyringElement() {
		assertThrows(KNXMLException.class, () -> Keyring.load("test/resources/NoKeyringElement.knxkeys"));
	}
}

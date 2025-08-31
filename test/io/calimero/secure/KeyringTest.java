/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2019, 2025 B. Malinowsky

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

package io.calimero.secure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.secure.Keyring.Interface;
import io.calimero.secure.Keyring.Interface.Type;
import io.calimero.xml.KNXMLException;

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
	void decryptBackboneKey() {
		final var keyring = Keyring.load(keyringUri);
		final var backbone = keyring.backbone().orElseThrow();

		final byte[] decrypted = HexFormat.of().parseHex("96F034FCCF510760CBD63DA0F70D4A9D");
		assertArrayEquals(decrypted, keyring.decryptKey(backbone.groupKey().get(), keyringPwd));
	}

	@Test
	void allEncryptedKeysHaveExpectedLength() {
		final var keyring = Keyring.load(keyringUri);
		final var backbone = keyring.backbone().orElseThrow();

		assertEquals(16, backbone.groupKey().get().length);

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
		final byte[] toolkey = HexFormat.of().parseHex("AEAC47C4653ED0B25249B4AB3F474479");
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
		final Interface iface = ifaces.getFirst();
		final GroupAddress group = new GroupAddress(1, 1, 1);
		assertTrue(iface.groups().containsKey(group));

		final byte[] groupAddrKey = HexFormat.of().parseHex("E14343050F4377E3159B90AFE0228216");
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
			new Keyring("test/resources/WrongSignature.knxkeys", keyringPwd)::load);

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

	@Test
	void backbonesAreEqual() throws UnknownHostException {
		final var bb1 = new Keyring.Backbone(multicastGroup(), groupKey(), Duration.ofSeconds(2));
		final var bb2 = new Keyring.Backbone(multicastGroup(), groupKey(), Duration.ofSeconds(2));
		assertEquals(bb1, bb2);
	}

	@Test
	void backbonesAreNotEqual() throws UnknownHostException {
		var bb1 = new Keyring.Backbone(multicastGroup(), groupKey(), Duration.ofSeconds(2));
		var bb2 = new Keyring.Backbone(InetAddress.getByName("224.0.23.13"), groupKey(), Duration.ofSeconds(2));
		assertNotEquals(bb1, bb2);

		bb1 = new Keyring.Backbone(multicastGroup(), groupKey(), Duration.ofSeconds(2));
		bb2 = new Keyring.Backbone(multicastGroup(), Arrays.copyOf(new byte[] { 2 }, 16), Duration.ofSeconds(2));
		assertNotEquals(bb1, bb2);

		bb1 = new Keyring.Backbone(multicastGroup(), groupKey(), Duration.ofSeconds(1));
		bb2 = new Keyring.Backbone(multicastGroup(), groupKey(), Duration.ofSeconds(2));
		assertNotEquals(bb1, bb2);
	}

	@Test
	void keyringEquals() {
		final var keyring1 = Keyring.load(keyringUri);
		final var keyring2 = Keyring.load(keyringUri);
		assertEquals(keyring1, keyring2);
	}

	@Test
	void keyringSameHashCode() {
		final var keyring1 = Keyring.load(keyringUri);
		final var keyring2 = Keyring.load(keyringUri);
		assertEquals(keyring1.hashCode(), keyring2.hashCode());
	}

	@Test
	void keyringToString() {
		final var keyring = Keyring.load(keyringUri);
		System.out.println(keyring);
	}

	private static InetAddress multicastGroup() throws UnknownHostException {
		return InetAddress.getByName("224.0.23.12");
	}

	private static byte[] groupKey() { return Arrays.copyOf(new byte[] { 1 }, 16); }

	@ParameterizedTest
	@ValueSource(strings = {"Backbone", "Tunneling", "USB"})
	void validInterfaceType(final String type) {
		Interface.Type.from(type);
	}

	@ParameterizedTest
	@ValueSource(strings = {"BackBone", "Tunnelling", "Usb", "blah"})
	void invalidInterfaceType(final String type) {
		assertThrows(KNXIllegalArgumentException.class, () -> Interface.Type.from(type));
	}

	@Test
	void decryptInterface() {
		final var keyring = Keyring.load(keyringUri);
		final var interfaces = keyring.interfaces();
		final var secIf = interfaces.get(host).getFirst();
		final var tunnelInterface = secIf.decrypt(keyringPwd);

		assertEquals(tunnelInterface.type(), Type.Tunneling);
		assertTrue(tunnelInterface.user() > 0);
		assertEquals(tunnelInterface.userKey().length, 16);
		assertEquals(tunnelInterface.deviceAuthCode().length, 16);
	}
}

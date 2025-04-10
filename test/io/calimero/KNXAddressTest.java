/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2014, 2019 B. Malinowsky

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

package io.calimero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.calimero.xml.KNXMLException;
import io.calimero.xml.XmlInputFactory;

class KNXAddressTest {

	@Test
	void createFromString() throws KNXFormatException {
		KNXAddress.create("1/2/3");
		KNXAddress.create("3.6.9");
		try {
			KNXAddress.create("4611");
			fail();
		}
		catch (final KNXFormatException e) {
			// fine, we can't create from raw addresses in base class
		}
	}

	@Test
	void createGroupAddressFromXmlWithoutGroupAttribute() {
		final var is = newInputStream("<knxAddress>1/1/3</knxAddress>");
		final var reader = XmlInputFactory.newInstance().createXMLStreamReader(is);
		final var address = KNXAddress.create(reader);
		assertEquals(GroupAddress.class, address.getClass());
	}

	@Test
	void createGroupAddressFromXmlWithWrongAttribute() {
		final var is = newInputStream("<knxAddress type=\"individual\">1/1/3</knxAddress>");
		final var reader = XmlInputFactory.newInstance().createXMLStreamReader(is);
		assertThrows(KNXMLException.class, () -> KNXAddress.create(reader), "address attribute has wrong type");
	}

	@Test
	void createIndividualAddressFromXmlWithoutIndividualAttribute() {

		final var is = newInputStream("<knxAddress>1.1.4</knxAddress>");
		final var reader = XmlInputFactory.newInstance().createXMLStreamReader(is);
		final var address = KNXAddress.create(reader);
		assertEquals(IndividualAddress.class, address.getClass());
	}

	@Test
	void createIndividualAddressFromXmlWithWrongAttribute() {
		final var is = newInputStream("<knxAddress type=\"group\">1.1.4</knxAddress>");
		final var reader = XmlInputFactory.newInstance().createXMLStreamReader(is);
		assertThrows(KNXMLException.class, () -> KNXAddress.create(reader), "address attribute has wrong type");
	}

	@Test
	void createInvalidAddressFromXmlWithoutAnyAttribute() {
		final var is = newInputStream("<knxAddress>1234</knxAddress>");
		final var reader = XmlInputFactory.newInstance().createXMLStreamReader(is);
		assertThrows(KNXMLException.class, () -> KNXAddress.create(reader), "element should not contain valid address");
	}

	private InputStream newInputStream(final String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}
}

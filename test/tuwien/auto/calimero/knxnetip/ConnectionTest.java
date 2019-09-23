/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019 B. Malinowsky

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIPSequential;
import tag.Slow;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.knxnetip.Connection.SecureSession;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

@KnxnetIPSequential
class ConnectionTest {

	private Connection conn;

	private final byte[] userKey1 = SecureConnection.hashUserPassword("user1".toCharArray());
	private final byte[] userKey2 = SecureConnection.hashUserPassword("user2".toCharArray());
	private final byte[] userKey3 = SecureConnection.hashUserPassword("user3".toCharArray());

	private final byte[] deviceAuthCode = SecureConnection.hashDeviceAuthenticationPassword("dev".toCharArray());

	@BeforeEach
	void init() throws KNXException {
		conn = Connection.newTcpConnection(Util.getLocalHost(), Util.getServer());
	}

	@AfterEach
	void cleanup() {
		conn.close();
	}

	@Test
	void multipleSessions() throws KNXException, InterruptedException {
		try (var session1 = conn.newSecureSession(1, userKey1, deviceAuthCode.clone())) {
			session1.ensureOpen();
			assertEquals(1, conn.sessions.size());
			try (var session2 = conn.newSecureSession(2, userKey2, deviceAuthCode.clone())) {
				session2.ensureOpen();
				assertEquals(2, conn.sessions.size());
				try (var session3 = conn.newSecureSession(3, userKey3, deviceAuthCode.clone())) {
					session3.ensureOpen();
					assertEquals(3, conn.sessions.size());
				}
			}
		}
		assertEquals(0, conn.sessions.size());
	}

	@Test
	void singleSecureConnection() throws KNXException, InterruptedException {
		try (var session1 = conn.newSecureSession(1, userKey1, deviceAuthCode.clone())) {
			try (var tunnel = SecureConnection.newTunneling(TunnelingLayer.LinkLayer, session1,
					KNXMediumSettings.BackboneRouter)) {

				assertEquals(1, conn.sessions.size());
				assertEquals(1, session1.securedConnections.size());
			}
		}
	}

	@Test
	void multipleSecureConnections() throws KNXException, InterruptedException {
		try (var session1 = conn.newSecureSession(1, userKey1, deviceAuthCode.clone())) {
			for (int i = 0; i < 8; i++)
				SecureConnection.newTunneling(TunnelingLayer.LinkLayer, session1, KNXMediumSettings.BackboneRouter);

			assertEquals(1, conn.sessions.size());
			assertEquals(8, session1.securedConnections.size());
		}
		assertEquals(0, conn.sessions.size());
	}

	@Test
	void multipleSessionsMultipleConnections() throws KNXException, InterruptedException {
		try (var session1 = conn.newSecureSession(1, userKey1, deviceAuthCode.clone())) {
			// doing a basic connect request here might take away the tunneling address of user 2
			SecureConnection.newTunneling(TunnelingLayer.LinkLayer, session1, KNXMediumSettings.BackboneRouter);
			try (var session2 = conn.newSecureSession(2, userKey2, deviceAuthCode.clone())) {
				SecureConnection.newTunneling(TunnelingLayer.LinkLayer, session2, KNXMediumSettings.BackboneRouter);
				try (var session3 = conn.newSecureSession(3, userKey3, deviceAuthCode.clone())) {
					SecureConnection.newTunneling(TunnelingLayer.LinkLayer, session3, KNXMediumSettings.BackboneRouter);

					assertEquals(3, conn.sessions.size());
					assertEquals(1, session1.securedConnections.size());
					assertEquals(1, session2.securedConnections.size());
					assertEquals(1, session3.securedConnections.size());
				}
			}
		}
	}

	@Test
	@Slow
	void manySecureSessions() {
		for (int i = 0; i < 5000; ++i) {
			final SecureSession session = conn.newSecureSession(1, userKey1.clone(), deviceAuthCode.clone());
			try {
				session.ensureOpen();
			}
			catch (KNXException | InterruptedException e) {
				e.printStackTrace();
				break;
			}
			finally {
				session.close();
			}
		}
		assertEquals(0, conn.sessions.size());
	}

	@Test
	@Slow
	@SuppressWarnings("resource")
	void manyAlternateSecureSessions() {
		SecureSession old = conn.newSecureSession(2, userKey2.clone(), deviceAuthCode.clone());
		boolean user1 = true;
		for (int i = 0; i < 5000; ++i) {
			final SecureSession session;
			if (user1)
				session = conn.newSecureSession(1, userKey1.clone(), deviceAuthCode.clone());
			else
				session = conn.newSecureSession(2, userKey2.clone(), deviceAuthCode.clone());

			user1 = !user1;
			try {
				session.ensureOpen();
			}
			catch (KNXException | InterruptedException e) {
				e.printStackTrace();
				break;
			}
			finally {
				old.close();
				old = session;
			}
		}
		assertEquals(1, conn.sessions.size());
	}
}

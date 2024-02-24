/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2022, 2024 B. Malinowsky

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

package io.calimero.serial;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import io.calimero.KNXException;
import io.calimero.serial.spi.SerialCom;
import io.calimero.serial.spi.SerialCom.FlowControl;
import io.calimero.serial.spi.SerialCom.Parity;
import io.calimero.serial.spi.SerialCom.StopBits;
import io.calimero.serial.spi.SerialConnectionProvider;
import io.calimero.serial.spi.SerialConnectionProvider.Settings;

/**
 * Connection factory for access to a serial communication port using a {@link SerialConnectionProvider} service provider.
 *
 * @author B. Malinowsky
 */
public final class SerialConnectionFactory {
	private static final ConnectionFactory<SerialConnectionProvider, SerialCom> factory = new ConnectionFactory<>(
			SerialConnectionProvider.class);
	private static final ConnectionFactory<SerialCom, SerialCom> oldFactory = new ConnectionFactory<>(
			SerialCom.class);

	private static final List<String> defaultPortPrefixes = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")
					? List.of("\\\\.\\COM")
					: List.of("/dev/ttyS", "/dev/ttyACM", "/dev/ttyUSB", "/dev/ttyAMA");


	private SerialConnectionFactory() {}

	static List<String> defaultPortPrefixes() { return defaultPortPrefixes; }

	/** {@return all available serial communication port identifiers} */
	public static Set<String> portIdentifiers() {
		return factory.providers().map(SerialConnectionProvider::portIdentifiers).flatMap(Set::stream)
				.collect(Collectors.toSet());
	}


	private record SettingsImpl(String portId, int baudrate, int databits, StopBits stopbits, Parity parity,
			FlowControl flowControl, Duration readIntervalTimeout, Duration receiveTimeout) implements Settings {
	}

	/**
	 * Opens a serial connection using one of the available serial connection providers.
	 *
	 * @param portId serial port identifier
	 * @param baudrate baudrate
	 * @param readIntervalTimeout enforce an inter-character timeout after reading the first character
	 * @param receiveTimeout maximum timeout to wait for next character
	 * @return new connection for the specified serial communication port, port resource is in open state
	 * @throws KNXException on failure to open or configure serial port, or no adapter available
	 */
	@SuppressWarnings({ "resource", "deprecation" })
	public static SerialCom open(final String portId, final int baudrate, final Duration readIntervalTimeout,
			final Duration receiveTimeout) throws KNXException {
		final int databits = 8;
		final var stopbits = StopBits.One;
		final var parity = Parity.Even;
		final var flowControl = FlowControl.None;
		final var settings = new SettingsImpl(portId, baudrate, databits, stopbits, parity,
				flowControl, readIntervalTimeout, receiveTimeout);

		try {
			return factory.open(p -> p.open(settings));
		}
		catch (KNXException | IOException e) {
			try {
				return oldFactory.open(p -> {
					try {
						p.open(portId);
						p.setSerialPortParams(baudrate, databits, stopbits, parity);
						p.setFlowControlMode(flowControl);
						return p;
					}
					catch (IOException | RuntimeException ex) {
						p.close();
						throw ex;
					}
				});
			}
			catch (final KNXException | IOException e1) {
				e.addSuppressed(e1);
				if (e instanceof KNXException)
					throw (KNXException) e;
				throw new KNXException("opening device " + portId, e);
			}
		}
	}
}

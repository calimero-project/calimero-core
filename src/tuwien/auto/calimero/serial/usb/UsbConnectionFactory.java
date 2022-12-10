/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2022, 2022 B. Malinowsky

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

import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.serial.usb.spi.UsbConnectionProvider;

public final class UsbConnectionFactory {
	private static final ConnectionFactory<UsbConnectionProvider, UsbConnection> factory = new ConnectionFactory<>(
			UsbConnectionProvider.class);

	public static UsbConnection open(final int vendorId, final int productId) throws KNXException {
		return factory.open(p -> p.open(vendorId, productId));
	}

	public static UsbConnection open(final String device) throws KNXException {
		return factory.open(p -> p.open(device));
	}
}

final class ConnectionFactory<P, C> {
	private static final Logger logger = LoggerFactory.getLogger("tuwien.auto.calimero.serial.usb");

	private final ServiceLoader<P> sl;

	interface ThrowingSupplier<P, C> {
		C open(P provider) throws KNXException;
	}

	public ConnectionFactory(final Class<P> service) { sl = ServiceLoader.load(service); }

	C open(final ThrowingSupplier<P, C> supplier) throws KNXException {
		Throwable t = null;
		for (final var provider : providers(true)) {
			try {
				return supplier.open(provider.get());
			}
			catch (final ServiceConfigurationError e) {
				final var ex = e.getCause() != null ? e.getCause() : e;
				logger.debug("skip service provider {}: {}", provider.type().getName(), ex.getMessage());
			}
			catch (final Throwable e) {
				t = e;
			}
		}
		if (t instanceof KNXException)
			throw (KNXException) t;
		if (t != null)
			throw new KNXException("failed to open USB device", t);
		throw new KNXException("no USB service provider available");
	}

	private synchronized List<Provider<P>> providers(final boolean refresh) {
		if (refresh)
			sl.reload();
		return sl.stream().collect(Collectors.toList());
	}
}

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

package io.calimero.serial;

import java.io.IOException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.calimero.KNXException;

/**
 * Internal use only.
 *
 * @param <P> Connection provider
 * @param <C> Connection
 */
public final class ConnectionFactory<P, C> {
	private static final Logger logger = LoggerFactory.getLogger("io.calimero.serial");

	private final ServiceLoader<P> sl;
	private final String svcName;

	public interface ThrowingFunction<P, C> {
		C open(P provider) throws KNXException, IOException;
	}

	public ConnectionFactory(final Class<P> service) {
		sl = ServiceLoader.load(service);
		svcName = service.getName();
	}

	public C open(final ThrowingFunction<P, C> openFunc) throws KNXException, IOException {
		final var tref = new AtomicReference<Throwable>();
		final var optC = providers().map(provider -> {
			try {
				final var conn = openFunc.open(provider);
				logger.debug("serial port setup: {}", conn);
				return Optional.of(conn);
			}
			catch (KNXException | IOException | RuntimeException t) {
				tref.set(t);
				return Optional.<C>empty();
			}
		}).flatMap(Optional::stream).findFirst();
		if (optC.isPresent())
			return optC.get();

		final var t = tref.get();
		if (t == null)
			throw new KNXException("no service provider available for " + svcName);
		if (t instanceof KNXException)
			throw (KNXException) t;
		if (t instanceof IOException)
			throw (IOException) t;
		throw new KNXException("failed to open connection", t);
	}

	public Stream<P> providers() {
		return providers(true).map(provider -> {
			try {
				logger.trace("instantiate service provider {}", provider.type().getName());
				return provider.get();
			}
			catch (final Throwable e) { // handles ServiceConfigurationError
				final var ex = e.getCause() != null ? e.getCause() : e;
				logger.debug("skip service provider {}: {}", provider.type().getName(), ex.getMessage());
			}
			return null;
		}).filter(p -> p != null);
	}

	private Stream<Provider<P>> providers(final boolean refresh) {
		if (refresh)
			sl.reload();
		return sl.stream();
	}
}

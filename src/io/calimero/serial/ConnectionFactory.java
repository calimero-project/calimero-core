/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2022, 2025 B. Malinowsky

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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

import io.calimero.KNXException;
import io.calimero.log.LogService;

/**
 * Internal use only.
 *
 * @param <P> Connection provider
 * @param <C> Connection
 */
public final class ConnectionFactory<P, C> {
	private static final Logger logger = LogService.getLogger(MethodHandles.lookup().lookupClass());

	private final ServiceLoader<P> sl;
	private final String svcName;

	public interface ThrowingFunction<P, C> {
		C open(P provider) throws KNXException, IOException;
	}

	public ConnectionFactory(final Class<P> service) {
		sl = ServiceLoader.load(service);
		svcName = service.getName();
	}

	public C open(final String name, final ThrowingFunction<P, C> openFunc) throws KNXException, IOException {
		final var providerExceptions = new ArrayList<Throwable>();
		final var connOpt = providers().map(provider -> {
			try {
				final var conn = openFunc.open(provider);
				logger.log(Level.DEBUG, "{0} port setup: {1}", provider, conn);
				return Optional.of(conn);
			}
			catch (KNXException | IOException | RuntimeException | ExceptionInInitializerError t) {
				var msg = t.getMessage();
				if (msg == null)
					msg = t.getCause() != null ? t.getCause().getMessage() : t.getClass().getSimpleName();
				logger.log(Level.DEBUG, "{0} unsuccessful: {1}", provider, msg);
				providerExceptions.add(t);
				return Optional.<C>empty();
			}
		}).flatMap(Optional::stream).findFirst();
		if (connOpt.isPresent())
			return connOpt.get();

		if (providerExceptions.isEmpty())
			throw new KNXException("no service provider available for " + svcName);
		if (providerExceptions.size() == 1) {
			final var x = providerExceptions.getFirst();
			if (x instanceof final KNXException e)
				throw e;
			if (x instanceof final IOException e)
				throw e;
		}
		final var t = new KNXException("failed to open connection '" + name + "'");
		providerExceptions.forEach(t::addSuppressed);
		throw t;
	}

	public Stream<P> providers() {
		return providers(true).map(provider -> {
			try {
				logger.log(Level.TRACE, "instantiate service provider {0}", provider.type().getName());
				return provider.get();
			}
			catch (final Throwable e) { // handles ServiceConfigurationError
				final var ex = e.getCause() != null ? e.getCause() : e;
				logger.log(Level.DEBUG, "skip service provider {0}: {1}", provider.type().getName(), ex.getMessage());
			}
			return null;
		}).filter(Objects::nonNull);
	}

	private Stream<Provider<P>> providers(final boolean refresh) {
		if (refresh)
			sl.reload();
		return sl.stream();
	}
}

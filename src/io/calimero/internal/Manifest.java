/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2024, 2024 B. Malinowsky

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

package io.calimero.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.jar.Attributes.Name;

import io.calimero.Settings;

public final class Manifest {

	public record BuildInfo(String version, Optional<String> revision, Optional<String> buildDate) {
		@Override
		public String toString() {
			return version + revision.map(r -> " (" + r + ")").orElse("");
		}
	}

	private Manifest() {}

	public static BuildInfo buildInfo(final Class<?> lookup) {
		try (final var is = manifestStream(lookup)) {
			if (is != null) {
				final var manifest = new java.util.jar.Manifest(is);
				final var attributes = manifest.getMainAttributes();
				return new BuildInfo(Optional.ofNullable(attributes.getValue(Name.IMPLEMENTATION_VERSION))
								.orElse(Settings.getLibraryVersion()),
						Optional.ofNullable(attributes.getValue("Revision")),
						Optional.ofNullable(attributes.getValue("Build-Date")));
			}
		}
		catch (IOException | RuntimeException ignore) {}
		return new BuildInfo(Settings.getLibraryVersion(), Optional.empty(), Optional.empty());
	}

	private static InputStream manifestStream(final Class<?> lookup) throws IOException {
		if (lookup.getModule().isNamed())
			return lookup.getResourceAsStream("/META-INF/MANIFEST.MF"); // requires leading slash

		// We could use ProtectionDomain of the lookup class, which won't work, e.g., on Android;
		// also, the protection domain's code source or location might be null.
		// final var f = new File(lookup.getProtectionDomain().getCodeSource().getLocation().toURI());
		final String pkg = lookup.getPackageName();
		final String id = pkg.substring(pkg.lastIndexOf('.') + 1).replace("calimero", "core");
		final var url = lookup.getClassLoader().resources("META-INF/MANIFEST.MF")
				.filter(u -> u.getPath().contains("calimero") && u.getPath().contains(id)).findFirst().orElse(null);
		return url != null ? url.openStream() : null;
	}
}

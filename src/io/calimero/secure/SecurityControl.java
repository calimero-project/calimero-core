/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2019, 2023 B. Malinowsky

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

import java.util.Objects;

import io.calimero.KNXIllegalArgumentException;

public final class SecurityControl {
	public enum DataSecurity {
		None, Auth, AuthConf;

		@Override
		public String toString() { return this == AuthConf ? "auth+conf" : super.toString().toLowerCase(); }
	}

	private final DataSecurity security;
	private final boolean toolAccess;
	private final boolean systemBroadcast;

	public static final SecurityControl Plain = new SecurityControl(DataSecurity.None);

	public static final SecurityControl SystemBroadcast = new SecurityControl(DataSecurity.AuthConf, true, true);

	public static SecurityControl of(final DataSecurity security, final boolean toolAccess) {
		if (security == DataSecurity.None) {
			if (toolAccess)
				throw new KNXIllegalArgumentException("tool access requires security");
			return Plain;
		}
		return new SecurityControl(security, toolAccess, false);
	}

	private SecurityControl(final DataSecurity security) { this(security, false, false); }

	private SecurityControl(final DataSecurity security, final boolean toolAccess, final boolean systemBroadcast) {
		this.security = security;
		this.toolAccess = toolAccess;
		this.systemBroadcast = systemBroadcast;
	}

	public DataSecurity security() { return security; }

	public boolean toolAccess() { return toolAccess; }

	public boolean systemBroadcast() { return systemBroadcast; }

	@Override
	public int hashCode() { return Objects.hash(security, toolAccess); }

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof final SecurityControl sc))
			return false;
		return security == sc.security && toolAccess == sc.toolAccess;
	}

	@Override
	public String toString() {
		return security + (toolAccess ? ", tool access" : "");
	}
}
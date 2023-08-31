/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero.log;

import java.lang.System.Logger;

/**
 * LogService provides access to {@link System.Logger}.
 * <p>
 * In addition to the common {@link #getLogger(String)} returning a standard System.Logger instance,
 * {@link #getAsyncLogger(String)} provides a logger which dispatches the logged information asynchronously. This
 * minimizes the overhead on the calling thread, independent of the used underlying logging framework implementation.
 *
 * @author B. Malinowsky
 */
public final class LogService {
	private LogService() {}

	/**
	 * Returns a logger identified by {@code name}.
	 *
	 * @param name logger name
	 * @return logger
	 * @see System#getLogger(String)
	 */
	public static Logger getLogger(final String name) { return System.getLogger(name); }

	public static Logger getLogger(final Class<?> clazz) { return getLogger(clazz.getName()); }

	/**
	 * Returns a logger identified by {@code name} with asynchronous processing of logged information. Note that with
	 * asynchronous loggers the stack traces will not show the original location.
	 *
	 * @param name logger name
	 * @return logger
	 * @see LogService#getLogger(Class)
	 */
	public static Logger getAsyncLogger(final String name) {
		return new AsyncLogger(getLogger(name));
	}
}

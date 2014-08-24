/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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

package tuwien.auto.calimero.log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global Manager for {@link LogService}s and {@link LogWriter}s.
 * <p>
 * There is only one instance of this manager in the library, obtained with
 * {@link #getManager()}.<br>
 * A log service can be queried and removed. A log writer can be added (i.e., registered)
 * and removed, either to a particular log service or as a global log writer. A global log
 * writer will receive all logging output from all registered log services.
 *
 * @author B. Malinowsky
 * @see LogWriter
 * @see LogService
 */
public final class LogManager
{
	private static final LogManager mgr = new LogManager();

	private final Map<String, LogService> loggers;

	private LogManager()
	{
		loggers = Collections.synchronizedMap(new HashMap<>());
	}

	/**
	 * Returns the only instance of the log manager.
	 * <p>
	 *
	 * @return the log manager object
	 */
	public static LogManager getManager()
	{
		return mgr;
	}

	/**
	 * Queries for a log service with the specified <code>name</code>.
	 * <p>
	 * If the log service with this name already exists in the manager, it will be
	 * returned, otherwise a new log service with this name will be created and added to
	 * the log services listed in the manager.
	 *
	 * @param name name of log service, the empty string is not allowed
	 * @return the LogService object
	 */
	LogService getLogService(final String name)
	{
		synchronized (loggers) {
			LogService l = loggers.get(name);
			if (l == null) {
				final Logger slf4jLogger = LoggerFactory.getLogger(name);
				l = new LogService(slf4jLogger);
				loggers.put(name, l);
			}
			return l;
		}
	}

	/**
	 * Queries for a slf4j logger with the specified <code>name</code>.
	 * <p>
	 * If the logger with that name already exists in the manager, it will be
	 * returned, otherwise a new logger with that name will be created and added to
	 * the log services listed in the manager.
	 *
	 * @param name name of logger, the empty string is not allowed
	 * @return the Logger object
	 * @deprecated Used for transition to slf4j
	 */
	public Logger getSlf4jLogger(final String name)
	{
		return getLogService(name).slf4j();
	}

	public void removeLogService(final String name)
	{
		// nop
	}

	// XXX update doc
	/**
	 * Drains the log output queues of the underlying log handler mechanism and shuts down
	 * logging.
	 * <p>
	 * An invocation blocks until all log messages currently (i.e., at the time of
	 * invoking this method) waiting for being dispatched by the log services got written
	 * out by the corresponding log writers.<br>
	 * Subsequent log events passed to the standard {@link LogService} are not handled but
	 * ignored.<br>
	 * This method is useful if a Java process wants to terminate immediately after
	 * shutting down all other KNX library services; in that case, log events might still
	 * be queued and waiting to be processed, e.g., written to a file on disk. Then, by
	 * calling this method, the log service handler mechanisms is guaranteed the required
	 * amount of time to process the remaining log events.
	 */
	public void flush()
	{
		// NYI
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * LogService provides access to slf4j logging.
 * <p>
 * In addition to the common {@link #getLogger(String)} returning a standard slf4j logger,
 * {@link #getAsyncLogger(String)} provides a logger which dispatches the logged information
 * asynchronously. This minimizes the overhead on the calling thread, independent of the used
 * underlying logging framework implementation.
 *
 * @author B. Malinowsky
 */
public final class LogService
{
	/** Enumeration of the supported slf4j log levels. */
	public enum LogLevel {
		/** Error level. */
		ERROR,
		/** Warn level. */
		WARN,
		/** Info level. */
		INFO,
		/** Debug level. */
		DEBUG,
		/** Trace level. */
		TRACE
	}

	private static final String loggerThreadName = "Calimero Async Logging";
	private static final ExecutorService dispatcher = Executors.newFixedThreadPool(1, (r) -> {
		final Thread t = Executors.defaultThreadFactory().newThread(r);
		t.setName(loggerThreadName);
		t.setDaemon(true);
		return t;
	});

	private LogService()
	{}

	/**
	 * Returns an slf4j logger identified by <code>name</code>.
	 *
	 * @param name logger name
	 * @return logger
	 * @see LoggerFactory#getLogger(Class)
	 */
	public static Logger getLogger(final String name)
	{
		return LoggerFactory.getLogger(name);
	}

	/**
	 * Returns an slf4j logger identified by <code>name</code> with asynchronous processing of
	 * logged information. Note that with asynchronous loggers the stack traces will not show the
	 * original location.
	 *
	 * @param name logger name
	 * @return logger
	 * @see LoggerFactory#getLogger(Class)
	 */
	public static Logger getAsyncLogger(final String name)
	{
		return new AsyncLogger(LogService.getLogger(name));
	}

	static void async(final Logger l, final LogLevel level, final String msg, final Throwable t)
	{
		async(l, level, (Marker) null, "{}", msg, t);
	}

	static void async(final Logger l, final LogLevel level, final Marker m, final String msg, final Throwable t)
	{
		async(l, level, (Marker) null, "{}", msg, t);
	}

	static void async(final Logger l, final LogLevel level, final String format, final Object... o)
	{
		async(l, level, null, format, o);
	}

	static void async(final Logger l, final LogLevel level, final Marker m, final String format, final Object... o)
	{
		if (!isEnabled(l, level))
			return;
		final Thread thread = Thread.currentThread();
		dispatcher.execute(() -> {
			final Thread logger = Thread.currentThread();
			logger.setName(thread.getName());
			log(l, level, m, format, o);
			logger.setName(loggerThreadName);
		});
	}

	/**
	 * Logs a message with the specified log level, format, and arguments using the supplied logger. This method works
	 * around the limitation that slf4j loggers don't have a generic <code>log</code> method.
	 *
	 * @param logger the logger
	 * @param level log level to use for the message
	 * @param format the format of the message to be logged
	 * @param arguments message arguments
	 */
	public static void log(final Logger logger, final LogLevel level, final String format, final Object... arguments)
	{
		log(logger, level, (Marker) null, format, arguments);
	}

	private static void log(final Logger logger, final LogLevel level, final Marker marker,
		final String format, final Object... o)
	{
		switch (level) {
		case DEBUG:
			logger.debug(marker, format, o);
			break;
		case TRACE:
			logger.trace(marker, format, o);
			break;
		case INFO:
			logger.info(marker, format, o);
			break;
		case WARN:
			logger.warn(marker, format, o);
			break;
		case ERROR:
			logger.error(marker, format, o);
			break;
		default:
			throw new KNXIllegalArgumentException("unknown log level");
		}
	}

	private static boolean isEnabled(final Logger logger, final LogLevel level)
	{
		switch (level) {
		case TRACE:
			return logger.isTraceEnabled();
		case DEBUG:
			return logger.isDebugEnabled();
		case INFO:
			return logger.isInfoEnabled();
		case WARN:
			return logger.isWarnEnabled();
		case ERROR:
			return logger.isErrorEnabled();
		default:
			throw new KNXIllegalArgumentException("unknown log level");
		}
	}
}

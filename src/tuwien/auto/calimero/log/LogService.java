/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

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
	// Enumeration of the supported slf4j log levels
	public static enum LogLevel {
		ERROR, WARN, INFO, TRACE, DEBUG
	}

	private static final ExecutorService dispatcher = Executors.newFixedThreadPool(1);

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

	/**
	 * @param l
	 */
	public static void removeLogger(final Logger l)
	{}

	static void async(final Logger l, final LogLevel level, final String msg, final Throwable t)
	{
		async(l, level, null, msg, t);
	}

	static void async(final Logger l, final LogLevel level, final Marker m, final String msg,
		final Throwable t)
	{
		final Thread thread = Thread.currentThread();
		dispatcher.execute(() -> {
			Thread.currentThread().setName(thread.getName());
			// TODO add marker
				log(l, level, msg, t);
			});
	}

	static void async(final Logger l, final LogLevel level, final String format, final Object... o)
	{
		async(l, level, null, format, o);
	}

	static void async(final Logger l, final LogLevel level, final Marker m, final String format,
		final Object... o)
	{
		final Thread thread = Thread.currentThread();
		dispatcher.execute(() -> {
			Thread.currentThread().setName(thread.getName());
			// TODO add marker
				log(l, level, format, o);
			});
	}

	/**
	 * Logs a message and an exception with the specified log level using the supplied logger.
	 * <p>
	 * This method works around the limitation that slf4j loggers don't have a generic
	 * <code>log</code> method.
	 *
	 * @param logger the logger
	 * @param level log level to use for the message
	 * @param msg the message to be logged
	 * @param t the exception (throwable) to log, can be <code>null</code>
	 */
	public static void log(final Logger logger, final LogLevel level, final String msg,
		final Throwable t)
	{
		switch (level) {
		case DEBUG:
			logger.debug(msg, t);
			break;
		case TRACE:
			logger.trace(msg, t);
			break;
		case INFO:
			logger.info(msg, t);
			break;
		case WARN:
			logger.warn(msg, t);
			break;
		case ERROR:
			logger.error(msg, t);
			break;
		default:
			throw new KNXIllegalArgumentException("unknown log level");
		}
	}

	/**
	 * Logs a message and an exception with the specified log level using the supplied logger.
	 * <p>
	 * This method works around the limitation that slf4j loggers don't have a generic
	 * <code>log</code> method.
	 *
	 * @param logger the logger
	 * @param level log level to use for the message
	 * @param format the format string
	 * @param o the arguments
	 * @deprecated Used for transition to slf4j.
	 */
	public static void log(final Logger logger, final LogLevel level, final String format,
		final Object... o)
	{
		switch (level) {
		case DEBUG:
			logger.debug(format, o);
			break;
		case TRACE:
			logger.trace(format, o);
			break;
		case INFO:
			logger.info(format, o);
			break;
		case WARN:
			logger.warn(format, o);
			break;
		case ERROR:
			logger.error(format, o);
			break;
		default:
			throw new KNXIllegalArgumentException("unknown log level");
		}
	}

	/**
	 * Workaround for the previously supported log level ALWAYS, logging the message with level
	 * INFO.
	 * <p>
	 *
	 * @param logger the logger
	 * @param msg the message to be logged
	 * @deprecated Used for transition to slf4j.
	 */
	public static void logAlways(final Logger logger, final String msg)
	{
		logger.info(msg);
	}

	static final void stopDispatcher() throws InterruptedException
	{
		dispatcher.shutdown();
		// give any remaining messages a chance to get logged
		dispatcher.awaitTermination(2, TimeUnit.SECONDS);
	}
}

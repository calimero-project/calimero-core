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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;

import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * A LogService is used to categorize logging information with regard to logging level and
 * topic, and offer it to its {@link LogWriter}s.
 * <p>
 * The default log level by the LogService is {@link LogLevel#ALL}. This means all log
 * information levels (except {@link LogLevel#OFF}) will be offered.
 * <p>
 * A LogWriter can register at a log service to receive information the log service is
 * offering to its writers.
 * <p>
 * Use {@link LogManager} to create new or get existing log services.<br>
 * Usage:<br>
 * - A log service may be created for different parts of the Calimero library, therefore,
 * it distinguishes log information by source.<br>
 * - A log service may be created for a particular subject, i.e., to divide information
 * into topics.<br>
 * A log service may restrict offered information through its own log level.<br>
 * - ...
 *
 * @author B. Malinowsky
 * @see LogLevel
 * @see LogWriter
 */
public class LogService
{
	// Interruption policy: drain log event queue and quit
	private static final class Dispatcher extends Thread
	{
		private static final class LogData
		{
			final String svc;
			final LogLevel lvl;
			final String msg;
			final Throwable trow;
			final Thread thread;

			LogData(final String service,
				final LogLevel level, final String message, final Throwable t, final Thread thr)
			{
				svc = service;
				lvl = level;
				msg = message;
				trow = t;
				thread = thr;
			}
		}

		private final List<LogData> data = new LinkedList<>();
		private volatile boolean quit;

		Dispatcher()
		{
			super("Log dispatcher");
			setDaemon(true);
			start();
		}

		@Override
		public void run()
		{
			try {
				while (true) {
					final LogData d;
					synchronized (data) {
						while (data.isEmpty())
							data.wait();
						d = data.remove(0);
					}
					dispatch(d);
				}
			}
			catch (final InterruptedException ie) {
				quit = true;
			}
			// empty log data list
			synchronized (data) {
				while (!data.isEmpty())
					dispatch(data.remove(0));
			}
		}

		void add(final String service, final LogLevel level, final String msg, final Throwable t)
		{
			if (!quit) {
				synchronized (data) {
					data.add(new LogData(service, level, msg, t, Thread.currentThread()));
					data.notify();
				}
			}
		}

		private void dispatch(final LogData d)
		{
			final boolean dev = false; //Settings.getLibraryMode() == Settings.DEV_MODE;
			final String svc = dev ? ("[" + d.thread.getName() + "] " + d.svc) : d.svc;
		}
	}

	private static final Dispatcher logger = new Dispatcher();

	private final Logger impl;


	// sets the slf4j to do the work
	protected LogService(final Logger l)
	{
		this.impl = l;
	}

	/**
	 * Returns the slf4j logger implementation.
	 * <p>
	 *
	 * @return the slf4j logger or <null> if none is used
	 * @deprecated Used for transition to slf4j.
	 */
	public final Logger slf4j()
	{
		return impl;
	}

	/**
	 * Offers <code>msg</code> with log level {@link LogLevel#TRACE}.
	 * <p>
	 *
	 * @param msg log information
	 */
	public void trace(final String msg)
	{
		log(LogLevel.TRACE, msg, null);
	}

	/**
	 * Offers <code>msg</code> with log level {@link LogLevel#INFO}.
	 * <p>
	 *
	 * @param msg log information
	 */
	public void info(final String msg)
	{
		log(LogLevel.INFO, msg, null);
	}

	/**
	 * Offers <code>msg</code> with log level {@link LogLevel#WARN}.
	 * <p>
	 *
	 * @param msg log information
	 */
	public void warn(final String msg)
	{
		log(LogLevel.WARN, msg, null);
	}

	/**
	 * Offers <code>msg</code> and the <code>throwable</code> object with log level
	 * {@link LogLevel#WARN}.
	 * <p>
	 *
	 * @param msg log information
	 * @param t throwable object
	 */
	public void warn(final String msg, final Throwable t)
	{
		log(LogLevel.WARN, msg, t);
	}

	/**
	 * Offers <code>msg</code> with log level {@link LogLevel#ERROR}.
	 * <p>
	 *
	 * @param msg log information
	 */
	public void error(final String msg)
	{
		log(LogLevel.ERROR, msg, null);
	}

	/**
	 * Offers <code>msg</code> and the <code>throwable</code> object with log level
	 * {@link LogLevel#ERROR}.
	 * <p>
	 *
	 * @param msg log information
	 * @param t throwable object
	 */
	public void error(final String msg, final Throwable t)
	{
		log(LogLevel.ERROR, msg, t);
	}

	/**
	 * Offers <code>msg</code> and the <code>throwable</code> object with log
	 * <code>level</code>.
	 * <p>
	 *
	 * @param level log level for this message and throwable
	 * @param msg log information
	 * @param t throwable object, can be <code>null</code>
	 */
	public void log(final LogLevel level, final String msg, final Throwable t)
	{
		forwardToImpl(level, msg, t);
	}

	static final void stopDispatcher()
	{
		logger.interrupt();
		while (logger.isAlive())
			try {
				logger.join();
			}
			catch (final InterruptedException ignore) {}
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
	 * @deprecated Used for transition to slf4j.
	 */
	public static void log(final Logger logger, final LogLevel level, final String msg,
		final Throwable t)
	{
		if (level.equals(LogLevel.TRACE))
			logger.trace(msg, t);
		else if (level.equals(LogLevel.INFO))
			logger.info(msg, t);
		else if (level.equals(LogLevel.WARN))
			logger.warn(msg, t);
		else if (level.equals(LogLevel.ERROR))
			logger.error(msg, t);
		else if (level.equals(LogLevel.ALWAYS))
			logger.trace(msg, t);
		else
			throw new KNXIllegalArgumentException("unknown log level");
	}

	// TODO slf4j_impl: temporarily used as forwarder to slf4j
	private void forwardToImpl(final LogLevel level, final String msg, final Throwable t)
	{
		log(impl, level, msg, t);
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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
*/

package tuwien.auto.calimero.log;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import tuwien.auto.calimero.Settings;

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
			final List wr;
			final String svc;
			final LogLevel lvl;
			final String msg;
			final Throwable trow;
			final Thread thread;

			LogData(final List writers, final String service, final LogLevel level,
				final String message, final Throwable t, final Thread thr)
			{
				wr = writers;
				svc = service;
				lvl = level;
				msg = message;
				trow = t;
				thread = thr;
			}
		}

		private final List data = new LinkedList();
		private volatile boolean quit;

		Dispatcher()
		{
			super("Log dispatcher");
			setDaemon(true);
			start();
		}

		public void run()
		{
			try {
				while (true) {
					final LogData d;
					synchronized (data) {
						while (data.isEmpty())
							data.wait();
						d = (LogData) data.remove(0);
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
					dispatch((LogData) data.remove(0));
			}
		}

		void add(final List writers, final String service, final LogLevel level, final String msg,
			final Throwable t)
		{
			if (!quit) {
				synchronized (data) {
					data.add(new LogData(writers, service, level, msg, t, Thread.currentThread()));
					data.notify();
				}
			}
		}

		private void dispatch(final LogData d)
		{
			final boolean dev = Settings.getLibraryMode() == Settings.DEV_MODE;
			final String svc = dev ? ("[" + d.thread.getName() + "] " + d.svc) : d.svc;
			synchronized (d.wr) {
				for (final Iterator i = d.wr.iterator(); i.hasNext();)
					((LogWriter) i.next()).write(svc, d.lvl, d.msg, d.trow);
			}
		}
	}

	private static final Dispatcher logger = new Dispatcher();

	/** Name of this log service. */
	protected final String name;
	private LogLevel logLevel = LogLevel.ALL;
	private List writers = new Vector();

	/**
	 * Creates a new log service with the specified <code>name</code>.
	 * <p>
	 * 
	 * @param name name of log service
	 */
	protected LogService(final String name)
	{
		this.name = name;
	}

	/**
	 * Creates a new log service with the specified <code>name</code> and log
	 * <code>level</code>.
	 * <p>
	 * 
	 * @param name name of log service
	 * @param level log level for this log service
	 */
	protected LogService(final String name, final LogLevel level)
	{
		this.name = name;
		setLogLevel(level);
	}

	/**
	 * Returns the name of this log service.
	 * <p>
	 * 
	 * @return the log service name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets a new log level for this log service.
	 * <p>
	 * All log information will be checked against and restricted to at most this level.<br>
	 * Log information not allowed will be ignored.
	 * <p>
	 * For example: set log level to {@link LogLevel#WARN} to allow log info with
	 * {@link LogLevel#FATAL}, {@link LogLevel#ERROR} and {@link LogLevel#WARN}.
	 * 
	 * @param level new log level
	 */
	public void setLogLevel(final LogLevel level)
	{
		logLevel = level;
	}

	/**
	 * Returns the currently set log level of this log service.
	 * <p>
	 * 
	 * @return the log {@link LogLevel}
	 */
	public LogLevel getLogLevel()
	{
		return logLevel;
	}

	/**
	 * Checks whether output with a specified level will be logged by this log service.
	 * <p>
	 * The <code>level</code> parameter is checked against the currently set log level of
	 * this log service. Only output with a log level specified equal or below the level
	 * of this service is logged, i.e., output of the same or less verbosity.<br>
	 * Output with assigned log level {@link LogLevel#OFF} is never logged.
	 * 
	 * @param level the log level to check for
	 * @return <code>true</code> if output of that level is loggable, <code>false</code>
	 *         otherwise
	 */
	public final boolean isLoggable(final LogLevel level)
	{
		return !level.higher(logLevel) && level != LogLevel.OFF;
	}

	/**
	 * Adds the <code>writer</code> to this log service.
	 * <p>
	 * No check is made to detect (or prevent) duplicate writers.
	 * 
	 * @param writer LogWriter to add
	 */
	public void addWriter(final LogWriter writer)
	{
		writers.add(writer);
	}

	/**
	 * Removes <code>writer</code> from this log service.
	 * <p>
	 * No check is made to detect and remove duplicate writers.
	 * 
	 * @param writer LogWriter to remove
	 */
	public void removeWriter(final LogWriter writer)
	{
		writers.remove(writer);
	}

	/**
	 * Removes all registered log writer from this log service.
	 * <p>
	 * 
	 * @param close should the writers be closed before removal
	 */
	public void removeAllWriters(final boolean close)
	{
		if (close)
			synchronized (writers) {
				for (final Iterator i = writers.iterator(); i.hasNext();)
					((LogWriter) i.next()).close();
			}
		writers = new Vector();
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
	 * Offers <code>msg</code> with log level {@link LogLevel#FATAL}.
	 * <p>
	 * 
	 * @param msg log information
	 */
	public void fatal(final String msg)
	{
		log(LogLevel.FATAL, msg, null);
	}

	/**
	 * Offers <code>msg</code> and the <code>throwable</code> object with log level
	 * {@link LogLevel#FATAL}.
	 * <p>
	 * 
	 * @param msg log information
	 * @param t throwable object
	 */
	public void fatal(final String msg, final Throwable t)
	{
		log(LogLevel.FATAL, msg, t);
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
		if (level != LogLevel.OFF && level.level <= logLevel.level)
			logger.add(writers, name, level, msg, t);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name + ", log level " + logLevel + ", " + writers.size() + " log writers";
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
}

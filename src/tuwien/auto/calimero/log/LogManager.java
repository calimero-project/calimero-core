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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
	
	private final Map loggers;
	private final List writers;

	private LogManager()
	{
		loggers = Collections.synchronizedMap(new HashMap());
		writers = new Vector();
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
	 * Checks whether a log service with <code>name</code> exists in the manager.
	 * <p>
	 * A log service is only listed in the manager, if it was initially queried using
	 * {@link #getLogService(String)}.
	 * 
	 * @param name name of log service
	 * @return <code>true</code> if log service exists, <code>false</code> otherwise
	 */
	public boolean hasLogService(final String name)
	{
		synchronized (loggers) {
			return loggers.get(name) != null;
		}
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
	public LogService getLogService(final String name)
	{
		synchronized (loggers) {
			LogService l = (LogService) loggers.get(name);
			if (l == null) {
				l = new LogService(name);
				loggers.put(name, l);
				for (final Iterator i = writers.iterator(); i.hasNext();)
					l.addWriter((LogWriter) i.next());
			}
			return l;
		}
	}

	/**
	 * Removes a log service from the manager.
	 * <p>
	 * If no log service with the specified name is found, no action is performed.
	 * 
	 * @param name name of log service
	 */
	public void removeLogService(final String name)
	{
		loggers.remove(name);
	}

	/**
	 * Returns the names of all registered log services.
	 * <p>
	 * 
	 * @return array of type String with log service names
	 */
	public String[] getAllLogServices()
	{
		return (String[]) loggers.keySet().toArray(new String[loggers.size()]);
	}

	/**
	 * Adds a log writer, either global or to a particular log service.
	 * <p>
	 * Note that the writer is added to the log service(s) regardless if it was already
	 * added before.<br>
	 * If the writer is added globally, it will receive logging information from all log
	 * services that are already registered or will be registered in the future.
	 * 
	 * @param logService name of a log service; to add the writer globally, use an empty
	 *        string or <code>null</code>
	 * @param writer log writer to add
	 * @return <code>true</code> if the writer was added successfully,<br>
	 *         <code>false</code> if the specified log service name was not found
	 * @see LogService#addWriter(LogWriter)
	 */
	public boolean addWriter(final String logService, final LogWriter writer)
	{
		if (logService != null && logService.length() > 0) {
			final LogService l = (LogService) loggers.get(logService);
			if (l != null)
				l.addWriter(writer);
			return l != null;
		}
		synchronized (loggers) {
			writers.add(writer);
			for (final Iterator i = loggers.values().iterator(); i.hasNext();)
				((LogService) i.next()).addWriter(writer);
			return true;
		}
	}

	/**
	 * Removes a log writer, either global or from a particular <code>logService</code>.
	 * <p>
	 * Note that for a writer to be removed global, it had to be added global before.
	 * 
	 * @param logService name of the log service of which the writer will be removed; to
	 *        remove the writer global, use an empty string or <code>null</code>
	 * @param writer log writer to remove
	 * @see LogService#removeWriter(LogWriter)
	 */
	public void removeWriter(final String logService, final LogWriter writer)
	{
		if (logService != null && logService.length() > 0) {
			final LogService l = (LogService) loggers.get(logService);
			if (l != null)
				l.removeWriter(writer);
		}
		else
			synchronized (loggers) {
				if (writers.remove(writer))
					for (final Iterator i = loggers.values().iterator(); i.hasNext();)
						((LogService) i.next()).removeWriter(writer);
			}
	}

	/**
	 * Returns all registered global log writers.
	 * <p>
	 * Log writers are global if they were not were not registered at a particular log service,
	 * receiving logging information from all log services.
	 * 
	 * @return array with global log writers
	 */
	public LogWriter[] getAllGlobalWriters()
	{
		return (LogWriter[]) writers.toArray(new LogWriter[writers.size()]);
	}

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
	 * 
	 * @param closeAllWriters if <code>true</code>, all writers in the log services known
	 *        to the log manager ({@link #getAllLogServices()}), as well as global writers
	 *        ({@link #getAllGlobalWriters()}), are closed ({@link LogWriter#close()});
	 *        closed log writers are removed
	 */
	public void shutdown(final boolean closeAllWriters)
	{
		LogService.stopDispatcher();
		if (!closeAllWriters)
			return;
		// we make local copies to minimize blocking on synchronized variables,
		// since closing file resources etc. can take some time
		final LogService[] lsa;
		synchronized (loggers) {
			lsa = (LogService[]) loggers.values().toArray(new LogService[loggers.size()]);
		}
		for (int i = 0; i < lsa.length; i++)
			lsa[i].removeAllWriters(true);
		// explicitly close global writers, to catch the odd case in which a global
		// writer was manually removed by all log services but not from the list of
		// global writers
		final LogWriter[] lwa;
		synchronized (writers) {
			lwa = getAllGlobalWriters();
			writers.clear();
		}
		for (int i = 0; i < lwa.length; i++)
			lwa[i].close();
	}
}

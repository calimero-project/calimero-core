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

/**
 * LogWriter is responsible for writing information to individual destinations.
 * <p>
 * A log writer has an assigned log level (by default {@link LogLevel#ALL}), which
 * specifies what information to write and what to ignore. A log level of
 * {@link LogLevel#OFF} will ignore any write requests. If a log writer is not used
 * anymore, {@link #close()} has to be called.
 * 
 * @author B. Malinowsky
 * @see LogLevel
 * @see LogService
 */
public abstract class LogWriter
{
	private static ErrorHandler errorHandler = new ErrorHandler();

	LogLevel logLevel = LogLevel.ALL;

	/**
	 * Sets the error handler used on all log writer errors.
	 * <p>
	 * 
	 * @param handler the new error handler
	 */
	public static synchronized void setErrorHandler(final ErrorHandler handler)
	{
		if (handler != null)
			errorHandler = handler;
	}

	/**
	 * Returns the used error handler.
	 * <p>
	 * 
	 * @return an {@link ErrorHandler} object
	 */
	public static synchronized ErrorHandler getErrorHandler()
	{
		return errorHandler;
	}

	/**
	 * Sets the log level for deciding which messages gets logged by this LogWriter.
	 * <p>
	 * 
	 * @param level new log level
	 */
	public final void setLogLevel(final LogLevel level)
	{
		logLevel = level;
	}

	/**
	 * Returns the log level used for deciding which messages will be logged.
	 * <p>
	 * 
	 * @return log {@link LogLevel}
	 */
	public final LogLevel getLogLevel()
	{
		return logLevel;
	}

	/**
	 * Writes a message out to this LogWriter.
	 * <p>
	 * The message has the associated log level <code>level</code>. It will only be
	 * written if the LogWriter logging level is not more restrictive than
	 * <code>level</code>. Otherwise the message is ignored. LogWriter is responsible
	 * for formatting the output.
	 * 
	 * @param logService log service name stating the source of the message
	 * @param level log level of message
	 * @param msg the message to write
	 */
	public abstract void write(String logService, LogLevel level, String msg);

	/**
	 * Like {@link #write(String, LogLevel, String)}, in addition a
	 * <code>Throwable</code> object is taken which will be added to the message.
	 * 
	 * @param logService log service name stating the source of the message
	 * @param level log level of message
	 * @param msg the message to write
	 * @param t Throwable object, might be <code>null</code>
	 */
	public abstract void write(String logService, LogLevel level, String msg, Throwable t);

	/**
	 * Flushes all buffered output.
	 */
	public abstract void flush();

	/**
	 * Closes the LogWriter and all its resources.
	 * <p>
	 * If necessary, all output is flushed before.
	 */
	public abstract void close();
}

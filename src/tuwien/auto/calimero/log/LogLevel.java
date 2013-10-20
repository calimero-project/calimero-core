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
 * Defines different log levels.
 * <p>
 * Defines several severity levels used to categorize logging information and
 * loggers. It also gives a clue about the chattiness of the respective object.
 * 
 * @author B. Malinowsky
 */
public class LogLevel
{
	private static final int LOG_OFF = 0;
	private static final int LOG_ALWAYS = 50;
	private static final int LOG_FATAL = 100;
	private static final int LOG_ERROR = 200;
	private static final int LOG_WARN = 300;
	private static final int LOG_INFO = 400;
	private static final int LOG_TRACE = 500;
	private static final int LOG_ALL = 1000;

	/**
	 * The lowest log level, logging is turned off.
	 */
	public static final LogLevel OFF = new LogLevel(LOG_OFF);
	/**
	 * This level identifies the attached object to be always logged if possible (and
	 * logging is not set to off).
	 */
	public static final LogLevel ALWAYS = new LogLevel(LOG_ALWAYS);
	/**
	 * This level identifies the attached object for fatal error purposes.
	 */
	public static final LogLevel FATAL = new LogLevel(LOG_FATAL);
	/**
	 * This level identifies the attached object for error purposes.
	 */
	public static final LogLevel ERROR = new LogLevel(LOG_ERROR);
	/**
	 * This level identifies the attached object for warning purposes.
	 */
	public static final LogLevel WARN = new LogLevel(LOG_WARN);
	/**
	 * This level identifies the attached object for informational purposes.
	 */
	public static final LogLevel INFO = new LogLevel(LOG_INFO);
	/**
	 * This level identifies the attached object for tracing purposes.
	 */
	public static final LogLevel TRACE = new LogLevel(LOG_TRACE);
	/**
	 * This marks a receiver of logged items that it is interested in all log levels
	 * without restrictions, i.e., everything that will be logged.
	 * It is equivalent to specifying the highest log level used for any log item (the most
	 * verbose/chattiest, TRACE or above).
	 */
	public static final LogLevel ALL = new LogLevel(LOG_ALL);

	final int level;

	/**
	 * Creates a new LogLevel object with <code>logLevel</code> assigned to it.
	 * @param logLevel log level represented by this object
	 */
	protected LogLevel(final int logLevel)
	{
		level = logLevel;
	}

	/**
	 * Compares for a higher log level than <code>l</code>.
	 * <p>
	 * 
	 * @param l level to compare
	 * @return true iff this object has a higher log level than <code>l</code>.
	 */
	public final boolean higher(final LogLevel l)
	{
		return level > l.level;
	}

	/**
	 * Compares the log level with <code>obj</code>.
	 * <p>
	 * 
	 * @param obj {@inheritDoc}
	 * @return <code>true</code> if <code>o</code> is of type
	 *         <code>LogLevel</code> and has the same log level as this object,
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(final Object obj)
	{
		if (obj instanceof LogLevel)
			return ((LogLevel) obj).level == level;
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		final int offset = 435273;
		return level + offset;
	}

	/**
	 * Returns the log level in textual format, like "off", "trace",
	 * .&nbsp;.&nbsp;.&nbsp; or "unknown" if level is not defined.
	 * 
	 * @return the log level as string
	 */
	public String toString()
	{
		switch (level) {
		case LOG_OFF:
			return "off";
		case LOG_TRACE:
			return "trace";
		case LOG_INFO:
			return "info";
		case LOG_WARN:
			return "warn";
		case LOG_ERROR:
			return "error";
		case LOG_FATAL:
			return "fatal";
		case LOG_ALWAYS:
			return "always";
		case LOG_ALL:
			return "all";
		default:
			return "unknown";
		}
	}
}

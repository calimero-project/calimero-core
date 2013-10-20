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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Calendar;

/**
 * LogWriter using a {@link Writer} for output.
 * <p>
 * An existing output stream has to be supplied on creation of this LogWriter. All output
 * will be checked against the internal log level, on logging permitted the output is
 * formatted (optional) and handed to the underlying writer.<br>
 * Output is not automatically flushed after each write by default.<br>
 * Using <code>autoFlush = true</code> in the constructor ensures that no data buffering
 * will delay the output. Note that this may degrade performance.<br>
 * For occasional flushing use {@link #flush()} manually.
 * 
 * @author B. Malinowsky
 */
public class LogStreamWriter extends LogWriter
{
	/** Calendar used to generate date/time of logged output message. */
	protected static final Calendar c = Calendar.getInstance();

	/**
	 * Set the formatting behavior of <code>LogStreamWriter</code>.
	 * <p>
	 * Determines if <code>LogStreamWriter</code> should call
	 * {@link #formatOutput(String, LogLevel, String, Throwable)} before writing out the
	 * log message. Defaults to <code>true</code>, but might be set to <code>false</code>
	 * by subtypes if message given to <code>write</code> is already formatted.
	 */
	protected boolean formatOutput = true;

	/**
	 * Line separator, retrieved from the property "line.separator" and set in the
	 * constructor.
	 */
	protected String lineSep;

	boolean autoFlush;
	private Writer out;
	private boolean closeOut = true;

	/**
	 * Creates an <i>unformatted</i> <code>LogStreamWriter</code> with specified log level and
	 * output stream.
	 * <p>
	 * A stream writer created by this method will not format the message (using
	 * {@link #formatOutput(String, LogLevel, String, Throwable)}) before writing it to the output
	 * stream. Parameter <code>autoFlush</code> sets flushing behavior on write() calls.
	 * 
	 * @param level log level assigned with this <code>LogStreamWriter</code>
	 * @param os an OutputStream used by this <code>LogStreamWriter</code>
	 * @param autoFlush flush output after every successful call to write()
	 * @param close <code>true</code> to close the output stream <code>os</code> when
	 *        {@link #close()} is executed, <code>false</code> to skip closing the supplied stream;
	 *        this parameter is useful when setting <code>os</code> to global streams like
	 *        <code>System.out</code> (in that case, <code>close</code> should be <code>false</code>
	 *        )
	 * @return the new log writer
	 * @see #LogStreamWriter(LogLevel, OutputStream)
	 */
	public static LogStreamWriter newUnformatted(final LogLevel level, final OutputStream os,
		final boolean autoFlush, final boolean close)
	{
		final LogStreamWriter w = new LogStreamWriter(level, os, autoFlush, close);
		w.formatOutput = false;
		return w;
	}
	
	/**
	 * Creates a <code>LogStreamWriter</code>.
	 * <p>
	 * Sets line separator; also called by subtypes creating the output stream on their own.
	 */
	protected LogStreamWriter()
	{
		try {
			lineSep = System.getProperty("line.separator");
		}
		catch (final SecurityException e) {}
		if (lineSep == null)
			lineSep = "\n";
	}

	/**
	 * Creates a <code>LogStreamWriter</code> with specified output stream.
	 * <p>
	 * The output stream is wrapped by a BufferedWriter.
	 * 
	 * @param os an OutputStream used by this LogStreamWriter
	 */
	public LogStreamWriter(final OutputStream os)
	{
		this();
		createWriter(os);
	}

	/**
	 * Creates a <code>LogStreamWriter</code> with specified log level and output stream.
	 * <p>
	 * 
	 * @param level log level assigned with this <code>LogStreamWriter</code>
	 * @param os an OutputStream used by this <code>LogStreamWriter</code>
	 * @see #LogStreamWriter(OutputStream)
	 */
	public LogStreamWriter(final LogLevel level, final OutputStream os)
	{
		this(os);
		setLogLevel(level);
	}

	/**
	 * Creates a <code>LogStreamWriter</code> with specified log level and output stream.
	 * <p>
	 * Parameter <code>autoFlush</code> sets flushing behavior on write() calls.
	 * 
	 * @param level log level assigned with this <code>LogStreamWriter</code>
	 * @param os an OutputStream used by this <code>LogStreamWriter</code>
	 * @param autoFlush flush output after every successful call to write()
	 * @see #LogStreamWriter(LogLevel, OutputStream)
	 */
	public LogStreamWriter(final LogLevel level, final OutputStream os, final boolean autoFlush)
	{
		this(level, os);
		this.autoFlush = autoFlush;
	}

	/**
	 * Creates a <code>LogStreamWriter</code> with specified log level and output stream.
	 * <p>
	 * Parameter <code>autoFlush</code> sets flushing behavior on write() calls.
	 * 
	 * @param level log level assigned with this <code>LogStreamWriter</code>
	 * @param os an OutputStream used by this <code>LogStreamWriter</code>
	 * @param autoFlush flush output after every successful call to write()
	 * @param close <code>true</code> to close the output stream <code>os</code> when
	 *        {@link #close()} is executed, <code>false</code> to skip closing the supplied stream;
	 *        this parameter is useful when setting <code>os</code> to global streams like
	 *        <code>System.out</code> (in that case, <code>close</code> should be <code>false</code>
	 *        )
	 * @see #LogStreamWriter(LogLevel, OutputStream)
	 */
	public LogStreamWriter(final LogLevel level, final OutputStream os, final boolean autoFlush,
		final boolean close)
	{
		this(level, os, autoFlush);
		closeOut = close;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogWriter#write
	 * (java.lang.String, tuwien.auto.calimero.log.LogLevel, java.lang.String)
	 */
	public void write(final String logService, final LogLevel level, final String msg)
	{
		doWrite(logService, level, msg, null);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogWriter#write
	 * (java.lang.String, tuwien.auto.calimero.log.LogLevel, java.lang.String,
	 * java.lang.Throwable)
	 */
	public void write(final String logService, final LogLevel level, final String msg,
		final Throwable t)
	{
		doWrite(logService, level, msg, t);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogWriter#flush()
	 */
	public synchronized void flush()
	{
		if (out != null)
			try {
				out.flush();
			}
			catch (final IOException e) {
				getErrorHandler().error(this, "on flush", e);
			}
	}

	/**
	 * {@inheritDoc} Depending on the parameter <code>close</code> of
	 * {@link #LogStreamWriter(LogLevel, OutputStream, boolean, boolean)}, the assigned
	 * output stream resource might not be closed
	 * 
	 * @see #LogStreamWriter(LogLevel, OutputStream, boolean, boolean)
	 */
	public synchronized void close()
	{
		if (out != null) {
			if (closeOut)
				try {
					out.close();
				}
				catch (final IOException e) {}
			out = null;
		}
	}

	/**
	 * Sets the underlying writer to use for logging output.
	 * <p>
	 * The log stream writer obtains ownership of the writer object.
	 * 
	 * @param w the Writer
	 */
	protected void setOutput(final Writer w)
	{
		out = w;
	}

	/**
	 * Checks if writer is prepared for logging, and output with log <code>level</code>
	 * should be accepted by this <code>LogStreamWriter</code>.
	 * <p>
	 * Whether a log writer is prepared for logging is to check for a set output
	 * destination, and, e.g., if the underlying output stream is open.
	 * 
	 * @param level log level to check against
	 * @return true if log is permitted, false otherwise
	 */
	protected boolean isLoggable(final LogLevel level)
	{
		if (out == null || level == LogLevel.OFF || level.higher(logLevel))
			return false;
		return true;
	}

	/**
	 * Creates a formatted output string from the input parameters.
	 * <p>
	 * Override this method to provide a different output format.<br>
	 * The output returned by default follows the shown format. The date/time format is
	 * according ISO 8601 representation, extended format with decimal fraction of second
	 * (milliseconds):<br>
	 * "YYYY-MM-DD hh:mm:ss,sss <code>level.toString()</code>, <code>logService</code>:
	 * <code>msg</code> (<code>t.getMessage()</code>)"<br>
	 * or, if throwable is <code>null</code> or throwable-message is <code>null</code><br>
	 * "YYYY-MM-DD hh:mm:ss,sss <code>level.toString()</code>, <code>logService</code>:
	 * <code>msg</code>".<br>
	 * If <code>logService</code> contains '.' in the name, only the part after the last
	 * '.' will be used. This way names like "package.subpackage.name" are shortened to
	 * "name". Nevertheless, if the first character after '.' is numeric, no truncation
	 * will be done to allow e.g. IP addresses in the log service name.
	 * 
	 * @param logService name of the log service the message comes from
	 * @param l log level of message and throwable
	 * @param msg message to format
	 * @param t an optional throwable object to format, might be <code>null</code>
	 * @return the formatted output
	 */
	protected String formatOutput(final String logService, final LogLevel l,
		final String msg, final Throwable t)
	{
		final StringBuffer b = new StringBuffer(150);
		final int yr;
		final int mth;
		final int day;
		final int hr;
		final int min;
		final int sec;
		final int ms;
		synchronized (c) {
			c.setTimeInMillis(System.currentTimeMillis());
			yr = c.get(Calendar.YEAR);
			mth = c.get(Calendar.MONTH) + 1;
			day = c.get(Calendar.DAY_OF_MONTH);
			hr = c.get(Calendar.HOUR_OF_DAY);
			min = c.get(Calendar.MINUTE);
			sec = c.get(Calendar.SECOND);
			ms = c.get(Calendar.MILLISECOND);
		}
		b.append(yr);
		b.append('-').append(pad2Digits(mth));
		b.append('-').append(pad2Digits(day));
		b.append(' ').append(pad2Digits(hr));
		b.append(':').append(pad2Digits(min));
		b.append(':').append(pad2Digits(sec));
		b.append(',');
		if (ms < 99)
			b.append('0');
		b.append(pad2Digits(ms));
		b.append(' ').append(l.toString());
		if (logService.length() > 0)
			b.append(", ");
		// get index after last dot character
		final int dot = logService.lastIndexOf('.') + 1;
		// if dot is in character sequence but followed by a digit, append unmodified name
		// otherwise, append only name part after dot
		if (dot > 0 && dot < logService.length() && Character.isDigit(logService.charAt(dot)))
			b.append(logService);
		else
			b.append(logService.substring(dot));

		b.append(": ").append(msg);
		if (t != null) {
			final String s = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
			b.append(" (").append(s).append(")");
			
			if (logLevel.equals(LogLevel.FATAL)) {
				final StackTraceElement[] trace = t.getStackTrace();
				for (int i = 0; i < trace.length; ++i)
					b.append("\n").append("\t").append(trace[i]);
			}
		}
		return b.toString();
	}

	void createWriter(final OutputStream os)
	{
		setOutput(new BufferedWriter(new OutputStreamWriter(os), 512));
	}

	private void doWrite(final String logService, final LogLevel level, final String msg,
		final Throwable t)
	{
		if (!isLoggable(level))
			return;
		try {
			final String s = formatOutput ? formatOutput(logService, level, msg, t)
					: (msg + (t != null ? ", " + t.getMessage() : ""));
			synchronized (this) {
				out.write(s);
				out.write(lineSep);
				if (autoFlush)
					out.flush();
			}
		}
		catch (final Exception e) {
			// IOException and RuntimeException
			getErrorHandler().error(this, "on write", e);
		}
	}

	private static String pad2Digits(final int i)
	{
		return i > 9 ? Integer.toString(i) : "0" + Integer.toString(i);
	}
}

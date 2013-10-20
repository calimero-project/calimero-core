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
 * An error handler is invoked on problems of a log writer which are not handled by the
 * writer itself.
 * <p>
 * Such problems shouldn't get logged using another log writer, to prevent the possibility
 * of a fault / failure chain, and shouldn't lead to disruption of the logging system at a
 * whole.<br>
 * Moreover, log writer errors most certainly can't be returned back to the owner of the
 * writer responsible for handling the errors. And handling errors directly in log
 * services can rarely be done in a satisfying way.<br>
 * So the associated error handler is used for such error events.<br>
 * The handler itself might be adapted to the application needs. For example, an
 * overridden {@link #error(LogWriter, String, Exception)} might close the log writer the
 * error originated from.
 * 
 * @author B. Malinowsky
 * @see LogWriter
 */
public class ErrorHandler
{
	/**
	 * Sets the maximum number {@link #error(LogWriter, String, Exception)} should handle
	 * error events.
	 * <p>
	 * The number should be set reasonably low to prevent a continued handling of repeated
	 * errors of the same source.<br>
	 * Initialized to 5 by default.
	 */
	protected int maxInvocations = 5;

	/**
	 * Counter how often error() was called (without abort).
	 */
	protected int invocations;

	/**
	 * Creates a new error handler.
	 */
	public ErrorHandler()
	{}

	/**
	 * Invoked on a log writer error event.
	 * <p>
	 * The default behavior used here prints the logging source and the error message to
	 * the system standard error stream (System.err). If an exception object is given, the
	 * exception and the last method calls of the log writer leading to the error are also
	 * printed.<br>
	 * Only predefined maximum invocations are handled, subsequent calls are ignored.
	 * 
	 * @param source the log writer the error originated from
	 * @param msg message describing the error
	 * @param e exception related to the error, may be <code>null</code>
	 */
	public synchronized void error(final LogWriter source, final String msg, final Exception e)
	{
		if (invocations >= maxInvocations)
			return;
		++invocations;

		String out = source + ": " + msg;
		StackTraceElement[] trace = null;
		if (e != null) {
			out += " (" + e.getMessage() + ")";
			trace = e.getStackTrace();
		}
		synchronized (System.err) {
			System.err.println(out);
			final String srcName = source.getClass().getName();
			if (trace != null)
				for (int i = 0; i < trace.length; ++i) {
					System.err.println("\t- " + trace[i]);
					if (trace[i].getClassName().equals(srcName))
						break;
				}
		}
	}
}

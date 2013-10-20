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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


/**
 * A LogWriter using a file resource as output destination for log information.
 * <p>
 * A file name is supplied on creation of this log writer, the file is opened and used for
 * further logging. After {@link #close()}ing the log writer, it cannot be opened
 * anymore.<br>
 * For output the platform's default character set is used.<br>
 * A maximum allowed file size may be specified to prevent file size explosion. If the
 * size limit is reached, the file content is deleted before any new output.
 * 
 * @author B. Malinowsky
 */
public class LogFileWriter extends LogStreamWriter
{
	// we do a distinction of the used default character set
	// for easier calculation of file size
	private static final int byteEnc;
	private static final int lineSep;

	static {
		// get line separator size
		lineSep = System.getProperty("line.separator").length();
		// get character encoding byte size
		byte[] buf = new String(new char[] { ' ', ' ' }).getBytes();
		// if length is 4 or 6 (with byte order mark) we have UTF-16
		if (buf.length == 4 || buf.length == 6)
			byteEnc = 2;
		else {
			// check for UTF-8 with a character encoded in more bytes
			buf = new String(new char[] { '\uFFFF' }).getBytes();
			// on length == 1 we have single byte encoding
			if (buf.length == 1)
				byteEnc = 1;
			else
				byteEnc = 3;
		}
	}

	private String file;
	// log file size in bytes
	private int logSize;
	// maximum allowed log file size in bytes
	private int maxSize;

	/**
	 * Creates a LogFileWriter to write to the output file named by <code>file</code>
	 * and open the file according to <code>append</code>.
	 * <p>
	 * 
	 * @param file file name in the file system to open or create, the path to the file
	 *        has to exist
	 * @param append set this true to append output at end of file, or false to start
	 *        writing into an empty file at the beginning
	 * @throws KNXLogException if path to file does not exist, if file can not be created
	 *         or opened
	 */
	public LogFileWriter(final String file, final boolean append) throws KNXLogException
	{
		if (append) {
			// if file does not exist, we will fail later anyway...
			final File f = new File(file);
			logSize = (int) f.length();
		}
		this.file = file;
		formatOutput = false;
		try {
			createWriter(new FileOutputStream(file, append));
		}
		catch (final FileNotFoundException e) {
			throw new KNXLogException("open log file " + file + ": " + e.getMessage(), e);
		}
		catch (final SecurityException e) {
			throw new KNXLogException("open log file " + file + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Like {@link #LogFileWriter(String, boolean)}, with the option to adjust the
	 * automatic flush behavior of data.
	 * <p>
	 * 
	 * @param file file name in the file system to open or create
	 * @param append set this true to append output at end of file, or false to start
	 *        writing into an empty file at the beginning
	 * @param autoFlush set true to force data be immediately written to file after every
	 *        write() call, set false to buffer data and flush only on full buffer
	 * @throws KNXLogException if file can not be created or opened
	 */
	public LogFileWriter(final String file, final boolean append, final boolean autoFlush)
		throws KNXLogException
	{
		this(file, append);
		this.autoFlush = autoFlush;
	}

	/**
	 * Like {@link #LogFileWriter(String, boolean)}, with the option to adjust the filter
	 * log level for information logged by LogFileWriter.
	 * <p>
	 * 
	 * @param level log level used by this LogWriter to filter log information
	 * @param file file name in the file system to open or create
	 * @param append set this true to append output at end of file, or false to start
	 *        writing into an empty file at the beginning
	 * @throws KNXLogException if file can not be created or opened
	 */
	public LogFileWriter(final LogLevel level, final String file, final boolean append)
		throws KNXLogException
	{
		this(file, append);
		setLogLevel(level);
	}

	/**
	 * Like {@link #LogFileWriter(LogLevel, String, boolean)}, with the option to specify
	 * the maximum file size allowed for all output written.
	 * <p>
	 * During opening of <code>file</code>, the file size is checked to be smaller than
	 * maxSize, otherwise the file content is erased.<br>
	 * If a call to write() would exceed the maximum file size specified, the file content
	 * is erased before the new log information is written.
	 * 
	 * @param level log level used by this LogWriter to filter log information
	 * @param file file name in the file system to open or create
	 * @param append set this true to append output at end of file, or false to start
	 *        writing into an empty file at the beginning
	 * @param maxSize maximum file size generated by this LogFileWriter
	 * @throws KNXLogException if file can not be created or opened
	 */
	public LogFileWriter(final LogLevel level, final String file, final boolean append,
		final int maxSize) throws KNXLogException
	{
		this(level, file, append);
		setMaxSize(maxSize);
		ensureMaxSize("");
	}

	/**
	 * Like {@link #LogFileWriter(LogLevel, String, boolean, int)}, with the option to
	 * adjust the automatic flush behavior of data.
	 * <p>
	 * 
	 * @param level log level used by this LogWriter to filter log information
	 * @param file file name in the file system to open or create
	 * @param append set this true to append output at end of file, or false to start
	 *        writing into an empty file at the beginning
	 * @param maxSize maximum file size generated by this LogFileWriter
	 * @param autoFlush set true to force data be immediately written to file after every
	 *        write() call, set false to buffer data and flush only on full buffer
	 * @throws KNXLogException if file can not be created or opened
	 */
	public LogFileWriter(final LogLevel level, final String file, final boolean append,
		final int maxSize, final boolean autoFlush) throws KNXLogException
	{
		this(level, file, append, maxSize);
		this.autoFlush = autoFlush;
	}

	/**
	 * Returns the file name of the file resource used by this LogFileWriter or "" if the
	 * log writer has already been closed.
	 * <p>
	 * The file name is the same as supplied on creation of this LogWriter, no path
	 * resolving etc. was done.
	 * 
	 * @return file name
	 */
	public final String getFileName()
	{
		return file;
	}

	/**
	 * Sets the maximum allowed file size generated by this LogFileWriter.
	 * <p>
	 * The value is only set if <code>size</code> >= 0.<br>
	 * If <code>size</code> has a value of 0, no file size limit is enforced.
	 * 
	 * @param size new allowed file size in bytes
	 */
	public final void setMaxSize(final int size)
	{
		if (size >= 0)
			maxSize = size;
	}

	/**
	 * Returns the maximum allowed file size generated by this LogFileWriter, or 0 if no
	 * maximum was set.
	 * <p>
	 * 
	 * @return maximum file size in bytes
	 */
	public final int getMaxSize()
	{
		return maxSize;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogStreamWriter#write
	 * (java.lang.String, tuwien.auto.calimero.log.LogLevel, java.lang.String)
	 */
	public void write(final String logService, final LogLevel level, final String msg)
	{
		doFileWrite(logService, level, msg, null);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogStreamWriter#write
	 * (java.lang.String, tuwien.auto.calimero.log.LogLevel, java.lang.String,
	 * java.lang.Throwable)
	 */
	public void write(final String logService, final LogLevel level, final String msg,
		final Throwable t)
	{
		doFileWrite(logService, level, msg, t);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.log.LogStreamWriter#close()
	 */
	public void close()
	{
		super.close();
		file = "";
	}

	private void doFileWrite(final String logService, final LogLevel level, final String msg,
		final Throwable t)
	{
		if (isLoggable(level)) {
			final String s = formatOutput(logService, level, msg, t);
			ensureMaxSize(s);
			synchronized (this) {
				super.write(logService, level, s);
				logSize += getByteLength(s) + lineSep;
			}
		}
	}

	private void ensureMaxSize(final String msg)
	{
		if (maxSize == 0)
			return;
		synchronized (this) {
			if (logSize + getByteLength(msg) + lineSep > maxSize) {
				// remember used file name
				final String fileName = file;
				close();
				try {
					// no append, to reset file length to 0
					createWriter(new FileOutputStream(fileName));
					file = fileName;
					logSize = 0;
				}
				catch (final FileNotFoundException e) {
					getErrorHandler().error(this, "on creation of file " + fileName, e);
				}
				catch (final SecurityException e) {
					getErrorHandler().error(this, "access denied", e);
				}
			}
		}
	}

	private int getByteLength(final String s)
	{
		if (byteEnc == 1)
			return s.length();
		if (byteEnc == 2)
			return s.length() * 2;
		return s.getBytes().length;
	}
}

/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2014, 2016 B. Malinowsky

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

import org.slf4j.Logger;
import org.slf4j.Marker;

import tuwien.auto.calimero.log.LogService.LogLevel;

class AsyncLogger implements Logger
{
	private final Logger logger;

	AsyncLogger(final Logger l)
	{
		logger = l;
	}

	@Override
	public String getName()
	{
		return logger.getName();
	}

	@Override
	public boolean isTraceEnabled()
	{
		return logger.isTraceEnabled();
	}

	@Override
	public void trace(final String msg)
	{
		LogService.async(logger, LogLevel.TRACE, null, msg, (Throwable) null);
	}

	@Override
	public void trace(final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.TRACE, format, arg);
	}

	@Override
	public void trace(final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.TRACE, format, arg1, arg2);
	}

	@Override
	public void trace(final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.TRACE, null, format, arguments);
	}

	@Override
	public void trace(final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.TRACE, null, msg, t);
	}

	@Override
	public boolean isTraceEnabled(final Marker marker)
	{
		return logger.isTraceEnabled(marker);
	}

	@Override
	public void trace(final Marker marker, final String msg)
	{
		LogService.async(logger, LogLevel.TRACE, marker, msg, (Throwable) null);
	}

	@Override
	public void trace(final Marker marker, final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.TRACE, marker, format, arg);
	}

	@Override
	public void trace(final Marker marker, final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.TRACE, marker, format, arg1, arg2);
	}

	@Override
	public void trace(final Marker marker, final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.TRACE, marker, format, arguments);
	}

	@Override
	public void trace(final Marker marker, final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.TRACE, marker, msg, t);
	}

	@Override
	public boolean isDebugEnabled()
	{
		return logger.isDebugEnabled();
	}

	@Override
	public void debug(final String msg)
	{
		LogService.async(logger, LogLevel.DEBUG, msg);
	}

	@Override
	public void debug(final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.DEBUG, format, arg);
	}

	@Override
	public void debug(final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.DEBUG, format, arg1, arg2);
	}

	@Override
	public void debug(final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.DEBUG, format, arguments);
	}

	@Override
	public void debug(final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.DEBUG, msg, t);
	}

	@Override
	public boolean isDebugEnabled(final Marker marker)
	{
		return logger.isDebugEnabled(marker);
	}

	@Override
	public void debug(final Marker marker, final String msg)
	{
		LogService.async(logger, LogLevel.DEBUG, marker, msg);
	}

	@Override
	public void debug(final Marker marker, final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.DEBUG, marker, format, arg);
	}

	@Override
	public void debug(final Marker marker, final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.DEBUG, marker, format, arg1, arg2);
	}

	@Override
	public void debug(final Marker marker, final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.DEBUG, marker, format, arguments);
	}

	@Override
	public void debug(final Marker marker, final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.DEBUG, marker, msg, t);
	}

	@Override
	public boolean isInfoEnabled()
	{
		return logger.isInfoEnabled();
	}

	@Override
	public void info(final String msg)
	{
		LogService.async(logger, LogLevel.INFO, msg);
	}

	@Override
	public void info(final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.INFO, format, arg);
	}

	@Override
	public void info(final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.INFO, format, arg1, arg2);
	}

	@Override
	public void info(final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.INFO, format, arguments);
	}

	@Override
	public void info(final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.INFO, msg, t);
	}

	@Override
	public boolean isInfoEnabled(final Marker marker)
	{
		return logger.isInfoEnabled(marker);
	}

	@Override
	public void info(final Marker marker, final String msg)
	{
		LogService.async(logger, LogLevel.INFO, marker, msg);
	}

	@Override
	public void info(final Marker marker, final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.INFO, marker, format, arg);
	}

	@Override
	public void info(final Marker marker, final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.INFO, marker, format, arg1, arg2);
	}

	@Override
	public void info(final Marker marker, final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.INFO, marker, format, arguments);
	}

	@Override
	public void info(final Marker marker, final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.INFO, marker, msg, t);
	}

	@Override
	public boolean isWarnEnabled()
	{
		return logger.isWarnEnabled();
	}

	@Override
	public void warn(final String msg)
	{
		LogService.async(logger, LogLevel.WARN, msg);
	}

	@Override
	public void warn(final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.WARN, format, arg);
	}

	@Override
	public void warn(final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.WARN, format, arg1, arg2);
	}

	@Override
	public void warn(final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.WARN, format, arguments);
	}

	@Override
	public void warn(final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.WARN, msg, t);
	}

	@Override
	public boolean isWarnEnabled(final Marker marker)
	{
		return logger.isWarnEnabled(marker);
	}

	@Override
	public void warn(final Marker marker, final String msg)
	{
		LogService.async(logger, LogLevel.WARN, marker, msg);
	}

	@Override
	public void warn(final Marker marker, final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.WARN, marker, format, arg);
	}

	@Override
	public void warn(final Marker marker, final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.WARN, marker, format, arg1, arg2);
	}

	@Override
	public void warn(final Marker marker, final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.WARN, marker, format, arguments);
	}

	@Override
	public void warn(final Marker marker, final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.WARN, marker, msg, t);
	}

	@Override
	public boolean isErrorEnabled()
	{
		return logger.isErrorEnabled();
	}

	@Override
	public void error(final String msg)
	{
		LogService.async(logger, LogLevel.ERROR, msg);
	}

	@Override
	public void error(final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.ERROR, format, arg);
	}

	@Override
	public void error(final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.ERROR, format, arg1, arg2);
	}

	@Override
	public void error(final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.ERROR, format, arguments);
	}

	@Override
	public void error(final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.ERROR, msg, t);
	}

	@Override
	public boolean isErrorEnabled(final Marker marker)
	{
		return logger.isErrorEnabled(marker);
	}

	@Override
	public void error(final Marker marker, final String msg)
	{
		LogService.async(logger, LogLevel.ERROR, marker, msg);
	}

	@Override
	public void error(final Marker marker, final String format, final Object arg)
	{
		LogService.async(logger, LogLevel.ERROR, marker, format, arg);
	}

	@Override
	public void error(final Marker marker, final String format, final Object arg1, final Object arg2)
	{
		LogService.async(logger, LogLevel.ERROR, marker, format, arg1, arg2);
	}

	@Override
	public void error(final Marker marker, final String format, final Object... arguments)
	{
		LogService.async(logger, LogLevel.ERROR, marker, format, arguments);
	}

	@Override
	public void error(final Marker marker, final String msg, final Throwable t)
	{
		LogService.async(logger, LogLevel.ERROR, marker, msg, t);
	}
}

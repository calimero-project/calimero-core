/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2020, 2025 B. Malinowsky

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

package io.calimero.internal;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public final class Executor {
	private static final ThreadFactory vtFactory = Thread.ofVirtual().name("calimero vt ", 0)
			.inheritInheritableThreadLocals(false).factory();


	@SuppressWarnings("serial")
	private static final class KillThreadAfterExecuteException extends RuntimeException { }

	private static final ThreadFactory vtWrapperFactory = r -> {
		final var thread = vtFactory.newThread(r);
		thread.setUncaughtExceptionHandler((t, e) -> {
			if (e instanceof KillThreadAfterExecuteException)
				return;
			t.getThreadGroup().uncaughtException(t, e);
		});

		return thread;
	};

	private static final ScheduledExecutorService vtScheduledExecutor;
	static {
		// STPE acts as a fixed-sized pool using corePoolSize threads and an unbounded queue
		final var stpe = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE, vtWrapperFactory) {

			@Override
			protected void afterExecute(final Runnable r, final Throwable t) {
				checkFailedFuture(r);
				throw new KillThreadAfterExecuteException();
			}
		};
		stpe.allowCoreThreadTimeOut(true);
		stpe.setKeepAliveTime(10, TimeUnit.SECONDS);

		vtScheduledExecutor = Executors.unconfigurableScheduledExecutorService(stpe);
	}

	private static void checkFailedFuture(final Runnable r) {
		if (r instanceof final Future<?> f && f.isDone()) try {
			f.get();
		}
		catch (InterruptedException | CancellationException ignore) {}
		catch (final ExecutionException e) {
			e.getCause().printStackTrace();
		}
	}

	private static final ExecutorService vtExecutor = Executors.newThreadPerTaskExecutor(vtWrapperFactory);



	public static void execute(final Runnable task) { vtExecutor.execute(task); }

	public static Thread execute(final Runnable task, final String name) {
		final var thread = Thread.ofVirtual().inheritInheritableThreadLocals(false).name(name).unstarted(task);
		thread.start();
		return thread;
	}

	public static ExecutorService executor() { return vtExecutor; }

	public static ScheduledExecutorService scheduledExecutor() { return vtScheduledExecutor; }
}

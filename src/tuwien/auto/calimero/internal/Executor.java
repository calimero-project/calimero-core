/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2020, 2022 B. Malinowsky

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

package tuwien.auto.calimero.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Access to executors using daemon threads.
 */
public final class Executor {
	private static final String idleThreadName = "Calimero idle thread";

	private static final ThreadFactory threadFactory = r -> {
		final Thread t = new Thread(r, idleThreadName);
		t.setDaemon(true);
		return t;
	};

	private static final ExecutorService executor;
	private static final ScheduledExecutorService scheduledExecutor;
	static {
		final var se = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), threadFactory) {
			@Override
			protected void afterExecute(final Runnable r, final Throwable t) {
				Thread.currentThread().setName(idleThreadName);
			}
		};
		executor = Executors.unconfigurableExecutorService(se);

		// STPE acts as a fixed-sized pool using corePoolSize threads and an unbounded queue
		final var stpe = new ScheduledThreadPoolExecutor(10, threadFactory) {
			@Override
			protected void afterExecute(final Runnable r, final Throwable t) {
				Thread.currentThread().setName(idleThreadName);
			}
		};
		stpe.allowCoreThreadTimeOut(true);
		stpe.setKeepAliveTime(61, TimeUnit.SECONDS);
		scheduledExecutor = Executors.unconfigurableScheduledExecutorService(stpe);
	}


	public static void execute(final Runnable task) { executor.execute(task); }

	public static Thread execute(final Runnable task, final String name) {
		final var thread = new Thread(task, name);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	public static ExecutorService executor() { return executor; }

	public static ScheduledExecutorService scheduledExecutor() { return scheduledExecutor; }
}

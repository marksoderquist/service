package com.parallelsymmetry.escape.service.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.parallelsymmetry.escape.utility.Controllable;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class TaskManager implements Persistent<TaskManager>, Controllable {

	private static final int DEFAULT_THREAD_COUNT = 5;

	private ExecutorService service;

	private int threadCount = DEFAULT_THREAD_COUNT;

	private Settings settings;

	private BlockingQueue<Runnable> queue;

	public TaskManager() {
		queue = new LinkedBlockingQueue<Runnable>();
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount( int count ) {
		this.threadCount = count;
		saveSettings( settings );
		try {
			stopAndWait();
		} catch( InterruptedException exception ) {
			Log.write( exception );
		}
		start();
	}

	@Override
	public synchronized void start() {
		if( isRunning() ) return;
		service = new ThreadPoolExecutor( 0, threadCount, 5, TimeUnit.SECONDS, queue );
	}

	@Override
	public synchronized void startAndWait() throws InterruptedException {
		start();
	}

	@Override
	public synchronized void startAndWait( long timeout, TimeUnit unit ) throws InterruptedException {
		start();
	}

	@Override
	public synchronized void stop() {
		if( service == null || service.isShutdown() ) return;
		service.shutdown();
	}

	@Override
	public synchronized void stopAndWait() throws InterruptedException {
		stopAndWait( 0, TimeUnit.SECONDS );
	}

	@Override
	public synchronized void stopAndWait( long timeout, TimeUnit unit ) throws InterruptedException {
		stop();
		if( service != null ) service.awaitTermination( timeout, unit );
	}

	@Override
	public boolean isRunning() {
		return service != null && !service.isTerminated();
	}

	/**
	 * Asynchronously submit a task.
	 * 
	 * @param <T>
	 * @param task
	 * @return
	 */
	public <T> Future<T> submit( Task<T> task ) {
		if( service == null ) return null;
		return service.submit( task );
	}

	/**
	 * Asynchronously submit a collection of tasks.
	 * 
	 * @param <T>
	 * @param tasks
	 * @return
	 */
	public <T> List<Future<T>> submitAll( Collection<? extends Task<T>> tasks ) {
		List<Future<T>> futures = new ArrayList<Future<T>>();

		for( Task<T> task : tasks ) {
			futures.add( service.submit( task ) );
		}

		return futures;
	}

	/**
	 * Synchronously submit a task.
	 * 
	 * @param <T>
	 * @param task
	 * @return
	 * @throws InterruptedException
	 */
	public <T> List<Future<T>> invoke( Task<T> task ) throws InterruptedException {
		return invoke( task, 0, TimeUnit.SECONDS );
	}

	/**
	 * Synchronously submit a task.
	 * 
	 * @param <T>
	 * @param task
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	public <T> List<Future<T>> invoke( Task<T> task, long timeout, TimeUnit unit ) throws InterruptedException {
		if( service == null ) return null;
		List<Task<T>> tasks = new ArrayList<Task<T>>();
		tasks.add( task );
		return service.invokeAll( tasks, timeout, unit );
	}

	/**
	 * Synchronously submit a collection of tasks.
	 * 
	 * @param <T>
	 * @param tasks
	 * @return
	 * @throws InterruptedException
	 */
	public <T> List<Future<T>> invokeAll( Collection<? extends Task<T>> tasks ) throws InterruptedException {
		return invokeAll( tasks, 0, TimeUnit.SECONDS );
	}

	/**
	 * Synchronously submit a collection of tasks.
	 * 
	 * @param <T>
	 * @param tasks
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	public <T> List<Future<T>> invokeAll( Collection<? extends Task<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException {
		if( service == null ) return null;
		return service.invokeAll( tasks, timeout, unit );
	}

	@Override
	public TaskManager loadSettings( Settings settings ) {
		this.settings = settings;

		this.threadCount = settings.getInt( "thread-count", DEFAULT_THREAD_COUNT );

		return this;
	}

	@Override
	public TaskManager saveSettings( Settings settings ) {
		settings.putInt( "thread-count", threadCount );

		return this;
	}

}

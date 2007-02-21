package org.novaworx.service;

import java.io.IOException;

import org.novaworx.util.Log;

/**
 * The Service class is a generic service class.
 * 
 * @author mvsoder
 */
public abstract class Service {

	private enum State {
		STARTING, STARTED, STOPPING, STOPPED
	};

	private String name;

	private Thread thread;

	private boolean execute;

	private State state = State.STOPPED;

	private final Object startedLock = new Object();

	private Exception exception;
	
	protected Service() {
		String className = getClass().getName();
		int index = className.lastIndexOf( '.' );
		this.name = className.substring( index + 1 );
	}

	protected Service( String name ) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Start the Service. This method creates the service thread and returns
	 * immediately.
	 */
	public final void start() {
		execute = true;
		thread = new Thread( new ServiceRunner(), name );
		thread.setPriority( Thread.NORM_PRIORITY );
		thread.setDaemon( true );
		thread.start();
	}

	/**
	 * Start the Service and wait for the start operation to complete before
	 * returning.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public final void startAndWait() throws Exception {
		start();
		waitForStartup();
		if( exception != null ) {
			throw new Exception( exception );
		}
	}

	/**
	 * Stop the Service. This method interrupts the service thread and returns
	 * immediately.
	 */
	public final void stop() {
		execute = false;
		if( thread != null) thread.interrupt();
	}

	/**
	 * Stop the Service and wait for the stop operation to complete before
	 * returning.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public final void stopAndWait() throws Exception {
		stop();
		waitForShutdown();
		if( exception != null ) {
			throw new Exception( exception );
		}
	}

	/**
	 * Check if the service is currently running.
	 * 
	 * @return True if connected, false otherwise.
	 */
	public final synchronized boolean isRunning() {
		return state == State.STARTED;
	}

	/**
	 * Restart the service.
	 * 
	 * @throws IOException
	 */
	public final void restart() throws Exception {
		// Don't use start() and stop(), they cause threading issues.
		stopAndWait();
		startAndWait();
	}

	/**
	 * Subclasses implement this method to start the service. Implementations of
	 * this method should not block the thread.
	 * 
	 * @throws IOException
	 */
	protected abstract void startService() throws Exception;

	/**
	 * Subclasses implement this method to stop the service. Implementations of
	 * this method should not block the thread.
	 * 
	 * @throws IOException
	 */
	protected abstract void stopService() throws Exception;

	/**
	 * Wait for the start operation to complete. Returns immediately if the
	 * service is already started.
	 * 
	 * @throws InterruptedException
	 */
	final synchronized void waitForStartup() throws InterruptedException {
		if( state == State.STARTED ) return;
		wait();
	}

	/**
	 * Wait for the stop operation to complete. Returns immediately if the service
	 * is already stopped.
	 * 
	 * @throws InterruptedException
	 */
	final synchronized void waitForShutdown() throws InterruptedException {
		if( state == State.STOPPED ) return;
		wait();
	}

	private final synchronized void startup() throws Exception {
		if( state == State.STARTED ) {
			Log.write( Log.INFO, getName() + " already started." );
			return;
		}

		if( state == State.STARTING ) {
			return;
		}

		Log.write( Log.DEBUG, "Starting " + getName() + "..." );
		try {
			state = State.STARTING;
			startService();
			state = State.STARTED;
			Log.write( Log.INFO, getName() + " started." );
		} finally {
			notifyAll();
		}
	}

	private final synchronized void shutdown() throws Exception {
		if( state == State.STOPPED ) {
			Log.write( Log.INFO, getName() + " already shutdown." );
			return;
		}

		if( state == State.STOPPING ) {
			return;
		}

		Log.write( Log.DEBUG, "Stopping " + getName() + "..." );
		try {
			state = State.STOPPING;
			stopService();
			state = State.STOPPED;
			Log.write( Log.INFO, getName() + " stopped." );
		} finally {
			notifyAll();
			synchronized( startedLock ) {
				startedLock.notifyAll();
			}
		}
	}

	private final class ServiceRunner implements Runnable {
		/**
		 * The implmentation of the Runnable interface.
		 */
		@Override
		public void run() {
			while( execute ) {
				try {
					startup();

					synchronized( startedLock ) {
						try {
							startedLock.wait();
						} catch( InterruptedException exception ) {
							// Intentionally ignore exception.
						}
					}
				} catch( Exception exception ) {
					Service.this.exception = exception;
					Log.write( exception );
				} finally {
					try {
						shutdown();
					} catch( InterruptedException exception ) {
						Log.write( Log.ERROR, Thread.currentThread().getName() + " interrupted." );
					} catch( Exception exception ) {
						Service.this.exception = exception;
						Log.write( exception );
					}
				}
			}
		}

	}

}

package org.novaworx.service;

import java.io.IOException;

import org.novaworx.util.Log;
import org.novaworx.util.TripLock;

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

	private volatile State state = State.STOPPED;

	private final TripLock startlock = new TripLock();

	private final TripLock runlock = new TripLock();

	private final TripLock stoplock = new TripLock();

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

		startlock.reset();
		runlock.reset();
		stoplock.reset();

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
		if( exception != null ) throw new Exception( exception );
	}

	public final void startAndWait( int timeout ) throws Exception {
		start();
		waitForStartup( timeout );
		if( exception != null ) throw new Exception( exception );
	}

	/**
	 * Stop the Service. This method interrupts the service thread and returns
	 * immediately.
	 */
	public final void stop() {
		execute = false;
		runlock.trip();
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
		if( exception != null ) throw new Exception( exception );
	}

	public final void stopAndWait( int timeout ) throws Exception {
		stop();
		waitForShutdown( timeout );
		if( exception != null ) throw new Exception( exception );
	}

	/**
	 * Check if the service is currently running.
	 * 
	 * @return True if connected, false otherwise.
	 */
	public final synchronized boolean isRunning() {
		return state == State.STARTED;
	}

	public final synchronized String getStatus() {
		return state.toString();
	}

	/**
	 * Restart the service.
	 * 
	 * @throws IOException
	 */
	public final void restart() throws Exception {
		// Don't use start() and stop(), they cause threading issues.
		System.out.println( "Calling stopAndWait()..." );
		stopAndWait();
		System.out.println( "stopAndWait() finished." );
		System.out.println( "Calling startAndWait()..." );
		startAndWait();
		System.out.println( "startAndWait() finished." );
	}

	/**
	 * Wait for the start operation to complete. Returns immediately if the
	 * service is already started.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForStartup() throws InterruptedException {
		System.out.println( getName() + ": Waiting for start lock." );
		startlock.hold();
		System.out.println( getName() + ": Start lock tripped." );
	}

	public final void waitForStartup( int timeout ) throws InterruptedException {
		startlock.hold( timeout );
	}

	/**
	 * Wait for the stop operation to complete. Returns immediately if the service
	 * is already stopped.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForShutdown() throws InterruptedException {
		System.out.println( getName() + ": Waiting for stop lock." );
		stoplock.hold();
		System.out.println( getName() + ": Stop lock tripped." );
	}

	public final void waitForShutdown( int timeout ) throws InterruptedException {
		stoplock.hold( timeout );
	}

	/**
	 * Subclasses implement this method to start the service. Implementations of
	 * this method should return when the service is started. This usually means
	 * starting a separate thread, waiting for the new thread to notify the
	 * calling thread that it has started successfully, and then returning.
	 * 
	 * @throws IOException
	 */
	protected abstract void startService() throws Exception;

	/**
	 * Subclasses implement this method to stop the service. Implementations of
	 * this method should return when the service has stopped. This usually means
	 * stopping a separate thread that was started previously and waiting for the
	 * thread to terminate before returning.
	 * 
	 * @throws IOException
	 */
	protected abstract void stopService() throws Exception;

	private final void startup() throws Exception {
		if( state == State.STARTING ) {
			return;
		}

		if( state == State.STARTED ) {
			Log.write( Log.INFO, getName() + " already started." );
			return;
		}

		Log.write( Log.DEBUG, "Starting " + getName() + "..." );
		try {
			synchronized( this ) {
				state = State.STARTING;
				startService();
				state = State.STARTED;
			}
			Log.write( Log.INFO, getName() + " started." );
		} finally {
			System.out.println( getName() + ": Notify from startup." );
			startlock.trip();
		}
	}

	private final void shutdown() throws Exception {
		if( state == State.STOPPING ) {
			return;
		}

		if( state == State.STOPPED ) {
			Log.write( Log.INFO, getName() + " already shutdown." );
			return;
		}

		Log.write( Log.DEBUG, "Stopping " + getName() + "..." );
		try {
			synchronized( this ) {
				state = State.STOPPING;
				stopService();
				state = State.STOPPED;
			}
			Log.write( Log.INFO, getName() + " stopped." );
		} finally {
			System.out.println( getName() + ": Notify from shutdown." );
			stoplock.trip();
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
					runlock.hold();
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

package org.novaworx.service;

import java.io.IOException;

import org.novaworx.util.ClassUtil;
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

	private String name = ClassUtil.getClassNameOnly( getClass() );

	private Thread thread;

	private volatile State state = State.STOPPED;

	private final TripLock startlock = new TripLock();

	private final TripLock runlock = new TripLock();

	private final TripLock stoplock = new TripLock( true );

	private Exception exception;

	protected Service() {}

	protected Service( String name ) {
		this();
		if( name != null ) this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Start the Service. This method creates the service thread and returns
	 * immediately.
	 */
	public final synchronized void start() {
		if( state != State.STOPPED ) return;

		stoplock.hold();

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
	public final synchronized void stop() {
		if( state != State.STARTED ) return;

		startlock.hold();
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

	public final String getStatus() {
		return state.toString();
	}

	/**
	 * Restart the service.
	 * 
	 * @throws IOException
	 */
	public final void restart() throws Exception {
		// Don't use start() and stop(), they cause threading issues.
		Log.write( Log.DEBUG, getName() + ".restart(): Calling stopAndWait()..." );
		stopAndWait();
		Log.write( Log.DEBUG, getName() + ".restart(): stopAndWait() finished." );
		Log.write( Log.DEBUG, getName() + ".restart(): Calling startAndWait()..." );
		startAndWait();
		Log.write( Log.DEBUG, getName() + ".restart(): startAndWait() finished." );
	}

	/**
	 * Wait for the start operation to complete. Returns immediately if the
	 * service is already started.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForStartup() throws InterruptedException {
		Log.write( Log.DEBUG, getName() + ": Waiting for start lock." );
		startlock.hold();
		Log.write( Log.DEBUG, getName() + ": Start lock tripped." );
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
		Log.write( Log.DEBUG, getName() + ": Waiting for stop lock." );
		stoplock.hold();
		Log.write( Log.DEBUG, getName() + ": Stop lock tripped." );
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

	private synchronized final void startup() throws Exception {
		if( state == State.STARTING ) {
			return;
		}

		if( state == State.STARTED ) {
			Log.write( Log.INFO, getName() + " already started." );
			return;
		}

		Log.write( Log.DEBUG, "Starting " + getName() + "..." );
		try {
			stoplock.reset();
			state = State.STARTING;
			startService();
			state = State.STARTED;
			Log.write( Log.INFO, getName() + " started." );
		} finally {
			Log.write( Log.DEBUG, getName() + ": Notify from startup." );
			startlock.trip();
		}
	}

	private synchronized final void shutdown() throws Exception {
		if( state == State.STOPPING ) {
			return;
		}

		if( state == State.STOPPED ) {
			Log.write( Log.INFO, getName() + " already shutdown." );
			return;
		}

		Log.write( Log.DEBUG, "Stopping " + getName() + "..." );
		try {
			startlock.reset();
			state = State.STOPPING;
			stopService();
			state = State.STOPPED;
			Log.write( Log.INFO, getName() + " stopped." );
		} finally {
			Log.write( Log.DEBUG, getName() + ": Notify from shutdown." );
			stoplock.trip();
		}
	}

	private final class ServiceRunner implements Runnable {
		/**
		 * The implmentation of the Runnable interface.
		 */
		@Override
		public void run() {
			try {
				runlock.reset();
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

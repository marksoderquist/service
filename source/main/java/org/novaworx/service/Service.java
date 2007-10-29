package org.novaworx.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.parallelsymmetry.util.ClassUtil;
import com.parallelsymmetry.util.Log;
import com.parallelsymmetry.util.TripLock;

/**
 * The Service class is a generic service class.
 * 
 * @author mvsoder
 */
public abstract class Service {

	public enum State {
		STARTING, STARTED, STOPPING, STOPPED
	};

	public enum EventType {
		STARTING, STARTED, STOPPING, STOPPED, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
	}

	private String name = ClassUtil.getClassNameOnly( getClass() );

	private Thread thread;

	private volatile State state = State.STOPPED;

	private final Object statelock = new Object();

	private final Object stateChangeLock = new Object();

	private final TripLock startlock = new TripLock();

	private final TripLock runlock = new TripLock();

	private final TripLock stoplock = new TripLock( true );

	private final TripLock startuplock = new TripLock();

	private final TripLock shutdownlock = new TripLock( true );

	private Exception exception;

	private Set<ServiceListener> listeners = new HashSet<ServiceListener>();

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
	public final void start() {
		if( getState() != State.STOPPED ) return;

		stoplock.hold();

		runlock.reset();

		startuplock.reset();
		thread = new Thread( new ServiceRunner(), name );
		thread.setPriority( Thread.NORM_PRIORITY );
		thread.setDaemon( true );
		thread.start();
		startuplock.hold();
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
		if( getState() != State.STARTED ) {
			Log.write( Log.TRACE, "State not started...is: " + getStatus() );
			return;
		}

		startlock.hold();
		shutdownlock.reset();
		runlock.trip();
		shutdownlock.hold();
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
	 * Convenience method to check if the service is currently running (state is
	 * STARTED).
	 * 
	 * @return True if running, false otherwise.
	 */
	public final boolean isRunning() {
		return getState() == State.STARTED;
	}

	public final boolean shouldExecute() {
		State state = getState();
		return state == State.STARTED || state == State.STARTING;
	}

	public final State getState() {
		return state;
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
		Log.write( Log.TRACE, getName() + ".restart(): Calling stopAndWait()..." );
		stopAndWait();
		Log.write( Log.TRACE, getName() + ".restart(): stopAndWait() finished." );
		Log.write( Log.TRACE, getName() + ".restart(): Calling startAndWait()..." );
		startAndWait();
		Log.write( Log.TRACE, getName() + ".restart(): startAndWait() finished." );
	}

	/**
	 * Wait for the start operation to complete. Returns immediately if the
	 * service is already started.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForStartup() throws InterruptedException {
		Log.write( Log.TRACE, getName() + ": Waiting for start lock." );
		startlock.hold();
		Log.write( Log.TRACE, getName() + ": Start lock tripped." );
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
		Log.write( Log.TRACE, getName() + ": Waiting for stop lock." );
		stoplock.hold();
		Log.write( Log.TRACE, getName() + ": Stop lock tripped." );
	}

	public final void waitForShutdown( int timeout ) throws InterruptedException {
		stoplock.hold( timeout );
	}

	public final void addListener( ServiceListener listener ) {
		listeners.add( listener );
	}

	public final void removeListener( ServiceListener listener ) {
		listeners.remove( listener );
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
		if( getState() == State.STARTING ) {
			return;
		}

		if( getState() == State.STARTED ) {
			Log.write( Log.WARN, getName() + " already started." );
			return;
		}

		synchronized( stateChangeLock ) {
			Log.write( Log.TRACE, "Starting " + getName() + "..." );
			try {
				stoplock.reset();
				setState( State.STARTING );
				startuplock.trip();
				fireEvent( EventType.STARTING );
				startService();
				setState( State.STARTED );
				fireEvent( EventType.STARTED );
				Log.write( Log.TRACE, getName() + " started." );
			} finally {
				Log.write( Log.DEBUG, getName() + ": Notify from startup." );
				startlock.trip();
			}
		}
	}

	private final void shutdown() throws Exception {
		if( getState() == State.STOPPING ) {
			return;
		}

		if( getState() == State.STOPPED ) {
			Log.write( Log.WARN, getName() + " already shutdown." );
			return;
		}

		synchronized( stateChangeLock ) {
			Log.write( Log.TRACE, "Stopping " + getName() + "..." );
			try {
				startlock.reset();
				setState( State.STOPPING );
				shutdownlock.trip();
				fireEvent( EventType.STOPPING );
				stopService();
				setState( State.STOPPED );
				fireEvent( EventType.STOPPED );
				Log.write( Log.TRACE, getName() + " stopped." );
			} finally {
				Log.write( Log.DEBUG, getName() + ": Notify from shutdown." );
				stoplock.trip();
			}
		}
	}

	private final void setState( State state ) {
		synchronized( statelock ) {
			this.state = state;
		}
	}

	protected final void fireEvent( EventType type ) {
		fireEvent( new ServiceEvent( this, type ) );
	}

	protected final void fireEvent( ServiceEvent event ) {
		for( ServiceListener listener : listeners ) {
			listener.serviceEventOccurred( event );
		}
	}

	private final class ServiceRunner implements Runnable {
		/**
		 * The implementation of the Runnable interface.
		 */
		@Override
		public void run() {
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

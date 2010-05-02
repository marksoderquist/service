package com.parallelsymmetry.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.parallelsymmetry.log.Log;
import com.parallelsymmetry.util.ClassUtil;

/**
 * The Service class is a generic service class.
 * 
 * @author mvsoder
 */
public abstract class Service {

	public enum State {
		STARTING, STARTED, STOPPING, STOPPED, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
	}

	private static final int FOREVER = -1;

	private String name = ClassUtil.getClassNameOnly( getClass() );

	private Thread thread;

	private State state = State.STOPPED;

	private Set<ServiceListener> listeners = new HashSet<ServiceListener>();

	protected Service() {
		this( null );
	}

	protected Service( String name ) {
		setName( name );

		thread = new Thread( new ServiceRunner(), this.name );
		thread.setPriority( Thread.NORM_PRIORITY );
		thread.setDaemon( true );
		thread.start();
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name != null ? name : ClassUtil.getClassNameOnly( getClass() );
	}

	/**
	 * Start the Service. This method creates the service thread and returns
	 * immediately.
	 */
	public final void start() {
		if( state == State.STOPPED ) changeState( State.STARTING );
	}

	/**
	 * Start the Service and wait for the start operation to complete before
	 * returning.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public final void startAndWait() throws InterruptedException {
		startAndWait( 0 );
	}

	/**
	 * Start the Service and wait for the start operation to complete or the
	 * timeout has elapsed before returning. A timeout of zero will wait
	 * indefinitely.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public final void startAndWait( int timeout ) throws InterruptedException {
		start();
		waitForStartup( timeout );
	}

	/**
	 * Stop the Service. This method interrupts the service thread and returns
	 * immediately.
	 */
	public final void stop() {
		if( state == State.STARTED ) changeState( State.STOPPING );
	}

	/**
	 * Stop the Service and wait for the stop operation to complete before
	 * returning.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public final void stopAndWait() throws Exception {
		stopAndWait( 0 );
	}

	public final void stopAndWait( int timeout ) throws Exception {
		stop();
		waitForShutdown( timeout );
	}

	/**
	 * Restart the service.
	 * 
	 * @throws IOException
	 */
	public final void reset() throws Exception {
		// Don't use start() and stop() because they are asynchronous.
		stopAndWait();
		startAndWait();
	}

	/**
	 * Convenience method to check if the service is currently running (state is
	 * STARTED).
	 * 
	 * @return True if running, false otherwise.
	 */
	public final boolean isRunning() {
		return state == State.STARTED;
	}

	public final boolean isServiceThread() {
		return Thread.currentThread() == thread;
	}

	public final boolean shouldExecute() {
		State state = this.state;
		return state == State.STARTED || state == State.STARTING;
	}

	public final State getState() {
		return state;
	}

	public final String getStatus() {
		return state.toString();
	}

	/**
	 * Wait for the start operation to complete. Returns immediately if the
	 * service is already started.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForStartup() throws InterruptedException {
		waitForStartup( FOREVER );
	}

	public final void waitForStartup( int timeout ) throws InterruptedException {
		waitForState( State.STARTED, timeout );
	}

	/**
	 * Wait for the stop operation to complete. Returns immediately if the service
	 * is already stopped.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForShutdown() throws InterruptedException {
		waitForShutdown( FOREVER );
	}

	public final void waitForShutdown( int timeout ) throws InterruptedException {
		waitForState( State.STOPPED, timeout );
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

	protected final void fireEvent( State state ) {
		fireEvent( new ServiceEvent( this, state ) );
	}

	protected final void fireEvent( ServiceEvent event ) {
		Log.write( Log.DEBUG, "-", getName(), " Event: ", event.getState() );
		for( ServiceListener listener : listeners ) {
			listener.serviceEventOccurred( event );
		}
	}

	private final void startup() throws Exception {
		try {
			startService();
		} finally {
			changeState( State.STARTED );
		}
	}

	private final void shutdown() throws Exception {
		try {
			stopService();
		} finally {
			changeState( State.STOPPED );
		}
	}

	private Object statelock = new Object();

	private void changeState( State state ) {
		if( this.state == state ) return;

		synchronized( statelock ) {
			this.state = state;
			statelock.notifyAll();
		}

		fireEvent( state );
	}

	private void waitForState( State state ) throws InterruptedException {
		waitForState( state, FOREVER );
	}

	private void waitForState( State state, int timeout ) throws InterruptedException {
		synchronized( statelock ) {
			long mark = System.currentTimeMillis();
			while( this.state != state ) {
				if( timeout == FOREVER ) {
					statelock.wait();
				} else {
					statelock.wait( timeout );
					if( System.currentTimeMillis() - mark > timeout ) return;
				}
			}
		}
	}

	private final class ServiceRunner implements Runnable {

		/**
		 * The implementation of the Runnable interface.
		 */
		@Override
		public void run() {
			try {
				while( true ) {
					waitForState( State.STARTING );
					try {
						startup();
					} catch( Exception exception ) {
						Log.write( exception );
					}
					waitForState( State.STOPPING );
					try {
						shutdown();
					} catch( Exception exception ) {
						Log.write( exception );
					}
				}
			} catch( InterruptedException exception ) {
				Log.write( Log.ERROR, exception, "Service runner failure." );
			}
		}

	}

}

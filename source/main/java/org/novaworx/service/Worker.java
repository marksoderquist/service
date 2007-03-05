package org.novaworx.service;

import org.novaworx.util.TripLock;

public abstract class Worker extends Service implements Runnable {

	private boolean execute;

	private boolean daemon;

	private Thread thread;

	private Exception exception;

	private final TripLock startLock = new TripLock();

	protected Worker() {
		this( false );
	}

	protected Worker( boolean daemon ) {
		super();
		this.daemon = daemon;
	}

	protected Worker( String name ) {
		this( name, false );
	}

	protected Worker( String name, boolean daemon ) {
		super( name );
		this.daemon = daemon;
	}

	public synchronized boolean isWorking() {
		return thread != null && thread.isAlive();
	}

	public synchronized boolean isExecutable() {
		return execute;
	}

	public void run() {
		try {
			startWorker();
		} catch( Exception exception ) {
			this.exception = exception;
		} finally {
			startLock.trip();
		}
		process();
	}

	/**
	 * Subclasses should overide this method with code that does any setup work.
	 * 
	 * @throws Exception
	 */
	protected void startWorker() throws Exception {}

	/**
	 * Implement this method to do worker processing. Thread blocking operations
	 * should be called from this method.
	 */
	protected abstract void process();

	/**
	 * Subclasses should override this method with code that terminates or closes
	 * any blocking operations. This method could also be used to stop other
	 * workers.
	 * <p>
	 * For example: If the worker thread is blocked on an InputStream.read()
	 * operation, this method should call the InputStream.close() method.
	 * 
	 * @throws Exception
	 */
	protected void stopWorker() throws Exception {}

	@Override
	protected final synchronized void startService() throws Exception {
		execute = true;
		thread = new Thread( this, getName() );
		thread.setPriority( Thread.NORM_PRIORITY );
		thread.setDaemon( this.daemon );
		thread.start();
		startLock.hold();
		if( exception != null ) throw exception;
	}

	@Override
	protected final synchronized void stopService() throws Exception {
		execute = false;
		stopWorker();
		thread.join();
	}

}

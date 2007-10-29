package org.novaworx.service;

import com.parallelsymmetry.util.TripLock;

public abstract class Worker extends Service implements Runnable {

	private boolean daemon;

	private Exception exception;

	private WorkerRunner runner;

	private final TripLock startlock = new TripLock();

	public Worker() {
		this( null, false );
	}

	public Worker( boolean daemon ) {
		this( null, daemon );
	}

	public Worker( String name ) {
		this( name, false );
	}

	public Worker( String name, boolean daemon ) {
		super( name );
		this.daemon = daemon;
		this.runner = new WorkerRunner();
	}

	public boolean isWorking() {
		return this.runner.isWorking();
	}

	public boolean isExecutable() {
		return this.runner.isExecutable();
	}

	@Override
	protected final void startService() throws Exception {
		runner.start();
		startlock.hold();
		if( exception != null ) throw exception;
	}

	@Override
	protected final void stopService() throws Exception {
		runner.stop();
		if( exception != null ) throw exception;
	}

	/**
	 * Subclasses should override this method with code that does any setup work.
	 * 
	 * @throws Exception
	 */
	protected void startWorker() throws Exception {}

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

	private class WorkerRunner implements Runnable {

		private Thread thread;

		private volatile boolean execute;

		public synchronized boolean isExecutable() {
			return execute;
		}

		public synchronized boolean isWorking() {
			return thread != null && thread.isAlive();
		}

		public void start() {
			execute = true;

			try {
				Worker.this.startWorker();
			} catch( Exception exception ) {
				Worker.this.exception = exception;
				return;
			}

			thread = new Thread( this, getName() );
			thread.setPriority( Thread.NORM_PRIORITY );
			thread.setDaemon( Worker.this.daemon );
			thread.start();
		}

		public void run() {
			startlock.trip();
			Worker.this.run();
		}

		public void stop() {
			this.execute = false;
			try {
				Worker.this.stopWorker();
			} catch( Exception exception ) {
				Worker.this.exception = exception;
				return;
			}

			if( Worker.this.daemon ) return;

			try {
				thread.join();
			} catch( InterruptedException exception ) {
				// Intentionally ignore exception.
			}
		}

	}

}

package org.novaworx.service;

public abstract class Worker extends Service implements Runnable {

	private String name;

	private boolean execute;

	private Thread thread;

	protected Worker() {
		super();
	}

	protected Worker( String name ) {
		super( name );
	}

	public synchronized boolean isExecutable() {
		return execute;
	}

	@Override
	protected synchronized void startService() throws Exception {
		execute = true;
		thread = new Thread( this, name + ":Worker" );
		thread.setPriority( Thread.NORM_PRIORITY );
		thread.setDaemon( false );
		thread.start();
	}

	@Override
	protected synchronized void stopService() throws Exception {
		execute = false;
		if( thread != null ) thread.interrupt();
	}
	
}

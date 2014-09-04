package com.parallelsymmetry.service;

import java.util.Map;

import com.parallelsymmetry.utility.log.Log;

public class JvmSureStop extends Thread {

	/**
	 * The amount of time to give the JVM to exit cleanly. After this amount of
	 * time the JVM is halted by calling Runtime.getRuntime().halt().
	 */
	public static final int JVM_SURE_STOP_DELAY = 5000;

	public JvmSureStop() {
		super( "JVM Sure Stop" );
		setDaemon( true );
	}

	@Override
	public void run() {
		try {
			Thread.sleep( JVM_SURE_STOP_DELAY );
		} catch( InterruptedException exception ) {
			return;
		}

		Log.write( Log.ERROR, "JVM did not exit cleanly. Here are the running non-daemon threads:" );
		Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
		for( Thread thread : threads.keySet() ) {
			if( thread.isDaemon() ) continue;
			Log.write( Log.INFO, "Thread: ", thread.getId(), " ", thread.getName(), "(a:", thread.isAlive(), " d:", thread.isDaemon(), ")" );
			StackTraceElement[] trace = threads.get( thread );
			for( StackTraceElement element : trace ) {
				Log.write( Log.TRACE, "  ", element.toString() );
			}
		}
		Log.write( Log.ERROR, "Halting now!" );
		Runtime.getRuntime().halt( -1 );
	}

}

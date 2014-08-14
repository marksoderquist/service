package com.parallelsymmetry.service;

import com.parallelsymmetry.utility.log.Log;

public class JvmSureStop extends Thread {

	/**
	 * The amount of time to give the JVM to exit cleanly. After this amount of
	 * time the JVM is halted by calling Runtime.getRuntime().halt().
	 */
	public static final int JVM_SURE_STOP_DELAY = 2000;

	public JvmSureStop() {
		setDaemon( true );
	}

	@Override
	public void run() {
		try {
			Thread.sleep( JVM_SURE_STOP_DELAY );
		} catch( InterruptedException exception ) {
			return;
		}
		Log.write( Log.ERROR, "JVM did not exit cleanly. Halting now!" );
		Runtime.getRuntime().halt( -1 );
	}

}

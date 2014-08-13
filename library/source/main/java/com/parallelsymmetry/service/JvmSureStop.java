package com.parallelsymmetry.service;

import com.parallelsymmetry.utility.log.Log;

public class JvmSureStop extends Thread {

	public JvmSureStop() {
		setDaemon( true );
	}

	@Override
	public void run() {
		try {
			Thread.sleep( 5000 );
		} catch( InterruptedException exception ) {
			return;
		}
		Log.write( Log.ERROR, "JVM did not exit cleanly. Halting now!" );
		System.err.println( "JVM did not exit cleanly. Halting now!" );
		Runtime.getRuntime().halt( -1 );
	}

}

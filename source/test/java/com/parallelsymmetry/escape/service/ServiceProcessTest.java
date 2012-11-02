package com.parallelsymmetry.escape.service;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.ArrayUtil;
import com.parallelsymmetry.escape.utility.ConsoleReader;
import com.parallelsymmetry.escape.utility.ThreadUtil;

public class ServiceProcessTest extends TestCase {

	private String[] startCommands = new String[] { "java", "-cp", System.getProperty( "java.class.path" ), "com.parallelsymmetry.escape.service.MockService", "-log.level", "info" };

	private String[] stopCommands = ArrayUtil.combine( startCommands, new String[] { "-stop" } );

	private boolean showStartOutput = false;

	private boolean showStopOutput = false;

	private Process startProcess;

	private Process stopProcess;

	@Override
	public void tearDown() throws Exception {
		try {
			startProcess.destroy();
			startProcess.waitFor();
		} catch( Throwable throwable ) {}
		try {
			stopProcess.destroy();
			stopProcess.waitFor();
		} catch( Throwable throwable ) {}

		assertFalse( isProcessRunning( startProcess ) );
		assertFalse( isProcessRunning( stopProcess ) );
	}

	public void testProcess() throws Exception {
		// Start a mock service process.
		ProcessBuilder builder = new ProcessBuilder( startCommands );
		builder.redirectErrorStream( true );
		startProcess = builder.start();

		if( showStartOutput ) {
			ConsoleReader startMonitor = new ConsoleReader( startProcess );
			startMonitor.start();
		}

		// Give the process two seconds to start.
		ThreadUtil.pause( 2000 );

		// Stop the startProcess with the stopProcess.
		ProcessBuilder stopBuilder = new ProcessBuilder( stopCommands );
		stopBuilder.redirectErrorStream( true );
		stopProcess = stopBuilder.start();

		if( showStopOutput ) {
			ConsoleReader stopMonitor = new ConsoleReader( stopProcess );
			stopMonitor.start();
		}

		// Give the processes two seconds to stop.
		ThreadUtil.pause( 2000 );

		assertFalse( isProcessRunning( stopProcess ) );
		assertFalse( isProcessRunning( startProcess ) );
	}

	protected boolean isProcessRunning( Process process ) {
		try {
			process.exitValue();
			return false;
		} catch( IllegalThreadStateException exception ) {
			return true;
		}
	}

}

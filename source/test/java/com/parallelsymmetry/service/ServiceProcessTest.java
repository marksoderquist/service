package com.parallelsymmetry.service;

import com.parallelsymmetry.utility.ArrayUtil;
import com.parallelsymmetry.utility.ConsoleReader;
import com.parallelsymmetry.utility.ThreadUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ServiceProcessTest extends BaseTestCase {

	private String[] startCommands = new String[]{ "java", "-cp", System.getProperty( "java.class.path" ), "com.parallelsymmetry.service.MockService", "-log.level", "info" };

	private String[] stopCommands = ArrayUtil.combine( startCommands, new String[]{ "-stop" } );

	private boolean showStartOutput = false;

	private boolean showStopOutput = false;

	private Process startProcess;

	private Process stopProcess;

	@AfterEach
	@Override
	public void teardown() throws Exception {
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
		super.teardown();
	}

	@Test
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

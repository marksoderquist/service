package com.parallelsymmetry.escape.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.LineParser;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.log.DefaultHandler;
import com.parallelsymmetry.escape.utility.log.Log;

public class ServiceTest extends TestCase {

	private static final String MOCK_SERVICE_NAME = "Mock Service";

	private static final String MOCK_RELEASE = "1.0.0 Alpha 0  1973-08-14 16:29:00";

	private int timeout = 5000;

	public void setUp() {
		Log.setLevel( Log.NONE );
	}

	public void testCall() throws Exception {
		MockService service = new MockService();
		service.call();
		service.waitForStartup( timeout );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getRelease().toHumanString() );

		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( service.isRunning() );
	}

	public void testCommandLineOutput() throws Exception {
		MockService service = new MockService();
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO ) );

		assertCommandLineHeader( parser );

		List<String> lines = parseCommandLineOutput( parser.getRemaining() );

		int indexA = lines.indexOf( "Mock Service started." );
		int indexB = lines.indexOf( "Mock Service stopped." );

		assertTrue( indexA < indexB );
	}

	public void testHelpCommandLineOutput() throws Exception {
		MockService service = new MockService();
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, "-help" ) );

		assertCommandLineHeader( parser );

		assertEquals( "Usage: java -jar <jar file name> [<option>...]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Commands:", parser.next() );
		assertEquals( "  If no command is specified the program is started.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -help [topic]    Show help information.", parser.next() );
		assertEquals( "  -version         Show version and copyright information only.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -stop            Stop the application and exit the VM.", parser.next() );
		assertEquals( "  -status          Print the application status.", parser.next() );
		assertEquals( "  -restart         Restart the application without exiting VM.", parser.next() );
		assertEquals( "  -watch           Watch an already running application.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Options:", parser.next() );
		assertEquals( "  -log.level <level>   Change the output log level. Levels are:", parser.next() );
		assertEquals( "                       none, error, warn, info, trace, debug, all", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );
	}

	public void testStatusCommandLineOutput() throws Exception {
		MockService service = new MockService();
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, "-status" ) );

		assertCommandLineHeader( parser );

		assertEquals( "Mock Service status: STOPPED", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );
	}

	public void testRequiredJavaVersion() throws Exception {
		String version = System.getProperty( "java.runtime.version" );
		System.setProperty( "java.runtime.version", "1.5" );

		LineParser parser = null;
		MockService service = new MockService();
		try {
			parser = new LineParser( getCommandLineOutput( service, Log.INFO ) );
		} finally {
			System.setProperty( "java.runtime.version", version );
		}
		assertFalse( "Service should not be running and is.", service.isRunning() );

		assertEquals( "Java 1.6 or higher is required, found: 1.5", parser.next() );
	}

	public void testLaunchWithStart() throws Exception {
		//Log.write( "...testLaunchWithStart()..." );
		MockService service = new MockService();
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 0, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 1, service.getStopCalledCount() );
	}

	public void testLaunchWithStop() throws Exception {
		//Log.write( "...testLaunchWithStop()..." );
		MockService service = new MockService();
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 0, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 1, service.getStopCalledCount() );
	}

	public void testLaunchWithRestart() throws Exception {
		//Log.write( "...testLaunchWithRestart()..." );
		MockService service = new MockService();
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 0, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( "-restart" );
		service.waitForStartup( timeout );

		Agent.State state = service.getState();
		assertTrue( "Service should be running and is not: " + state, service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 2, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 1, service.getStopCalledCount() );

		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 2, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 2, service.getStopCalledCount() );
	}

	public void testFastStartStop() throws Exception {
		//Log.write( "...testFastStartStop()..." );
		MockService service = new MockService();
		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );

		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );

		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );

		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );
	}

	public void testFastRestarts() throws Exception {
		//Log.write( "...testFastRestarts()..." );
		MockService service = new MockService();
		service.call( "-start" );
		service.waitForStartup( timeout );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( "-stop" );
		service.waitForShutdown( timeout );
		assertFalse( "Service should not be running and is.", service.isRunning() );
	}

	public void testPassParameters() throws Exception {
		//Log.write( "...testPassParameters()..." );
		MockService service1 = new MockService();
		LineParser parser = new LineParser( getCommandLineOutput( service1, Log.INFO, "-status" ) );

		assertCommandLineHeader( parser );

		assertEquals( "Mock Service status: STOPPED", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );

		assertFalse( "Service should not be running and is.", service1.isRunning() );
		service1.call( "-start", "-log.level", "debug" );
		assertTrue( "Service should be running and is not.", service1.isRunning() );

		MockService service2 = new MockService();
		service2.call( "-stop" );
		service1.waitForShutdown( timeout );
		assertEquals( Service.State.STOPPED, service1.getState() );
		assertFalse( "Service should not be running and is.", service1.isRunning() );
		assertEquals( Service.State.STOPPED, service2.getState() );
		assertFalse( "Service should not be running and is.", service2.isRunning() );
	}

	private List<String> parseCommandLineOutput( String output ) {
		LineParser parser = new LineParser( output );

		String line = null;
		List<String> lines = new ArrayList<String>();
		while( ( line = parser.next() ) != null ) {
			lines.add( line );
		}

		return lines;
	}

	private String getCommandLineOutput( Service service, Level level, String... commands ) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DefaultHandler handler = new DefaultHandler( new PrintStream( buffer ) );
		handler.setLevel( level );
		Log.addHandler( handler );

		try {
			service.call( commands );
			service.waitForStartup( timeout );
			//assertTrue( service.isRunning() );

			if( service.isRunning() ) {
				service.call( new String[] { "-stop" } );
				service.waitForShutdown( timeout );
				assertFalse( service.isRunning() );
			}
		} finally {
			Log.removeHandler( handler );
		}

		return buffer.toString( "UTF-8" );
	}

	private void assertCommandLineHeader( LineParser parser ) {
		int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );

		assertEquals( MOCK_SERVICE_NAME + " " + MOCK_RELEASE, parser.next() );
		assertEquals( "(C) 1973-" + currentYear + " Parallel Symmetry All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY.  This is open software,", parser.next() );
		assertEquals( "and you are welcome to redistribute it under certain conditions.", parser.next() );
		assertEquals( "", parser.next() );
	}

}

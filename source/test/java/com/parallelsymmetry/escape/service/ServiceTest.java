package com.parallelsymmetry.escape.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.LineParser;
import com.parallelsymmetry.escape.utility.log.DefaultHandler;
import com.parallelsymmetry.escape.utility.log.Log;

public class ServiceTest extends TestCase {

	private static final String MOCK_SERVICE_NAME = "Mock Service";

	private static final String MOCK_RELEASE = "1.0.0 Alpha 0  1970-01-01 00:00:00";

	private MockService service = new MockService();

	private int timeout = 1000;

	public void setUp() {
		Log.setLevel( Log.NONE );
	}

	public void testCall() throws Exception {
		service.call( "-log.level", "none" );
		service.waitForStartup( timeout );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getRelease().toHumanString() );

		service.call( "-stop", "-log.level", "none" );
		service.waitForShutdown( timeout );
		assertFalse( service.isRunning() );
	}

	public void testCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( Log.INFO ) );

		assertCommandLineHeader( parser );

		List<String> lines = parseCommandLineOutput( parser.getRemaining() );

		int indexA = lines.indexOf( "Mock Service started." );
		int indexB = lines.indexOf( "Mock Service stopped." );

		assertTrue( indexA < indexB );
	}

	public void testHelpCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( Log.INFO, "-help" ) );

		assertCommandLineHeader( parser );

		assertEquals( "Usage: java -jar <jar file name> [<option>...]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Options:", parser.next() );
		assertEquals( "  -help [topic]    Show help information.", parser.next() );
		assertEquals( "  -version         Show version and copyright information only.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -stop            Stop the application and exit the VM.", parser.next() );
		assertEquals( "  -start           Start the application.", parser.next() );
		assertEquals( "  -status          Print the application status.", parser.next() );
		assertEquals( "  -restart         Restart the application without exiting VM.", parser.next() );
		assertEquals( "  -watch           Watch an already running application.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -log.level <level>   Change the output log level. Levels are:", parser.next() );
		assertEquals( "                       none, error, warn, info, trace, debug, all", parser.next() );

	}

	public void testStartThrowsException() throws Exception {
		service.startAndWait( timeout );
		assertFalse( "Calling start directly should not start the service.", service.isRunning() );
	}

	public void testStopThrowsException() throws Exception {
		service.call( "-log.level", "none" );
		service.waitForStartup( timeout );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getRelease().toHumanString() );

		service.stopAndWait( timeout );
		assertTrue( "Calling stop directly should should not stop the service.", service.isRunning() );

		service.call( "-stop", "-log.level", "none" );
		service.waitForShutdown( timeout );
		assertFalse( service.isRunning() );
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

	private String getCommandLineOutput( Level level, String... commands ) throws Exception {
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
		assertEquals( MOCK_SERVICE_NAME + " " + MOCK_RELEASE, parser.next() );
		assertEquals( "(C) 2010 Parallel Symmetry All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY.  This is open software,", parser.next() );
		assertEquals( "and you are welcome to redistribute it under certain conditions.", parser.next() );
		assertEquals( "", parser.next() );
	}

}

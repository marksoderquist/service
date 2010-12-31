package com.parallelsymmetry.escape.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Handler;

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
		service.call( new String[] { "-log.level", "none" } );
		service.waitForStartup( timeout );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getRelease().toHumanString() );

		service.call( new String[] { "-stop", "-log.level", "none" } );
		service.waitForShutdown( timeout );
		assertFalse( service.isRunning() );
	}

	public void testStartStopCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput() );

		assertCommandLineHeader( parser );

		assertEquals( "Mock Service started.", parser.next() );
		assertMatches( "Connected to peer: 127.0.0.1:[0-9]*", parser.next() );
		assertEquals( "Peer disconnected.", parser.next() );
		assertEquals( "Mock Service stopped.", parser.next() );
	}

	public void testStartThrowsException() throws Exception {
		service.startAndWait( timeout );
		assertFalse( "Calling start directly should not start the service.", service.isRunning() );
	}

	public void testStopThrowsException() throws Exception {
		service.call( new String[] { "-start", "-log.level", "none" } );
		service.waitForStartup( timeout );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getRelease().toHumanString() );

		service.stopAndWait( timeout );
		assertTrue( "Calling stop directly should should not stop the service.", service.isRunning() );

		service.call( new String[] { "-stop", "-log.level", "none" } );
		service.waitForShutdown( timeout );
		assertFalse( service.isRunning() );
	}

	private String getCommandLineOutput( String... commands ) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DefaultHandler handler = new DefaultHandler( new PrintStream( buffer ) );
		Handler defaultHandler = Log.getDefaultHandler();

		if( Log.getLevel() == Log.NONE ) Log.removeHandler( defaultHandler );
		Log.addHandler( handler );

		try {
			service.call( commands );
			service.waitForStartup( timeout );
			assertTrue( service.isRunning() );

			service.call( new String[] { "-stop" } );
			service.waitForShutdown( timeout );
			assertFalse( service.isRunning() );
		} finally {
			Log.removeHandler( handler );
			if( Log.getLevel() == Log.NONE ) Log.addHandler( defaultHandler );
		}

		return buffer.toString( "UTF-8" );
	}

	private void assertMatches( String pattern, String value ) {
		assertTrue( value.matches( pattern ) );
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

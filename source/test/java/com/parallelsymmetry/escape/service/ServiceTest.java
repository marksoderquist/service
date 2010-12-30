package com.parallelsymmetry.escape.service;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.log.Log;

public class ServiceTest extends TestCase {

	private MockService service = new MockService();

	private int timeout = 1000;

	public void setUp() {
		Log.setLevel( Log.NONE );
	}

	public void testCall() throws Exception {
		service.call( new String[] { "-start", "-log.level", "none" } );
		service.waitForStartup( timeout );
		assertTrue( service.isRunning() );

		assertEquals( "1.0.0 Alpha 1  2010-12-30 20:10:22", service.getRelease().toHumanString() );

		service.call( new String[] { "-stop", "-log.level", "none" } );
		service.waitForShutdown( timeout );
		assertFalse( service.isRunning() );
	}

	public void testStartThrowsException() throws Exception {
		service.startAndWait( timeout );
		assertFalse( "Calling start directly should throw an exception.", service.isRunning() );
		//	fail( "Calling start directly should throw an exception." );
		//} catch( RuntimeException exception ) {
		// Intentionally ignore exception.
		//}
	}

	//	public void testStopThrowsException() throws Exception {
	//		service.call( new String[] { "-start" } );
	//		service.waitForStartup( timeout );
	//		assertTrue( service.isRunning() );
	//
	//		try {
	//			service.stopAndWait( timeout );
	//			fail( "Calling start directly should throw an exception." );
	//		} catch( RuntimeException exception ) {
	//			// Intentionally ignore exception.
	//		}
	//
	//		service.call( new String[] { "-stop" } );
	//		service.waitForShutdown( timeout );
	//		assertFalse( service.isRunning() );
	//	}

}

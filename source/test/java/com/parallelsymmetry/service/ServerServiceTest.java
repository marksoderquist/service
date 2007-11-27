package com.parallelsymmetry.service;

import junit.framework.TestCase;

import com.parallelsymmetry.util.Log;

// TODO One of these tests is causing Hudson to hang on occasion.
public class ServerServiceTest extends TestCase {

	//private static final int PORT = 23423;

	@Override
	public void setUp() {
		Log.setLevel( Log.DEBUG );
	}

	@Override
	public void tearDown() {
		Log.setLevel( null );
	}

	public void testStartStop() throws Exception {
		ServerService service = new ServerService();
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );
		int localPort = service.getLocalPort();
		assertTrue( "Server port should be greater than zero: " + localPort, localPort > 0 );
		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );
		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

	//	public void testStartStopWithPort() throws Exception {
	//		ServerService service = new ServerService( PORT );
	//		service.startAndWait();
	//		assertTrue( "Service is not running.", service.isRunning() );
	//		service.stopAndWait();
	//		assertFalse( "Service is not stopped.", service.isRunning() );
	//		service.startAndWait();
	//		assertTrue( "Service is not running.", service.isRunning() );
	//		service.stopAndWait();
	//		assertFalse( "Service is not stopped.", service.isRunning() );
	//	}

	// public void testConnect() throws Exception {
	// SocketService service = new SocketService( server.getLocalPort() );
	// assertFalse( "Service should not be running.", service.isRunning() );
	// service.startAndWait();
	// assertTrue( "Service is not running.", service.isRunning() );
	//
	// service.stopAndWait();
	// assertFalse( "Service is not stopped.", service.isRunning() );
	// }

	//	public void testRestart() throws Exception {
	//		ServerService service = new ServerService( PORT );
	//		assertFalse( "Service should not be running.", service.isRunning() );
	//		service.startAndWait();
	//		assertTrue( "Service is not running.", service.isRunning() );
	//
	//		service.restart();
	//		assertTrue( "Service is not running.", service.isRunning() );
	//
	//		service.stopAndWait();
	//		assertFalse( "Service is not stopped.", service.isRunning() );
	//	}

	//
	// public void testWrite() throws Exception {
	// SocketService service = new SocketService( server.getLocalPort() );
	// assertFalse( "Service should not be running.", service.isRunning() );
	// service.startAndWait();
	// assertTrue( "Service is not running.", service.isRunning() );
	//
	// String message = "Test message.";
	// service.getOutputStream().write( message.getBytes( Charset.forName(
	// "US-ASCII" ) ) );
	// assertEquals( "Incorrect message.", message, server.getMessage(
	// message.length() ) );
	//
	// service.stopAndWait();
	// assertFalse( "Service is not stopped.", service.isRunning() );
	// }

}

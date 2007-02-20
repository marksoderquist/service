package org.novaworx.service;

import junit.framework.TestCase;

public class ServerServiceTest extends TestCase {

	private static final int PORT = 23423;

	@Override
	public void setUp() throws Exception {}

	public void testStartStop() throws Exception {
		ServerService service = new ServerService( PORT );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );
		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );
		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

	// public void testConnect() throws Exception {
	// SocketService service = new SocketService( server.getLocalPort() );
	// assertFalse( "Service should not be running.", service.isRunning() );
	// service.startAndWait();
	// assertTrue( "Service is not running.", service.isRunning() );
	//
	// service.stopAndWait();
	// assertFalse( "Service is not stopped.", service.isRunning() );
	// }
	//
	public void testRestart() throws Exception {
		ServerService service = new ServerService( PORT );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		service.restart();
		assertTrue( "Service is not running.", service.isRunning() );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

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

	@Override
	public void tearDown() throws Exception {}

}

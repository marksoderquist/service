package com.parallelsymmetry.service;

import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import com.parallelsymmetry.util.Log;

public class ServerServiceTest extends TestCase {

	private static final int PORT = 23423;

	@Override
	public void setUp() {
		Log.setLevel( Log.NONE );
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

	public void testStartStopWithPort() throws Exception {
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

	public void testConnect() throws Exception {
		ServerService server = new ServerService( PORT );
		server.startAndWait();

		SocketService service = new SocketService( server.getLocalPort() );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );

		server.stopAndWait();
	}

	public void testRestart() throws Exception {
		ServerService service = new ServerService( PORT );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		service.restart();
		assertTrue( "Service is not running.", service.isRunning() );

		service.restart();
		assertTrue( "Service is not running.", service.isRunning() );

		service.restart();
		assertTrue( "Service is not running.", service.isRunning() );

		service.restart();
		assertTrue( "Service is not running.", service.isRunning() );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

	public void testWrite() throws Exception {
		MockServer server = new MockServer( PORT );
		server.startAndWait();

		SocketService service = new SocketService( server.getLocalPort() );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		String message = "Test message.";
		service.getOutputStream().write( message.getBytes( Charset.forName( "US-ASCII" ) ) );
		assertEquals( "Incorrect message.", message, server.getMessage( message.length() ) );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );

		server.stopAndWait();
	}

	private static final class MockServer extends ServerService {

		public MockServer() {
			super();
		}

		public MockServer( int port ) {
			super( port );
		}

		public MockServer( String host, int port ) {
			super( host, port );
		}

		public MockServer( String name, String host, int port ) {
			super( name, host, port );
		}

		public MockServer( String name ) {
			super( name );
		}

		public String getMessage( int length ) {
			StringBuilder builder = new StringBuilder();

			for( int index = 0; index < length; index++ ) {
				try {
					int data = getInputStream().read();
					if( data < 0 ) return builder.toString();
					builder.append( (char)data );
				} catch( IOException exception ) {
					Log.write( exception );
				}
			}

			return builder.toString();
		}

	}

}

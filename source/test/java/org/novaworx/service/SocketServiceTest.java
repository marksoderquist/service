package org.novaworx.service;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

import org.novaworx.util.Log;

import junit.framework.TestCase;

public class SocketServiceTest extends TestCase {

	private TestServerSocketService server;

	@Override
	public void setUp() throws Exception {
		Log.setLevel( Log.NONE );
		server = new TestServerSocketService();
		server.startAndWait();
		assertTrue( server.isRunning() );
		int localPort = server.getLocalPort();
		assertTrue( "Server port should be greater than zero: " + localPort, localPort > 0 );
	}

	public void testStartStop() throws Exception {
		SocketService service = new SocketService( server.getLocalPort() );
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
		SocketService service = new SocketService( server.getLocalPort() );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

	public void testRestart() throws Exception {
		SocketService service = new SocketService( server.getLocalPort() );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		service.restart();
		assertTrue( "Service is not running.", service.isRunning() );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

	public void testWrite() throws Exception {
		SocketService service = new SocketService( server.getLocalPort() );
		assertFalse( "Service should not be running.", service.isRunning() );
		service.startAndWait();
		assertTrue( "Service is not running.", service.isRunning() );

		String message = "Test message.";
		service.getOutputStream().write( message.getBytes( Charset.forName( "US-ASCII" ) ) );
		assertEquals( "Incorrect message.", message, server.getMessage( message.length() ) );

		service.stopAndWait();
		assertFalse( "Service is not stopped.", service.isRunning() );
	}

	@Override
	public void tearDown() throws Exception {
		server.stopAndWait();
		Log.setLevel( null );
	}

	private static class TestServerSocketService extends ServerService {

		private final StringBuilder builder = new StringBuilder();

		@Override
		public void handleSocket( Socket socket ) throws IOException {
			TestClient client = new TestClient( this, socket );
			client.start();
			super.handleSocket( socket );
		}

		public void append( int read ) {
			synchronized( builder ) {
				builder.append( (char)read );
				builder.notifyAll();
			}
		}

		public String getMessage( int count ) {
			while( builder.length() < count ) {
				synchronized( builder ) {
					try {
						builder.wait();
					} catch( InterruptedException exception ) {
						//
					}
				}
			}
			String message = null;
			synchronized( builder ) {
				message = builder.toString();
				builder.delete( 0, builder.length() );
			}
			return message;
		}

	}

	private static class TestClient implements Runnable {

		private Thread thread;

		private boolean execute;

		private TestServerSocketService server;

		private Socket socket;

		private Exception exception;

		public TestClient( TestServerSocketService server, Socket socket ) {
			this.server = server;
			this.socket = socket;
		}

		public void start() {
			execute = true;
			thread = new Thread( this, "TestClient" );
			thread.setPriority( Thread.NORM_PRIORITY );
			thread.setDaemon( true );
			thread.start();
		}

		public void stop() {
			execute = false;
			try {
				socket.close();
			} catch( IOException exception ) {
				exception.printStackTrace();
			}
		}

		public Exception getException() {
			return exception;
		}

		@Override
		public void run() {
			while( execute ) {
				try {
					int read = socket.getInputStream().read();
					if( read < 0 ) return;
					server.append( read );
					Thread.yield();
				} catch( IOException exception ) {
					this.exception = exception;
				}
			}
		}

	}

}

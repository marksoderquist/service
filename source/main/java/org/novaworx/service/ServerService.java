package org.novaworx.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.novaworx.util.Log;
import org.novaworx.util.TripLock;

public class ServerService extends IOService {

	private int port;

	private ServerSocket server;

	private ServerRunner runner;

	private TripLock connectlock = new TripLock();

	public ServerService() {
		this( null, 0 );
	}

	public ServerService( int port ) {
		this( null, 0 );
	}

	public ServerService( String name ) {
		this( name, 0 );
	}

	public ServerService( String name, int port ) {
		super( name );
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public int getLocalPort() {
		if( server == null ) return 0;
		return server.getLocalPort();
	}

	@Override
	protected final void startService() throws Exception {
		server = new ServerSocket( port );
		server.setReuseAddress( true );

		startServer();
		
		runner = new ServerRunner();
		runner.start();
	}

	@Override
	protected final void stopService() throws Exception {
		if( runner != null ) runner.stopAndWait();
		stopServer();
	}

	protected void startServer() throws Exception {}

	protected void stopServer() throws Exception {}

	protected void handleSocket( Socket socket ) throws IOException {
		Log.write( "Client connected: " + socket.getRemoteSocketAddress() );
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );

		connectlock.hold();

		socket.close();
		setRealInputStream( null );
		setRealOutputStream( null );
	}

	private class ServerRunner implements Runnable {

		private Thread thread;

		private boolean execute;

		public void start() {
			execute = true;
			thread = new Thread( this, getName() );
			thread.setPriority( Thread.NORM_PRIORITY );
			thread.setDaemon( false );
			thread.start();
		}

		public void stop() {
			this.execute = false;
			connectlock.trip();
			try {
				server.close();
			} catch( IOException exception ) {
				Log.write( exception );
			}
		}

		public void stopAndWait() {
			stop();
			try {
				thread.join();
			} catch( InterruptedException exception ) {
				// Intentionally ignore exception.
			}
		}

		public void run() {
			Socket socket = null;
			while( execute ) {
				try {
					connectlock.reset();
					socket = server.accept();
					handleSocket( socket );
				} catch( SocketException exception ) {
					if( !"socket closed".equals( exception.getMessage().toLowerCase() ) ) {
						Log.write( exception );
					}
				} catch( IOException exception ) {
					Log.write( exception );
				}
			}
		}
	}

}

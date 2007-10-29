package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.parallelsymmetry.util.Log;
import com.parallelsymmetry.util.TripLock;

public class ServerService extends IOService {

	private int port;

	private ServerSocket server;

	private ServerRunner runner;

	private TripLock startlock = new TripLock();

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
	protected final void connect() throws Exception {
		Log.write( Log.DEBUG, getName() + ": Connecting..." );
		server = new ServerSocket( port );
		server.setReuseAddress( true );
		runner = new ServerRunner();
		runner.start();
		startlock.resetAndHold();
		startServer();
		Log.write( Log.DEBUG, getName() + ": Connected." );
	}

	@Override
	protected final void disconnect() throws Exception {
		Log.write( Log.DEBUG, getName() + ": Disconnecting..." );
		stopServer();
		if( runner != null ) runner.stopAndWait();
		Log.write( Log.DEBUG, getName() + ": Disconnected." );
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
				if( server != null ) server.close();
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
					startlock.trip();
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

package com.parallelsymmetry.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.parallelsymmetry.util.Log;
import com.parallelsymmetry.util.TripLock;

public class ServerService extends IOService {

	private String host;

	private int port;

	private ServerSocket server;

	private ServerRunner runner;

	private TripLock startlock = new TripLock();

	private TripLock connectlock = new TripLock();

	public ServerService() {
		this( null, 0 );
	}

	public ServerService( int port ) {
		this( null, port );
	}

	public ServerService( String name ) {
		this( name, 0 );
	}

	public ServerService( String name, int port ) {
		this( name, null, port );
	}

	public ServerService( String name, String host ) {
		this( name, host, 0 );
	}

	public ServerService( String name, String host, int port ) {
		super( name );
		this.host = host;
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
		InetSocketAddress address = host == null ? new InetSocketAddress( port ) : new InetSocketAddress( host, port );
		Log.write( Log.DEBUG, getName() + ": Opening " + address + "..." );
		server = new ServerSocket();
		server.setReuseAddress( true );
		Log.write( Log.DEBUG, getName() + ": Binding " + address + "..." );
		server.bind( address );
		Log.write( Log.DEBUG, getName() + ": Starting on " + address + "..." );
		runner = new ServerRunner();
		startlock.reset();
		runner.start();
		startlock.hold();
		startServer();
		Log.write( Log.TRACE, getName() + ": Started on " + server.getLocalSocketAddress() + "." );
	}

	@Override
	protected final void disconnect() throws Exception {
		Log.write( Log.DEBUG, getName() + ": Disconnecting..." );
		stopServer();
		if( runner != null ) runner.stopAndWait();
		Log.write( Log.TRACE, getName() + ": Disconnected." );
	}

	protected void startServer() throws Exception {}

	protected void stopServer() throws Exception {}

	protected void handleSocket( Socket socket ) throws IOException {
		Log.write( "Client connected: " + socket.getInetAddress() + ": " + socket.getPort() );
		setRealInputStream( new BufferedInputStream( socket.getInputStream() ) );
		setRealOutputStream( new BufferedOutputStream( socket.getOutputStream() ) );

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
			//SocketChannel channel = null;
			Socket socket = null;
			while( execute ) {
				try {
					connectlock.reset();
					startlock.trip();
					socket = server.accept();
					handleSocket( socket );
				} catch( SocketException exception ) {
					String message = exception.getMessage();
					if( message == null || !"socket closed".equals( message.toLowerCase() ) ) Log.write( exception );
				} catch( IOException exception ) {
					Log.write( exception );
				}
			}
		}
	}

}

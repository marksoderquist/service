package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.parallelsymmetry.util.Log;
import com.parallelsymmetry.util.TripLock;

public class ServerService extends IOService {

	private String host;

	private int port;

	private ServerSocketChannel server;

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
		return server.socket().getLocalPort();
	}

	@Override
	protected final void connect() throws Exception {
		InetSocketAddress address = host == null ? new InetSocketAddress( port ) : new InetSocketAddress( host, port );
		server = ServerSocketChannel.open();
		server.socket().setReuseAddress( true );
		server.socket().bind( address );
		Log.write( Log.DEBUG, getName() + ": Starting on " + server.socket().getLocalSocketAddress() + "..." );
		runner = new ServerRunner();
		startlock.reset();
		runner.start();
		startlock.hold();
		startServer();
		Log.write( Log.DEBUG, getName() + ": Started on " + server.socket().getLocalSocketAddress() + "." );
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
		Log.write( "Client connected: " + socket.getInetAddress() + ": " + socket.getPort() );
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
			SocketChannel channel = null;
			Socket socket = null;
			while( execute ) {
				try {
					connectlock.reset();
					startlock.trip();
					channel = server.accept();
					socket = channel.socket();
					handleSocket( socket );
				} catch( AsynchronousCloseException exception ) {
					// Intentionally ignore exception.
				} catch( IOException exception ) {
					Log.write( exception );
				}
			}
		}
	}

}

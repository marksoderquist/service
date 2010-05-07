package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.Socket;

import com.parallelsymmetry.log.Log;
import com.parallelsymmetry.util.IOPump;

public class ServerSocketConnection implements Connection, ServerListener {

	private String name;

	private ServerService server;

	private SocketService socket;

	private Plug plugA;

	private Plug plugB;

	private IOPump a2bPump;

	private IOPump b2aPump;

	private boolean forward;

	public ServerSocketConnection( String name, ServerService server, SocketService socket ) {
		if( server == null ) throw new NullPointerException( "Server service cannot be null." );
		if( socket == null ) throw new NullPointerException( "Socket service cannot be null." );

		this.name = name;
		this.server = server;
		this.socket = socket;
		this.plugB = socket;
		this.forward = true;

		initialize();
	}

	public ServerSocketConnection( String name, SocketService socket, ServerService server ) {
		if( socket == null ) throw new NullPointerException( "Socket service cannot be null." );
		if( server == null ) throw new NullPointerException( "Server service cannot be null." );

		this.name = name;
		this.socket = socket;
		this.server = server;
		this.plugA = socket;
		this.forward = false;

		initialize();
	}

	private void initialize() {
		server.setServerListener( this );
		socket.setConnectOnce( true );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void connect() throws Exception {
		server.startAndWait();
	}

	@Override
	public void waitFor() throws InterruptedException {
		a2bPump.waitFor();
		b2aPump.waitFor();
	}

	@Override
	public void disconnect() throws Exception {
		stopPumps();
		socket.stopAndWait();
		server.stopAndWait();
	}

	@Override
	public void handleSocket( Socket socket ) throws IOException {
		if( forward ) {
			plugA = new PlugAdapter( socket.getInputStream(), socket.getOutputStream() );
		} else {
			plugB = new PlugAdapter( socket.getInputStream(), socket.getOutputStream() );
		}

		try {
			this.socket.startAndWait();
		} catch( InterruptedException exception ) {
			return;
		}

		try {
			startPumps();
			if( forward ) {
				a2bPump.waitFor();
			} else {
				b2aPump.waitFor();
			}
		} catch( InterruptedException exception ) {
			// Intentionally ignore exception.
		}

		socket.close();
		stopPumps();

		try {
			this.socket.stopAndWait();
		} catch( Exception exception ) {
			// Intentionally ignore exception.
		}

		Log.write( Log.ERROR, "ServerSocketConnection.handleSocket() complete." );
	}

	private void startPumps() throws InterruptedException {
		// Create the IO pumps.
		a2bPump = new IOPump( getName() + " dn", plugA.getInputStream(), plugB.getOutputStream() );
		b2aPump = new IOPump( getName() + " up", plugB.getInputStream(), plugA.getOutputStream() );

		// Set the interrupt on stop flag.
		a2bPump.setInterruptOnStop( true );
		b2aPump.setInterruptOnStop( true );

		// Set the log enabled flag.
		a2bPump.setLogEnabled( true );
		b2aPump.setLogEnabled( true );

		// Set the log content flag.
		a2bPump.setLogContent( true );
		b2aPump.setLogContent( true );

		// Start the stream pumps.
		a2bPump.startAndWait();
		b2aPump.startAndWait();

		Log.write( getName() + " IO pumps started." );
	}

	private void stopPumps() {
		if( b2aPump != null ) b2aPump.stop();
		if( a2bPump != null ) a2bPump.stop();
		Log.write( getName() + " IO pumps stopped." );
	}

}

package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.Socket;

import com.parallelsymmetry.log.Log;
import com.parallelsymmetry.util.IOPump;

public class Connection implements ServerListener {

	private String name;

	private IOService serviceA;

	private IOService serviceB;

	private Plug plugA;

	private Plug plugB;

	private IOPump a2bPump;

	private IOPump b2aPump;

	int server;

	public Connection( String name, IOService serviceA, IOService serviceB ) {
		if( serviceA == null || serviceB == null ) throw new NullPointerException( "Plug cannot be null." );
		this.name = name;
		this.serviceA = serviceA;
		this.serviceB = serviceB;

		if( serviceA instanceof ServerService ) {
			( (ServerService)serviceA ).setServerListener( this );
			this.server = 1;
		} else {
			this.plugA = serviceA;
		}

		if( serviceB instanceof ServerService ) {
			( (ServerService)serviceB ).setServerListener( this );
			this.server = 2;
		} else {
			this.plugB = serviceB;
		}
	}

	public String getName() {
		return name;
	}

	public void connect() throws Exception {
		if( server == 1 && serviceB instanceof SocketService ) {
			// Don't start a socket service connected to a server service.
		} else {
			serviceB.startAndWait();
		}

		if( server == 2 && serviceA instanceof SocketService ) {
			// Don't start a socket service connected to a server service.
		} else {
			serviceA.startAndWait();
		}

		if( server == 0 ) startPumps();
	}

	public void waitFor() throws InterruptedException {
		a2bPump.waitFor();
		b2aPump.waitFor();
	}

	public void disconnect() throws Exception {
		stopPumps();

		serviceB.stopAndWait();
		serviceA.stopAndWait();
	}

	@Override
	public void handleSocket( Socket socket ) throws IOException {
		try {
			switch( server ) {
				case 1: {
					plugA = new PlugAdapter( socket.getInputStream(), socket.getOutputStream() );
					serviceB.startAndWait();
					break;
				}
				case 2: {
					plugB = new PlugAdapter( socket.getInputStream(), socket.getOutputStream() );
					serviceB.startAndWait();
					break;
				}
				default: {
					return;
				}
			}
		} catch( InterruptedException exception ) {
			return;
		}

		try {
			startPumps();
		} catch( InterruptedException exception ) {
			// Intentionally ignore exception.
		}
	}

	private void startPumps() throws InterruptedException {
		// Create the IO pumps.
		a2bPump = new IOPump( getName() + " dn", plugA.getInputStream(), plugB.getOutputStream() );
		b2aPump = new IOPump( getName() + " up", plugB.getInputStream(), plugA.getOutputStream() );

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
	}

}

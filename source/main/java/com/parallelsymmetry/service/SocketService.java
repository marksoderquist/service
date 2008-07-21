package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.Socket;

import com.parallelsymmetry.util.Log;

public class SocketService extends IOService {

	private String host;

	private int port;

	private Socket socket;

	public SocketService( int port ) {
		this( null, null, port );
	}

	public SocketService( String name, int port ) {
		this( name, null, port );
	}

	public SocketService( String name, String host, int port ) {
		super( name );
		this.host = host;
		this.port = port;
	}

	@Override
	protected void connect() throws IOException {
		Log.write( Log.DEBUG, getName() + ": Connecting..." );
		socket = new Socket( host, port );
		Log.write( "Connected to: " + socket.getInetAddress() + ":" + socket.getPort() );
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );
		Log.write( Log.DEBUG, getName() + ": Connected." );
	}

	@Override
	protected void disconnect() throws IOException {
		Log.write( Log.DEBUG, getName() + ": Disconnecting..." );
		if( socket != null ) socket.close();
		setRealInputStream( null );
		setRealOutputStream( null );
		Log.write( Log.DEBUG, getName() + ": Disconnected." );
	}

}

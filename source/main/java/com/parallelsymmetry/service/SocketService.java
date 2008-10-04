package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.parallelsymmetry.util.Log;

public class SocketService extends IOService {

	/**
	 * The connection timeout in seconds.
	 */
	public static final int TIMEOUT = 5;

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
		String server = host == null ? InetAddress.getLocalHost().getHostName() : host;
		socket = new Socket();
		socket.connect( new InetSocketAddress( server, port ), TIMEOUT * 1000 );
		Log.write( "Connected to: " + socket.getInetAddress() + ":" + socket.getPort() );
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );
		Log.write( Log.TRACE, getName() + ": Connected." );
	}

	@Override
	protected void disconnect() throws IOException {
		Log.write( Log.DEBUG, getName() + ": Disconnecting..." );
		if( socket != null ) socket.close();
		setRealInputStream( null );
		setRealOutputStream( null );
		Log.write( Log.TRACE, getName() + ": Disconnected." );
	}

}

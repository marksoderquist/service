package com.parallelsymmetry.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.parallelsymmetry.log.Log;

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
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );
		Log.write( getName() + ": Connected to: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() );
	}

	@Override
	protected void disconnect() throws IOException {
		Log.write( Log.DEBUG, getName() + ": Disconnecting..." );
		if( socket != null ) {
			Log.write( Log.DEBUG, getName() + ": Closing socket..." );
			socket.close();
			Log.write( Log.DEBUG, getName() + ": Socket closed." );
		}
		setRealInputStream( null );
		setRealOutputStream( null );
		Log.write( getName() + ": Disconnected." );
	}

}

package org.novaworx.service;

import java.io.IOException;
import java.net.Socket;

import org.novaworx.util.Log;

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
	protected boolean isConnected() {
		return socket != null && socket.isConnected();
	}

	@Override
	protected void connect() throws IOException {
		socket = new Socket( host, port );
		Log.write( "Connected to: " + host + ":" + port );
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );
	}

	@Override
	protected void disconnect() throws IOException {
		if( socket != null ) socket.close();
		setRealInputStream( null );
		setRealOutputStream( null );
	}
}

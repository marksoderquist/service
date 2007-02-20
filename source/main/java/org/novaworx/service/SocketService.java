package org.novaworx.service;

import java.io.IOException;
import java.net.Socket;

import org.novaworx.util.Log;

public class SocketService extends IOService {

	private String host;

	private int port;

	private Socket socket;

	public SocketService( int port ) {
		this( null, port );
	}

	public SocketService( String host, int port ) {
		this.host = host;
		this.port = port;
	}

	@Override
	protected void startService() throws IOException {
		socket = new Socket( host, port );
		Log.write( "Connected to: " + host + ":" + port );
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );
	}

	@Override
	protected void stopService() throws IOException {
		if( socket != null ) socket.close();
		setRealInputStream( null );
		setRealOutputStream( null );
	}

}

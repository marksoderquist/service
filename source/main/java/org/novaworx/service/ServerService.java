package org.novaworx.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.novaworx.util.Log;

public class ServerService extends IOService {

	private int port;

	private ServerSocket server;

	private ServerRunner runner;

	public ServerService() {
		this( 0 );
	}

	public ServerService( int port ) {
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
	protected final void startService() throws IOException {
		server = new ServerSocket( port );
		server.setReuseAddress( true );

		runner = new ServerRunner();
		runner.start();
	}

	@Override
	protected final void stopService() throws IOException {
		if( runner != null ) runner.stopAndWait();
	}

	protected void handleSocket( Socket socket ) throws IOException {
		Log.write( "Client connected: " + socket.getRemoteSocketAddress() );
		setRealInputStream( socket.getInputStream() );
		setRealOutputStream( socket.getOutputStream() );

		synchronized( socket ) {
			try {
				socket.wait();
			} catch( InterruptedException exception ) {
				// Intentionally ignore exception.
			}
		}

		socket.close();
		setRealInputStream( null );
		setRealOutputStream( null );
	}

	private class ServerRunner implements Runnable {

		private Thread thread;

		private boolean execute;

		public void start() {
			execute = true;
			thread = new Thread( this, getName() + ":ServerRunner" );
			thread.setPriority( Thread.NORM_PRIORITY );
			thread.setDaemon( true );
			thread.start();
		}

		public void stop() {
			this.execute = false;
			thread.interrupt();
			try {
				server.close();
			} catch( IOException exception ) {
				Log.write( exception );
			}
		}

		public synchronized void stopAndWait() {
			stop();
			try {
				thread.join();
			} catch( InterruptedException exception ) {
				// Intentionally ignore exception.
			}
		}

		public void run() {
			while( execute ) {
				try {
					Socket socket = server.accept();
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

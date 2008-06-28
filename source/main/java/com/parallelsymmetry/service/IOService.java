package com.parallelsymmetry.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.parallelsymmetry.util.Log;

public abstract class IOService extends Service {

	private static final int RECONNECT_WAIT = 1000;

	private InputStream input = new ServiceInputStream();

	private OutputStream output = new ServiceOutputStream();

	private InputStream realInput;

	private OutputStream realOutput;

	private boolean stopOnException;

	private boolean connected;

	public IOService() {
		this( null, false );
	}

	public IOService( boolean stopOnException ) {
		this( null, stopOnException );
	}

	public IOService( String name ) {
		this( name, false );
	}

	public IOService( String name, boolean stopOnException ) {
		super( name );
		this.stopOnException = stopOnException;
	}

	public InputStream getInputStream() {
		return input;
	}

	public OutputStream getOutputStream() {
		return output;
	}

	protected void setRealInputStream( InputStream input ) {
		synchronized( this.input ) {
			realInput = input;
			this.input.notifyAll();
		}
	}

	protected void setRealOutputStream( OutputStream output ) {
		synchronized( this.output ) {
			realOutput = output;
			this.output.notifyAll();
		}
	}

	@Override
	protected void startService() throws Exception {
		reconnect( 3 );
	}

	@Override
	protected void stopService() throws Exception {
		internalDisconnect();
	}

	protected void reconnect() {
		reconnect( 0 );
	}

	protected void reconnect( int attempts ) {
		int attempt = 0;
		while( shouldExecute() && ( attempts > 0 && attempt < attempts ) ) {
			attempt++;
			try {
				if( connected ) internalDisconnect();
				internalConnect();
				break;
			} catch( Exception exception ) {
				Log.write( "Failed to connect! Waiting " + ( RECONNECT_WAIT / 1000.0 ) + " seconds..." );
				Log.write( exception );
				try {
					Thread.sleep( RECONNECT_WAIT );
				} catch( InterruptedException sleepException ) {
					// Intentionally ignore exception.
				}
			}
		}
	}

	public final boolean isConnected() {
		return connected;
	}

	protected abstract void connect() throws Exception;

	protected abstract void disconnect() throws Exception;

	private final void internalConnect() throws Exception {
		fireEvent( State.CONNECTING );
		connect();
		fireEvent( State.CONNECTED );
		connected = true;
	}

	private final void internalDisconnect() throws Exception {
		fireEvent( State.DISCONNECTING );
		connected = false;
		fireEvent( State.DISCONNECTED );
		disconnect();
	}

	private class ServiceInputStream extends InputStream {

		private void checkForReadablility() {
			synchronized( input ) {
				while( realInput == null ) {
					try {
						input.wait();
					} catch( InterruptedException exception ) {
						// Intentionally ignore exception.
					}
				}
			}
		}

		@Override
		public int read() throws IOException {
			checkForReadablility();
			try {
				int bite = realInput.read();
				if( bite < 0 ) {
					if( !isRunning() ) return -1;
					reconnect();
					return read();
				}
				return bite;
			} catch( IOException exception ) {
				if( !isRunning() ) return -1;
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
				return read();
			}
		}

		@Override
		public int read( byte[] data ) throws IOException {
			checkForReadablility();
			try {
				int read = realInput.read( data );
				if( read < 0 ) {
					if( !isRunning() ) return -1;
					reconnect();
					return read( data );
				}
				return read;
			} catch( IOException exception ) {
				if( !isRunning() ) return -1;
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
				return read( data );
			}
		}

		@Override
		public int read( byte[] data, int offset, int length ) throws IOException {
			checkForReadablility();
			try {
				int read = realInput.read( data, offset, length );
				if( read < 0 ) {
					if( !isRunning() ) return -1;
					reconnect();
					return read( data, offset, length );
				}
				return read;
			} catch( IOException exception ) {
				if( !isRunning() ) return -1;
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
				return read( data, offset, length );
			}
		}

	}

	private class ServiceOutputStream extends OutputStream {

		private void checkForWritablility() {
			synchronized( output ) {
				while( realOutput == null ) {
					try {
						output.wait();
					} catch( InterruptedException exception ) {
						// Intentionally ignore exception.
					}
				}
			}
		}

		@Override
		public void write( int bite ) throws IOException {
			checkForWritablility();
			try {
				realOutput.write( bite );
			} catch( IOException exception ) {
				if( !isRunning() ) return;
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
				write( bite );
			}
		}

		@Override
		public void write( byte[] data ) throws IOException {
			checkForWritablility();
			try {
				realOutput.write( data );
			} catch( IOException exception ) {
				if( !isRunning() ) return;
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
				write( data );
			}
		}

		@Override
		public void write( byte[] data, int offset, int length ) throws IOException {
			checkForWritablility();
			try {
				realOutput.write( data, offset, length );
			} catch( IOException exception ) {
				if( !isRunning() ) return;
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
				write( data, offset, length );
			}
		}

		@Override
		public void flush() throws IOException {
			checkForWritablility();
			try {
				realOutput.flush();
			} catch( IOException exception ) {
				if( stopOnException ) {
					stop();
					throw exception;
				}
				reconnect();
			}
		}

	}

}

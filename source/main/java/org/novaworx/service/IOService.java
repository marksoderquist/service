package org.novaworx.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.novaworx.util.Log;

public abstract class IOService extends Service {

	private static final int RECONNECT_WAIT = 5000;

	private InputStream input = new ServiceInputStream();

	private OutputStream output = new ServiceOutputStream();

	private InputStream realInput;

	private OutputStream realOutput;

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

	protected void reconnect() {
		if( !isRunning() ) return;

		try {
			restart();
		} catch( Exception exception ) {
			Log.write( exception );
			try {
				Thread.sleep( RECONNECT_WAIT );
			} catch( InterruptedException sleepException ) {
				// Intentionally ignore exception.
			}
		}
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
					return read(data);
				}
				return read;
			} catch( IOException exception ) {
				if( !isRunning() ) return -1;
				reconnect();
				return read(data);
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
				reconnect();
			}
		}

	}

}

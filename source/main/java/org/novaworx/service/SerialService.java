package org.novaworx.service;

import java.io.IOException;

import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.novaworx.util.Log;
import org.novaworx.util.ThreadUtil;

/**
 * Install the comm.jar into the maven repository with the following command:
 * 
 * <pre>
 *        mvn install:install-file -Dfile=comm.jar -DgroupId=javax.comm -DartifactId=comm -Dversion=3.0 -Dpackaging=jar -DgeneratePom=true
 * </pre>
 * 
 * @author mvsoder
 */
public class SerialService extends IOService {

	private static final int CONNECT_TIMEOUT = 100;

	private static final int RETRY_COUNT = 5;

	private String name;

	private SerialPort port;

	private SerialSettings[] settings;

	public SerialService( String name, String port, int baud, int bits, int parity, int stop ) {
		this( name, port, new SerialSettings( baud, bits, parity, stop ) );
	}

	public SerialService( String name, String port, SerialSettings... settings ) {
		super( name );
		this.name = port;
		this.settings = settings;
	}

	public SerialPort getSerialPort() {
		return port;
	}

	@Override
	protected void connect() throws IOException {
		CommPortIdentifier identifier;
		try {
			Log.write( Log.DEBUG, "Opening serial port[" + name + ", " + settings[ 0 ].toString() + "]..." );
			identifier = CommPortIdentifier.getPortIdentifier( name );
			port = (SerialPort)identifier.open( getName(), CONNECT_TIMEOUT );

			int attempt = 0;
			while( attempt < RETRY_COUNT ) {
				try {
					port.setSerialPortParams( settings[ 0 ].getBaud(), settings[ 0 ].getBits(), settings[ 0 ].getStop(), settings[ 0 ].getParity() );
					break;
				} catch( UnsupportedCommOperationException exception ) {
					if( attempt < RETRY_COUNT ) {
						ThreadUtil.pause( 10 );
					} else {
						throw new IOException( exception );
					}
				} finally {
					attempt++;
				}
			}

			setRealInputStream( port.getInputStream() );
			setRealOutputStream( port.getOutputStream() );
			Log.write( "Serial port open." );
		} catch( NoSuchPortException exception ) {
			throw new IOException( exception );
		} catch( PortInUseException exception ) {
			throw new IOException( exception );
		}
	}

	@Override
	protected void disconnect() throws IOException {
		if( port != null ) port.close();
		port = null;
	}

}

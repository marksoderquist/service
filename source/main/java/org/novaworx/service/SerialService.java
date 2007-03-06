package org.novaworx.service;

import java.io.IOException;

import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.novaworx.util.Log;

/**
 * Install the comm.jar into the maven repository with the following command:
 * 
 * <pre>
 *  mvn install:install-file -Dfile=comm.jar -DgroupId=javax.comm -DartifactId=comm -Dversion=3.0 -Dpackaging=jar -DgeneratePom=true
 * </pre>
 * 
 * @author mvsoder
 */
public class SerialService extends IOService {

	private static final int CONNECT_TIMEOUT = 1000;

	private String name;

	private SerialPort port;

	private int baud;

	private int bits;

	private int stop;

	private int parity;

	public SerialService( String name, String port, int baud, int bits, int stop, int parity ) {
		super( name );
		this.name = port;
		this.baud = baud;
		this.bits = bits;
		this.stop = stop;
		this.parity = parity;
	}

	public SerialPort getSerialPort() {
		return port;
	}

	@Override
	protected void startService() throws IOException {
		CommPortIdentifier identifier;
		try {
			Log.write( Log.DEBUG, "Opening serial port..." );
			identifier = CommPortIdentifier.getPortIdentifier( name );
			port = (SerialPort)identifier.open( "Perform MiniPC", CONNECT_TIMEOUT );
			port.setSerialPortParams( baud, bits, stop, parity );
			setRealInputStream( port.getInputStream() );
			setRealOutputStream( port.getOutputStream() );
			Log.write( "Serial port open." );
		} catch( NoSuchPortException exception ) {
			throw new IOException( exception );
		} catch( PortInUseException exception ) {
			throw new IOException( exception );
		} catch( UnsupportedCommOperationException exception ) {
			throw new IOException( exception );
		}

	}

	@Override
	protected void stopService() throws IOException {
		System.out.println( "Closing serial port." );
		if( port != null ) port.close();
	}

}

package org.novaworx.service;

import java.text.ParseException;

import javax.comm.SerialPort;

import junit.framework.TestCase;

public class SerialSettingsTest extends TestCase {

	public void testParse() throws Exception {
		SerialSettings settings = null;

		try {
			settings = SerialSettings.parse( null );
			fail( "ParseException should have been thrown" );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "" );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		settings = SerialSettings.parse( "300,5,n,1" );
		assertEquals( 300, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_5, settings.getBits() );
		assertEquals( SerialPort.PARITY_NONE, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_1, settings.getStop() );

		settings = SerialSettings.parse( "2400,6,e,1.5" );
		assertEquals( 2400, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_6, settings.getBits() );
		assertEquals( SerialPort.PARITY_EVEN, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_1_5, settings.getStop() );

		settings = SerialSettings.parse( "4800,7,o,2" );
		assertEquals( 4800, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_7, settings.getBits() );
		assertEquals( SerialPort.PARITY_ODD, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_2, settings.getStop() );

		settings = SerialSettings.parse( "9600,8,m,1" );
		assertEquals( 9600, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_8, settings.getBits() );
		assertEquals( SerialPort.PARITY_MARK, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_1, settings.getStop() );

		settings = SerialSettings.parse( "14400,8,s,1" );
		assertEquals( 14400, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_8, settings.getBits() );
		assertEquals( SerialPort.PARITY_SPACE, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_1, settings.getStop() );

		settings = SerialSettings.parse( "19200,7,e,1" );
		assertEquals( 19200, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_7, settings.getBits() );
		assertEquals( SerialPort.PARITY_EVEN, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_1, settings.getStop() );

		settings = SerialSettings.parse( "57600,8,n,1" );
		assertEquals( 57600, settings.getBaud() );
		assertEquals( SerialPort.DATABITS_8, settings.getBits() );
		assertEquals( SerialPort.PARITY_NONE, settings.getParity() );
		assertEquals( SerialPort.STOPBITS_1, settings.getStop() );
	}

	public void testParseBaudFailures() throws Exception {
		SerialSettings settings = null;

		try {
			settings = SerialSettings.parse( "bad,8,n,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );
	}

	public void testParseBitsFailures() throws Exception {
		SerialSettings settings = null;

		try {
			settings = SerialSettings.parse( "300,bad,n,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "300,4,n,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "300,9,n,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );
	}

	public void testParseParityFailures() throws Exception {
		SerialSettings settings = null;

		try {
			settings = SerialSettings.parse( "300,8,bad,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "300,8,a,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "300,8,8,1" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );
	}

	public void testParseStopBitsFailures() throws Exception {
		SerialSettings settings = null;

		try {
			settings = SerialSettings.parse( "300,8,n,bad" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "300,8,n,0" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );

		try {
			settings = SerialSettings.parse( "300,8,n,4" );
			fail( "ParseException should have been thrown." );
		} catch( ParseException exception ) {
			// Allow test to pass.
		}
		assertNull( settings );
	}

}

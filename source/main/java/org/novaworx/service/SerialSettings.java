package org.novaworx.service;

import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.comm.SerialPort;

public class SerialSettings {

	private int baud;

	private int bits;

	private int parity;

	private int stop;

	public SerialSettings( int baud, int bits, int parity, int stop ) {
		this.baud = baud;
		this.bits = bits;
		this.parity = parity;
		this.stop = stop;
	}

	public static final SerialSettings parse( String settings ) throws ParseException {
		if( settings == null ) throw new ParseException( "Settings cannot be null.", 0 );
		if( "".equals( settings ) ) throw new ParseException( "Settings cannot be empty.", 0 );

		StringTokenizer tokenizer = new StringTokenizer( settings, "," );

		int baud = -1;
		int bits = -1;
		int parity = -1;
		int stop = -1;

		try {
			baud = Integer.parseInt( tokenizer.nextToken() );
		} catch( Exception exception ) {
			throw new ParseException( exception.getMessage(), 0 );
		}

		try {
			bits = Integer.parseInt( tokenizer.nextToken() );
		} catch( Exception exception ) {
			throw new ParseException( exception.getMessage(), 1 );
		}
		if( bits < SerialPort.DATABITS_5 || bits > SerialPort.DATABITS_8 ) {
			throw new ParseException( "Invalid data bits value: " + bits, 1 );
		}

		String parityString = null;
		try {
			parityString = tokenizer.nextToken().toLowerCase();
			if( "n".equals( parityString ) ) {
				parity = SerialPort.PARITY_NONE;
			} else if( "e".equals( parityString ) ) {
				parity = SerialPort.PARITY_EVEN;
			} else if( "o".equals( parityString ) ) {
				parity = SerialPort.PARITY_ODD;
			} else if( "m".equals( parityString ) ) {
				parity = SerialPort.PARITY_MARK;
			} else if( "s".equals( parityString ) ) {
				parity = SerialPort.PARITY_SPACE;
			}
		} catch( Exception exception ) {
			throw new ParseException( exception.getMessage(), 2 );
		}
		if( parity < SerialPort.PARITY_NONE || parity > SerialPort.PARITY_SPACE ) {
			throw new ParseException( "Invalid parity value: " + parityString, 2 );
		}

		// Parse the stop string.
		String stopString = null;
		try {
			stopString = tokenizer.nextToken();
			if( stopString.equals( "1.5" ) ) {
				stop = SerialPort.STOPBITS_1_5;
			} else {
				stop = Integer.parseInt( stopString );
			}
		} catch( NumberFormatException exception ) {
			throw new ParseException( "Value cannot be parsed: " + stopString, 3 );
		} catch( NoSuchElementException exception ) {
			throw new ParseException( "No more values to parse.", 3 );
		}
		if( stop < SerialPort.STOPBITS_1 || stop > SerialPort.STOPBITS_1_5 ) {
			throw new ParseException( "Invalid stop bits value: " + stopString, 3 );
		}

		return new SerialSettings( baud, bits, parity, stop );
	}

	public int getBaud() {
		return baud;
	}

	public int getBits() {
		return bits;
	}

	public int getParity() {
		return parity;
	}

	public int getStop() {
		return stop;
	}

}

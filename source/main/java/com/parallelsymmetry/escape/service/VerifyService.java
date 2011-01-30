package com.parallelsymmetry.escape.service;

import java.util.logging.Level;

import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.log.Log;

public class VerifyService extends Service {

	public static final void main( String[] commands ) {
		VerifyService service = new VerifyService();
		if( commands.length == 0 ) Log.setLevel( Log.TRACE );
		service.call( commands );
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {}

	@Override
	protected void process( Parameters parameters ) throws Exception {
		// -------------------0--------1---------2---------3---------4---------5---------6---------7---------8
		// -------------------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write();
		Log.write( "Welcome to the Verfiy service. The Verfiy service is provided for reference" );
		Log.write( "purposes only. Please note the Verfiy service automatically stops regardless" );
		Log.write( "of what parameters are specified on the command line." );
		Log.write();

		Level level = Log.getLevel();
		Log.setLevel( Log.ALL );
		Log.write( "Sample log messages:" );
		Log.write( Log.ERROR, "Error message." );
		Log.write( Log.WARN, "Warning message." );
		Log.write( Log.INFO, "Information message." );
		Log.write( Log.TRACE, "Trace message." );
		Log.write( Log.DEBUG, "Debug message." );
		Log.write();
		Log.setLevel( level );

		getPackManager().getNewPacks();

		call( new String[] { "-stop" } );
	}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {}

}

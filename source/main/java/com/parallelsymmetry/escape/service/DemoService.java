package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.log.Log;

public class DemoService extends Service {

	public static final void main( String[] commands ) {
		DemoService service = new DemoService();
		System.err.println( "Home: " + service.getHomeFolder() );
		service.call( service.getHomeFolder() == null ? new String[] { "-log.tag", "-log.level", "trace" } : commands );
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {}

	@Override
	protected void process( Parameters parameters ) throws Exception {
		// -------------------0--------1---------2---------3---------4---------5---------6---------7---------8
		// -------------------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write( Log.NONE );
		Log.write( Log.NONE, "Welcome to the Demo Service. The Demo service is provided for reference" );
		Log.write( Log.NONE, "purposes only. Please note the Demo Service automatically stops regardless" );
		Log.write( Log.NONE, "of what parameters are specified on the command line." );
		Log.write( Log.NONE );
		Log.write( Log.ERROR, "Error message." );
		Log.write( Log.WARN, "Warning message." );
		Log.write( Log.INFO, "Information message." );
		Log.write( Log.TRACE, "Trace message." );
		Log.write( Log.DEBUG, "Debug message." );
		call( new String[] { "-stop" } );
	}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {}

}

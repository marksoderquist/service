package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.log.Log;

public class DemoService extends Service {

	public static final void main( String[] commands ) {
		new DemoService().call( "-log.tag", "-log.level", "trace");
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		Log.write( "Welcome to the Demo service." );
	}

	@Override
	protected void process( Parameters parameters ) throws Exception {
		call( new String[] { "-stop" } );
	}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		Log.write( "Thank you for using the Demo service." );
	}

}

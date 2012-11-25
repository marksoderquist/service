package com.parallelsymmetry.service.systest;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.service.ServiceFlag;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.log.Log;

public class VerifyService extends Service {

	public static final String AUTO_TERMINATION_MESSAGE = "*** AUTOMATIC TERMINATION ***";

	public static final int AUTO_TERMINATE_TIMEOUT = 2000;

	private Timer timer;

	public static final void main( String[] commands ) {
		try {
			VerifyService service = new VerifyService();
			service.call( commands );
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	public VerifyService() throws Exception {
		super();
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		timer = new Timer();
		timer.schedule( new TerminateTask(), AUTO_TERMINATE_TIMEOUT );
	}

	@Override
	protected void process( Parameters parameters ) throws Exception {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write();
		Log.write( "Welcome to the Verfiy service. The Verfiy service is provided for reference" );
		Log.write( "purposes only. Please note the Verfiy service automatically stops after five" );
		Log.write( "seconds regardless of what parameters are specified on the command line." );
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
	}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		timer.cancel();
	}

	private class TerminateTask extends TimerTask {

		@Override
		public void run() {
			Log.write( AUTO_TERMINATION_MESSAGE );
			call( new String[] { ServiceFlag.STOP } );
		}

	}

}

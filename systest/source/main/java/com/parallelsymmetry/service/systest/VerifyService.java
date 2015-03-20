package com.parallelsymmetry.service.systest;

import java.util.Timer;
import java.util.TimerTask;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.service.ServiceFlag;
import com.parallelsymmetry.service.ServiceSettingsPath;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.setting.Settings;

public class VerifyService extends Service {

	public static final String AUTO_TERMINATION_MESSAGE = "*** AUTOMATIC TERMINATION ***";

	public static final int AUTO_TERMINATE_TIMEOUT = 2000;

	private Timer timer;

	public static final void main( String[] commands ) {
		try {
			VerifyService service = new VerifyService();
			service.process( commands );
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	public VerifyService() throws Exception {
		super();
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		timer = new Timer( true );
		timer.schedule( new TerminateTask(), AUTO_TERMINATE_TIMEOUT );
	}

	@Override
	protected void process( Parameters parameters, boolean peer ) throws Exception {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write();
		Log.write( "Welcome to the Verify service. The Verify service is provided for reference" );
		Log.write( "purposes only. Please note the Verify service automatically stops after " + AUTO_TERMINATE_TIMEOUT );
		Log.write( "milliseconds regardless of what parameters are specified on the command line." );
		Log.write();

		Settings updateSettings = getSettings().getNode( ServiceSettingsPath.UPDATE_SETTINGS_PATH );
		Log.write( Log.TRACE, "Update check: " + updateSettings.get( "check", null ) );
		Log.write( Log.TRACE, "Update found: " + updateSettings.get( "found", null ) );
		Log.write( Log.TRACE, "Update apply: " + updateSettings.get( "apply", null ) );
	}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		timer.cancel();
	}

	private class TerminateTask extends TimerTask {

		@Override
		public void run() {
			Log.write( AUTO_TERMINATION_MESSAGE );
			processInternal( new String[] { ServiceFlag.STOP } );
		}

	}

}

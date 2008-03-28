package com.parallelsymmetry.service;

public class ExceptionCountingService extends CountingService {

	public ExceptionCountingService( int startupPause, int shutdownPause ) {
		super( startupPause, shutdownPause );
	}

	@Override
	protected void startService() throws Exception {
		super.startService();
		throw new Exception( "Test exception during startService()." );
	}

	@Override
	protected void stopService() throws Exception {
		super.stopService();
		throw new Exception( "Test exception during stopService()." );
	}

}

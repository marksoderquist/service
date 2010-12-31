package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.utility.Parameters;

public class MockService extends Service {

	public MockService() {
		super( "/test.program.xml" );
	}

	public static final void main( String[] commands ) {
		new MockService().call( commands );
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		// TODO Implement Service.startService().

	}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		// TODO Implement Service.stopService().

	}

}

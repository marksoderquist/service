package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.ThreadUtil;

public class MockService extends Service {

	private int startCount;

	private int stopCount;

	private int startupPause;

	private int shutdownPause;

	public MockService() {
		this( 0, 0 );
	}

	public MockService( String name ) {
		this( name, 0, 0 );
	}

	public MockService( int startupPause, int shutdownPause ) {
		this( null, startupPause, shutdownPause );
	}

	public MockService( String name, int startupPause, int shutdownPause ) {
		super( name );
		this.startupPause = startupPause;
		this.shutdownPause = shutdownPause;
	}

	public static final void main( String[] commands ) {
		new MockService().call( commands );
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		ThreadUtil.pause( startupPause );
		startCount++;
	}
	
	@Override
	protected void process( Parameters parameters ) throws Exception {}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		ThreadUtil.pause( shutdownPause );
		stopCount++;
	}

	public int getStartCalledCount() {
		return startCount;
	}

	public int getStopCalledCount() {
		return stopCount;
	}

}

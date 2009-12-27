/**
 * 
 */
package com.parallelsymmetry.service;

import com.parallelsymmetry.util.ThreadUtil;

class CountingService extends Service {

	private int startupPause;

	private int shutdownPause;

	private int startServiceCount;

	private int stopServiceCount;

	/**
	 * @param serviceTest
	 */
	public CountingService( int startupPause, int shutdownPause ) {
		this.startupPause = startupPause;
		this.shutdownPause = shutdownPause;
	}

	@Override
	protected void startService() throws Exception {
		startServiceCount++;
		ThreadUtil.pause( startupPause );
	}

	@Override
	protected void stopService() throws Exception {
		stopServiceCount++;
		ThreadUtil.pause( shutdownPause );
	}

	public void resetCounts() {
		startServiceCount = 0;
		stopServiceCount = 0;
	}

	public int getStartupPause() {
		return startupPause;
	}

	public int getShutdownPause() {
		return shutdownPause;
	}

	public int getStartServiceCount() {
		return startServiceCount;
	}

	public int getStopServiceCount() {
		return stopServiceCount;
	}
}

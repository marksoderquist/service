package org.novaworx.service;

import junit.framework.TestCase;

import org.novaworx.util.Log;
import org.novaworx.util.ThreadUtil;

public class ServiceTest extends TestCase {

	@Override
	public void setUp() {
		Log.setLevel( Log.NONE );
		Log.write();
	}

	@Override
	public void tearDown() {
		Log.setLevel( null );
	}

	public void testStartAndStop() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );

		service.start();
		assertEquals( Service.State.STARTING, service.getState() );
		ThreadUtil.pause( service.getStartPause() * 2 );
		assertEquals( Service.State.STARTED, service.getState() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );

		service.stop();
		assertEquals( Service.State.STOPPING, service.getState() );
		ThreadUtil.pause( service.getStartPause() * 2 );
		assertEquals( Service.State.STOPPED, service.getState() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testDoubleStart() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.start();
		service.start();
		service.waitForStartup();
		service.start();
		service.waitForStartup();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.stop();
		service.waitForShutdown();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testStartAndWait() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testStop() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.start();
		service.waitForStartup();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.stop();
		service.waitForShutdown();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testDoubleStop() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.start();
		service.waitForStartup();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.stop();
		service.stop();
		service.waitForShutdown();
		service.stop();
		service.waitForShutdown();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testStopAndWait() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testRestart() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.restart();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 2, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 2, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 2, service.getStopServiceCount() );
	}

	public void testFastRestarts() throws Exception {
		CountingService service = new CountingService();
		assertFalse( service.isRunning() );
		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		int count = 10;
		for( int index = 0; index < count; index++ ) {
			service.restart();
		}
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", count + 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", count, service.getStopServiceCount() );
		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", count + 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", count + 1, service.getStopServiceCount() );
	}

	private class CountingService extends Service {

		private final int startPause = 10;

		private final int stopPause = 10;

		private int startServiceCount;

		private int stopServiceCount;

		@Override
		protected void startService() {
			startServiceCount++;
			ThreadUtil.pause( startPause );
		}

		@Override
		protected void stopService() {
			stopServiceCount++;
			ThreadUtil.pause( stopPause );
		}

		public void reset() {
			startServiceCount = 0;
			stopServiceCount = 0;
		}

		public int getStartPause() {
			return startPause;
		}

		public int getStopPause() {
			return stopPause;
		}

		public int getStartServiceCount() {
			return startServiceCount;
		}

		public int getStopServiceCount() {
			return stopServiceCount;
		}
	}

}

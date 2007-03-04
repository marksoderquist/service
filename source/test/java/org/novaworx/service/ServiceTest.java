package org.novaworx.service;

import junit.framework.TestCase;

import org.novaworx.util.Log;

public class ServiceTest extends TestCase {

	@Override
	public void setUp() {
		Log.setLevel( Log.NONE );
	}

	@Override
	public void tearDown() {
		Log.setLevel( null );
	}

	public void testStart() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.start();
		service.waitForStartup();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertFalse( service.wasStopServiceCalled() );
		service.clearFlags();

		service.stop();
		service.waitForShutdown();
		assertFalse( service.isRunning() );
		assertFalse( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
	}

	public void testStartAndWait() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.startAndWait();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertFalse( service.wasStopServiceCalled() );
		service.clearFlags();

		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertFalse( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
	}

	public void testStop() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.start();
		service.waitForStartup();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertFalse( service.wasStopServiceCalled() );
		service.clearFlags();

		service.stop();
		service.waitForShutdown();
		assertFalse( service.isRunning() );
		assertFalse( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
	}

	public void testStopAndWait() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.startAndWait();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertFalse( service.wasStopServiceCalled() );
		service.clearFlags();

		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertFalse( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
	}

	public void testRestart() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.startAndWait();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertFalse( service.wasStopServiceCalled() );
		service.clearFlags();

		service.restart();
		service.waitForStartup();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
		service.clearFlags();

		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertFalse( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
	}
	
	public void testFastRestarts() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.startAndWait();
		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertFalse( service.wasStopServiceCalled() );
		service.clearFlags();

		service.restart();
		service.restart();
		service.restart();
		service.restart();
		service.restart();

		assertTrue( service.isRunning() );
		assertTrue( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
		service.clearFlags();

		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertFalse( service.wasStartServiceCalled() );
		assertTrue( service.wasStopServiceCalled() );
	}

	private class TestConnectionService extends Service {

		private boolean startServiceCalled;

		private boolean stopServiceCalled;

		@Override
		protected void startService() {
			startServiceCalled = true;
		}

		@Override
		protected void stopService() {
			stopServiceCalled = true;
		}

		public void clearFlags() {
			startServiceCalled = false;
			stopServiceCalled = false;
		}

		public boolean wasStartServiceCalled() {
			return startServiceCalled;
		}

		public boolean wasStopServiceCalled() {
			return stopServiceCalled;
		}
	}

}

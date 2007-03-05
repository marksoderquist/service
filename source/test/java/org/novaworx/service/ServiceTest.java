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

	public void testStartAndStop() throws Exception {
		TestConnectionService service = new TestConnectionService();
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

	public void testDoubleStart() throws Exception {
		TestConnectionService service = new TestConnectionService();
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
		TestConnectionService service = new TestConnectionService();
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
		TestConnectionService service = new TestConnectionService();
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

	public void testStopAndWait() throws Exception {
		TestConnectionService service = new TestConnectionService();
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
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );

		service.restart();
		System.out.println( "Checking state..." );
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 2, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );

		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 2, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 2, service.getStopServiceCount() );
	}

	public void testFastRestarts() throws Exception {
		TestConnectionService service = new TestConnectionService();
		assertFalse( service.isRunning() );

		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );

		service.restart();
		service.restart();
		service.restart();
		service.restart();
		service.restart();

		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 6, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 5, service.getStopServiceCount() );

		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 6, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 6, service.getStopServiceCount() );
	}

	private class TestConnectionService extends Service {

		private int startServiceCount;

		private int stopServiceCount;

		@Override
		protected void startService() {
			startServiceCount++;
		}

		@Override
		protected void stopService() {
			stopServiceCount++;
		}

		public void reset() {
			startServiceCount = 0;
			stopServiceCount = 0;
		}

		public int getStartServiceCount() {
			return startServiceCount;
		}

		public int getStopServiceCount() {
			return stopServiceCount;
		}
	}

}

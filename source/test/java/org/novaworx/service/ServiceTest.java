package org.novaworx.service;

import junit.framework.TestCase;

public class ServiceTest extends TestCase {

	public void testStart() throws Exception {
		TestConnectionHandler handler = new TestConnectionHandler();
		assertFalse( handler.isRunning() );

		handler.start();
		handler.waitForStartup();
		assertTrue( handler.isRunning() );
		assertTrue( handler.wasStartServiceCalled() );
		assertFalse( handler.wasStopServiceCalled() );
		handler.clearFlags();

		handler.stop();
		handler.waitForShutdown();
		assertFalse( handler.isRunning() );
		assertFalse( handler.wasStartServiceCalled() );
		assertTrue( handler.wasStopServiceCalled() );
	}

	public void testStartAndWait() throws Exception {
		TestConnectionHandler handler = new TestConnectionHandler();
		assertFalse( handler.isRunning() );

		handler.startAndWait();
		assertTrue( handler.isRunning() );
		assertTrue( handler.wasStartServiceCalled() );
		assertFalse( handler.wasStopServiceCalled() );
		handler.clearFlags();

		handler.stopAndWait();
		assertFalse( handler.isRunning() );
		assertFalse( handler.wasStartServiceCalled() );
		assertTrue( handler.wasStopServiceCalled() );
	}

	public void testStop() throws Exception {
		TestConnectionHandler handler = new TestConnectionHandler();
		assertFalse( handler.isRunning() );

		handler.start();
		handler.waitForStartup();
		assertTrue( handler.isRunning() );
		assertTrue( handler.wasStartServiceCalled() );
		assertFalse( handler.wasStopServiceCalled() );
		handler.clearFlags();

		handler.stop();
		handler.waitForShutdown();
		assertFalse( handler.isRunning() );
		assertFalse( handler.wasStartServiceCalled() );
		assertTrue( handler.wasStopServiceCalled() );
	}

	public void testStopAndWait() throws Exception {
		TestConnectionHandler handler = new TestConnectionHandler();
		assertFalse( handler.isRunning() );

		handler.startAndWait();
		assertTrue( handler.isRunning() );
		assertTrue( handler.wasStartServiceCalled() );
		assertFalse( handler.wasStopServiceCalled() );
		handler.clearFlags();

		handler.stopAndWait();
		assertFalse( handler.isRunning() );
		assertFalse( handler.wasStartServiceCalled() );
		assertTrue( handler.wasStopServiceCalled() );
	}

	public void testReconnect() throws Exception {
		TestConnectionHandler handler = new TestConnectionHandler();
		assertFalse( handler.isRunning() );

		handler.startAndWait();
		assertTrue( handler.isRunning() );
		assertTrue( handler.wasStartServiceCalled() );
		assertFalse( handler.wasStopServiceCalled() );
		handler.clearFlags();

		handler.restart();
		handler.waitForStartup();
		assertTrue( handler.isRunning() );
		assertTrue( handler.wasStartServiceCalled() );
		assertTrue( handler.wasStopServiceCalled() );
		handler.clearFlags();

		handler.stopAndWait();
		assertFalse( handler.isRunning() );
		assertFalse( handler.wasStartServiceCalled() );
		assertTrue( handler.wasStopServiceCalled() );
	}

	private class TestConnectionHandler extends Service {

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

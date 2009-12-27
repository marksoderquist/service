package com.parallelsymmetry.service;

import junit.framework.TestCase;

import com.parallelsymmetry.log.Log;

public abstract class ServiceTest extends TestCase {

	protected CountingService service;

	@Override
	public void setUp() {
		Log.setLevel( Log.NONE );
	}

	@Override
	public void tearDown() {
		Log.setLevel( null );
	}

	public void testStartAndStop() throws Exception {
		//Log.write( "testStartAndStop()..." );
		assertFalse( service.isRunning() );

		service.start();
		service.waitForStartup();
		assertEquals( Service.State.STARTED, service.getState() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );

		service.stop();
		service.waitForShutdown();
		assertEquals( Service.State.STOPPED, service.getState() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
	}

	public void testDoubleStart() throws Exception {
		//Log.write( "testDoubleStart()..." );
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
		//Log.write( "testStartAndWait()..." );
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
		//Log.write( "testStop()..." );
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
		//Log.write( "testDoubleStop()..." );
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
		//Log.write( "testStopAndWait()..." );
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

	public void testReset() throws Exception {
		//Log.write( "testReset()..." );
		assertFalse( service.isRunning() );
		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		service.reset();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 2, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 1, service.getStopServiceCount() );
		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", 2, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 2, service.getStopServiceCount() );
	}

	public void testFastResets() throws Exception {
		//Log.write( "testFastResets()..." );
		assertFalse( service.isRunning() );
		service.startAndWait();
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", 0, service.getStopServiceCount() );
		int count = 10;
		for( int index = 0; index < count; index++ ) {
			service.reset();
		}
		assertTrue( service.isRunning() );
		assertEquals( "Wrong start call count.", count + 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", count, service.getStopServiceCount() );
		service.stopAndWait();
		assertFalse( service.isRunning() );
		assertEquals( "Wrong start call count.", count + 1, service.getStartServiceCount() );
		assertEquals( "Wrong stop call count.", count + 1, service.getStopServiceCount() );
	}

}

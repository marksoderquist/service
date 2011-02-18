package com.parallelsymmetry.escape.service.task;

import java.util.concurrent.Future;

import junit.framework.TestCase;

public class TaskManagerTest extends TestCase {

	private TaskManager manager;

	@Override
	public void setUp() {
		manager = new TaskManager();
	}

	public void testStartAndStop() throws Exception {
		assertFalse( manager.isRunning() );

		manager.startAndWait();
		assertTrue( manager.isRunning() );

		manager.stopAndWait();
		assertFalse( manager.isRunning() );
	}

	public void testRestart() throws Exception {
		assertFalse( manager.isRunning() );

		manager.startAndWait();
		assertTrue( manager.isRunning() );

		manager.stopAndWait();
		assertFalse( manager.isRunning() );

		manager.startAndWait();
		assertTrue( manager.isRunning() );

		manager.stopAndWait();
		assertFalse( manager.isRunning() );
	}

	public void testStopBeforeStart() throws Exception {
		assertFalse( manager.isRunning() );

		manager.stopAndWait();
		assertFalse( manager.isRunning() );
	}

	public void testSubmitNullResult() throws Exception {
		assertFalse( manager.isRunning() );

		manager.startAndWait();
		assertTrue( manager.isRunning() );

		MockTask task = new MockTask();
		Future<Object> future = manager.submit( task );
		assertNull( future.get() );
		assertFalse( task.isRunning() );
		assertTrue( task.isComplete() );
		assertTrue( task.isSuccess() );
		assertFalse( task.isCancelled() );
	}

	public void testSubmitWithResult() throws Exception {
		assertFalse( manager.isRunning() );

		manager.startAndWait();
		assertTrue( manager.isRunning() );

		Object result = new Object();
		MockTask task = new MockTask( result );
		Future<Object> future = manager.submit( task );
		assertEquals( result, future.get() );
		assertFalse( task.isRunning() );
		assertTrue( task.isComplete() );
		assertTrue( task.isSuccess() );
		assertFalse( task.isCancelled() );
	}

	public void testFailedTask() throws Exception {
		assertFalse( manager.isRunning() );

		manager.startAndWait();
		assertTrue( manager.isRunning() );

		MockTask task = new MockTask( null, true );
		Future<Object> future = manager.submit( task );
		try {
			assertNull( future.get() );
			fail();
		} catch( Exception exception ) {
			assertNotNull( exception );
		}
		assertFalse( task.isRunning() );
		assertTrue( task.isComplete() );
		assertFalse( task.isSuccess() );
		assertFalse( task.isCancelled() );
	}

	public void testSubmitBeforeStart() throws Exception {
		assertFalse( manager.isRunning() );

		MockTask task = new MockTask();

		manager.submit( task );
		assertFalse( manager.isRunning() );
		assertFalse( task.isRunning() );
		assertFalse( task.isComplete() );
		assertFalse( task.isSuccess() );
		assertFalse( task.isCancelled() );
	}

	private static final class MockTask extends Task<Object> {

		private Object object;

		private boolean fail;

		public MockTask() {
			this( null, false );
		}

		public MockTask( Object object ) {
			this( object, false );
		}

		public MockTask( Object object, boolean fail ) {
			this.object = object;
			this.fail = fail;
		}

		@Override
		protected Object execute() throws Exception {
			if( fail ) throw new Exception( "Intentionally fail task." );
			return object;
		}

	}

}

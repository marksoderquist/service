package org.novaworx.service;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.novaworx.util.Log;
import org.novaworx.util.ThreadUtil;

public class WorkerTest extends TestCase {

	@Override
	public void setUp() {
		Log.setLevel( Log.NONE );
		Log.write();
	}

	public void testStartStopCount() throws Exception {
		CountingWorker worker = new CountingWorker();
		assertFalse( worker.isWorking() );
		worker.startAndWait();
		assertTrue( worker.isWorking() );
		worker.stopAndWait();
		assertFalse( worker.isWorking() );
		assertEquals( 1, worker.getStartCount() );
		assertEquals( 1, worker.getStopCount() );
	}

	public void testStartAndStop() throws Exception {
		Worker worker = new BlockingIOWorker();
		assertFalse( worker.isWorking() );
		worker.startAndWait();
		assertTrue( worker.isWorking() );
		worker.stopAndWait();
		assertFalse( worker.isWorking() );
	}

	public void testRestart() throws Exception {
		Worker worker = new BlockingIOWorker();
		assertFalse( worker.isWorking() );
		worker.startAndWait();
		assertTrue( "Worker not working after start.", worker.isWorking() );
		worker.restart();
		assertTrue( "Worker not working after restart.", worker.isWorking() );
		worker.stopAndWait();
		assertFalse( worker.isWorking() );
	}

	public void testFastRestarts() throws Exception {
		Worker worker = new BlockingIOWorker();
		assertFalse( worker.isWorking() );
		worker.startAndWait();
		assertTrue( worker.isWorking() );
		worker.restart();
		worker.restart();
		worker.restart();
		worker.restart();
		worker.restart();
		assertTrue( worker.isWorking() );
		worker.stopAndWait();
		assertFalse( worker.isWorking() );
	}

	private static class CountingWorker extends Worker {

		private int startCount;

		private int stopCount;

		public int getStartCount() {
			return startCount;
		}

		public int getStopCount() {
			return stopCount;
		}

		@Override
		public void startWorker() {
			Log.write( "Start worker..." );
			startCount++;
		}

		@Override
		public void stopWorker() {
			Log.write( "Stop worker..." );
			stopCount++;
		}

		@Override
		public void run() {
			while( shouldExecute() ) {
				ThreadUtil.pause( 1 );
			}
		}

	}

	private static class BlockingIOWorker extends Worker {
		InputStream input;

		@Override
		protected void startWorker() throws Exception {
			input = new TestInputStream();
		}

		@Override
		public void run() {
			try {
				input.read();
			} catch( IOException exception ) {
				// Intentionally ignore exception
			}
		}

		@Override
		protected void stopWorker() throws Exception {
			input.close();
		}
	}

	private static class TestInputStream extends InputStream {

		private boolean closed;

		@Override
		public synchronized int read() throws IOException {
			while( !closed ) {
				try {
					this.wait();
				} catch( InterruptedException exception ) {}
			}
			return -1;
		}

		@Override
		public synchronized void close() {
			closed = true;
			this.notifyAll();
		}

	}

}

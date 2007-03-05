package org.novaworx.service;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.novaworx.util.Log;

public class WorkerTest extends TestCase {

	private Worker worker;

	@Override
	public void setUp() {
		Log.setLevel( Log.NONE );
		worker = new BlockingIOWorker();
	}

	public void testStartAndStop() throws Exception {
		assertFalse( worker.isWorking() );
		worker.startAndWait();
		assertTrue( worker.isWorking() );
		worker.stopAndWait();
		assertFalse( worker.isWorking() );
	}

	public void testRestart() throws Exception {
		assertFalse( worker.isWorking() );
		worker.startAndWait();
		assertTrue( "Worker not working after start.", worker.isWorking() );
		worker.restart();
		assertTrue( "Worker not working after restart.", worker.isWorking() );
		worker.stopAndWait();
		assertFalse( worker.isWorking() );
	}

	public void testFastRestarts() throws Exception {
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

	private static class BlockingIOWorker extends Worker {
		InputStream input;

		@Override
		protected void startWorker() throws Exception {
			input = new TestInputStream();
		}

		public void process() {
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

		@Override
		public synchronized int read() throws IOException {
			try {
				this.wait();
			} catch( InterruptedException exception ) {}
			return -1;
		}

		@Override
		public synchronized void close() {
			notifyAll();
		}

	}

}

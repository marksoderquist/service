package com.parallelsymmetry.escape.service.task;

import java.util.concurrent.Callable;

public abstract class Task<V> implements Callable<V> {

	private boolean running;

	private boolean complete;

	private boolean success;

	private boolean cancel;

	public final boolean isRunning() {
		return running;
	}

	public final boolean isComplete() {
		return complete;
	}

	public final boolean isSuccess() {
		return success;
	}

	public final boolean isCancelled() {
		return cancel;
	}

	public final void cancel() {
		cancel = true;
	}

	@Override
	public final V call() throws Exception {
		running = true;
		try {
			V result = execute();
			success = true;
			return result;
		} finally {
			complete = true;
			running = false;
		}
	}

	public abstract V execute() throws Exception;

}

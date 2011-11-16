package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.utility.task.Task;

public abstract class ServiceTask<V> extends Task<V> {

	private Service service;

	public ServiceTask( Service service ) {
	}

	public ServiceTask( Service service, String name ) {
		super( name );
		this.service = service;
	}

	@Override
	public V call() throws Exception {
		try {
			return super.call();
		} catch( Throwable throwable ) {
			service.error( throwable );
			return null;
		}
	}

}

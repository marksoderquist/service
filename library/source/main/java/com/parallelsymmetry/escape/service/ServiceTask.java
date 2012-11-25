package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.utility.task.Task;

public abstract class ServiceTask<V> extends Task<V> {

	private Service service;

	public ServiceTask( Service service ) {
		this.service = service;
	}

	public ServiceTask( Service service, String name ) {
		super( name );
		this.service = service;
	}

	protected Service getService() {
		return service;
	}

}

package com.parallelsymmetry.service;

public class ServiceEvent {

	private Service service;

	private Service.State state;

	public ServiceEvent( Service service, Service.State type ) {
		this.service = service;
		this.state = type;
	}

	public Service getService() {
		return service;
	}

	public Service.State getState() {
		return state;
	}

}

package org.novaworx.service;

public class ServiceEvent {

	private Service service;

	private Service.EventType type;

	public ServiceEvent( Service service, Service.EventType type ) {
		this.service = service;
		this.type = type;
	}

	public Service getService() {
		return service;
	}

	public Service.EventType getType() {
		return type;
	}

}

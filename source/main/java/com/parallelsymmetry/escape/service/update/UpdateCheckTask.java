package com.parallelsymmetry.escape.service.update;

import java.util.TimerTask;

import com.parallelsymmetry.escape.service.Service;

public abstract class UpdateCheckTask extends TimerTask {

	private Service service;

	public UpdateCheckTask( Service service ) {
		this.service = service;
	}

	public Service getService() {
		return service;
	}

	@Override
	public void run() {
		try {
			execute();
		} finally {
			service.getUpdateManager().scheduleCheckUpdateTask( service.getUpdateCheckTask() );
		}
	}

	public abstract void execute();
}
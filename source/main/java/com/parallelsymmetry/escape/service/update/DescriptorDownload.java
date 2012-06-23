package com.parallelsymmetry.escape.service.update;

import java.io.FileInputStream;
import java.net.URI;

import com.parallelsymmetry.escape.service.task.Download;
import com.parallelsymmetry.escape.service.task.DownloadTask;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.task.Task;
import com.parallelsymmetry.escape.utility.task.TaskListener;

public final class DescriptorDownload extends Task<Descriptor> {

	private DownloadTask task;

	public DescriptorDownload( URI uri ) {
		super( uri.toString() );
		this.task = new DownloadTask( uri );
	}

	public long getMinimum() {
		return task.getMinimum();
	}

	public long getMaximum() {
		return task.getMaximum();
	}

	public long getProgress() {
		return task.getProgress();
	}

	public void addTaskListener( TaskListener listener ) {
		task.addTaskListener( listener );
	}

	public void removeTaskListener( TaskListener listener ) {
		task.removeTaskListener( listener );
	}

	@Override
	public Descriptor execute() throws Exception {
		Download download = task.execute();
		if( download == null ) return null;
		return new Descriptor( new FileInputStream( download.getTarget() ) );
	}

}

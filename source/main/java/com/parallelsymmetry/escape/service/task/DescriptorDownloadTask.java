package com.parallelsymmetry.escape.service.task;

import java.io.FileInputStream;
import java.net.URI;

import com.parallelsymmetry.escape.utility.BundleKey;
import com.parallelsymmetry.escape.utility.Bundles;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.task.Task;
import com.parallelsymmetry.escape.utility.task.TaskListener;

public class DescriptorDownloadTask extends Task<Descriptor> {

	private DownloadTask task;

	public DescriptorDownloadTask( URI uri ) {
		super( Bundles.getString( BundleKey.PROMPTS, "download" ) + uri.toString() );
		this.task = new DownloadTask( uri );
	}

	public URI getUri() {
		return task.getUri();
	}

	@Override
	public long getMinimum() {
		return task.getMinimum();
	}

	@Override
	public long getMaximum() {
		return task.getMaximum();
	}

	@Override
	public long getProgress() {
		return task.getProgress();
	}

	@Override
	public void addTaskListener( TaskListener listener ) {
		task.addTaskListener( listener );
	}

	@Override
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
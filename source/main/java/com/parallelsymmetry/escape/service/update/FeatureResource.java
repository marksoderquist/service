package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

import com.parallelsymmetry.escape.service.task.Download;

public final class FeatureResource {

	public enum Type {
		FILE, PACK
	};

	private FeatureResource.Type type;

	private URI uri;

	private Future<Download> future;
	
	private File file;

	public FeatureResource( FeatureResource.Type type, URI uri ) {
		this.type = type;
		this.uri = uri;
	}

	public FeatureResource.Type getType() {
		return type;
	}

	public URI getUri() {
		return uri;
	}
	
	public void waitFor() throws Exception {
		file = future.get().getTarget();
	}

	public File getLocalFile() {
		return file;
	}

	public void setFuture( Future<Download> future ) {
		this.future = future;
	}

	@Override
	public String toString() {
		return type.name() + ": " + uri;
	}

}

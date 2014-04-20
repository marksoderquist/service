package com.parallelsymmetry.service.product;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

import com.parallelsymmetry.service.task.Download;

public final class ProductResource {

	public enum Type {
		FILE, PACK
	};

	private ProductResource.Type type;

	private URI uri;

	private Future<Download> future;

	private File file;

	private Throwable throwable;

	public ProductResource( ProductResource.Type type, URI uri ) {
		this.type = type;
		this.uri = uri;
	}

	public ProductResource.Type getType() {
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

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable( Throwable throwable ) {
		this.throwable = throwable;
	}

	@Override
	public String toString() {
		return type.name() + ": " + uri;
	}

}

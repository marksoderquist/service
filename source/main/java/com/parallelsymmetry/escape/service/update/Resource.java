package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.net.URI;

public final class Resource {

	public enum Type {
		JAR, PACK
	};

	private Resource.Type type;

	private URI uri;

	private File file;

	public Resource( Resource.Type type, URI uri ) {
		this.type = type;
		this.uri = uri;
	}

	public Resource.Type getType() {
		return type;
	}

	public URI getUri() {
		return uri;
	}

	public File getInstallFile() {
		return file;
	}

	public void setInstallFile( File file ) {
		this.file = file;
	}

	@Override
	public String toString() {
		return type.name() + ": " + uri;
	}

}
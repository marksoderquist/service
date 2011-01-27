package com.parallelsymmetry.escape.service.update;

import java.net.URI;

public class UpdateSource {

	private URI uri;

	public UpdateSource( URI uri ) {
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof UpdateSource ) ) return false;
		UpdateSource that = (UpdateSource)object;
		return getUri().toString().equals( that.getUri().toString() );
	}

	@Override
	public int hashCode() {
		return getUri().toString().hashCode();
	}

}

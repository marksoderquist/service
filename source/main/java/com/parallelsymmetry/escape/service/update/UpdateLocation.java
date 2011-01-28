package com.parallelsymmetry.escape.service.update;

import java.net.URI;

public class UpdateLocation {

	private URI uri;

	public UpdateLocation( URI uri ) {
		this.uri = uri;
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof UpdateLocation ) ) return false;
		UpdateLocation that = (UpdateLocation)object;
		return getUri().toString().equals( that.getUri().toString() );
	}

	@Override
	public int hashCode() {
		return getUri().toString().hashCode();
	}

}

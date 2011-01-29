package com.parallelsymmetry.escape.service.pack;

import java.net.URI;

public class PackSite {

	private String name;

	private URI uri;

	public PackSite( URI uri ) {
		this.uri = uri;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof PackSite ) ) return false;
		PackSite that = (PackSite)object;
		return getUri().toString().equals( that.getUri().toString() );
	}

	@Override
	public int hashCode() {
		return getUri().toString().hashCode();
	}

}

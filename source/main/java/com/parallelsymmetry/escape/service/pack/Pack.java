package com.parallelsymmetry.escape.service.pack;

import java.net.URI;
import java.net.URISyntaxException;

import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.log.Log;

public class Pack {

	private String group;

	private String artifact;

	private Release release;

	private String name;

	private String provider;

	private URI uri;

	private Pack() {}

	public String getGroup() {
		return group;
	}

	public String getArtifact() {
		return artifact;
	}

	public Release getRelease() {
		return release;
	}

	public String getName() {
		return name;
	}

	public String getProvider() {
		return provider;
	}

	public URI getUpdateUri() {
		return uri;
	}

	public static final Pack load( Descriptor descriptor ) {
		if( descriptor == null ) return null;

		String group = descriptor.getValue( "/pack/group" );
		String artifact = descriptor.getValue( "/pack/artifact" );
		String version = descriptor.getValue( "/pack/version" );
		String timestamp = descriptor.getValue( "/pack/timestamp" );
		String name = descriptor.getValue( "/pack/name" );
		String provider = descriptor.getValue( "/pack/provider" );
		String uri = descriptor.getValue( "/pack/update/uri" );

		Pack pack = new Pack();

		pack.group = group;
		pack.artifact = artifact;
		pack.release = new Release( version, timestamp );
		pack.name = name;
		pack.provider = provider;

		try {
			if( uri != null ) pack.uri = new URI( uri );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}

		return pack;
	}

	@Override
	public String toString() {
		return group + "|" + artifact;
	}

}

package com.parallelsymmetry.escape.service.pack;

import java.net.URI;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class PackSite implements Persistent<PackSite> {

	private String name;

	private URI uri;

	private Settings settings;

	public PackSite() {}

	public PackSite( URI uri ) {
		this.uri = uri;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
		saveSettings( settings );
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public PackSite loadSettings( Settings settings ) {
		this.settings = settings;

		name = settings.get( "/name" );
		uri = URI.create( settings.get( "/uri" ) );

		return this;
	}

	@Override
	public PackSite saveSettings( Settings settings ) {
		settings.put( "/name", name );
		settings.put( "/uri", uri.toString() );

		return this;
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

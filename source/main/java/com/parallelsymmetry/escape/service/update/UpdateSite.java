package com.parallelsymmetry.escape.service.update;

import java.net.URI;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateSite implements Persistent<UpdateSite> {

	private String name;

	private URI uri;

	private Settings settings;

	public UpdateSite() {}

	public UpdateSite( URI uri ) {
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
	public UpdateSite loadSettings( Settings settings ) {
		this.settings = settings;

		name = settings.get( "name", null );
		uri = URI.create( settings.get( "uri", null ) );

		return this;
	}

	@Override
	public UpdateSite saveSettings( Settings settings ) {
		settings.put( "name", name );
		settings.put( "uri", uri.toString() );

		return this;
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof UpdateSite ) ) return false;
		UpdateSite that = (UpdateSite)object;
		return getUri().toString().equals( that.getUri().toString() );
	}

	@Override
	public int hashCode() {
		return getUri().toString().hashCode();
	}

}

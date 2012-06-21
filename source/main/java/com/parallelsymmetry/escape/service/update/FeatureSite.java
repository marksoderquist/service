package com.parallelsymmetry.escape.service.update;

import java.net.URI;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class FeatureSite implements Persistent {

	private String name;

	private URI uri;

	private Settings settings;

	public FeatureSite() {}

	public FeatureSite( URI uri ) {
		this.uri = uri;
	}

	public FeatureSite( URI uri, String name ) {
		this.uri = uri;
		this.name = name;
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
	public void loadSettings( Settings settings ) {
		this.settings = settings;

		name = settings.get( "name", null );
		uri = URI.create( settings.get( "uri", null ) );
	}

	@Override
	public void saveSettings( Settings settings ) {
		settings.put( "name", name );
		settings.put( "uri", uri.toString() );
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof FeatureSite ) ) return false;
		FeatureSite that = (FeatureSite)object;
		return getUri().toString().equals( that.getUri().toString() );
	}

	@Override
	public int hashCode() {
		return getUri().toString().hashCode();
	}

}

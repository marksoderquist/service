package com.parallelsymmetry.escape.service.update;

import java.net.URI;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class FeatureSite implements Persistent {

	private URI uri;

	private String name;

	private boolean enabled;

	private Settings settings;

	public FeatureSite() {}

	public FeatureSite( URI uri ) {
		this.uri = uri;
	}

	public FeatureSite( URI uri, String name ) {
		this.uri = uri;
		this.name = name;
	}

	public FeatureSite( URI uri, String name, boolean enabled ) {
		this.uri = uri;
		this.name = name;
		this.enabled = enabled;
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

	public void setUri( URI uri ) {
		this.uri = uri;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}

	@Override
	public void loadSettings( Settings settings ) {
		this.settings = settings;

		uri = URI.create( settings.get( "uri", null ) );
		name = settings.get( "name", null );
		enabled = settings.getBoolean( "enabled", false );
	}

	@Override
	public void saveSettings( Settings settings ) {
		settings.put( "uri", uri.toString() );
		settings.put( "name", name );
		settings.putBoolean( "enabled", enabled );
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

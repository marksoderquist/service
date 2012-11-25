package com.parallelsymmetry.escape.product;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.w3c.dom.Node;

import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.UriUtil;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.setting.Persistent;
import com.parallelsymmetry.utility.setting.Settings;

public class ProductCatalog implements Persistent {

	public static final String CATALOG_PATH = "/catalog";

	public static final String ICON_PATH = CATALOG_PATH + "/icon/@uri";

	public static final String NAME_PATH = CATALOG_PATH + "/name";

	public static final String SOURCES_PATH = CATALOG_PATH + "/sources/source";

	// When the attribute is in the root node there is no path info.
	private static final String SOURCE_PATH = "@uri";

	private String name;

	private URI iconUri;

	private URI sourceUri;

	private boolean enabled;

	private boolean removable;

	private List<URI> sources = new CopyOnWriteArrayList<URI>();

	private Settings settings;

	/*
	 * This constructor is required for the Settings API.
	 */
	public ProductCatalog() {}

	public ProductCatalog( Descriptor descriptor, URI base ) {
		update( descriptor, base );
	}

	public ProductCatalog update( Descriptor descriptor, URI base ) {
		String iconUri = descriptor.getValue( ICON_PATH );
		String name = descriptor.getValue( NAME_PATH );

		try {
			if( iconUri != null ) this.iconUri = UriUtil.resolve( base, new URI( iconUri ) );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}
		this.name = name;

		this.sources.clear();
		for( Node node : descriptor.getNodes( SOURCES_PATH ) ) {
			try {
				URI uri = UriUtil.resolve( base, new URI( new Descriptor( node ).getValue( SOURCE_PATH ) ) );
				Log.write( Log.DEBUG, "Adding catalog source: " + uri );
				this.sources.add( uri );
			} catch( URISyntaxException exception ) {
				Log.write( exception );
			}
		}

		return this;
	}

	public URI getIconUri() {
		return iconUri;
	}

	public void setIconUri( URI uri ) {
		this.iconUri = uri;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
		saveSettings( settings );
	}

	public URI getSourceUri() {
		return sourceUri;
	}

	public void setSourceUri( URI uri ) {
		this.sourceUri = uri;
		saveSettings( settings );
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
		saveSettings( settings );
	}

	public boolean isRemovable() {
		return removable;
	}

	public void setRemovable( boolean removable ) {
		this.removable = removable;
		saveSettings( settings );
	}

	public List<URI> getSources() {
		return new ArrayList<URI>( sources );
	}

	public void setSources( List<URI> sources ) {
		this.sources.clear();
		this.sources.addAll( sources );
		saveSettings( settings );
	}

	@Override
	public void loadSettings( Settings settings ) {
		if( settings == null ) return;

		name = settings.get( "name", null );
		enabled = settings.getBoolean( "enabled", true );
		removable = settings.getBoolean( "removable", true );

		try {
			iconUri = URI.create( settings.get( "iconUri", null ) );
		} catch( NullPointerException exception ) {
			// Intentionally ignore exception.
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
		try {
			sourceUri = URI.create( settings.get( "sourceUri", null ) );
		} catch( NullPointerException exception ) {
			// Intentionally ignore exception.
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	@Override
	public void saveSettings( Settings settings ) {
		if( settings == null ) return;

		settings.put( "name", name );
		settings.putBoolean( "enabled", enabled );
		settings.putBoolean( "removable", removable );

		settings.put( "iconUri", iconUri == null ? null : iconUri.toString() );
		settings.put( "sourceUri", sourceUri == null ? null : sourceUri.toString() );
	}

}

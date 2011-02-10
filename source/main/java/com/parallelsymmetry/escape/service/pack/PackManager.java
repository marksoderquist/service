package com.parallelsymmetry.escape.service.pack;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class PackManager implements Persistent<PackManager> {

	public static final String DEFAULT_SITE_DESCRIPTOR = "content.xml";

	public static final String DEFAULT_PACK_DESCRIPTOR = "pack.xml";

	private static final String SITE_LIST = "/sites";

	private Service service;

	private List<PackSite> sites;

	private Settings settings;

	public PackManager( Service service ) {
		this.service = service;
		sites = new CopyOnWriteArrayList<PackSite>();
	}

	public boolean newPacksDetected() throws Exception {
		return getPackUpdates().size() > 0;
	}

	public List<Pack> getAvailablePacks() throws Exception {
		for( PackSite site : sites ) {
			URI uri = site.getUri();
			if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();

			// Load the site content descriptor.
			URI siteUri = uri.resolve( DEFAULT_SITE_DESCRIPTOR );

			// If there is not a site content descriptor try a pack descriptor.
			URI packUri = uri.resolve( DEFAULT_PACK_DESCRIPTOR );
		}
		return null;
	}

	public List<Pack> getInstalledPacks() {
		List<Pack> packs = new ArrayList<Pack>();

		packs.add( service.getPack() );

		return packs;
	}

	public List<Pack> getPackUpdates() throws Exception {
		List<Pack> newPacks = new ArrayList<Pack>();
		List<Pack> oldPacks = getInstalledPacks();

		for( Pack oldPack : oldPacks ) {
			// This URI should be a direct link to the pack descriptor.
			URI uri = oldPack.getUpdateUri();
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have an update URI: " + oldPack.toString() );
				continue;
			}

			if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();

			Log.write( Log.WARN, "Pack URI: " + uri );

			Pack newPack = Pack.load( new Descriptor( uri.toString() ) );

			if( newPack.getRelease().compareTo( oldPack.getRelease() ) > 0 ) newPacks.add( newPack );
		}

		return newPacks;
	}

	public int getSiteCount() {
		return sites.size();
	}

	public PackSite getSite( int index ) {
		return sites.get( index );
	}

	public void addSite( PackSite site ) {
		sites.add( site );
		saveSettings( settings );
	}

	public void removeSite( PackSite site ) {
		sites.remove( site );
		saveSettings( settings );
	}

	@Override
	public PackManager loadSettings( Settings settings ) {
		this.settings = settings;

		List<Settings> sites = settings.getList( SITE_LIST );
		for( Settings siteSettings : sites ) {
			this.sites.add( new PackSite().loadSettings( siteSettings ) );
		}

		return this;
	}

	@Override
	public PackManager saveSettings( Settings settings ) {
		settings.removeNode( SITE_LIST );

		for( PackSite site : sites ) {
			site.saveSettings( settings.addListNode( SITE_LIST ) );
		}

		return this;
	}

}

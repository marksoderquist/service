package com.parallelsymmetry.escape.service.pack;

import java.io.IOException;
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

	private Service service;

	private List<PackSite> sites;

	private Settings settings;

	public PackManager( Service service ) {
		this.service = service;
		sites = new CopyOnWriteArrayList<PackSite>();
	}

	public boolean packsDetected() throws IOException {
		// Get all the packs.
		List<Pack> availablePacks = getAvailablePacks();
		List<Pack> installedPacks = getInstalledPacks();

		// Compare the versions.

		return false;
	}

	public List<Pack> getAvailablePacks() throws IOException {
		for( PackSite site : sites ) {

		}
		return null;
	}

	public List<Pack> getInstalledPacks() {
		List<Pack> packs = new ArrayList<Pack>();

		packs.add( service.getPack() );

		return packs;
	}

	public List<Pack> getNewPacks() throws Exception {
		List<Pack> packs = getInstalledPacks();

		for( Pack oldPack : packs ) {
			URI uri = oldPack.getUpdateUri();
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have an update URI: " + oldPack.toString() );
				continue;
			}

			Pack newPack = Pack.load( new Descriptor( uri.toString() ) );

			if( newPack.getRelease().compareTo( oldPack.getRelease() ) > 0 ) packs.add( newPack );
		}

		return packs;
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

		List<Settings> sites = settings.getList( "/sites" );
		for( Settings siteSettings : sites ) {
			this.sites.add( new PackSite().loadSettings( siteSettings ) );
		}

		return this;
	}

	@Override
	public PackManager saveSettings( Settings settings ) {
		settings.removeNode( "/sites" );

		for( PackSite site : sites ) {
			site.saveSettings( settings.addListNode( "/sites" ) );
		}

		return this;
	}

}

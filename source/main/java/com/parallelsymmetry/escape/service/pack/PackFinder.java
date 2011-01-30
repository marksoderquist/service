package com.parallelsymmetry.escape.service.pack;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class PackFinder implements Persistent<PackFinder> {

	private Set<PackSite> sites;

	private Settings settings;

	private PackFinder() {
		sites = new CopyOnWriteArraySet<PackSite>();
	}

	public void addSite( PackSite site ) {
		sites.add( site );
		saveSettings( settings );
	}

	@Override
	public PackFinder loadSettings( Settings settings ) {
		this.settings = settings;

		List<Settings> sites = settings.getList( "/sites" );
		for( Settings siteSettings : sites ) {
			this.sites.add( new PackSite().loadSettings( siteSettings ) );
		}

		return this;
	}

	@Override
	public PackFinder saveSettings( Settings settings ) {
		settings.removeNode( "/sites" );

		for( PackSite site : sites ) {
			site.saveSettings( settings.addListNode( "/sites" ) );
		}

		return this;
	}

}

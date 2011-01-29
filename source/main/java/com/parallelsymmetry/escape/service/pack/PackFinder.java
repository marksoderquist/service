package com.parallelsymmetry.escape.service.pack;

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
		// TODO Implement Persistent<PackFinder>.loadSettings().
		return null;
	}

	@Override
	public PackFinder saveSettings( Settings settings ) {
		// TODO Implement Persistent<PackFinder>.saveSettings().
		return null;
	}

}

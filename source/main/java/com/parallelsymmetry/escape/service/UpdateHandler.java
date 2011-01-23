package com.parallelsymmetry.escape.service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateHandler implements Iterable<UpdatePackage> {

	private Service service;

	private List<UpdatePackage> updates;

	public UpdateHandler( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArrayList<UpdatePackage>();
		loadUpdaterSettings();
	}

	@Override
	public Iterator<UpdatePackage> iterator() {
		return updates.iterator();
	}

	public void addUpdateItem( UpdatePackage item ) {
		updates.add( item );
		saveUpdaterSettings();
	}

	public void removeUpdateItem( UpdatePackage item ) {
		updates.remove( item );
		saveUpdaterSettings();
	}

	public boolean updatesDetected() {
		for( UpdatePackage update : updates ) {
			if( update.getSource().exists() ) return true;
		}
		return false;
	}

	public void applyUpdates() {
		// Need to start a new VM with the updater.
	}

	private void loadUpdaterSettings() {
		// TODO Load the updater settings.
		List<Settings> settings = service.getSettings().getList( "/services/update/updates" );

		Log.write( Log.WARN, "Count: " + settings.size() );

		for( Settings packageSettings : settings ) {
			Log.write( "Name: " + packageSettings.get( "/name" ) );
			Log.write( "URL: " + packageSettings.get( "/url" ) );
		}
	}

	private void saveUpdaterSettings() {
		// TODO Save the updater settings.
		Settings settings = service.getSettings();
	}

}

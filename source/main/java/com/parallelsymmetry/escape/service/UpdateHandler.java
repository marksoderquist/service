package com.parallelsymmetry.escape.service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
		List<Settings> settings = service.getSettings().getList( "/services/update/updates" );

		for( Settings packageSettings : settings ) {
			updates.add( new UpdatePackage().load( packageSettings ) );
		}
	}

	private void saveUpdaterSettings() {
		service.getSettings().removeNode( "/services/update/updates" );

		for( UpdatePackage update : updates ) {
			update.save( service.getSettings().addListNode( "/services/update/updates" ) );
		}
	}

}

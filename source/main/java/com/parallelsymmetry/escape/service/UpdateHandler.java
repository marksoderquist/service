package com.parallelsymmetry.escape.service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UpdateHandler implements Iterable<UpdatePackage> {

	private List<UpdatePackage> updates;

	public UpdateHandler() {
		updates = new CopyOnWriteArrayList<UpdatePackage>();
	}

	@Override
	public Iterator<UpdatePackage> iterator() {
		return updates.iterator();
	}

	public void addUpdateItem( UpdatePackage item ) {
		updates.add( item );
	}

	public void removeUpdateItem( UpdatePackage item ) {
		updates.remove( item );
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
	
}

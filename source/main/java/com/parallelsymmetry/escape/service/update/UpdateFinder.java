package com.parallelsymmetry.escape.service.update;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Agent to periodically look for updates.
 * 
 * @author SoderquistMV
 */
public class UpdateFinder {
	
	private Set<UpdateSource> sources;
	
	private UpdateFinder() {
		sources = new CopyOnWriteArraySet<UpdateSource>();
	}

	
	
}

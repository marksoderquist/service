package com.parallelsymmetry.escape.service.update;

import java.io.File;

import com.parallelsymmetry.escape.utility.setting.Settings;

public class StagedUpdate {

	private File source;

	private File target;

	StagedUpdate() {}

	public StagedUpdate( File source, File target ) {
		this.source = source;
		this.target = target;
	}

	public File getSource() {
		return source;
	}

	public File getTarget() {
		return target;
	}

	StagedUpdate load( Settings settings ) {
		source = new File( settings.get( "/source" ) );
		target = new File( settings.get( "/target" ) );
		return this;
	}

	StagedUpdate save( Settings settings ) {
		settings.put( "/source", source.getPath() );
		settings.put( "/target", target.getPath() );
		return this;
	}

}

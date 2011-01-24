package com.parallelsymmetry.escape.service;

import java.io.File;

import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdatePackage {

	private File source;

	private File target;

	UpdatePackage() {}

	public UpdatePackage( File source, File target ) {
		this.source = source;
		this.target = target;
	}

	public File getSource() {
		return source;
	}

	public File getTarget() {
		return target;
	}

	UpdatePackage load( Settings settings ) {
		source = new File( settings.get( "/source" ) );
		target = new File( settings.get( "/target" ) );
		return this;
	}

	UpdatePackage save( Settings settings ) {
		settings.put( "/source", source.getPath() );
		settings.put( "/target", target.getPath() );
		return this;
	}

}

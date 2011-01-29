package com.parallelsymmetry.escape.service.update;

import java.io.File;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class StagedUpdate implements Persistent<StagedUpdate>{

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

	public StagedUpdate loadSettings( Settings settings ) {
		source = new File( settings.get( "/source" ) );
		target = new File( settings.get( "/target" ) );
		return this;
	}

	public StagedUpdate saveSettings( Settings settings ) {
		settings.put( "/source", source.getPath() );
		settings.put( "/target", target.getPath() );
		return this;
	}

}

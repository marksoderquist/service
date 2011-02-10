package com.parallelsymmetry.escape.service.update;

import java.io.File;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateInfo implements Persistent<UpdateInfo>{

	private File source;

	private File target;

	public UpdateInfo() {}

	public UpdateInfo( File source, File target ) {
		this.source = source;
		this.target = target;
	}

	public File getSource() {
		return source;
	}

	public File getTarget() {
		return target;
	}

	public UpdateInfo loadSettings( Settings settings ) {
		source = new File( settings.get( "/source" ) );
		target = new File( settings.get( "/target" ) );
		return this;
	}

	public UpdateInfo saveSettings( Settings settings ) {
		settings.put( "/source", source.getPath() );
		settings.put( "/target", target.getPath() );
		return this;
	}

}

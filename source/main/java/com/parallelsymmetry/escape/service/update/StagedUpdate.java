package com.parallelsymmetry.escape.service.update;

import java.io.File;

import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class StagedUpdate implements Persistent {

	private File source;

	private File target;

	public StagedUpdate() {}

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

	public void loadSettings( Settings settings ) {
		String sourcePath = settings.get( "source", null );
		String targetPath = settings.get( "target", null );
		source = sourcePath == null ? null : new File( sourcePath );
		target = targetPath == null ? null : new File( targetPath );
	}

	public void saveSettings( Settings settings ) {
		settings.put( "source", source.getPath() );
		settings.put( "target", target.getPath() );
	}

}

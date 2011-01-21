package com.parallelsymmetry.escape.service;

import java.io.File;

public class UpdatePackage {

	private File source;

	private File target;

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

}

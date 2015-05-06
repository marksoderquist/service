package com.parallelsymmetry.service.product;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import com.parallelsymmetry.utility.log.Log;

public class ProductClassLoader extends URLClassLoader {

	private URI codebase;

	private ClassLoader parent;

	public ProductClassLoader( URL[] urls, ClassLoader parent, URI codebase ) {
		super( urls, null );
		this.codebase = codebase;
		this.parent = parent;
	}

	/**
	 * Change the default class loader behavior to load module classes from the
	 * module class loader first then delegate to the parent class loader if the
	 * class could not be found.
	 */
	@Override
	public Class<?> loadClass( final String name ) throws ClassNotFoundException {
		Class<?> type = null;

		ClassNotFoundException exception = null;

		if( type == null ) {
			try {
				type = super.loadClass( name, true );
			} catch( ClassNotFoundException cnf ) {
				exception = cnf;
			}
		}

		if( type == null ) {
			try {
				type = parent.loadClass( name );
			} catch( ClassNotFoundException cnf ) {
				exception = cnf;
			}
		}

		if( type == null ) {
			throw ( exception == null ? new ClassNotFoundException( name ) : exception );
		} else {
			resolveClass( type );
		}

		return type;
	}

	/**
	 * Used to find native library files used with modules. This allows a module
	 * to package needed native libraries in the module and be loaded at runtime.
	 */
	@Override
	protected String findLibrary( String libname ) {
		Log.write( Log.DEVEL, "Codebase URI: " + codebase );
		URI uri = codebase.resolve( System.mapLibraryName( libname ) );
		//Log.write( Log.DEVEL, "Library URI: " + uri );
		File file = new File( uri );

		if( file.exists() ) Log.write( Log.DEVEL, "Library found: " + file.toURI() );

		return file.exists() ? file.toString() : super.findLibrary( libname );
	}

	// /home/arco/Projects/psm/module/openderby/target/main/java/librxtxSerial.so
	// /home/arco/Projects/psm/module/openderby/target/main/java/librxtxSerial.so

}

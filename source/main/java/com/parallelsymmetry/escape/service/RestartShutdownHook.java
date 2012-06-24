package com.parallelsymmetry.escape.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.List;

import com.parallelsymmetry.escape.utility.JavaUtil;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.log.Log;

public class RestartShutdownHook extends Thread {

	private volatile ProcessBuilder builder;

	private Service service;

	public RestartShutdownHook( Service service ) {
		this.service = service;

		builder = new ProcessBuilder( OperatingSystem.isWindows() ? "javaw" : "java" );
		builder.directory( new File( System.getProperty( "user.dir" ) ) );

		// Add the VM parameters to the commands.
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		List<String> commands = runtimeBean.getInputArguments();
		for( String command : commands ) {
			builder.command().add( command );
		}

		// Add the classpath information.
		List<URI> uris = JavaUtil.getClasspath();
		boolean jar = uris.size() == 1 && uris.get( 0 ).getPath().endsWith( ".jar" );
		if( jar ) {
			builder.command().add( "-jar" );
		} else {
			builder.command().add( "-cp" );
		}
		builder.command().add( runtimeBean.getClassPath() );
		if( !jar ) builder.command().add( service.getClass().getName() );

		// Add the original command line parameters.
		for( String command : service.getParameters().getCommands() ) {
			builder.command().add( command );
		}

		Log.write( Log.DEBUG, TextUtil.toString( builder.command(), " " ) );
	}

	@Override
	public void run() {
		if( builder == null ) return;

		try {
			builder.start();
		} catch( IOException exception ) {
			service.error( exception );
		}
	}

}

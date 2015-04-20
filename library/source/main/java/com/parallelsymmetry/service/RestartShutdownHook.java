package com.parallelsymmetry.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.parallelsymmetry.utility.JavaUtil;
import com.parallelsymmetry.utility.OperatingSystem;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;

public class RestartShutdownHook extends Thread {

	private volatile ProcessBuilder builder;

	public RestartShutdownHook( Service service, String... commands ) {
		super( "Restart Hook" );

		builder = new ProcessBuilder( OperatingSystem.getJavaExecutablePath() );
		builder.directory( new File( System.getProperty( "user.dir" ) ) );

		// Add the VM parameters to the commands.
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		for( String command : runtimeBean.getInputArguments() ) {
			if( !builder.command().contains( command ) ) builder.command().add( command );
		}

		// Add the classpath information.
		List<URI> classpath = JavaUtil.getClasspath();
		boolean jar = classpath.size() == 1 && classpath.get( 0 ).getPath().endsWith( ".jar" );
		if( jar ) {
			builder.command().add( "-jar" );
		} else {
			builder.command().add( "-cp" );
		}
		builder.command().add( runtimeBean.getClassPath() );
		if( !jar ) builder.command().add( service.getClass().getName() );

		Parameters overrideParameters = Parameters.parse( commands );

		// Collect program flags.
		Map<String, List<String>> flags = new HashMap<String, List<String>>();
		for( String name : service.getParameters().getFlags() ) {
			flags.put( name, service.getParameters().getValues( name ) );
		}
		for( String name : overrideParameters.getFlags() ) {
			flags.put( name, overrideParameters.getValues( name ) );
		}

		// Collect program URIs.
		List<String> uris = new ArrayList<String>();
		for( String uri : service.getParameters().getUris() ) {
			if( !uris.contains( uri ) ) uris.add( uri );
		}
		for( String uri : overrideParameters.getUris() ) {
			if( !uris.contains( uri ) ) uris.add( uri );
		}

		// Add the collected flags.
		for( String flag : flags.keySet() ) {
			builder.command().add( flag );
			for( String value : flags.get( flag ) ) {
				builder.command().add( value );
			}
		}

		// Add the collected URIs.
		if( uris.size() > 0 ) {
			builder.command().add( "--" );
			for( String uri : uris ) {
				builder.command().add( uri );
			}
		}

		Log.write( Log.TRACE, "Restart command: ", TextUtil.toString( builder.command(), " " ) );
	}

	@Override
	public void run() {
		if( builder == null ) return;

		try {
			builder.start();
		} catch( IOException exception ) {
			Log.write( exception );
		}
	}

}

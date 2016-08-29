package com.parallelsymmetry.service;

import com.parallelsymmetry.service.product.ProductUpdate;
import com.parallelsymmetry.updater.UpdaterFlag;
import com.parallelsymmetry.utility.JavaUtil;
import com.parallelsymmetry.utility.OperatingSystem;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static com.parallelsymmetry.service.RestartShutdownHook.getRestartExecutablePath;

/**
 * This shutdown hook is used when the program detects that there are updates
 * have been staged to be applied as the program starts. The program then
 * registers an instance of this shutdown hook, and stops the program, which
 * triggers this shutdown hook to start the update program.
 *
 * @author soderquistmv
 */
public class UpdateShutdownHook extends Thread {

	private volatile ProcessBuilder builder;

	public UpdateShutdownHook( Service service, Map<String, ProductUpdate> updates, File updaterTarget, File updaterLogFile, String... commands ) throws URISyntaxException, IOException {
		super( "Update Hook" );

		builder = new ProcessBuilder( OperatingSystem.getJavaExecutablePath() );
		builder.directory( updaterTarget.getParentFile() );

		builder.command().add( "-jar" );
		builder.command().add( updaterTarget.toString() );

		// Specify where to put the updater log.
		builder.command().add( LogFlag.LOG_FILE );
		builder.command().add( updaterLogFile.getAbsolutePath() );
		builder.command().add( LogFlag.LOG_DATE );
		builder.command().add( LogFlag.LOG_FILE_APPEND );
		builder.command().add( LogFlag.LOG_LEVEL );
		builder.command().add( Log.getLevel().getName() );

		// Add the specified commands.
		for( String command : commands ) {
			builder.command().add( command );
		}

		// Add the updates.
		builder.command().add( UpdaterFlag.UPDATE );
		for( ProductUpdate update : updates.values() ) {
			builder.command().add( update.getSource().getAbsolutePath() );
			builder.command().add( update.getTarget().getAbsolutePath() );
		}

		// Add the launch parameters.
		builder.command().add( UpdaterFlag.LAUNCH );
		builder.command().add( getRestartExecutablePath() );

		if( !OperatingSystem.isWindows() ) {
			// Add the VM parameters to the commands.
			RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
			List<String> runtimeFlags = runtimeBean.getInputArguments();
			for( String flag : runtimeFlags ) {
				if( flag.startsWith( Parameters.SINGLE ) ) {
					builder.command().add( "\\" + flag );
				} else {
					builder.command().add( flag );
				}
			}

			// Add the classpath information.
			List<URI> uris = JavaUtil.parseClasspath( runtimeBean.getClassPath() );
			if( uris.size() == 1 && uris.get( 0 ).getPath().endsWith( ".jar" ) ) {
				builder.command().add( "\\-jar" );
			} else {
				builder.command().add( "\\-cp" );
			}
			builder.command().add( runtimeBean.getClassPath() );
		}

		// Add the original command line parameters.
		for( String command : service.getParameters().getOriginalCommands() ) {
			if( command.startsWith( Parameters.SINGLE ) ) {
				builder.command().add( "\\" + command );
			} else {
				builder.command().add( command );
			}
		}

		builder.command().add( UpdaterFlag.LAUNCH_HOME );
		builder.command().add( System.getProperty( "user.dir" ) );

		Log.write( Log.TRACE, "Update command: " + TextUtil.toString( builder.command(), " " ) );
	}

	@Override
	public void run() {
		if( builder == null ) return;

		try {
			builder.start();
			System.out.println( "Update process started." );
		} catch( IOException exception ) {
			Log.write( exception );
		}
	}

}

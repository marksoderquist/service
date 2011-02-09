package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateManager implements Persistent<UpdateManager> {

	private Service service;

	private List<UpdateInfo> updates;

	private Settings settings;

	private File updater;

	public UpdateManager( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArrayList<UpdateInfo>();
		updater = new File( service.getHomeFolder(), "updater.jar" );
	}

	public boolean hasUpdates() {
		Set<UpdateInfo> staged = new HashSet<UpdateInfo>();
		Set<UpdateInfo> cleanup = new HashSet<UpdateInfo>();

		for( UpdateInfo update : updates ) {
			if( update.getSource().exists() ) {
				staged.add( update );
			} else {
				cleanup.add( update );
			}
		}

		for( UpdateInfo update : cleanup ) {
			updates.remove( update );
		}
		if( cleanup.size() > 0 ) saveSettings( settings );

		return staged.size() > 0;
	}

	public void applyUpdates() throws Exception {
		Log.write( Log.DEBUG, "Starting update process..." );

		// Copy the updater to a temporary location.
		File updaterSource = updater;
		File updaterTarget = new File( FileUtil.TEMP_FOLDER, service.getArtifact() + "-updater.jar" );

		if( !updaterSource.exists() ) throw new RuntimeException( "Update library not found: " + updaterSource );
		if( !FileUtil.copy( updaterSource, updaterTarget ) ) throw new RuntimeException( "Update library not staged: " + updaterTarget );

		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		List<String> commands = runtimeBean.getInputArguments();

		// Start the updater in a new JVM.
		ProcessBuilder builder = new ProcessBuilder();

		builder.directory( updaterTarget.getParentFile() );

		builder.command().add( "java" );
		builder.command().add( "-jar" );
		builder.command().add( updaterTarget.toString() );

		// TODO The logging should be configurable.
		builder.command().add( "-log.file" );
		builder.command().add( new File( "updater.log" ).getAbsolutePath() );
		builder.command().add( "-log.append" );

		// Add the updates.
		builder.command().add( "--update" );
		for( UpdateInfo update : updates ) {
			builder.command().add( update.getSource().getAbsolutePath() );
			builder.command().add( update.getTarget().getAbsolutePath() );
		}

		// FIXME The launch parameters should come from the original command line.
		builder.command().add( "--launch" );
		builder.command().add( "java" );
		builder.command().add( "\\-jar" );
		for( String command : commands ) {
			if( command.startsWith( Parameters.SINGLE ) ) {
				builder.command().add( "\\" + command );
			} else {
				builder.command().add( command );
			}
		}

		builder.command().add( "verify.jar" );
		for( String command : service.getParameters().getCommands() ) {
			if( command.startsWith( Parameters.SINGLE ) ) {
				builder.command().add( "\\" + command );
			} else {
				builder.command().add( command );
			}
		}

		builder.command().add( "\\-update" );
		builder.command().add( "false" );

		builder.command().add( "-launch.home" );
		builder.command().add( System.getProperty( "user.dir" ) );

		// Print the process commands.
		Log.write( Log.DEBUG, "Launching: " + TextUtil.toString( builder.command(), " " ) );

		builder.start();
		Log.write( Log.TRACE, "Update process started." );
		
		// Remove the updates settings.
		updates.clear();
		saveSettings( settings );

		// The program should be allowed, but not forced, to exit at this point.
		Log.write( "Program exiting to allow updates to be processed." );
	}

	public void addUpdateItem( UpdateInfo item ) {
		updates.add( item );
		saveSettings( settings );
	}

	public void removeUpdateItem( UpdateInfo item ) {
		updates.remove( item );
		saveSettings( settings );
	}

	/**
	 * Get the path to the updater library.
	 * 
	 * @return
	 */
	public File getUpdaterPath() {
		return updater;
	}

	/**
	 * Get the path to the updater library.
	 * 
	 * @param file
	 */
	public void setUpdaterPath( File file ) {
		this.updater = file;
	}

	@Override
	public UpdateManager loadSettings( Settings settings ) {
		this.settings = settings;

		List<Settings> updates = settings.getList( "/updates" );
		for( Settings updateSettings : updates ) {
			this.updates.add( new UpdateInfo().loadSettings( updateSettings ) );
		}

		return this;
	}

	@Override
	public UpdateManager saveSettings( Settings settings ) {
		settings.removeNode( "/updates" );

		for( UpdateInfo update : updates ) {
			update.saveSettings( settings.addListNode( "/updates" ) );
		}

		return this;
	}

}

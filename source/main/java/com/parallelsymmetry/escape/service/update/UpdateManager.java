package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.Iterator;
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

public class UpdateManager implements Iterable<UpdateInfo>, Persistent<UpdateManager> {

	private static final String UPDATER = "lib/updater-standalone.jar";

	private Service service;

	private List<UpdateInfo> updates;

	private Settings settings;

	public UpdateManager( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArrayList<UpdateInfo>();
	}

	@Override
	public Iterator<UpdateInfo> iterator() {
		return updates.iterator();
	}

	public void addUpdateItem( UpdateInfo item ) {
		updates.add( item );
		saveSettings( settings );
	}

	public void removeUpdateItem( UpdateInfo item ) {
		updates.remove( item );
		saveSettings( settings );
	}

	public boolean updatesDetected() {
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
		File updaterSource = new File( service.getHomeFolder(), UPDATER );
		File updaterTarget = new File( FileUtil.TEMP_FOLDER, service.getArtifact() + "-updater.jar" );

		if( !FileUtil.copy( updaterSource, updaterTarget ) ) {
			Log.write( Log.WARN, "Update library not staged, update aborted." );
			return;
		}

		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		List<String> commands = runtimeBean.getInputArguments();

		// Start the updater in a new JVM.
		ProcessBuilder builder = new ProcessBuilder();

		builder.directory( updaterTarget.getParentFile() );

		builder.command().add( "java" );
		builder.command().add( "-jar" );
		builder.command().add( updaterTarget.toString() );
		builder.command().add( "-log.file" );
		builder.command().add( new File( "updater.log.txt" ).getAbsolutePath() );
		builder.command().add( "--update" );
		for( UpdateInfo update : updates ) {
			builder.command().add( update.getSource().getAbsolutePath() );
			builder.command().add( update.getTarget().getAbsolutePath() );
		}
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
		builder.command().add( "service.jar" );
		for( String command : service.getParameters().getCommands() ) {
			if( command.startsWith( Parameters.SINGLE ) ) {
				builder.command().add( "\\" + command );
			} else {
				builder.command().add( command );
			}
		}

		// Print the process commands.
		Log.write( Log.DEBUG, TextUtil.toString( builder.command(), " " ) );

		builder.start();
		Log.write( Log.TRACE, "Update process started." );

		// The program should be allowed, but not forced, to exit at this point.
		Log.write( "Program exiting to allow updates to be processed." );
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

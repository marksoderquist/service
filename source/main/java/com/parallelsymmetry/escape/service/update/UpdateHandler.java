package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.io.IOException;
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
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateHandler implements Iterable<StagedUpdate> {

	private static final String UPDATER = "lib/updater.jar";

	private Service service;

	private List<StagedUpdate> updates;

	public UpdateHandler( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArrayList<StagedUpdate>();
		loadSettings();
	}

	@Override
	public Iterator<StagedUpdate> iterator() {
		return updates.iterator();
	}

	public void addUpdateItem( StagedUpdate item ) {
		updates.add( item );
		saveSettings();
	}

	public void removeUpdateItem( StagedUpdate item ) {
		updates.remove( item );
		saveSettings();
	}

	public boolean updatesDetected() {
		Set<StagedUpdate> staged = new HashSet<StagedUpdate>();
		Set<StagedUpdate> cleanup = new HashSet<StagedUpdate>();

		for( StagedUpdate update : updates ) {
			if( update.getSource().exists() ) {
				staged.add( update );
			} else {
				cleanup.add( update );
			}
		}
		
		for( StagedUpdate update : cleanup ) {
			updates.remove( update );
		}
		if( cleanup.size() > 0 ) saveSettings();
		
		return staged.size() > 0;
	}

	public void applyUpdates() throws IOException {
		// Copy the updater to a temporary location.
		File updaterSource = new File( service.getHomeFolder(), UPDATER );
		File updaterTarget = new File( FileUtil.TEMP_FOLDER, service.getArtifact() + "-updater.jar" );
		FileUtil.copy( updaterSource, updaterTarget );

		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
		List<String> commands = runtimeBean.getInputArguments();

		// Start the updater in a new JVM.
		ProcessBuilder builder = new ProcessBuilder();

		builder.directory( updaterTarget.getParentFile() );

		builder.command().add( "java" );
		builder.command().add( "-jar" );
		builder.command().add( updaterTarget.toString() );
		builder.command().add( "--update" );
		for( StagedUpdate update : updates ) {
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
		for( String command : builder.command() ) {
			System.out.print( command );
			System.out.print( " " );
		}
		System.out.println();

		builder.start();

		// The program should be allowed, but not forced, to exit at this point.
	}

	private void loadSettings() {
		List<Settings> settings = service.getSettings().getList( "/services/update/updates" );

		for( Settings updateSettings : settings ) {
			updates.add( new StagedUpdate().loadSettings( updateSettings ) );
		}
	}

	private void saveSettings() {
		service.getSettings().removeNode( "/services/update/updates" );

		for( StagedUpdate update : updates ) {
			update.saveSettings( service.getSettings().addListNode( "/services/update/updates" ) );
		}
	}

}

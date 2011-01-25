package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateHandler implements Iterable<StagedUpdate> {

	private static final String UPDATER = "lib/updater.jar";

	private Service service;

	private List<StagedUpdate> updates;

	public UpdateHandler( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArrayList<StagedUpdate>();
		loadUpdaterSettings();
	}

	@Override
	public Iterator<StagedUpdate> iterator() {
		return updates.iterator();
	}

	public void addUpdateItem( StagedUpdate item ) {
		updates.add( item );
		saveUpdaterSettings();
	}

	public void removeUpdateItem( StagedUpdate item ) {
		updates.remove( item );
		saveUpdaterSettings();
	}

	public boolean updatesDetected() {
		//		for( StagedUpdate update : updates ) {
		//			if( update.getSource().exists() ) return true;
		//		}
		return updates.size() > 0;
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
		builder.command().add( "program.jar" );
		for( String command : service.getParameters().getCommands() ) {
			if( command.startsWith( Parameters.SINGLE ) ) {
				builder.command().add( "\\" + command );
			} else {
				builder.command().add( command );
			}
		}

		for( String command : builder.command() ) {
			Log.write( Log.TRACE, "Command: " + command );
		}

		builder.start();

		// The program should be allowed, but not forced, to exit at this point.
	}

	private void loadUpdaterSettings() {
		List<Settings> settings = service.getSettings().getList( "/services/update/updates" );

		for( Settings updateSettings : settings ) {
			updates.add( new StagedUpdate().load( updateSettings ) );
		}
	}

	private void saveUpdaterSettings() {
		service.getSettings().removeNode( "/services/update/updates" );

		for( StagedUpdate update : updates ) {
			update.save( service.getSettings().addListNode( "/services/update/updates" ) );
		}
	}

}

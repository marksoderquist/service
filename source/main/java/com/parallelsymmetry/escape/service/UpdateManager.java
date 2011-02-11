package com.parallelsymmetry.escape.service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.parallelsymmetry.escape.service.pack.UpdatePack;
import com.parallelsymmetry.escape.service.pack.UpdateSite;
import com.parallelsymmetry.escape.service.update.UpdateInfo;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateManager implements Persistent<UpdateManager> {

	public static final String DEFAULT_SITE_DESCRIPTOR = "content.xml";

	public static final String DEFAULT_PACK_DESCRIPTOR = "pack.xml";

	private static final String SITE_LIST = "sites";

	private static final String UPDATE_LIST = "updates";

	private Service service;

	private List<UpdateSite> sites;

	private List<UpdateInfo> updates;

	private File updater;

	private Settings settings;

	public UpdateManager( Service service ) {
		this.service = service;
		sites = new CopyOnWriteArrayList<UpdateSite>();
		updates = new CopyOnWriteArrayList<UpdateInfo>();
		updater = new File( service.getHomeFolder(), "updater.jar" );
	}

	public List<UpdatePack> getInstalledPacks() {
		List<UpdatePack> packs = new ArrayList<UpdatePack>();

		packs.add( service.getPack() );

		return packs;
	}

	public List<UpdatePack> getAvailablePacks() throws Exception {
		for( UpdateSite site : sites ) {
			URI uri = site.getUri();
			if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();

			// Load the site content descriptor.
			URI siteUri = uri.resolve( DEFAULT_SITE_DESCRIPTOR );

			// If there is not a site content descriptor try a pack descriptor.
			URI packUri = uri.resolve( DEFAULT_PACK_DESCRIPTOR );
		}
		return null;
	}

	public int getSiteCount() {
		return sites.size();
	}

	public UpdateSite getSite( int index ) {
		return sites.get( index );
	}

	public void addSite( UpdateSite site ) {
		sites.add( site );
		saveSettings( settings );
	}

	public void removeSite( UpdateSite site ) {
		sites.remove( site );
		saveSettings( settings );
	}

	public boolean areUpdatesPosted() throws Exception {
		return getPostedUpdates().size() > 0;
	}

	public List<UpdatePack> getPostedUpdates() throws Exception {
		List<UpdatePack> newPacks = new ArrayList<UpdatePack>();
		List<UpdatePack> oldPacks = getInstalledPacks();

		for( UpdatePack oldPack : oldPacks ) {
			// This URI should be a direct link to the pack descriptor.
			URI uri = oldPack.getUpdateUri();
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have an update URI: " + oldPack.toString() );
				continue;
			}

			if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();

			Log.write( Log.WARN, "Pack URI: " + uri );

			UpdatePack newPack = UpdatePack.load( new Descriptor( uri.toString() ) );

			if( newPack.getRelease().compareTo( oldPack.getRelease() ) > 0 ) newPacks.add( newPack );
		}

		return newPacks;
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

	public boolean areUpdatesStaged() {
		// Reload the settings in the event they have changed.
		Level level = Log.getLevel();
		Log.setLevel( Log.DEBUG );
		loadSettings( settings );
		Log.setLevel( level );

		Set<UpdateInfo> staged = new HashSet<UpdateInfo>();
		Set<UpdateInfo> remove = new HashSet<UpdateInfo>();

		for( UpdateInfo update : updates ) {
			if( update.getSource().exists() ) {
				staged.add( update );
				Log.write( Log.DEBUG, "Update found: " + update.getSource() );
			} else {
				remove.add( update );
				Log.write( Log.WARN, "Update missing: " + update.getSource() );
			}
		}

		// Remove updates that cannot be found.
		if( remove.size() > 0 ) {
			for( UpdateInfo update : remove ) {
				updates.remove( update );
			}
			saveSettings( settings );
		}

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
	}

	@Override
	public UpdateManager loadSettings( Settings settings ) {
		this.settings = settings;

		this.sites = settings.getList( UpdateSite.class, SITE_LIST );
		this.updates = settings.getList( UpdateInfo.class, UPDATE_LIST );

		// TODO Load the update check schedule from the settings.

		return this;
	}

	@Override
	public UpdateManager saveSettings( Settings settings ) {
		settings.removeNode( SITE_LIST );
		settings.removeNode( UPDATE_LIST );

		settings.putList( SITE_LIST, sites );
		settings.putList( UPDATE_LIST, updates );
		
		settings.flush();

		return this;
	}

}

package com.parallelsymmetry.escape.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.parallelsymmetry.escape.service.pack.UpdatePack;
import com.parallelsymmetry.escape.service.pack.UpdateSite;
import com.parallelsymmetry.escape.service.update.PackProvider;
import com.parallelsymmetry.escape.service.update.Resource;
import com.parallelsymmetry.escape.service.update.UpdateInfo;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.agent.AgentEvent;
import com.parallelsymmetry.escape.utility.agent.AgentListener;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class UpdateManager implements AgentListener, Persistent<UpdateManager> {

	private static final String CHECK_STARTUP = "check-startup";

	public static final String DEFAULT_SITE_DESCRIPTOR = "content.xml";

	public static final String DEFAULT_PACK_DESCRIPTOR = "pack.xml";

	private static final String SITE_LIST = "sites";

	private static final String UPDATE_LIST = "updates";

	private Service service;

	private List<UpdateSite> sites;

	private List<UpdateInfo> updates;

	private boolean checkForUpdatesOnStartup;

	private File updater;

	private Settings settings;

	public UpdateManager( Service service ) {
		this.service = service;
		sites = new CopyOnWriteArrayList<UpdateSite>();
		updates = new CopyOnWriteArrayList<UpdateInfo>();
		updater = new File( service.getHomeFolder(), "updater.jar" );

		service.addListener( this );
	}

	public List<UpdatePack> getInstalledPacks() {
		List<UpdatePack> packs = new ArrayList<UpdatePack>();

		packs.add( service.getPack() );

		return packs;
	}

	// TODO This method will move to a pack manager in the program library.
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
		UpdateContext context = new UpdateContext();
		return getPostedUpdates( context );
	}

	public List<UpdatePack> getPostedUpdates( UpdateContext context ) throws Exception {
		List<UpdatePack> newPacks = new ArrayList<UpdatePack>();
		List<UpdatePack> oldPacks = getInstalledPacks();

		// In order to handle exceptions correctly a context object may need to be used.
		List<IOException> exceptions = new ArrayList<IOException>();

		// TODO This loop would be more efficient if done concurrently.
		for( UpdatePack oldPack : oldPacks ) {
			// This URI should be a direct link to the pack descriptor.
			URI uri = getResolvedUpdateUri( oldPack );
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have an update URI: " + oldPack.toString() );
				continue;
			} else {
				Log.write( Log.DEBUG, "Installed pack URI: " + uri );
			}

			try {
				UpdatePack newPack = loadUpdatePack( uri );
				if( newPack.getRelease().compareTo( oldPack.getRelease() ) > 0 ) newPacks.add( newPack );
			} catch( IOException exception ) {
				exceptions.add( exception );
				Log.write( exception );
			}
		}

		return newPacks;
	}

	private static final class UpdateContext {

		public Set<UpdatePack> postedUpdates = new HashSet<UpdatePack>();

		public List<Exception> exceptions = new ArrayList<Exception>();

	}

	public void stagePostedUpdates() throws Exception {
		UpdateContext context = new UpdateContext();
		stagePostedUpdates( context );
		if( context.exceptions.size() > 0 ) throw context.exceptions.get( 0 );
	}

	public void stagePostedUpdates( UpdateContext context ) throws Exception {
		List<UpdatePack> packs = getPostedUpdates();
		getPostedUpdates( context );

		File programDataFolder = service.getProgramDataFolder();
		File stageFolder = new File( programDataFolder, "stage" );

		Log.write( Log.WARN, "Pack stage folder: " + stageFolder );

		// Determine all the resources to download.
		Map<UpdatePack, Set<Resource>> packResources = new HashMap<UpdatePack, Set<Resource>>();
		for( UpdatePack pack : packs ) {
			Log.write( Log.WARN, "Determining resources: " + getResolvedUpdateUri( pack ) );

			Set<Resource> resources = new PackProvider( pack ).getResources();

			for( Resource resource : resources ) {
				Log.write( Log.WARN, "Resource: " + resource.getUri() );
			}

			packResources.put( pack, resources );
		}

		// Download all resources, create an update for each pack, save them to the staging location, and add staged updates to list.
		// TODO Stage the update.
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

	public boolean checkForUpdatesOnStartup() {
		return checkForUpdatesOnStartup;
	}

	public void checkForUpdatesOnStartup( boolean check ) {
		checkForUpdatesOnStartup = check;
		saveSettings( settings );
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
				Log.write( Log.DEBUG, "Staged update found: " + update.getSource() );
			} else {
				remove.add( update );
				Log.write( Log.WARN, "Staged update missing: " + update.getSource() );
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

		// TODO The logging should be configurable. This is here for integration testing.
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

		// Remove the updates settings.
		updates.clear();
		saveSettings( settings );

		// Print the process commands.
		Log.write( Log.DEBUG, "Launching: " + TextUtil.toString( builder.command(), " " ) );

		builder.start();
		Log.write( Log.TRACE, "Update process started." );
	}

	@Override
	public UpdateManager loadSettings( Settings settings ) {
		this.settings = settings;

		this.checkForUpdatesOnStartup = settings.getBoolean( CHECK_STARTUP, false );
		this.sites = settings.getList( UpdateSite.class, SITE_LIST );
		this.updates = settings.getList( UpdateInfo.class, UPDATE_LIST );

		// TODO Load the update check schedule from the settings.

		return this;
	}

	@Override
	public UpdateManager saveSettings( Settings settings ) {
		settings.putBoolean( CHECK_STARTUP, checkForUpdatesOnStartup );
		settings.putList( SITE_LIST, sites );
		settings.putList( UPDATE_LIST, updates );

		settings.flush();

		return this;
	}

	@Override
	public void agentEventOccurred( AgentEvent event ) {
		if( event.getState() == Agent.State.STARTED && checkForUpdatesOnStartup() ) {
			try {
				stagePostedUpdates();
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}
	}

	public static final Descriptor loadDescriptor( URI uri ) throws IOException {
		Descriptor pack = null;

		try {
			pack = new Descriptor( uri.toString() );
		} catch( ParserConfigurationException exception ) {
			Log.write( exception );
		} catch( SAXException exception ) {
			Log.write( exception );
		}

		return pack;
	}

	public static final UpdatePack loadUpdatePack( URI uri ) throws IOException {
		return UpdatePack.load( loadDescriptor( uri ) );
	}

	private URI getResolvedUpdateUri( UpdatePack pack ) {
		URI uri = pack.getUpdateUri();
		if( uri == null ) return null;
		if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();
		return uri;
	}

}

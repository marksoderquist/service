package com.parallelsymmetry.escape.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import com.parallelsymmetry.escape.product.CardProvider;
import com.parallelsymmetry.escape.product.ProductCard;
import com.parallelsymmetry.escape.product.ProductResource;
import com.parallelsymmetry.escape.service.task.DescriptorDownloadTask;
import com.parallelsymmetry.escape.service.task.DownloadTask;
import com.parallelsymmetry.escape.updater.UpdaterFlag;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.JavaUtil;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.log.LogFlag;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.SettingEvent;
import com.parallelsymmetry.escape.utility.setting.SettingListener;
import com.parallelsymmetry.escape.utility.setting.Settings;

/**
 * The update manager handles discovery, staging and applying product updates.
 * <p>
 * Discovery involves checking for updates over the network (usually over the
 * Internet) and comparing the release information of installed packs with the
 * release information of the discovered packs. If the discovered pack is
 * determined to be newer than the installed pack it is considered an update.
 * <p>
 * Staging involves downloading new pack data and preparing it to be applied by
 * the update application.
 * <p>
 * Applying involves configuring and executing a separate update process to
 * apply the staged updates. This requires the calling process to terminate to
 * allow the update process to change required files.
 * 
 * @author SoderquistMV
 */
public class ServiceUpdateManager extends Agent implements Persistent {

	public enum CheckOption {
		DISABLED, MANUAL, STARTUP, INTERVAL, SCHEDULE
	}

	public enum FoundOption {
		SELECT, CACHESELECT, STAGE
	}

	public enum ApplyOption {
		VERIFY, SKIP, RESTART
	}

	private static final String CHECK = "check";

	private static final String FOUND = "found";

	private static final String APPLY = "apply";

	private static final String UPDATES = "updates";

	private static final String UPDATER_JAR_NAME = "updater.jar";

	private Service service;

	private CheckOption checkOption;

	private FoundOption foundOption;

	private ApplyOption applyOption;

	private File updater;

	private Settings settings;

	private Set<StagedUpdate> updates;

	private Map<String, ProductCard> installedPacks;

	private Timer timer;

	public ServiceUpdateManager( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArraySet<StagedUpdate>();
		installedPacks = new ConcurrentHashMap<String, ProductCard>();
		updater = new File( service.getHomeFolder(), UPDATER_JAR_NAME );

		checkOption = CheckOption.DISABLED;
		foundOption = FoundOption.STAGE;
		applyOption = ApplyOption.RESTART;

		service.getSettings().addSettingListener( "/update", new SettingChangeHandler() );
	}

	/**
	 * Get the installed packs. The map entry key is the product card key and the
	 * map entry value is the product card that represents the pack.
	 * 
	 * @return A map of the installed packs.
	 */
	public Map<String, ProductCard> getInstalledPacks() {
		Map<String, ProductCard> packs = new ConcurrentHashMap<String, ProductCard>();

		// Add the service pack.
		packs.put( service.getCard().getKey(), service.getCard() );

		// Add the installed packs.
		packs.putAll( installedPacks );

		return packs;
	}

	public void addInstalledPack( ProductCard card ) {
		installedPacks.put( card.getKey(), card );
	}

	public void removeInstalledPack( ProductCard card ) {
		installedPacks.remove( card.getKey() );
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

	public CheckOption getCheckOption() {
		return checkOption;
	}

	public void setCheckOption( CheckOption checkOption ) {
		this.checkOption = checkOption;
		saveSettings( settings );
	}

	public FoundOption getFoundOption() {
		return foundOption;
	}

	public void setFoundOption( FoundOption foundOption ) {
		this.foundOption = foundOption;
		saveSettings( settings );
	}

	public ApplyOption getApplyOption() {
		return applyOption;
	}

	public void setApplyOption( ApplyOption applyOption ) {
		this.applyOption = applyOption;
		saveSettings( settings );
	}

	public void checkForUpdates() {
		try {
			Log.write( Log.TRACE, "Checking for updates..." );
			if( service.getUpdateManager().stagePostedUpdates() ) {
				Log.write( Log.TRACE, "Updates staged, restarting..." );
				service.serviceRestart();
			}
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	/**
	 * Gets the set of posted product updates. If there are no posted updates
	 * found an empty set is returned.
	 * 
	 * @return The set of posted updates.
	 * @throws Exception
	 */
	public Set<ProductCard> getPostedUpdates() throws Exception {
		Set<ProductCard> newPacks = new HashSet<ProductCard>();
		if( !isEnabled() ) return newPacks;

		Log.write( Log.TRACE, "Checking for updates..." );

		Map<String, ProductCard> oldPacks = getInstalledPacks();

		Map<ProductCard, Future<Descriptor>> futures = new HashMap<ProductCard, Future<Descriptor>>();
		for( ProductCard oldPack : oldPacks.values() ) {
			URI uri = getResolvedUpdateUri( oldPack.getSourceUri() );
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have source defined: " + oldPack.toString() );
				continue;
			} else {
				Log.write( Log.DEBUG, "Installed pack source: " + uri );
			}

			futures.put( oldPack, service.getTaskManager().submit( new DescriptorDownloadTask( uri ) ) );
		}

		for( ProductCard oldPack : oldPacks.values() ) {
			Future<Descriptor> future = futures.get( oldPack );
			if( future == null ) continue;
			ProductCard newPack = ProductCard.load( future.get() );

			// Handle the development command line flag.
			boolean development = service.getParameters().isSet( ServiceFlag.DEVELOPMENT );
			if( development && oldPack.getArtifact().equals( Service.DEVELOPMENT_PREFIX + newPack.getArtifact() ) ) {
				newPack.setArtifact( Service.DEVELOPMENT_PREFIX + newPack.getArtifact() );
			}

			// Validate the pack key.
			if( !oldPack.getKey().equals( newPack.getKey() ) ) {
				Log.write( Log.WARN, "Pack mismatch: ", oldPack.getKey(), " != ", newPack.getKey() );
				continue;
			}

			Log.write( Log.DEBUG, "Old release: ", oldPack.getArtifact(), " ", oldPack.getRelease() );
			Log.write( Log.DEBUG, "New release: ", newPack.getArtifact(), " ", newPack.getRelease() );

			if( newPack.getRelease().compareTo( oldPack.getRelease() ) > 0 ) {
				Log.write( Log.TRACE, "Update found for: " + oldPack.toString() );
				newPacks.add( newPack );
			}
		}

		return newPacks;
	}

	public boolean cacheSelectedUpdates( Set<ProductCard> packs ) throws Exception {
		throw new RuntimeException( "Method not implemented yet." );
	}

	public boolean stageCachedUpdates( Set<ProductCard> packs ) throws Exception {
		throw new RuntimeException( "Method not implemented yet." );
	}

	/**
	 * Attempt to stage the product packs from posted updates.
	 * 
	 * @return true if one or more product packs were staged.
	 * @throws Exception
	 */
	public boolean stagePostedUpdates() throws Exception {
		if( !isEnabled() ) return false;

		Set<ProductCard> packs = getPostedUpdates();
		if( packs.size() == 0 ) return false;

		return stageSelectedUpdates( packs );
	}

	/**
	 * Attempt to stage the product packs described by the specified product
	 * cards.
	 * 
	 * @param cards
	 * @return true if one or more product packs were staged.
	 * @throws Exception
	 */
	public boolean stageSelectedUpdates( Set<ProductCard> cards ) throws Exception {
		File programDataFolder = service.getProgramDataFolder();
		File stageFolder = new File( programDataFolder, "stage" );
		stageFolder.mkdirs();

		Log.write( Log.DEBUG, "Pack stage folder: " + stageFolder );

		// Determine all the resources to download.
		Map<ProductCard, Set<ProductResource>> productResources = new HashMap<ProductCard, Set<ProductResource>>();
		for( ProductCard card : cards ) {
			Set<ProductResource> resources = new CardProvider( card, service.getTaskManager() ).getResources();

			for( ProductResource resource : resources ) {
				URI uri = getResolvedUpdateUri( resource.getUri() );
				Log.write( Log.DEBUG, "Resource source: " + uri );
				resource.setFuture( service.getTaskManager().submit( new DownloadTask( uri ) ) );
			}

			productResources.put( card, resources );
		}

		// Download all resources.
		for( ProductCard card : cards ) {
			for( ProductResource resource : productResources.get( card ) ) {
				resource.waitFor();
				Log.write( Log.DEBUG, "Resource target: " + resource.getLocalFile() );
			}
		}

		Map<String, ProductCard> installedPacks = getInstalledPacks();

		for( String key : installedPacks.keySet() ) {
			Log.write( Log.TRACE, "Installed pack: " + key );
		}

		// Create an update for each pack.
		for( ProductCard card : cards ) {
			ProductCard installedPack = installedPacks.get( card.getKey() );

			// Check that the pack is valid.
			if( installedPack == null || !installedPack.isInstallFolderValid() ) {
				Log.write( Log.WARN, "Pack not installed: " + card );
				continue;
			}

			File update = new File( stageFolder, card.getKey() + ".pak" );
			createUpdatePack( productResources.get( card ), update );
			updates.add( new StagedUpdate( update, installedPack.getInstallFolder() ) );
			Log.write( Log.TRACE, "Update staged: " + update );
		}
		saveSettings( settings );

		return true;
	}

	public boolean areUpdatesStaged() {
		Set<StagedUpdate> staged = new HashSet<StagedUpdate>();
		Set<StagedUpdate> remove = new HashSet<StagedUpdate>();

		for( StagedUpdate update : updates ) {
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
			for( StagedUpdate update : remove ) {
				updates.remove( update );
			}
			saveSettings( settings );
		}

		return staged.size() > 0;
	}

	/**
	 * Launch the update program to apply the staged updates. This method is
	 * generally called when the program starts and, if the update program is
	 * successfully started, the program should be terminated to allow for the
	 * updates to be applied.
	 * 
	 * @throws Exception
	 */
	public boolean applyStagedUpdates() throws Exception {
		if( !isEnabled() || updates.size() == 0 ) return false;

		if( service.getParameters().isSet( ServiceFlag.DEVELOPMENT ) ) {
			Log.write( Log.TRACE, "Running in development. Updates cannot be applied and will be cleaned up." );

			for( StagedUpdate update : updates ) {
				update.getSource().delete();
			}
		} else {
			Log.write( Log.DEBUG, "Starting update process..." );
			// Copy the updater to a temporary location.
			File updaterSource = updater;
			File updaterTarget = new File( FileUtil.TEMP_FOLDER, service.getCard().getArtifact() + "-updater.jar" );

			if( updaterSource == null || !updaterSource.exists() ) throw new RuntimeException( "Update library not found: " + updaterSource );
			if( !FileUtil.copy( updaterSource, updaterTarget ) ) throw new RuntimeException( "Update library not staged: " + updaterTarget );

			// Check if process elevation is necessary.
			boolean elevate = false;
			for( StagedUpdate update : updates ) {
				elevate |= !FileUtil.isWritable( update.getTarget() );
			}

			// Start the updater in a new JVM.
			ProcessBuilder builder = new ProcessBuilder();
			builder.directory( updaterTarget.getParentFile() );

			builder.command().add( OperatingSystem.getJavaExecutableName() );
			builder.command().add( "-jar" );
			builder.command().add( updaterTarget.toString() );

			// If file logging is enabled append the update process to the log.
			Parameters parameters = service.getParameters();
			if( parameters.isSet( LogFlag.LOG_FILE ) ) {
				builder.command().add( LogFlag.LOG_FILE );
				builder.command().add( new File( parameters.get( LogFlag.LOG_FILE ) ).getAbsolutePath() );
				if( parameters.isTrue( LogFlag.LOG_FILE_APPEND ) ) builder.command().add( LogFlag.LOG_FILE_APPEND );
			}

			// Add the updates.
			builder.command().add( UpdaterFlag.UPDATE );
			for( StagedUpdate update : updates ) {
				builder.command().add( update.getSource().getAbsolutePath() );
				builder.command().add( update.getTarget().getAbsolutePath() );
			}

			// Add the launch parameters.
			builder.command().add( UpdaterFlag.LAUNCH );
			builder.command().add( OperatingSystem.getJavaExecutableName() );

			// Add the VM parameters to the commands.
			RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
			List<String> commands = runtimeBean.getInputArguments();
			for( String command : commands ) {
				if( command.startsWith( Parameters.SINGLE ) ) {
					builder.command().add( "\\" + command );
				} else {
					builder.command().add( command );
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

			// Add the original command line parameters.
			for( String command : service.getParameters().getCommands() ) {
				if( command.startsWith( Parameters.SINGLE ) ) {
					builder.command().add( "\\" + command );
				} else {
					builder.command().add( command );
				}
			}

			builder.command().add( "\\" + ServiceFlag.UPDATE );
			builder.command().add( "false" );

			builder.command().add( UpdaterFlag.LAUNCH_HOME );
			builder.command().add( System.getProperty( "user.dir" ) );

			// Configure the builder with elevated privilege commands.
			if( elevate ) OperatingSystem.elevateProcessBuilder( builder );

			// Print the process commands.
			Log.write( Log.DEBUG, "Launching: " + TextUtil.toString( builder.command(), " " ) );

			// Start the process.
			builder.start();
			Log.write( Log.TRACE, "Update process started." );
		}

		// Remove the updates settings.
		updates.clear();
		saveSettings( settings );

		return true;
	}

	@Override
	public void loadSettings( Settings settings ) {
		this.settings = settings;

		this.checkOption = CheckOption.valueOf( settings.get( CHECK, CheckOption.DISABLED.name() ) );
		this.foundOption = FoundOption.valueOf( settings.get( FOUND, FoundOption.STAGE.name() ) );
		this.applyOption = ApplyOption.valueOf( settings.get( APPLY, ApplyOption.RESTART.name() ) );
		this.updates = settings.getSet( UPDATES, new HashSet<StagedUpdate>() );
	}

	@Override
	public void saveSettings( Settings settings ) {
		if( settings == null ) return;

		settings.put( CHECK, checkOption.name() );
		settings.put( FOUND, foundOption.name() );
		settings.put( APPLY, applyOption.name() );
		settings.putSet( UPDATES, updates );
		settings.flush();
	}

	@Override
	protected void startAgent() throws Exception {
		if( !isEnabled() ) return;

		timer = new Timer();
		scheduleCheckUpdateTask( new UpdateCheckTask( service ) );
	}

	@Override
	protected void stopAgent() throws Exception {
		if( !isEnabled() ) return;

		if( timer != null ) timer.cancel();
	}

	private boolean isEnabled() {
		return checkOption != CheckOption.DISABLED;
	}

	private URI getResolvedUpdateUri( URI uri ) {
		if( uri == null ) return null;
		if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();
		return uri;
	}

	private void createUpdatePack( Set<ProductResource> resources, File update ) throws IOException {
		File updateFolder = FileUtil.createTempFolder( "update", "folder" );

		for( ProductResource resource : resources ) {
			switch( resource.getType() ) {
				case FILE: {
					// Just copy the file.
					String path = resource.getUri().getPath();
					String name = path.substring( path.lastIndexOf( "/" ) + 1 );
					File target = new File( updateFolder, name );
					FileUtil.copy( resource.getLocalFile(), target );
					break;
				}
				case PACK: {
					// Unpack the file.
					FileUtil.unzip( resource.getLocalFile(), updateFolder );
					break;
				}
			}
		}

		FileUtil.deleteOnExit( updateFolder );

		FileUtil.zip( updateFolder, update );
	}

	private void scheduleCheckUpdateTask( UpdateCheckTask task ) {
		switch( checkOption ) {
			case INTERVAL: {
				// TODO Schedule the task by interval.
				break;
			}
			case SCHEDULE: {
				// TODO Schedule the task by schedule.
				break;
			}
		}
	}

	/**
	 * NOTE: This class is Persistent and changing the package will most likely
	 * result in a ClassNotFoundException being thrown at runtime.
	 * 
	 * @author SoderquistMV
	 */
	private static final class StagedUpdate implements Persistent {

		private File source;

		private File target;

		/*
		 * This constructor is used by the settings API via reflection.
		 */
		@SuppressWarnings( "unused" )
		public StagedUpdate() {}

		public StagedUpdate( File source, File target ) {
			this.source = source;
			this.target = target;
		}

		public File getSource() {
			return source;
		}

		public File getTarget() {
			return target;
		}

		@Override
		public void loadSettings( Settings settings ) {
			String sourcePath = settings.get( "source", null );
			String targetPath = settings.get( "target", null );
			source = sourcePath == null ? null : new File( sourcePath );
			target = targetPath == null ? null : new File( targetPath );
		}

		@Override
		public void saveSettings( Settings settings ) {
			settings.put( "source", source.getPath() );
			settings.put( "target", target.getPath() );
		}

	}

	private final class UpdateCheckTask extends TimerTask {

		private Service service;

		public UpdateCheckTask( Service service ) {
			this.service = service;
		}

		@Override
		public void run() {
			service.getUpdateManager().checkForUpdates();
		}

	}

	private final class SettingChangeHandler implements SettingListener {

		@Override
		public void settingChanged( SettingEvent event ) {
			if( CHECK.equals( event.getKey() ) ) {
				setCheckOption( CheckOption.valueOf( event.getNewValue() ) );
			}
		}

	}

}

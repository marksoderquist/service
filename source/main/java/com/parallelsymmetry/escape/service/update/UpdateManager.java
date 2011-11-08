package com.parallelsymmetry.escape.service.update;

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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.logging.Level;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.service.ServiceFlag;
import com.parallelsymmetry.escape.service.task.DownloadTask;
import com.parallelsymmetry.escape.updater.UpdaterFlag;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.JavaUtil;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.agent.AgentEvent;
import com.parallelsymmetry.escape.utility.agent.AgentListener;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.log.LogFlag;
import com.parallelsymmetry.escape.utility.setting.Persistent;
import com.parallelsymmetry.escape.utility.setting.Settings;

/**
 * The update manager handles discovery, staging and applying program updates.
 * <p>
 * Discovery involves checking for updates over the network (usually over the
 * Internet) and comparing the release information of installed packs with the
 * release information of the discovered packs. If the discovered pack is
 * determined to be newer than the installed pack it is considered an update.
 * <p>
 * Staging involves downloading new pack data and preparing it to be applied 
 * by the update application.
 * <p>
 * Applying involves configuring and executing a separate update process to 
 * apply the staged updates. This requires the calling process to terminate
 * to allow the update process to change required files.
 * 
 * @author SoderquistMV
 */
public class UpdateManager implements AgentListener, Persistent {

	private static final String CHECK_STARTUP = "check-startup";

	private static final String UPDATE_LIST = "updates";

	private Service service;

	private List<UpdateInfo> updates;

	private boolean checkForUpdatesOnStartup;

	private File updater;

	private Settings settings;

	private Set<FeaturePack> installedPacks;

	public UpdateManager( Service service ) {
		this.service = service;
		updates = new CopyOnWriteArrayList<UpdateInfo>();
		installedPacks = new CopyOnWriteArraySet<FeaturePack>();
		updater = new File( service.getHomeFolder(), "updater.jar" );

		service.addListener( this );
	}

	public List<FeaturePack> getInstalledPacks() {
		List<FeaturePack> packs = new ArrayList<FeaturePack>();

		// Add the service pack.
		packs.add( service.getPack() );

		// Add the installed packs.
		for( FeaturePack pack : installedPacks ) {
			packs.add( pack );
		}

		return packs;
	}

	public void addInstalledPack( FeaturePack pack ) {
		installedPacks.add( pack );
	}

	public void removeInstalledPack( FeaturePack pack ) {
		installedPacks.remove( pack );
	}

	/**
	 * This method returns true if the getPostedUpdates() method returns any
	 * updates.
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean areUpdatesPosted() throws Exception {
		return getPostedUpdates().size() > 0;
	}

	public List<FeaturePack> getPostedUpdates() throws Exception {
		List<FeaturePack> newPacks = new ArrayList<FeaturePack>();
		List<FeaturePack> oldPacks = getInstalledPacks();

		Map<FeaturePack, Future<Descriptor>> futures = new HashMap<FeaturePack, Future<Descriptor>>();
		for( FeaturePack oldPack : oldPacks ) {
			URI uri = getResolvedUpdateUri( oldPack.getUpdateUri() );
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have an update URI: " + oldPack.toString() );
				continue;
			} else {
				Log.write( Log.DEBUG, "Installed pack URI: " + uri );
			}

			futures.put( oldPack, service.getTaskManager().submit( new DescriptorDownload( uri ) ) );
		}

		for( FeaturePack oldPack : oldPacks ) {
			Future<Descriptor> future = futures.get( oldPack );
			if( future == null ) continue;
			FeaturePack newPack = FeaturePack.load( future.get() );

			Log.write( Log.DEBUG, "Old " + oldPack.getArtifact() + " release: " + oldPack.getRelease() );
			Log.write( Log.DEBUG, "New " + oldPack.getArtifact() + " release: " + newPack.getRelease() );

			if( newPack.getRelease().compareTo( oldPack.getRelease() ) > 0 ) {
				Log.write( Log.TRACE, "Update found for: " + oldPack.toString() );
				newPacks.add( newPack );
			}
		}

		return newPacks;
	}

	public void stagePostedUpdates() throws Exception {
		List<FeaturePack> packs = getPostedUpdates();
		File programDataFolder = service.getProgramDataFolder();
		File stageFolder = new File( programDataFolder, "stage" );
		stageFolder.mkdirs();

		Log.write( Log.DEBUG, "Pack stage folder: " + stageFolder );

		// Determine all the resources to download.
		Map<FeaturePack, Set<FeatureResource>> packResources = new HashMap<FeaturePack, Set<FeatureResource>>();
		for( FeaturePack pack : packs ) {
			Set<FeatureResource> resources = new PackProvider( service, pack ).getResources();

			for( FeatureResource resource : resources ) {
				URI uri = getResolvedUpdateUri( resource.getUri() );
				Log.write( Log.DEBUG, "Resource source: " + uri );
				resource.setFuture( service.getTaskManager().submit( new DownloadTask( uri ) ) );
			}

			packResources.put( pack, resources );
		}

		// Download all resources.
		for( FeaturePack pack : packs ) {
			for( FeatureResource resource : packResources.get( pack ) ) {
				resource.waitFor();
				Log.write( Log.DEBUG, "Resource target: " + resource.getLocalFile() );
			}
		}

		Map<String, FeaturePack> packsMap = getInstalledPacksMap();

		// Create an update for each pack.
		for( FeaturePack pack : packs ) {
			File update = new File( stageFolder, pack.getKey() + ".pak" );
			createUpdatePack( packResources.get( pack ), update );
			updates.add( new UpdateInfo( update, packsMap.get( pack.getKey() ).getInstallFolder() ) );
			Log.write( Log.WARN, "Update staged: " + update );
		}
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

		// Start the updater in a new JVM.
		ProcessBuilder builder = new ProcessBuilder();

		builder.directory( updaterTarget.getParentFile() );

		builder.command().add( "java" );
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
		for( UpdateInfo update : updates ) {
			builder.command().add( update.getSource().getAbsolutePath() );
			builder.command().add( update.getTarget().getAbsolutePath() );
		}

		// Add the launch parameters.
		builder.command().add( UpdaterFlag.LAUNCH );
		builder.command().add( "java" );

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

		// Remove the updates settings.
		updates.clear();
		saveSettings( settings );

		// Print the process commands.
		Log.write( Log.DEBUG, "Launching: " + TextUtil.toString( builder.command(), " " ) );

		builder.start();
		Log.write( Log.TRACE, "Update process started." );
	}

	@Override
	public void loadSettings( Settings settings ) {
		this.settings = settings;

		this.checkForUpdatesOnStartup = settings.getBoolean( CHECK_STARTUP, false );
		this.updates = settings.getList( UPDATE_LIST, new ArrayList<UpdateInfo>() );

		// TODO Load the check update schedule from the settings.
	}

	@Override
	public void saveSettings( Settings settings ) {
		settings.putBoolean( CHECK_STARTUP, checkForUpdatesOnStartup );
		settings.putList( UPDATE_LIST, updates );

		settings.flush();
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

	private Map<String, FeaturePack> getInstalledPacksMap() {
		List<FeaturePack> packs = getInstalledPacks();

		Map<String, FeaturePack> map = new HashMap<String, FeaturePack>();

		for( FeaturePack pack : packs ) {
			map.put( pack.getKey(), pack );
		}

		return map;
	}

	private URI getResolvedUpdateUri( URI uri ) {
		if( uri == null ) return null;
		if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();
		return uri;
	}

	private void createUpdatePack( Set<FeatureResource> resources, File update ) throws IOException {
		File updateFolder = FileUtil.createTempFolder( "update", "folder" );

		for( FeatureResource resource : resources ) {
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

}

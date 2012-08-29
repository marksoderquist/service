package com.parallelsymmetry.escape.product;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.parallelsymmetry.escape.product.ProductManagerEvent.Type;
import com.parallelsymmetry.escape.service.ProductManagerListener;
import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.service.ServiceFlag;
import com.parallelsymmetry.escape.service.task.DescriptorDownloadTask;
import com.parallelsymmetry.escape.service.task.DownloadTask;
import com.parallelsymmetry.escape.updater.UpdaterFlag;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.JavaUtil;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.UriUtil;
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

public class ProductManager extends Agent implements Persistent {

	public enum CheckOption {
		DISABLED, MANUAL, STARTUP, INTERVAL, SCHEDULE
	}

	public enum FoundOption {
		SELECT, CACHESELECT, STAGE
	}

	public enum ApplyOption {
		VERIFY, SKIP, RESTART
	}

	public static final String DEFAULT_CATALOG_FILE_NAME = "catalog.xml";

	public static final String DEFAULT_PRODUCT_FILE_NAME = "product.xml";

	public static final String PRODUCT_DESCRIPTOR_PATH = "/META-INF/" + DEFAULT_PRODUCT_FILE_NAME;

	public static final String MODULE_RESOURCE_CLASS_NAME_XPATH = ProductCard.PRODUCT_PATH + "/resources/module/@class";

	public static final String UPDATER_JAR_NAME = "updater.jar";

	public static final String UPDATE_FOLDER_NAME = "updates";

	private static final String CHECK = "check";

	private static final String FOUND = "found";

	private static final String APPLY = "apply";

	private static final String CATALOGS_SETTINGS_KEY = "catalogs";

	private static final String PRODUCT_SETTINGS_KEY = "products";

	private static final String REMOVES_SETTINGS_KEY = "removes";

	private static final String UPDATES_SETTINGS_KEY = "updates";

	private static final String PRODUCT_ENABLED_KEY = "enabled";

	private Service service;

	private Set<ProductCatalog> catalogs;

	private Map<String, ProductModule> modules;

	private Set<ClassLoader> loaders;

	private File homeModuleFolder;

	private File userProductFolder;

	private CheckOption checkOption;

	private FoundOption foundOption;

	private ApplyOption applyOption;

	private File updater;

	private Settings settings;

	private Set<StagedUpdate> updates;

	private Map<String, ProductCard> productCards;

	private Map<String, ProductState> productStates;

	private Set<String> includedProducts;

	private Timer timer;

	private UpdateCheckTask task;

	private Set<ProductManagerListener> listeners;

	// FIXME Cleanup use of the term module.

	public ProductManager( Service service ) {
		this.service = service;
		catalogs = new CopyOnWriteArraySet<ProductCatalog>();
		modules = new ConcurrentHashMap<String, ProductModule>();
		loaders = new CopyOnWriteArraySet<ClassLoader>();
		updates = new CopyOnWriteArraySet<StagedUpdate>();
		productCards = new ConcurrentHashMap<String, ProductCard>();
		productStates = new ConcurrentHashMap<String, ProductState>();
		listeners = new CopyOnWriteArraySet<ProductManagerListener>();

		includedProducts = new HashSet<String>();
		includedProducts.add( service.getCard().getProductKey() );
		includedProducts.add( "com.parallelsymmetry.escape.updater" );

		// Add service product.
		registerProduct( service.getCard() );
		setUpdatable( service.getCard(), true );
		setRemovable( service.getCard(), false );
		setEnabled( service.getCard(), true );

		// Default options.
		checkOption = CheckOption.DISABLED;
		foundOption = FoundOption.STAGE;
		applyOption = ApplyOption.RESTART;

		service.getSettings().addSettingListener( Service.PRODUCT_MANAGER_SETTINGS_PATH, new SettingChangeHandler() );
	}

	public int getCatalogCount() {
		return catalogs.size();
	}

	public void addCatalog( ProductCatalog source ) {
		catalogs.add( source );
		saveSettings( settings );
	}

	public void removeCatalog( ProductCatalog source ) {
		catalogs.remove( source );
		saveSettings( settings );
	}

	public void setCatalogEnabled( ProductCatalog catalog, boolean enabled ) {
		catalog.setEnabled( enabled );
		saveSettings( settings );
	}

	public Set<ProductCatalog> getCatalogs() {
		return new HashSet<ProductCatalog>( catalogs );
	}

	public Set<ProductModule> getModules() {
		return new HashSet<ProductModule>( modules.values() );
	}

	public Set<ProductCard> getProductCards() {
		return new HashSet<ProductCard>( productCards.values() );
	}

	public void installProducts( ProductCard... cards ) throws Exception {
		installProducts( new HashSet<ProductCard>( Arrays.asList( cards ) ) );
	}

	public void installProducts( Set<ProductCard> cards ) throws Exception {
		Log.write( Log.DEBUG, "Number of products to install: " + cards.size() );

		// Download the product resources.
		Map<ProductCard, Set<ProductResource>> productResources = downloadProductResources( cards );

		// Install the products.
		Set<InstalledProduct> installedProducts = new HashSet<InstalledProduct>();
		for( ProductCard card : cards ) {
			try {
				installedProducts.add( new InstalledProduct( getProductInstallFolder( card ) ) );
				installProductImpl( card, productResources );
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}

		Set<InstalledProduct> products = service.getSettings().getSet( REMOVES_SETTINGS_KEY, new HashSet<InstalledProduct>() );
		products.removeAll( installedProducts );
		service.getSettings().putSet( REMOVES_SETTINGS_KEY, products );
	}

	public void uninstallProducts( ProductCard... cards ) throws Exception {
		uninstallProducts( new HashSet<ProductCard>( Arrays.asList( cards ) ) );
	}

	public void uninstallProducts( Set<ProductCard> cards ) throws Exception {
		Log.write( Log.DEBUG, "Number of products to remove: " + cards.size() );

		// Remove the products.
		Set<InstalledProduct> removedProducts = new HashSet<InstalledProduct>();
		for( ProductCard card : cards ) {
			removedProducts.add( new InstalledProduct( getProductInstallFolder( card ) ) );
			removeProductImpl( card );
		}

		Set<InstalledProduct> products = service.getSettings().getSet( REMOVES_SETTINGS_KEY, new HashSet<InstalledProduct>() );
		products.addAll( removedProducts );
		service.getSettings().putSet( REMOVES_SETTINGS_KEY, products );
	}

	public boolean isInstalled( ProductCard card ) {
		return productCards.get( card.getProductKey() ) != null;
	}

	public boolean isReleaseInstalled( ProductCard card ) {
		ProductCard internal = productCards.get( card.getProductKey() );
		return internal != null && internal.getRelease().equals( card.getRelease() );
	}

	public boolean isUpdatable( ProductCard card ) {
		ProductState state = productStates.get( card.getProductKey() );
		return state != null && state.updatable;
	}

	public void setUpdatable( ProductCard card, boolean updatable ) {
		if( isUpdatable( card ) == updatable ) return;
		ProductState state = productStates.get( card.getProductKey() );
		if( state == null ) return;

		state.updatable = updatable;
		fireProductManagerEvent( new ProductManagerEvent( this, Type.PRODUCT_CHANGED, card ) );
	}

	public boolean isRemovable( ProductCard card ) {
		ProductState state = productStates.get( card.getProductKey() );
		return state != null && state.removable;
	}

	public void setRemovable( ProductCard card, boolean removable ) {
		if( isRemovable( card ) == removable ) return;
		ProductState state = productStates.get( card.getProductKey() );
		if( state == null ) return;

		state.removable = removable;
		fireProductManagerEvent( new ProductManagerEvent( this, Type.PRODUCT_CHANGED, card ) );
	}

	public boolean isEnabled( ProductCard card ) {
		return getProductSettings( card ).getBoolean( PRODUCT_ENABLED_KEY, false );
	}

	public void setEnabled( ProductCard card, boolean enabled ) {
		if( isEnabled( card ) == enabled ) return;

		setEnabledImpl( card, enabled );

		getProductSettings( card ).putBoolean( PRODUCT_ENABLED_KEY, enabled );
		fireProductManagerEvent( new ProductManagerEvent( this, enabled ? Type.PRODUCT_ENABLED : Type.PRODUCT_DISABLED, card ) );
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
		scheduleCheckUpdateTask();
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
		if( !isEnabled() ) return;

		try {
			Log.write( Log.TRACE, "Checking for updates..." );
			int stagedUpdateCount = service.getProductManager().stagePostedUpdates();
			if( stagedUpdateCount > 0 ) {
				Log.write( Log.TRACE, "Updates staged, restarting..." );
				service.serviceRestart( ServiceFlag.NOUPDATECHECK );
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

		Set<ProductCard> oldPacks = getProductCards();
		Map<ProductCard, DescriptorDownloadTask> tasks = new HashMap<ProductCard, DescriptorDownloadTask>();
		//Map<ProductCard, Future<Descriptor>> futures = new HashMap<ProductCard, Future<Descriptor>>();
		for( ProductCard oldPack : oldPacks ) {
			URI uri = getResolvedUpdateUri( oldPack.getSourceUri() );
			if( uri == null ) {
				Log.write( Log.WARN, "Installed pack does not have source defined: " + oldPack.toString() );
				continue;
			} else {
				Log.write( Log.DEBUG, "Installed pack source: " + uri );
			}

			tasks.put( oldPack, new DescriptorDownloadTask( uri ) );
			service.getTaskManager().submit( tasks.get( oldPack ) );
		}

		for( ProductCard oldPack : oldPacks ) {
			DescriptorDownloadTask task = tasks.get( oldPack );
			//Future<Descriptor> future = futures.get( oldPack );
			if( task == null ) continue;
			//if( future == null ) continue;
			Descriptor descriptor = task.get();
			ProductCard newPack = new ProductCard( task.getUri(), descriptor );

			// Validate the pack key.
			if( !oldPack.getProductKey().equals( newPack.getProductKey() ) ) {
				Log.write( Log.WARN, "Pack mismatch: ", oldPack.getProductKey(), " != ", newPack.getProductKey() );
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
	public int stagePostedUpdates() throws Exception {
		if( !isEnabled() ) return 0;
		return stageSelectedUpdates( getPostedUpdates() );
	}

	public File getProductInstallFolder( ProductCard card ) {
		File installFolder = new File( service.getProgramDataFolder(), Service.PRODUCT_INSTALL_FOLDER_NAME );
		return new File( installFolder, card.getGroup() + "." + card.getArtifact() );
	}

	/**
	 * Attempt to stage the product packs described by the specified product
	 * cards.
	 * 
	 * @param updateCards
	 * @return true if one or more product packs were staged.
	 * @throws Exception
	 */
	public int stageSelectedUpdates( Set<ProductCard> updateCards ) throws Exception {
		if( updateCards.size() == 0 ) return 0;

		File stageFolder = new File( service.getProgramDataFolder(), UPDATE_FOLDER_NAME );
		stageFolder.mkdirs();

		Log.write( Log.TRACE, "Number of packs to stage for update: " + updateCards.size() );
		Log.write( Log.DEBUG, "Pack stage folder: " + stageFolder );

		// Download the product resources.
		Map<ProductCard, Set<ProductResource>> productResources = downloadProductResources( updateCards );

		// Create an update for each product.
		for( ProductCard updateCard : updateCards ) {
			ProductCard productCard = productCards.get( updateCard.getProductKey() );

			File installFolder = productCard.getInstallFolder();
			boolean installFolderValid = installFolder != null && installFolder.exists();

			if( !installFolderValid ) {
				Log.write( Log.ERROR, "Error staging update for: " + productCard.getProductKey() );
				Log.write( Log.ERROR, "Invalid install folder: " + installFolder );
			}

			// Check that the product is known and installed.
			if( productCard == null || !installFolderValid ) {
				Log.write( Log.WARN, "Update not staged: " + updateCard );
				continue;
			}

			File updatePack = new File( stageFolder, getStagedUpdateFileName( updateCard ) );
			createUpdatePack( productResources.get( updateCard ), updatePack );
			updates.add( new StagedUpdate( updatePack, installFolder ) );
			Log.write( Log.TRACE, "Update staged: " + updatePack );
		}
		saveSettings( settings );

		return updates.size();
	}

	public String getStagedUpdateFileName( ProductCard card ) {
		return card.getGroup() + "." + card.getArtifact() + ".pak";
	}

	public boolean areUpdatesStaged() {
		return getStagedUpdateCount() > 0;
	}

	public int getStagedUpdateCount() {
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

		return staged.size();
	}

	/**
	 * Launch the update program to apply the staged updates. This method is
	 * generally called when the program starts and, if the update program is
	 * successfully started, the program should be terminated to allow for the
	 * updates to be applied.
	 * 
	 * @throws Exception
	 */
	public int applyStagedUpdates() throws Exception {
		if( !isEnabled() || updates.size() == 0 ) return 0;

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

		builder.command().add( UpdaterFlag.LAUNCH_HOME );
		builder.command().add( System.getProperty( "user.dir" ) );

		// Configure the builder with elevated privilege commands.
		if( elevate ) OperatingSystem.elevateProcessBuilder( builder );

		// Print the process commands.
		Log.write( Log.DEBUG, "Launching: " + TextUtil.toString( builder.command(), " " ) );

		// Start the process.
		builder.start();
		Log.write( Log.TRACE, "Update process started." );

		// Store the update count because the collection will be cleared.
		int count = updates.size();

		// Remove the updates settings.
		updates.clear();
		saveSettings( settings );

		return count;
	}

	public Settings getProductSettings( ProductCard card ) {
		return service.getSettings().getNode( PRODUCT_SETTINGS_KEY + "/" + card.getProductKey() );
	}

	public void loadModules( File... folders ) throws Exception {
		String moduleDescriptorPath = PRODUCT_DESCRIPTOR_PATH.startsWith( "/" ) ? PRODUCT_DESCRIPTOR_PATH.substring( 1 ) : PRODUCT_DESCRIPTOR_PATH;

		ClassLoader parent = getClass().getClassLoader();

		// Look for modules on the classpath.
		Enumeration<URL> urls = parent.getResources( moduleDescriptorPath );
		while( urls.hasMoreElements() ) {
			URI uri = urls.nextElement().toURI();
			ProductCard card = new ProductCard( UriUtil.getParent( uri ), new Descriptor( uri ) );
			loadClasspathModule( card, UriUtil.getParent( uri ), parent );
		}

		// Look for modules in the specified folders.
		for( File folder : folders ) {
			if( !folder.exists() ) continue;
			if( !folder.isDirectory() ) continue;

			// Look for simple modules (not common).
			File[] jars = folder.listFiles( FileUtil.JAR_FILE_FILTER );
			for( File jar : jars ) {
				Log.write( Log.DEBUG, "Searching for simple module: " + jar.toURI() );
				URI uri = URI.create( "jar:" + jar.toURI().toASCIIString() + "!" + PRODUCT_DESCRIPTOR_PATH );
				ProductCard card = new ProductCard( jar.getParentFile().toURI(), new Descriptor( uri ) );
				loadSimpleModule( card, jar.toURI(), parent );
			}

			// Look for complex modules (most common).
			File[] moduleFolders = folder.listFiles( FileUtil.FOLDER_FILTER );
			for( File moduleFolder : moduleFolders ) {
				Log.write( Log.DEBUG, "Searching for complex module: " + moduleFolder.toURI() );

				jars = moduleFolder.listFiles( FileUtil.JAR_FILE_FILTER );
				for( File jar : jars ) {
					try {
						URI uri = URI.create( "jar:" + jar.toURI().toASCIIString() + "!" + PRODUCT_DESCRIPTOR_PATH );
						ProductCard card = new ProductCard( jar.getParentFile().toURI(), new Descriptor( uri ) );
						loadComplexModule( card, moduleFolder.toURI(), parent );
					} catch( FileNotFoundException exception ) {
						// Not finding a product card is a common situation with dependencies.
					} catch( Throwable throwable ) {
						Log.write( throwable, jar );
					}
				}
			}
		}
	}

	public Class<?> getClassForName( String name ) throws ClassNotFoundException {
		Class<?> clazz = null;

		try {
			clazz = Class.forName( name );
		} catch( ClassNotFoundException exception ) {
			// Intentionally ignore exception.
		}

		// FIXME There is potential for class name/version conflicts with this implementation.
		if( clazz == null ) {
			for( ClassLoader loader : loaders ) {
				try {
					clazz = Class.forName( name, true, loader );
				} catch( NoClassDefFoundError error ) {
					// Intentionally ignore exception.
				} catch( ClassNotFoundException exception ) {
					// Intentionally ignore exception.
				}
				if( clazz != null ) break;
			}
		}

		if( clazz == null ) throw new ClassNotFoundException( name );

		return clazz;
	}

	public void addProductManagerListener( ProductManagerListener listener ) {
		listeners.add( listener );
	}

	public void removeProductManagerListener( ProductManagerListener listener ) {
		listeners.remove( listener );
	}

	@Override
	public void loadSettings( Settings settings ) {
		this.settings = settings;

		this.catalogs = settings.getSet( CATALOGS_SETTINGS_KEY, this.catalogs );
		this.updates = settings.getSet( UPDATES_SETTINGS_KEY, this.updates );

		this.checkOption = CheckOption.valueOf( settings.get( CHECK, CheckOption.DISABLED.name() ) );
		this.foundOption = FoundOption.valueOf( settings.get( FOUND, FoundOption.STAGE.name() ) );
		this.applyOption = ApplyOption.valueOf( settings.get( APPLY, ApplyOption.RESTART.name() ) );
	}

	@Override
	public void saveSettings( Settings settings ) {
		if( settings == null ) return;

		settings.putSet( CATALOGS_SETTINGS_KEY, catalogs );
		settings.putSet( UPDATES_SETTINGS_KEY, updates );

		settings.put( CHECK, checkOption.name() );
		settings.put( FOUND, foundOption.name() );
		settings.put( APPLY, applyOption.name() );

		settings.flush();
	}

	@Override
	protected void startAgent() throws Exception {
		cleanRemovedProducts();
		
		// Disable updates if the NOUPDATE flag is set.
		if( service.getParameters().isSet( ServiceFlag.NOUPDATE ) ) {
			checkOption = CheckOption.DISABLED;
			return;
		}

		// Create the update check timer.
		timer = new Timer();

		// Define the product folders.
		homeModuleFolder = new File( service.getHomeFolder(), Service.PRODUCT_INSTALL_FOLDER_NAME );
		userProductFolder = new File( service.getProgramDataFolder(), Service.PRODUCT_INSTALL_FOLDER_NAME );

		// Load products.
		loadModules( new File[] { homeModuleFolder, userProductFolder } );
	}

	@Override
	protected void stopAgent() throws Exception {
		if( timer != null ) timer.cancel();
	}

	void registerProduct( ProductCard card ) {
		productCards.put( card.getProductKey(), card );
		productStates.put( card.getProductKey(), new ProductState() );
	}

	void unregisterProduct( ProductCard card ) {
		productCards.remove( card.getProductKey() );
		productStates.remove( card.getProductKey() );
	}

	/**
	 * Schedule the check update task according to the settings. This method may
	 * safely be called as many times as necessary from any thread.
	 */
	private void scheduleCheckUpdateTask() {
		synchronized( this ) {
			if( task != null ) {
				task.cancel();
				Log.write( Log.DEBUG, "Update task cancelled." );
			}
	
			// Don't schedule tasks if the NOUPDATECHECK flag is set. 
			if( service.getParameters().isSet( ServiceFlag.NOUPDATECHECK ) ) return;
	
			switch( checkOption ) {
				case INTERVAL: {
					task = new UpdateCheckTask( service );
					// TODO Schedule the task by interval.
					break;
				}
				case SCHEDULE: {
					task = new UpdateCheckTask( service );
					// TODO Schedule the task by schedule.
					break;
				}
			}
	
			Log.write( Log.DEBUG, "Update task scheduled." );
		}
	}

	private void installProductImpl( ProductCard card, Map<ProductCard, Set<ProductResource>> productResources ) throws Exception {
		File installFolder = getProductInstallFolder( card );

		Log.write( Log.TRACE, "Install product to: " + installFolder );
		Set<ProductResource> resources = productResources.get( card );

		// Install all the resource files to the install folder.
		copyProductResources( resources, installFolder );

		// Load the product.
		loadModules( userProductFolder );

		// Set the enabled state.
		setEnabledImpl( card, isEnabled( card ) );

		// Notify listeners of install.
		fireProductManagerEvent( new ProductManagerEvent( this, Type.PRODUCT_INSTALLED, card ) );

		// Enable the product.
		setEnabled( card, true );
	}

	private void removeProductImpl( ProductCard card ) {
		File installFolder = getProductInstallFolder( card );

		Log.write( Log.TRACE, "Remove product from: " + installFolder );

		// Disable the product.
		setEnabled( card, false );

		// Remove the module.
		modules.remove( card.getProductKey() );

		// Remove the product from the manager.
		unregisterProduct( card );

		// Remove the product settings.
		getProductSettings( card ).removeNode();

		// Notify listeners of remove.
		fireProductManagerEvent( new ProductManagerEvent( this, Type.PRODUCT_REMOVED, card ) );
	}

	private void setEnabledImpl( ProductCard card, boolean enabled ) {
		ProductModule module = modules.get( card.getProductKey() );
		if( module != null ) {
			if( enabled ) {
				enableModule( module );
			} else {
				disableModule( module );
			}
		}
	}

	private void enableModule( ProductModule module ) {
		loaders.add( module.getClass().getClassLoader() );

		try {
			module.register();
			module.create();
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	private void disableModule( ProductModule module ) {
		try {
			module.destroy();
			module.unregister();
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}

		loaders.remove( module.getClass().getClassLoader() );
	}

	private boolean isEnabled() {
		return checkOption != CheckOption.DISABLED;
	}

	private void cleanRemovedProducts() {
		// Check for products marked for removal and remove the files.
		Set<InstalledProduct> products = service.getSettings().getSet( REMOVES_SETTINGS_KEY, new HashSet<InstalledProduct>() );
		for( InstalledProduct product : products ) {
			FileUtil.delete( product.getTarget() );
		}
		service.getSettings().removeNode( REMOVES_SETTINGS_KEY );
	}

	private URI getResolvedUpdateUri( URI uri ) {
		if( uri == null ) return null;
		if( uri.getScheme() == null ) uri = new File( uri.getPath() ).toURI();
		return uri;
	}

	private void createUpdatePack( Set<ProductResource> resources, File update ) throws IOException {
		File updateFolder = FileUtil.createTempFolder( "update", "folder" );

		copyProductResources( resources, updateFolder );

		FileUtil.deleteOnExit( updateFolder );

		FileUtil.zip( updateFolder, update );
	}

	private void copyProductResources( Set<ProductResource> resources, File folder ) throws IOException {
		for( ProductResource resource : resources ) {
			if( resource.getLocalFile() == null ) continue;
			switch( resource.getType() ) {
				case FILE: {
					// Just copy the file.
					String path = resource.getUri().getPath();
					String name = path.substring( path.lastIndexOf( "/" ) + 1 );
					File target = new File( folder, name );
					FileUtil.copy( resource.getLocalFile(), target );
					break;
				}
				case PACK: {
					// Unpack the file.
					FileUtil.unzip( resource.getLocalFile(), folder );
					break;
				}
			}
		}
	}

	private Map<ProductCard, Set<ProductResource>> downloadProductResources( Set<ProductCard> cards ) {
		// Determine all the resources to download.
		Map<ProductCard, Set<ProductResource>> productResources = new HashMap<ProductCard, Set<ProductResource>>();
		for( ProductCard card : cards ) {
			try {
				Set<ProductResource> resources = new PackProvider( card, service.getTaskManager() ).getResources();

				for( ProductResource resource : resources ) {
					URI uri = getResolvedUpdateUri( resource.getUri() );
					Log.write( Log.DEBUG, "Resource source: " + uri );
					resource.setFuture( service.getTaskManager().submit( new DownloadTask( uri ) ) );
				}

				productResources.put( card, resources );
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}

		// Download all resources.
		for( ProductCard card : cards ) {
			for( ProductResource resource : productResources.get( card ) ) {
				try {
					resource.waitFor();
					Log.write( Log.DEBUG, "Resource target: " + resource.getLocalFile() );
				} catch( Exception exception ) {
					Log.write( exception );
				}
			}
		}

		return productResources;
	}

	/**
	 * A classpath module is found on the classpath.
	 * 
	 * @param descriptor
	 * @param codebase
	 * @param parent
	 * @return
	 * @throws Exception
	 */
	private ProductModule loadClasspathModule( ProductCard card, URI codebase, ClassLoader parent ) throws Exception {
		ClassLoader loader = new ModuleClassLoader( codebase, new URL[0], parent );
		ProductModule module = loadModule( card, loader, false, false );
		return module;
	}

	/**
	 * A simple module is entirely contained inside a jar file.
	 * 
	 * @param descriptor
	 * @param jarUri
	 * @param parent
	 * @return
	 * @throws Exception
	 */
	private ProductModule loadSimpleModule( ProductCard card, URI jarUri, ClassLoader parent ) throws Exception {
		URI codebase = UriUtil.getParent( jarUri );

		// Get the jar file.
		File jarfile = new File( jarUri );
		card.setInstallFolder( jarfile.getParentFile() );

		// Create the class loader.
		ClassLoader loader = new ModuleClassLoader( codebase, new URL[] { jarfile.toURI().toURL() }, parent );
		return loadModule( card, loader, true, true );
	}

	/**
	 * A complex module, the most common, is entirely contained in a folder.
	 * 
	 * @param descriptor
	 * @param moduleFolderUri
	 * @param parent
	 * @return
	 * @throws Exception
	 */
	private ProductModule loadComplexModule( ProductCard card, URI moduleFolderUri, ClassLoader parent ) throws Exception {
		// Get the folder to load from.
		File folder = new File( moduleFolderUri );
		card.setInstallFolder( folder );

		// Find all the jars.
		Set<URL> urls = new HashSet<URL>();
		File[] files = folder.listFiles( FileUtil.JAR_FILE_FILTER );
		for( File file : files ) {
			urls.add( file.toURI().toURL() );
		}

		// Create the class loader.
		ClassLoader loader = new ModuleClassLoader( moduleFolderUri, urls.toArray( new URL[urls.size()] ), parent );
		return loadModule( card, loader, true, true );
	}

	private ProductModule loadModule( ProductCard card, ClassLoader loader, boolean updatable, boolean removable ) throws Exception {
		// Ignore included products.
		if( includedProducts.contains( card.getProductKey() ) ) return null;

		// Register the product.
		registerProduct( card );
		Log.write( Log.DEBUG, "Loading module: " + card.getProductKey() );

		// Validate class name.
		String className = card.getDescriptor().getValue( MODULE_RESOURCE_CLASS_NAME_XPATH );
		if( className == null ) return null;

		// Check if module is already loaded.
		ProductModule module = modules.get( card.getProductKey() );
		if( module != null ) return module;

		// Load the module.
		try {
			Class<?> moduleClass = loader.loadClass( className );
			Constructor<?> constructor = moduleClass.getConstructor( ProductCard.class );
			module = (ProductModule)constructor.newInstance( card );
			registerModule( module, updatable, removable );
			Log.write( Log.TRACE, "Module loaded:  " + card.getProductKey() );
		} catch( Throwable throwable ) {
			Log.write( Log.WARN, "Module failed:  " + card.getProductKey() + " (" + className + ")" );
			Log.write( Log.TRACE, throwable );
			return null;
		}

		return module;
	}

	private void registerModule( ProductModule module, boolean updatable, boolean removable ) {
		ProductCard card = module.getCard();

		// Add the module to the collection.
		modules.put( card.getProductKey(), module );

		// Set the enabled flag.
		setUpdatable( card, card.getSourceUri() != null );
		setRemovable( card, true );
	}

	private void fireProductManagerEvent( ProductManagerEvent event ) {
		for( ProductManagerListener listener : listeners ) {
			listener.eventOccurred( event );
		}
	}

	private static final class ModuleClassLoader extends URLClassLoader {

		private URI codebase;

		private ClassLoader parent;

		public ModuleClassLoader( URI codebase, URL[] urls, ClassLoader parent ) {
			super( urls, null );
			this.codebase = codebase;
			this.parent = parent;
		}

		/**
		 * Change the default class loader behavior to load module classes from the
		 * parent class loader first then delegate to the module class loader if the
		 * class could not be found.
		 */
		@Override
		public Class<?> loadClass( final String name ) throws ClassNotFoundException {
			Class<?> type = null;

			if( type == null ) {
				try {
					type = parent.loadClass( name );
				} catch( Throwable error ) {
					// Intentionally ignore exception.
				}
			}

			if( type == null ) type = super.loadClass( name );

			if( type != null ) resolveClass( type );

			return type;
		}

		/**
		 * Used to find native library files used with modules. This allows a module
		 * to package needed native libraries in the module and be loaded at
		 * runtime.
		 */
		@Override
		protected String findLibrary( String libname ) {
			File file = new File( codebase.resolve( System.mapLibraryName( libname ) ) );
			return file.exists() ? file.toString() : null;
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

	/**
	 * NOTE: This class is Persistent and changing the package will most likely
	 * result in a ClassNotFoundException being thrown at runtime.
	 * 
	 * @author SoderquistMV
	 */
	static final class InstalledProduct implements Persistent {

		private File target;

		/*
		 * This constructor is used by the settings API via reflection.
		 */
		public InstalledProduct() {}

		public InstalledProduct( File target ) {
			this.target = target;
		}

		public File getTarget() {
			return target;
		}

		@Override
		public void loadSettings( Settings settings ) {
			String targetPath = settings.get( "target", null );
			target = targetPath == null ? null : new File( targetPath );
		}

		@Override
		public void saveSettings( Settings settings ) {
			settings.put( "target", target.getPath() );
		}

		@Override
		public String toString() {
			return target.toString();
		}

		@Override
		public int hashCode() {
			return target.toString().hashCode();
		}

		@Override
		public boolean equals( Object object ) {
			return this.toString().equals( object.toString() );
		}

	}

	private final class ProductState {

		public boolean updatable;

		public boolean removable;

		public ProductState() {
			this.updatable = false;
			this.removable = false;
		}

	}

	private final class UpdateCheckTask extends TimerTask {

		private Service service;

		public UpdateCheckTask( Service service ) {
			this.service = service;
		}

		@Override
		public void run() {
			service.getProductManager().checkForUpdates();
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

package com.parallelsymmetry.service;

import com.parallelsymmetry.service.product.ProductManager;
import com.parallelsymmetry.service.product.ProductUtil;
import com.parallelsymmetry.service.product.ServiceModule;
import com.parallelsymmetry.service.product.ServiceProduct;
import com.parallelsymmetry.utility.*;
import com.parallelsymmetry.utility.agent.Agent;
import com.parallelsymmetry.utility.agent.ServerAgent;
import com.parallelsymmetry.utility.agent.Worker;
import com.parallelsymmetry.utility.log.DefaultFormatter;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;
import com.parallelsymmetry.utility.product.ProductCard;
import com.parallelsymmetry.utility.product.ProductCardException;
import com.parallelsymmetry.utility.setting.*;
import com.parallelsymmetry.utility.task.Task;
import com.parallelsymmetry.utility.task.TaskManager;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

//import com.parallelsymmetry.utility.setting.PreferencesSettingsProvider;

public abstract class Service extends Agent implements ServiceProduct {

	public static final String MODULE_INSTALL_FOLDER_NAME = "modules";

	public static final String LOG_FOLDER_NAME = "logs";

	public static final String LOG_FILE_PATTERN = "program.%u.log";

	protected static final String DEFAULT_PRODUCT_PATH = "/META-INF/product.xml";

	protected static final String DEFAULT_SETTINGS_PATH = "/settings/default.map";

	private static final String JAVA_VERSION_MINIMUM = "1.6";

	private static final String PEER_LOGGER_NAME = "peer";

	//public static final String LOCALE = "locale";

	public static final String TEST_PREFIX = "$";

	public static final String DEVL_PREFIX = "#";

	private Thread shutdownHook = new ShutdownHook( this );

	private Parameters parameters = Parameters.create();

	private String execModePrefix = "";

	private BaseSettingsProvider defaultSettingProvider;

	private Settings settings;

	private Descriptor descriptor;

	private ProductCard card;

	private String javaVersionMinimum = JAVA_VERSION_MINIMUM;

	private int port;

	private Socket socket;

	private String name;

	private File home;

	private String logFilePattern;

	private PeerServer peerServer;

	private TaskManager taskManager;

	protected ProductManager productManager;

	/**
	 * Construct the service with the default descriptor path.
	 */
	public Service() {
		this( null );
	}

	/**
	 * Construct the service with the specified name and descriptor. The
	 * descriptor must conform to the product descriptor specification.
	 *
	 * @param name
	 * @param descriptor
	 */
	public Service( String name ) {
		super( name );
		this.name = name;

		try {
			URI uri = getDescriptorUri();
			Log.write( Log.DEBUG, "Product descriptor: " + uri.toString() );
			describe( uri, new Descriptor( uri ) );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		// Create the settings object.
		settings = new Settings();
		defaultSettingProvider = new BaseSettingsProvider( new MapSettingsProvider( getClass().getResourceAsStream( DEFAULT_SETTINGS_PATH ) ) );
		settings.setDefaultProvider( defaultSettingProvider );

		peerServer = new PeerServer( this );
		taskManager = new TaskManager();
		productManager = new ProductManager( this );
	}

	/**
	 * Process the command line parameters. This method is the entry point for the
	 * service and should be called from the main() method of the implementing
	 * class.
	 *
	 * @param commands The command line commands to process.
	 * @throws RuntimeException if not called from the main() method or the
	 * processInternal() method.
	 */
	public synchronized void process( String... commands ) {
		// Check the calling method.
		String source = Thread.currentThread().getStackTrace()[ 2 ].getMethodName();
		if( !"main".equals( source ) && !"processInternal".equals( source ) ) throw new RuntimeException( "This method should be called directly from the main() method!" );

		try {
			// Parse the parameters.
			try {
				parameters = Parameters.parse( commands, getValidCommandLineFlags() );
			} catch( InvalidParameterException exception ) {
				Log.write( exception );
				printHelp( null );
				return;
			}

			configureExecMode( parameters );

			// Process the parameters.
			processParameters( parameters, false );
		} catch( Throwable programThrowable ) {
			try {
				error( programThrowable );
			} catch( Throwable fatalThrowable ) {
				fatalThrowable.printStackTrace();
			}
		}
	}

	/**
	 * Intended as an entry point for calling the service internally. Particularly
	 * useful for testing purposes.
	 *
	 * @param commands
	 * @throws RuntimeException if called directly from the main() method.
	 */
	public synchronized void processInternal( String... commands ) {
		// Check the calling method.
		if( "main".equals( Thread.currentThread().getStackTrace()[ 2 ].getMethodName() ) ) throw new RuntimeException( "This method should not be called directly from the main() method!" );

		// Process the commands.
		process( commands );
	}

	@Override
	public ProductCard getCard() {
		return card;
	}

	@Override
	public Service getService() {
		return this;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public Settings getSettings() {
		return settings;
	}

	public void setDefaultSettings( SettingsProvider provider ) {
		defaultSettingProvider.setProvider( provider );
	}

	public void addDefaultSettings( SettingsProvider provider ) {
		if( provider == null ) return;
		defaultSettingProvider.addProvider( provider );
	}

	public void removeDefaultSettings( SettingsProvider provider ) {
		if( provider == null ) return;
		defaultSettingProvider.removeProvider( provider );
	}

	/**
	 * Get the home folder. If the home folder is null that means that the program
	 * is not installed locally and was most likely started with a technology like
	 * Java Web Start.
	 *
	 * @return
	 */
	public final File getHomeFolder() {
		return home;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final File getDataFolder() {
		// If this location is changed then data will need to be moved from the old location to the new location.
		return getServiceDataFolder();
	}

	public final File getServiceDataFolder() {
		// If this location is changed then data will need to be moved from the old location to the new location.
		return OperatingSystem.getUserProgramDataFolder( execModePrefix + card.getArtifact(), execModePrefix + getName() );
	}

	public final File getProductModuleDataFolder() {
		// If this location is changed then data will need to be moved from the old location to the new location.
		return new File( getServiceDataFolder(), "data" );
	}

	public final TaskManager getTaskManager() {
		return taskManager;
	}

	public final ProductManager getProductManager() {
		return productManager;
	}

	public void printHelp() {
		printHelp( "true" );
	}

	public void printHelp( String topic ) {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		if( "true".equals( topic ) ) {
			Log.write( Log.HELP, "Usage: java -jar <jar file name> [<option>...]" );
			Log.write( Log.HELP );

			Log.write( Log.HELP, "Commands:" );
			printHelpCommands();

			Log.write( Log.HELP, "Options:" );
			printHelpOptions();
		}
	}

	public void printHelpCommands() {
		Log.write( Log.HELP, "  If no command is specified the program is started." );
		Log.write( Log.HELP );
		Log.write( Log.HELP, "  -help [topic]    Show help information." );
		Log.write( Log.HELP, "  -version         Show version and copyright information only." );
		Log.write( Log.HELP );
		Log.write( Log.HELP, "  -stop            Stop the program and exit the VM." );
		Log.write( Log.HELP, "  -status          Print the program status." );
		Log.write( Log.HELP, "  -restart         Restart the program without exiting VM." );
		Log.write( Log.HELP, "  -watch           Watch an already running program." );
		Log.write( Log.HELP );
	}

	public void printHelpOptions() {
		Log.write( Log.HELP, "  -log.color           Use ANSI color in the console output." );
		Log.write( Log.HELP, "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( Log.HELP, "                       none, error, warn, info, trace, debug, all" );
	}

	public int notify( Object message ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle" ), message, JOptionPane.INFORMATION_MESSAGE );
	}

	public int notify( Object message, int messageType ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle" ), message, JOptionPane.DEFAULT_OPTION, messageType );
	}

	public int notify( Object message, int optionType, int messageType ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle" ), message, optionType, messageType, null );
	}

	public int notify( Object message, int optionType, int messageType, Icon icon ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle" ), message, optionType, messageType, icon, null, null );
	}

	public int notify( Object message, int optionType, int messageType, Icon icon, Object[] options, Object initialValue ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle" ), message, optionType, messageType, icon, options, initialValue );
	}

	public int notify( String title, Object message ) {
		return notify( title, message, JOptionPane.INFORMATION_MESSAGE );
	}

	public int notify( String title, Object message, int messageType ) {
		return notify( title, message, JOptionPane.DEFAULT_OPTION, messageType );
	}

	public int notify( String title, Object message, int optionType, int messageType ) {
		return notify( title, message, optionType, messageType, null );
	}

	public int notify( String title, Object message, int optionType, int messageType, Icon icon ) {
		return notify( title, message, optionType, messageType, icon, null, null );
	}

	public int notify( String title, Object message, int optionType, int messageType, Icon icon, Object[] options, Object initialValue ) {
		// Services do not have any interaction with users but these methods allow 
		// the service to interact with users of sub-classed programs.

		Log.write( Log.INFO, message.toString().trim() );

		return -1;
	}

	public void error( Object message ) {
		//UIManager.getString( "OptionPane.messageDialogTitle", null )
		error( UIManager.getString( "OptionPane.errorDialogTitle", null ), message, null );
	}

	public void error( Throwable throwable ) {
		error( UIManager.getString( "OptionPane.errorDialogTitle", null ), null, throwable );
	}

	public void error( Object message, Throwable throwable ) {
		error( UIManager.getString( "OptionPane.errorDialogTitle", null ), message, throwable );
	}

	public void error( String title, Object message ) {
		error( title, message, null );
	}

	public void error( String title, Throwable throwable ) {
		error( title, null, throwable );
	}

	public String[] error( String title, Object message, Throwable throwable ) {
		String[] messages = null;

		if( message == null && throwable != null ) {
			StringBuilder builder = new StringBuilder();

			builder.append( "<html>" );
			builder.append( throwable.getClass().getName() );
			builder.append( ":" );
			builder.append( "<br/>" );
			builder.append( "<br/>" );
			builder.append( throwable.getMessage() );
			builder.append( "</html>" );

			message = builder.toString();
		}

		if( message != null ) {
			messages = new String[ 1 ];
			messages[ 0 ] = message.toString();
		}

		StringWriter writer = new StringWriter();
		if( throwable != null ) throwable.printStackTrace( new PrintWriter( writer ) );

		// Show message on console.
		if( message != null ) Log.write( Log.ERROR, message.toString().trim() );
		Log.write( Log.ERROR, writer.toString().trim() );

		return messages;
	}

	/**
	 * Restart the service supplying extra commands if desired.
	 *
	 * @param commands
	 */
	public void serviceRestart( String... commands ) {
		// Register a shutdown hook to restart the application.
		RestartShutdownHook restartShutdownHook = new RestartShutdownHook( this, commands );
		Runtime.getRuntime().addShutdownHook( restartShutdownHook );

		// Request the program stop.
		if( !requestStop() ) {
			Runtime.getRuntime().removeShutdownHook( restartShutdownHook );
			return;
		}

		// The shutdown hook should restart the application.
		Log.write( Log.INFO, "Restarting..." );
	}

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void startAgent() throws Exception {
		Log.write( Log.DEBUG, getName() + " starting..." );
		PerformanceCheck.writeTimeAfterStart( "Service.startAgent() start" );

		Runtime.getRuntime().addShutdownHook( shutdownHook );

		// Start the peer server.
		peerServer.startAndWait();
		PerformanceCheck.writeTimeAfterStart( "Service.startAgent() peer server" );

		// Start the task manager.
		taskManager.loadSettings( settings.getNode( ServiceSettingsPath.TASK_MANAGER_SETTINGS_PATH ) );
		taskManager.startAndWait();

		// Start the product manager.
		productManager.startAndWait();

		// Register the modules.
		registerAllModules();

		startService( parameters );

		// Allocate the modules.
		createAllModules();

		Log.write( getName() + " started." );

		// Check for updates.
		productManager.scheduleUpdateCheck( true );
	}

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void stopAgent() throws Exception {
		Log.write( Log.DEBUG, getName() + " stopping..." );
		if( socket != null ) socket.close();

		// Deallocate the modules.
		destroyAllModules();

		stopService( parameters );

		// Unregister modules.
		unregisterAllModules();

		productManager.stopAndWait();

		// Give the program time to wrap up before shutting down the task manager.
		ThreadUtil.pause( 100 );

		taskManager.stopAndWait();
		taskManager.saveSettings( settings.getNode( ServiceSettingsPath.TASK_MANAGER_SETTINGS_PATH ) );

		peerServer.stopAndWait();

		try {
			Runtime.getRuntime().removeShutdownHook( shutdownHook );
		} catch( IllegalStateException exception ) {
			// Intentionally ignore exception.
		}
		Log.write( getName() + " stopped." );

		/*
		 * Start the JvmSureStop to force the JVM to halt after a reasonable amount
		 * of time if the JVM does not exit cleanly. Do this only when not running
		 * the unit tests.
		 */
		// Do not add this as a shutdown hook, it hangs the JVM.
		if( !TestUtil.isTest() ) new JvmSureStop().start();
	}

	protected abstract void startService( Parameters parameters ) throws Exception;

	protected abstract void process( Parameters parameters, boolean peer ) throws Exception;

	protected abstract void stopService( Parameters parameters ) throws Exception;

	protected boolean requestStop() {
		getTaskManager().submit( new ShutdownTask( this ) );
		return true;
	}

	protected void printHeader() {
		String summary = card.getLicenseSummary();

		printAsciiArtTitle();

		Log.write( Log.HELP, getName() + " " + card.getRelease().toHumanString() );
		Log.write( Log.HELP, card.getCopyright(), " ", card.getCopyrightNotice() );
		if( summary != null ) {
			Log.write( Log.HELP );
			Log.write( Log.HELP, TextUtil.reline( summary, 75 ) );
		}
		Log.write( Log.HELP, TextUtil.pad( 75, '-' ) );

		Log.write( Log.TRACE, "Java: " + System.getProperty( "java.runtime.version" ) );

		Log.write( Log.DEBUG, "Classpath: " );
		try {
			List<URI> uris = JavaUtil.parseClasspath( System.getProperty( "java.class.path" ) );
			for( URI uri : uris ) {
				Log.write( Log.DEBUG, "  ", uri );
			}
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}
	}

	protected void printAsciiArtTitle() {
		Log.write( Log.HELP, TextUtil.pad( 75, '-' ) );
	}

	protected void printStatus() {
		Log.write( Log.INFO, getName() + " status: " + getStatus() );
	}

	protected URI getDescriptorUri() throws URISyntaxException {
		URL url = getClass().getResource( DEFAULT_PRODUCT_PATH );
		return url == null ? null : url.toURI();
	}

	/**
	 * Override this method and return a set of valid command line flags if you
	 * want the service to validate command line flags. By default this method
	 * returns null and allows any command line flags.
	 *
	 * @return
	 */
	protected Set<String> getValidCommandLineFlags() {
		Set<String> flags = new HashSet<String>();

		flags.add( LogFlag.LOG_TAG );
		flags.add( LogFlag.LOG_COLOR );
		flags.add( LogFlag.LOG_DATE );
		flags.add( LogFlag.LOG_PREFIX );
		flags.add( LogFlag.LOG_LEVEL );
		flags.add( LogFlag.LOG_FILE );
		flags.add( LogFlag.LOG_FILE_LEVEL );
		flags.add( LogFlag.LOG_FILE_APPEND );

		flags.add( ServiceFlag.ARTIFACT );
		flags.add( ServiceFlag.EXECMODE );
		flags.add( ServiceFlag.HOME );
		flags.add( ServiceFlag.LOCALE );

		flags.add( ServiceFlag.HELP );
		flags.add( ServiceFlag.VERSION );
		flags.add( ServiceFlag.STATUS );
		flags.add( ServiceFlag.RESTART );
		flags.add( ServiceFlag.STOP );
		flags.add( ServiceFlag.WATCH );
		flags.add( ServiceFlag.SETTINGS_RESET );
		flags.add( ServiceFlag.NOUPDATE );
		flags.add( ServiceFlag.NOUPDATECHECK );

		return flags;
	}

	private static void setLocale( Parameters parameters ) {
		String locale = parameters.get( ServiceFlag.LOCALE );

		String[] parts = locale.split( "_" );

		String l = parts.length > 0 ? parts[ 0 ] : "";
		String c = parts.length > 1 ? parts[ 1 ] : "";
		String v = parts.length > 2 ? parts[ 2 ] : "";

		Locale.setDefault( new Locale( l, c, v ) );
	}

	private final void describe( URI codebase, Descriptor descriptor ) throws ProductCardException {
		if( this.descriptor != null ) return;
		this.descriptor = descriptor;

		card = new ProductCard( codebase, descriptor );

		// Determine the program name.
		if( name == null ) setName( card.getName() );
		setName( name == null ? card.getName() : name );

		// Minimum Java runtime version.
		javaVersionMinimum = descriptor.getValue( ProductCard.RESOURCES_PATH + "/java/@version", JAVA_VERSION_MINIMUM );
	}

	/**
	 * Process the program parameters. This method is called from both the
	 * process() method if another instance of the program is not running or the
	 * peerExists() method if another instance is running.
	 *
	 * @param parameters
	 */
	private final void processParameters( Parameters parameters, boolean peer ) {
		if( this.parameters == null ) this.parameters = parameters;

		PerformanceCheck.writeTimeAfterStart( "Service.processParameters() start" );

		try {
			// Initialize logging.
			if( !peer ) {
				Log.config( parameters );

				// TODO Fix log file name collision when two non-peer instances run.
				if( !parameters.isSet( LogFlag.LOG_FILE ) ) {
					try {
						File folder = new File( getDataFolder(), LOG_FOLDER_NAME );
						folder.mkdirs();

						StringBuilder pattern = new StringBuilder( folder.getCanonicalPath() );
						pattern.append( File.separatorChar );
						pattern.append( LOG_FILE_PATTERN );

						logFilePattern = pattern.toString();

						FileHandler handler = new FileHandler( logFilePattern, parameters.isTrue( LogFlag.LOG_FILE_APPEND ) );
						handler.setLevel( Log.getLevel() );
						if( parameters.isSet( LogFlag.LOG_FILE_LEVEL ) ) handler.setLevel( Log.parseLevel( parameters.get( LogFlag.LOG_FILE_LEVEL ) ) );

						DefaultFormatter formatter = new DefaultFormatter();
						formatter.setShowDate( true );
						handler.setFormatter( formatter );
						Log.addHandler( handler );
					} catch( IOException exception ) {
						Log.write( exception );
					}
				}

				// Set the locale.
				if( parameters.isSet( ServiceFlag.LOCALE ) ) setLocale( parameters );

				// Print the program header.
				if( !isRunning() ) printHeader();

				Log.write( Log.TRACE, "Processing parameters: " + parameters.toString() );

				// Verify Java environment.
				if( !checkJava( parameters ) ) return;

				configureOnce( parameters );

				// Check for existing peer.
				if( peerExists( parameters ) ) return;
				PerformanceCheck.writeTimeAfterStart( "Service.processParameters() no peer" );
			}

			// If the watch parameter is set then exit before doing anything else.
			if( parameters.isTrue( ServiceFlag.WATCH ) ) return;

			if( parameters.isTrue( ServiceFlag.STOP ) ) {
				stopAndWait();
				return;
			} else if( parameters.isTrue( ServiceFlag.RESTART ) ) {
				restart();
				return;
			} else if( parameters.isTrue( ServiceFlag.STATUS ) ) {
				printStatus();
				return;
			} else if( parameters.isTrue( ServiceFlag.VERSION ) ) {
				return;
			} else if( parameters.isSet( ServiceFlag.HELP ) ) {
				printHelp( parameters.get( ServiceFlag.HELP ) );
				return;
			}

			// This logic is not trivial, the nested if statements help clarify it.
			if( !peer ) {
				if( !parameters.isSet( ServiceFlag.NOUPDATE ) ) {
					int updateResult = productManager.updateProduct();

					if( updateResult > 0 ) {
						// The program should be allowed, but not forced, to exit at this point.
						Log.write( "Program exiting to apply updates." );

						// Do not call System.exit() or Runtime.getRuntime().exit(). Just return.
						return;
					}
				}

				// Start the program.
				startAndWait();
			}

			// Process parameters.
			process( parameters, peer );
		} catch( Exception exception ) {
			Log.write( exception );
			return;
		}
	}

	private final boolean checkJava( Parameters parameters ) {
		String minimumVersion = javaVersionMinimum;
		String runtimeVersion = System.getProperty( "java.runtime.version" );

		Log.write( Log.DEBUG, "Comparing Java version: " + runtimeVersion + " >= " + minimumVersion );

		if( JavaUtil.compareJavaVersion( minimumVersion, runtimeVersion ) > 0 ) {
			error( "Java " + javaVersionMinimum + " or higher is required, found: " + System.getProperty( "java.runtime.version" ) );
			return false;
		}
		return true;
	}

	private final void configureOnce( Parameters parameters ) {
		if( isRunning() ) return;

		configureArtifact( parameters );

		configureHome( parameters );

		configureSettings( parameters );

		configureServices( parameters );
	}

	private final void configureExecMode( Parameters parameters ) {
		if( parameters.isSet( ServiceFlag.EXECMODE ) ) {
			if( ServiceFlagValue.TEST.equals( parameters.get( ServiceFlag.EXECMODE ) ) && !card.getArtifact().startsWith( TEST_PREFIX ) ) {
				execModePrefix = TEST_PREFIX;
			} else if( ServiceFlagValue.DEVL.equals( parameters.get( ServiceFlag.EXECMODE ) ) && !card.getArtifact().startsWith( DEVL_PREFIX ) ) {
				execModePrefix = DEVL_PREFIX;
			}
		}
	}

	private final void configureArtifact( Parameters parameters ) {
		// Set the artifact name if specified.
		if( parameters.isSet( ServiceFlag.ARTIFACT ) ) card.setArtifact( parameters.get( ServiceFlag.ARTIFACT ) );
		Log.write( Log.TRACE, "Card: ", card.getProductKey() );
	}

	/**
	 * Find the home directory. This method expects the program jar file to be
	 * installed in a sub-directory of the home directory. Example:
	 * <code>$HOME/lib/program.jar</code>
	 *
	 * @param parameters
	 * @return
	 */
	private final void configureHome( Parameters parameters ) {
		try {
			// If the HOME flag was specified on the command line use it.
			if( home == null && parameters.get( ServiceFlag.HOME ) != null ) {
				home = new File( parameters.get( ServiceFlag.HOME ) ).getCanonicalFile();
			}

			// Check the code source.
			if( home == null ) {
				try {
					URI uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
					if( "file".equals( uri.getScheme() ) && uri.getPath().endsWith( ".jar" ) ) home = new File( uri ).getParentFile();
				} catch( URISyntaxException exception ) {
					Log.write( exception );
				}
			}

			// Check the execmode flag.
			if( home == null && parameters.isSet( ServiceFlag.EXECMODE ) ) {
				home = new File( System.getProperty( "user.dir" ), "target/install" );
				home.mkdirs();

				// Copy the updater library.
				File updaterSource = new File( System.getProperty( "user.dir" ), "../updater/target/updater-" + card.getRelease().getVersion() + ".jar" ).getCanonicalFile();
				File updaterTarget = new File( home, "updater.jar" ).getCanonicalFile();
				FileUtil.copy( updaterSource, updaterTarget );
				Log.write( Log.DEBUG, "Updater copied: " + updaterSource );
			}

			// Use the user directory as a last resort.
			if( home == null ) home = new File( System.getProperty( "user.dir" ) );

			// Canonicalize the home path.
			if( home != null ) home = home.getCanonicalFile();
		} catch( IOException exception ) {
			exception.printStackTrace();
		}

		Log.write( Log.TRACE, "Home: ", home );
		Log.write( Log.TRACE, "Log : ", logFilePattern );

		card.setInstallFolder( home );
	}

	private final void configureSettings( Parameters parameters ) {
		// Add the parameters settings provider.
		try {
			settings.addProvider( new ParametersSettingsProvider( parameters ) );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		// Add the preferences settings provider.
		//		String preferencesPath = "/" + card.getGroup() + "." + card.getArtifact();
		//		if( parameters.isSet( ServiceFlag.EXECMODE ) ) preferencesPath = "/" + card.getGroup() + execModePrefix + card.getArtifact();
		//		Preferences preferences = Preferences.userRoot().node( preferencesPath );
		//		if( preferences != null ) settings.addProvider( new PreferencesSettingProvider( preferences ) );
		//		Log.write( Log.DEBUG, "Preferences path: " + preferencesPath );

		// Add the persistent settings provider.
		File providerFile = new File( getDataFolder(), "settings.properties" );
		settings.addProvider( new PersistentMapSettingsProvider( providerFile ) );

		// Reset the settings specified on the command line.
		if( parameters.isTrue( ServiceFlag.SETTINGS_RESET ) ) {
			if( "".equals( execModePrefix ) ) {
				Log.write( Log.ERROR, "Parameters: ", parameters.toString() );
				Log.write( Log.ERROR, new RuntimeException( "Resetting the program settings for a production instance!" ) );
			}
			Log.write( Log.WARN, "Resetting the program settings..." );
			settings.reset();
		}

		settings.addSettingListener( "/network", new NetworkSettingsChangeHandler( this ) );
		configureNetworkSettings();
	}

	private final void configureServices( Parameters parameters ) {
		if( settings == null ) throw new RuntimeException( "Settings not initialized." );

		// Set proxy handlers.
		Authenticator.setDefault( new ServiceProxyAuthenticator( this ) );
		ProxySelector.setDefault( new ServiceProxySelector( this ) );

		// Register the product.
		productManager.registerProduct( this );
		productManager.setEnabled( getCard(), true );
		productManager.setUpdatable( getCard(), true );
		productManager.setRemovable( getCard(), false );

		// Configure the product manager.
		productManager.loadSettings( settings.getNode( ServiceSettingsPath.PRODUCT_MANAGER_SETTINGS_PATH ) );
		productManager.setUpdaterPath( new File( getHomeFolder(), ProductManager.UPDATER_JAR_NAME ) );
	}

	private final void configureNetworkSettings() {
		Settings settings = getSettings().getNode( "/network" );
		boolean enableipv6 = settings.getBoolean( "enableipv6", false );
		boolean preferipv6 = settings.getBoolean( "preferipv6", false );

		if( enableipv6 ) {
			System.clearProperty( "java.net.preferIPv4Stack" );
			if( preferipv6 ) {
				System.setProperty( "java.net.preferIPv6Addresses", "true" );
			} else {
				System.clearProperty( "java.net.preferIPv6Addresses" );
			}
		} else {
			System.setProperty( "java.net.preferIPv4Stack", "true" );
			System.clearProperty( "java.net.preferIPv6Addresses" );
		}
	}

	private final boolean peerExists( Parameters parameters ) {
		boolean exists = false;
		String peer = null;
		String host = parameters.get( "host", "localhost" );
		int port = peerServer.getServicePortNumber();

		if( port != 0 ) {
			// Connect to the peer, if possible, and pass the parameters.
			try {
				socket = new Socket( host, port );
				peer = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

				Log.write( getName() + " connected to peer." );
				Log.write( Log.TRACE, "Connected to peer: " + peer );

				ObjectOutputStream output = new ObjectOutputStream( socket.getOutputStream() );
				output.writeObject( parameters.getResolvedCommands() );
				output.flush();
				Log.write( Log.TRACE, "Parameters sent to peer." );
			} catch( ConnectException exception ) {
				Log.write( "Peer not found: " + host + ":" + port );
				return false;
			} catch( IOException exception ) {
				Log.write( Log.WARN, exception, "Could not connect to peer." );
				peerServer.resetServicePortNumber();
				return false;
			}

			Log.setLevel( PEER_LOGGER_NAME, Log.ALL );
			try {
				Object object;
				BufferedInputStream bufferedInput = new BufferedInputStream( socket.getInputStream() );
				ObjectInputStream objectInput = new ObjectInputStream( bufferedInput );
				while( (object = objectInput.readObject()) != null ) {
					LogRecord entry = (LogRecord)object;
					Log.writeTo( PEER_LOGGER_NAME, entry );
				}
				Log.write( Log.TRACE, "Disconnected from peer: " + peer );
			} catch( EOFException exception ) {
				Log.write( Log.TRACE, "Disconnected from peer: " + peer );
			} catch( SocketException exception ) {
				Log.write( Log.TRACE, "Disconnected from peer: " + peer );
			} catch( Exception exception ) {
				Log.write( Log.TRACE, "Peer connection terminated." );
				Log.write( exception );
			} finally {
				if( socket != null ) {
					try {
						socket.close();
					} catch( IOException exception ) {
						// Intentionally ignore exception.
					}
				}
			}
			exists = true;
		}

		return exists;
	}

	/**
	 * Register the modules. This allows the modules to set any values that may be
	 * needed while the workareas are being generated.
	 */
	private void registerAllModules() {
		Log.write( Log.DEBUG, "Registering modules..." );
		for( ServiceModule module : productManager.getModules() ) {
			try {
				if( getProductManager().isEnabled( module.getCard() ) ) module.register();
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

	/**
	 * Create the modules. At this point a module may start referring to other
	 * registered modules.
	 */
	private void createAllModules() {
		Log.write( Log.DEBUG, "Creating modules..." );
		for( ServiceModule module : productManager.getModules() ) {
			try {
				if( getProductManager().isEnabled( module.getCard() ) ) module.create();
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

	/**
	 * Destroy the module. At this point a module may still refer to the
	 * application frame, workareas, and other registered modules.
	 */
	private void destroyAllModules() {
		Log.write( Log.DEBUG, "Destroying modules..." );
		for( ServiceModule module : productManager.getModules() ) {
			try {
				if( getProductManager().isEnabled( module.getCard() ) ) module.destroy();
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

	/**
	 * Unregister the modules. This allows the modules to clean up any resources
	 * that may have been allocated during program operation.
	 */
	private void unregisterAllModules() {
		Log.write( Log.DEBUG, "Unregistering modules..." );
		for( ServiceModule module : productManager.getModules() ) {
			try {
				if( getProductManager().isEnabled( module.getCard() ) ) module.unregister();
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

	private static final class PeerServer extends ServerAgent {

		private Service service;

		private List<PeerHandler> handlers = new CopyOnWriteArrayList<PeerHandler>();

		public PeerServer( Service service ) {
			super( "Peer Server", "localhost" );
			this.service = service;
		}

		@Override
		protected void startServer() {
			storeServicePortNumber();
		}

		@Override
		protected void handleSocket( Socket socket ) throws IOException {
			String peerRemote = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
			String peerLocal = socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
			Log.write( Log.TRACE, "Peer connected from: ", peerRemote, " to ", peerLocal );
			PeerHandler handler = new PeerHandler( peerRemote, service, socket );
			handlers.add( handler );
			handler.start();
		}

		@Override
		protected void stopServer() {
			resetServicePortNumber();
			for( PeerHandler handler : handlers ) {
				try {
					// Cannot wait for handlers to stop because 
					// a handler may have called this method.
					handler.stop();
				} catch( Exception exception ) {
					Log.write( exception );
				}
			}
		}

		private final int getServicePortNumber() {
			Settings productSettings = ProductUtil.getSettings( service );
			int result = productSettings.getInt( "port", service.port );
			if( result < 0 || result > 65535 ) result = service.port;
			return result;
		}

		private final void storeServicePortNumber() {
			Settings productSettings = ProductUtil.getSettings( service );
			productSettings.putInt( "port", getLocalPort() );
			productSettings.flush();
		}

		private final void resetServicePortNumber() {
			Settings productSettings = ProductUtil.getSettings( service );
			productSettings.putInt( "port", service.port );
			productSettings.flush();
		}

	}

	private static final class PeerHandler extends Worker {

		private Service service;

		private Socket socket;

		private Handler logHandler;

		private ObjectInputStream input;

		private String peer;

		public PeerHandler( String peer, Service service, Socket socket ) {
			super( "Peer Handler " + peer, false );
			this.peer = peer;
			this.service = service;
			this.socket = socket;
		}

		@Override
		public void run() {
			// Read the parameters from the input stream.
			try {
				input = new ObjectInputStream( socket.getInputStream() );
				Parameters parameters = Parameters.parse( (String[])input.readObject() );
				Log.write( Log.TRACE, "Peer parameters: ", parameters );

				// Set up the peer log handler.
				logHandler = new PeerLogHandler( this, socket.getOutputStream() );
				Level level = Log.parseLevel( parameters.get( "log.level" ) );
				logHandler.setLevel( level == null ? Log.INFO : level );
				Log.addHandler( logHandler );

				// Process the parameters.
				service.processParameters( parameters, true );

				// If the watch flag is set then just watch.
				if( "true".equals( parameters.get( "watch" ) ) ) watch();
			} catch( InvalidParameterException exception ) {
				Log.write( exception );
			} catch( SocketException exception ) {
				if( !"socket closed".equals( exception.getMessage().toLowerCase().trim() ) ) {
					Log.write( exception );
				}
				return;
			} catch( IOException exception ) {
				Log.write( exception );
				return;
			} catch( ClassNotFoundException exception ) {
				Log.write( exception );
				return;
			} finally {
				if( logHandler != null ) {
					Log.removeHandler( logHandler );
					logHandler.close();
				}
				closeSocket();
				Log.write( Log.TRACE, "Peer disconnected: " + peer );
			}

		}

		public void closeSocket() {
			if( socket.isClosed() ) return;

			try {
				socket.close();
			} catch( IOException exception ) {
				Log.write( exception );
			}
		}

		@Override
		protected void stopWorker() throws Exception {
			if( logHandler != null ) logHandler.close();
			closeSocket();

			// Notify watching threads.
			synchronized( this ) {
				notifyAll();
			}
		}

		private synchronized void watch() {
			while( shouldExecute() && !socket.isClosed() ) {
				try {
					this.wait();
				} catch( InterruptedException exception ) {
					// Ignore interruption.
				}
			}
		}

	}

	private static class PeerLogHandler extends Handler {

		private PeerHandler peer;

		private ObjectOutputStream output;

		private boolean closed;

		public PeerLogHandler( PeerHandler peer, OutputStream out ) throws IOException {
			this.peer = peer;
			this.output = new ObjectOutputStream( out );
		}

		@Override
		public void publish( LogRecord record ) {
			if( closed || record.getLevel().intValue() < getLevel().intValue() ) return;

			try {
				output.writeObject( record );
				output.flush();
			} catch( SocketException exception ) {
				peer.stop();
			} catch( Exception exception ) {
				log( exception );
			}
		}

		@Override
		public void flush() {
			if( closed ) return;

			try {
				output.flush();
			} catch( SocketException exception ) {
				peer.stop();
			} catch( Exception exception ) {
				log( exception );
			}
		}

		@Override
		public void close() throws SecurityException {
			if( closed ) return;

			closed = true;

			try {
				output.close();
			} catch( SocketException exception ) {
				peer.stop();
			} catch( Exception exception ) {
				log( exception );
			} finally {
				Log.removeHandler( this );
			}
		}

		private void log( Throwable throwable ) {
			Level level = getLevel();
			setLevel( Log.NONE );
			Log.write( throwable );
			setLevel( level );
		}

	}

	private static final class NetworkSettingsChangeHandler implements SettingListener {

		private Service service;

		public NetworkSettingsChangeHandler( Service service ) {
			this.service = service;
		}

		@Override
		public void settingChanged( SettingEvent event ) {
			if( "/network".equals( event.getNodePath() ) & "enableipv6".equals( event.getKey() ) | "preferipv6".equals( event.getKey() ) ) {
				service.configureNetworkSettings();
			}
		}

	}

	private static final class ShutdownHook extends Thread {

		private Service service;

		public ShutdownHook( Service service ) {
			this.service = service;
		}

		@Override
		public void run() {
			try {
				service.stop();
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}

	}

	private static final class ShutdownTask extends Task<Void> {

		private Service service;

		public ShutdownTask( Service service ) {
			this.service = service;
		}

		@Override
		public Void execute() throws Exception {
			try {
				service.stop();
			} catch( Exception exception ) {
				Log.write( exception );
			}

			return null;
		}

	}

}

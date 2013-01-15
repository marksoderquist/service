package com.parallelsymmetry.service;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.parallelsymmetry.service.product.Product;
import com.parallelsymmetry.service.product.ProductCard;
import com.parallelsymmetry.service.product.ProductCardException;
import com.parallelsymmetry.service.product.ProductManager;
import com.parallelsymmetry.service.product.ProductModule;
import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.JavaUtil;
import com.parallelsymmetry.utility.OperatingSystem;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.Release;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.agent.Agent;
import com.parallelsymmetry.utility.agent.ServerAgent;
import com.parallelsymmetry.utility.agent.Worker;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.setting.DescriptorSettingProvider;
import com.parallelsymmetry.utility.setting.ParametersSettingProvider;
import com.parallelsymmetry.utility.setting.PreferencesSettingProvider;
import com.parallelsymmetry.utility.setting.SettingEvent;
import com.parallelsymmetry.utility.setting.SettingListener;
import com.parallelsymmetry.utility.setting.Settings;
import com.parallelsymmetry.utility.task.Task;
import com.parallelsymmetry.utility.task.TaskManager;

public abstract class Service extends Agent implements Product {

	public static final String PRODUCT_INSTALL_FOLDER_NAME = "products";

	public static final String MANAGER_SETTINGS_ROOT = "/manager";

	public static final String TASK_MANAGER_SETTINGS_PATH = MANAGER_SETTINGS_ROOT + "/task";

	public static final String PRODUCT_MANAGER_SETTINGS_PATH = MANAGER_SETTINGS_ROOT + "/product";

	private static final String DEFAULT_DESCRIPTOR_PATH = "/META-INF/product.xml";

	private static final String DEFAULT_SETTINGS_PATH = "/META-INF/settings.xml";

	private static final String JAVA_VERSION_MINIMUM = "1.6";

	private static final String PEER_LOGGER_NAME = "peer";

	public static final String LOCALE = "locale";

	public static final String TEST_PREFIX = "$";

	public static final String DEVL_PREFIX = "#";

	private static RestartShutdownHook restartShutdownHook;

	private Thread shutdownHook = new ShutdownHook( this );

	private Parameters parameters = Parameters.create();

	private String execModePrefix = "";

	private Settings settings;

	private Descriptor descriptor;

	private ProductCard card;

	private String javaVersionMinimum = JAVA_VERSION_MINIMUM;

	private Socket socket;

	private String name;

	private File home;

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
		try {
			InputStream input = getClass().getResourceAsStream( DEFAULT_SETTINGS_PATH );
			settings.setDefaultProvider( new DescriptorSettingProvider( new Descriptor( input ) ) );
			input.close();
		} catch( Exception exception ) {
			Log.write( exception );
		}

		peerServer = new PeerServer( this );
		taskManager = new TaskManager();
		productManager = new ProductManager( this );
	}

	/**
	 * Process the command line parameters. This method is the entry point for the
	 * service and is normally called from the main() method of the implementing
	 * class.
	 * 
	 * @param commands
	 */
	public synchronized void call( String... commands ) {
		try {
			try {
				parameters = Parameters.parse( commands, getValidCommandLineFlags() );
			} catch( InvalidParameterException exception ) {
				Log.write( exception );
				printHelp( null );
				return;
			}

			processParameters( parameters, false );
		} catch( Throwable programThrowable ) {
			try {
				error( programThrowable );
			} catch( Throwable fatalThrowable ) {
				fatalThrowable.printStackTrace();
			}
		}
	}

	@Override
	public ProductCard getCard() {
		return card;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public Settings getSettings() {
		return settings;
	}

	/**
	 * Get the home folder. If the home folder is null that means that the program
	 * is not installed locally and was most likely started with a technology like
	 * Java Web Start.
	 * 
	 * @return
	 */
	public File getHomeFolder() {
		return home;
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public ProductManager getProductManager() {
		return productManager;
	}

	public File getProgramDataFolder() {
		return OperatingSystem.getUserProgramDataFolder( execModePrefix + card.getArtifact(), execModePrefix + getName() );
	}

	public void printHelp() {
		printHelp( "true" );
	}

	public void printHelp( String topic ) {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		if( "true".equals( topic ) ) {
			Log.write( Log.NONE, "Usage: java -jar <jar file name> [<option>...]" );
			Log.write( Log.NONE );

			Log.write( Log.NONE, "Commands:" );
			printHelpCommands();

			Log.write( Log.NONE, "Options:" );
			printHelpOptions();
		}
	}

	public void printHelpCommands() {
		Log.write( Log.NONE, "  If no command is specified the program is started." );
		Log.write( Log.NONE );
		Log.write( Log.NONE, "  -help [topic]    Show help information." );
		Log.write( Log.NONE, "  -version         Show version and copyright information only." );
		Log.write( Log.NONE );
		Log.write( Log.NONE, "  -stop            Stop the program and exit the VM." );
		Log.write( Log.NONE, "  -status          Print the program status." );
		Log.write( Log.NONE, "  -restart         Restart the program without exiting VM." );
		Log.write( Log.NONE, "  -watch           Watch an already running program." );
		Log.write( Log.NONE );
	}

	public void printHelpOptions() {
		Log.write( Log.NONE, "  -log.color           Use ANSI color in the console output." );
		Log.write( Log.NONE, "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( Log.NONE, "                       none, error, warn, info, trace, debug, all" );
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
			messages = new String[1];
			messages[0] = message.toString();
		}

		StringWriter writer = new StringWriter();
		if( throwable != null ) throwable.printStackTrace( new PrintWriter( writer ) );

		// Show message on console.
		if( message != null ) Log.write( Log.ERROR, message );
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
		restartShutdownHook = new RestartShutdownHook( this, commands );
		Runtime.getRuntime().addShutdownHook( restartShutdownHook );

		// Request the program stop.
		if( !requestStop() ) {
			Runtime.getRuntime().removeShutdownHook( restartShutdownHook );
			return;
		}

		// The shutdown hook should restart the application.
		Log.write( Log.INFO, "Restarting..." );
	}

	protected URI getDescriptorUri() throws URISyntaxException {
		URL url = getClass().getResource( DEFAULT_DESCRIPTOR_PATH );
		return url == null ? null : url.toURI();
	}

	protected abstract void startService( Parameters parameters ) throws Exception;

	protected abstract void process( Parameters parameters ) throws Exception;

	protected abstract void stopService( Parameters parameters ) throws Exception;

	protected boolean requestStop() {
		getTaskManager().submit( new ShutdownTask( this ) );
		return true;
	}

	/**
	 * Override this method and return a set of valid command line flags if you
	 * want the service to validate command line flags. By default this method
	 * returns null and allows any command line flags.
	 * 
	 * @return
	 */
	protected Set<String> getValidCommandLineFlags() {
		return null;
	}

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void startAgent() throws Exception {
		Log.write( Log.DEBUG, getName() + " starting..." );
		Runtime.getRuntime().addShutdownHook( shutdownHook );

		// Start the peer server.
		peerServer.startAndWait();

		// Start the task manager.
		taskManager.loadSettings( settings.getNode( TASK_MANAGER_SETTINGS_PATH ) );
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
		if( !parameters.isSet( ServiceFlag.NOUPDATECHECK ) && productManager.getCheckOption() == ProductManager.CheckOption.STARTUP ) productManager.checkForUpdates();
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

		taskManager.stopAndWait();
		taskManager.saveSettings( settings.getNode( TASK_MANAGER_SETTINGS_PATH ) );

		peerServer.stopAndWait();

		try {
			Runtime.getRuntime().removeShutdownHook( shutdownHook );
		} catch( IllegalStateException exception ) {
			// Intentionally ignore exception.
		}
		Log.write( getName() + " stopped." );
	}

	protected final boolean isProgramUpdated() {
		// Get the previous release.
		Release that = Release.decode( settings.get( "/service/release", null ) );

		// Set the current release.
		settings.put( "/service/release", Release.encode( this.getCard().getRelease() ) );

		// Return the result.
		return that == null ? false : this.getCard().getRelease().compareTo( that ) > 0;
	}

	private static void setLocale( Parameters parameters ) {
		String locale = parameters.get( LOCALE );

		String[] parts = locale.split( "_" );

		String l = parts.length > 0 ? parts[0] : "";
		String c = parts.length > 1 ? parts[1] : "";
		String v = parts.length > 2 ? parts[2] : "";

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

		Log.write( Log.DEBUG, "Processing parameters: " + parameters.toString() );

		try {
			// Initialize logging.
			if( !peer ) {
				Log.config( parameters );

				// TODO Fix log file name collision when two instances run.
				//			if( !parameters.isSet( LogFlag.LOG_FILE ) ) {
				//				try {
				//					File folder = getProgramDataFolder();
				//					String pattern = new File( folder, "program.log" ).getCanonicalPath().replace( '\\', '/' );
				//					folder.mkdirs();
				//
				//					FileHandler handler = new FileHandler( pattern, parameters.isTrue( LogFlag.LOG_FILE_APPEND ) );
				//					handler.setLevel( Log.getLevel() );
				//					if( parameters.isSet( LogFlag.LOG_FILE_LEVEL ) ) handler.setLevel( Log.parseLevel( parameters.get( LogFlag.LOG_FILE_LEVEL ) ) );
				//					handler.setFormatter( new DefaultFormatter() );
				//					Log.addHandler( handler );
				//				} catch( IOException exception ) {
				//					Log.write( exception );
				//				}
				//			}

				// Set the locale.
				if( parameters.isSet( LOCALE ) ) setLocale( parameters );

				// Print the program header.
				if( !isRunning() ) printHeader();

				// Verify Java environment.
				if( !checkJava( parameters ) ) return;

				configureOnce( parameters );

				// Check for existing peer.
				if( peerExists( parameters ) ) return;
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
			} else if( parameters.isTrue( ServiceFlag.HELP ) ) {
				printHelp( parameters.get( ServiceFlag.HELP ) );
				return;
			}

			// This logic is not trivial, the nested if statements help clarify it.
			if( !peer & !parameters.isSet( ServiceFlag.NOUPDATE ) ) {
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

			// Process parameters.
			process( parameters );
		} catch( Exception exception ) {
			Log.write( exception );
			return;
		}
	}

	private final boolean checkJava( Parameters parameters ) {
		String javaRuntimeVersion = System.getProperty( "java.runtime.version" );
		Log.write( Log.DEBUG, "Comparing Java version: " + javaRuntimeVersion + " >= " + javaVersionMinimum );
		if( javaVersionMinimum.compareTo( javaRuntimeVersion ) > 0 ) {
			error( "Java " + javaVersionMinimum + " or higher is required, found: " + javaRuntimeVersion );
			return false;
		}
		return true;
	}

	private final void configureOnce( Parameters parameters ) {
		if( isRunning() ) return;

		if( parameters.isSet( ServiceFlag.EXECMODE ) ) {
			if( ServiceFlagValue.TEST.equals( parameters.get( ServiceFlag.EXECMODE ) ) && !card.getArtifact().startsWith( TEST_PREFIX ) ) {
				execModePrefix = TEST_PREFIX;
			} else if( ServiceFlagValue.DEVL.equals( parameters.get( ServiceFlag.EXECMODE ) ) && !card.getArtifact().startsWith( DEVL_PREFIX ) ) {
				execModePrefix = DEVL_PREFIX;
			}
		}

		configureHome( parameters );

		configureArtifact( parameters );

		configureSettings( parameters );

		configureServices( parameters );
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

		card.setInstallFolder( home );
	}

	private final void configureArtifact( Parameters parameters ) {
		// Set the artifact name if specified.
		if( parameters.isSet( ServiceFlag.ARTIFACT ) ) card.setArtifact( parameters.get( ServiceFlag.ARTIFACT ) );
		Log.write( Log.TRACE, "Card: ", card.getProductKey() );
	}

	private final void configureSettings( Parameters parameters ) {
		// Add the parameters settings provider.
		try {
			settings.addProvider( new ParametersSettingProvider( parameters ) );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		// Add the preferences settings provider.
		String preferencesPath = "/" + card.getGroup() + "." + card.getArtifact();
		if( parameters.isSet( ServiceFlag.EXECMODE ) ) preferencesPath = "/" + card.getGroup() + execModePrefix + card.getArtifact();
		Preferences preferences = Preferences.userRoot().node( preferencesPath );
		if( preferences != null ) settings.addProvider( new PreferencesSettingProvider( preferences ) );
		Log.write( Log.DEBUG, "Preferences path: " + preferencesPath );

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
		productManager.registerProduct( getCard() );
		productManager.setEnabled( getCard(), true );
		productManager.setUpdatable( getCard(), true );
		productManager.setRemovable( getCard(), false );
		
		// Configure the product manager.
		productManager.loadSettings( settings.getNode( PRODUCT_MANAGER_SETTINGS_PATH ) );
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

	private final void printHeader() {
		String notice = card.getLicenseSummary();

		Log.write( Log.NONE, TextUtil.pad( 75, '-' ) );
		Log.write( Log.NONE, getName() + " " + card.getRelease().toHumanString() );
		Log.write( Log.NONE, card.getCopyright(), " ", card.getCopyrightNotice() );
		Log.write( Log.NONE );
		if( notice != null ) {
			Log.write( Log.NONE, TextUtil.reline( notice, 75 ) );
			Log.write( Log.NONE );
		}

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

	private final void printStatus() {
		Log.write( getName() + " status: " + getStatus() );
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
				output.writeObject( parameters.getCommands() );
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
				while( ( object = objectInput.readObject() ) != null ) {
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
		for( ProductModule module : productManager.getModules() ) {
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
		for( ProductModule module : productManager.getModules() ) {
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
		for( ProductModule module : productManager.getModules() ) {
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
		for( ProductModule module : productManager.getModules() ) {
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
			String peer = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
			Log.write( Log.TRACE, "Peer connected: ", peer );
			PeerHandler handler = new PeerHandler( peer, service, socket );
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
			return service.getSettings().getInt( "/service/port", 0 );
		}

		private final void storeServicePortNumber() {
			service.getSettings().putInt( "/service/port", getLocalPort() );
			service.getSettings().flush();
		}

		private final void resetServicePortNumber() {
			service.getSettings().put( "/service/port", null );
			service.getSettings().flush();
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

				Log.write( Log.TRACE, "Parameters read from peer." );

				Parameters parameters = Parameters.parse( (String[])input.readObject() );

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

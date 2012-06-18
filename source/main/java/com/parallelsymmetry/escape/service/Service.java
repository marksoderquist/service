package com.parallelsymmetry.escape.service;

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
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.parallelsymmetry.escape.service.update.FeaturePack;
import com.parallelsymmetry.escape.service.update.UpdateManager;
import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.agent.ServerAgent;
import com.parallelsymmetry.escape.utility.agent.Worker;
import com.parallelsymmetry.escape.utility.log.DefaultFormatter;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.log.LogFlag;
import com.parallelsymmetry.escape.utility.setting.DescriptorSettingProvider;
import com.parallelsymmetry.escape.utility.setting.ParametersSettingProvider;
import com.parallelsymmetry.escape.utility.setting.PreferencesSettingProvider;
import com.parallelsymmetry.escape.utility.setting.SettingEvent;
import com.parallelsymmetry.escape.utility.setting.SettingListener;
import com.parallelsymmetry.escape.utility.setting.Settings;
import com.parallelsymmetry.escape.utility.task.Task;
import com.parallelsymmetry.escape.utility.task.TaskManager;

public abstract class Service extends Agent {

	public static final String MANAGER_SETTINGS_ROOT = "/manager";

	public static final String LOCALE = "locale";

	public static final String DEVELOPMENT_PREFIX = "#";

	private static final String PEER_LOGGER_NAME = "peer";

	private static final String TASK_MANAGER_SETTINGS_PATH = MANAGER_SETTINGS_ROOT + "/task";

	private static final String DEFAULT_DESCRIPTOR_PATH = "/META-INF/program.xml";

	private static final String DEFAULT_SETTINGS_PATH = "/META-INF/settings.xml";

	private static final String JAVA_VERSION_MINIMUM = "1.6.0";

	private static final String COPYRIGHT = "(C)";

	private static RestartShutdownHook restartShutdownHook;

	private Thread shutdownHook = new ShutdownHook( this );

	private Parameters parameters = Parameters.create();

	private Settings settings;

	private Descriptor descriptor;

	private FeaturePack pack;

	private String javaVersionMinimum = JAVA_VERSION_MINIMUM;

	private Socket socket;

	private String name;

	private File home;

	private PeerServer peerServer;

	private TaskManager taskManager;

	private UpdateManager updateManager;

	/**
	 * Construct the service with the default descriptor path of
	 * &quot;/META-INF/program.xml&quot;.
	 */
	public Service() {
		this( null, null );
	}

	/**
	 * Construct the service with the specified name and the default descriptor
	 * path of &quot;/META-INF/program.xml&quot;.
	 * 
	 * @param name
	 */
	public Service( String name ) {
		this( name, null );
	}

	/**
	 * Construct the service with the specified descriptor. The descriptor must
	 * conform to the Escape service descriptor specification.
	 * 
	 * @param descriptor
	 */
	public Service( Descriptor descriptor ) {
		this( null, descriptor );
	}

	/**
	 * Construct the service with the specified name and descriptor. The
	 * descriptor must conform to the Escape service descriptor specification.
	 * 
	 * @param name
	 * @param descriptor
	 */
	public Service( String name, Descriptor descriptor ) {
		super( name );
		this.name = name;

		if( descriptor == null ) {
			try {
				InputStream input = getClass().getResourceAsStream( DEFAULT_DESCRIPTOR_PATH );
				if( input != null ) Log.write( Log.DEBUG, "Application descriptor found: " + DEFAULT_DESCRIPTOR_PATH );
				descriptor = new Descriptor( input );
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}

		describe( descriptor );

		// Create the settings object.
		settings = new Settings();
		try {
			InputStream input = getClass().getResourceAsStream( DEFAULT_SETTINGS_PATH );
			settings.setDefaultProvider( new DescriptorSettingProvider( new Descriptor( input ) ) );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		// Create the task queue.
		taskManager = new TaskManager();

		// Create update manager.
		updateManager = new UpdateManager( this );

		// Create the peer server.
		peerServer = new PeerServer( this );
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
			parameters = Parameters.parse( commands, getValidCommandLineFlags() );
		} catch( InvalidParameterException exception ) {
			Log.write( exception );
			printHelp( null );
			return;
		}

		processParameters( parameters, false );
	}

	public String getGroup() {
		return pack.getGroup();
	}

	public String getArtifact() {
		return pack.getArtifact();
	}

	public Release getRelease() {
		return pack.getRelease();
	}

	public String getProvider() {
		return pack.getProvider();
	}

	public String getCopyright() {
		int currentYear = DateUtil.getCurrentYear();
		int inceptionYear = pack.getInceptionYear();
		if( inceptionYear == 0 ) inceptionYear = Calendar.getInstance().get( Calendar.YEAR );

		return COPYRIGHT + " " + ( currentYear == inceptionYear ? currentYear : inceptionYear + "-" + currentYear ) + " " + pack.getCopyrightHolder();
	}

	public String getCopyrightHolder() {
		return pack.getCopyrightHolder();
	}

	public String getCopyrightNotice() {
		return pack.getCopyrightNotice();
	}

	public String getLicenseSummary() {
		return TextUtil.reline( pack.getLicenseSummary(), 72 );
	}

	public Parameters getParameters() {
		return parameters;
	}

	public Settings getSettings() {
		return settings;
	}

	public FeaturePack getPack() {
		return pack;
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

	public UpdateManager getUpdateManager() {
		return updateManager;
	}

	public File getProgramDataFolder() {
		return OperatingSystem.getUserProgramDataFolder( getArtifact(), getName() );
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

	public int notify( String title, Object message ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle", null ), message, JOptionPane.INFORMATION_MESSAGE );
	}

	public int notify( String title, Object message, int messageType ) {
		return notify( UIManager.getString( "OptionPane.messageDialogTitle", null ), message, JOptionPane.DEFAULT_OPTION, messageType );
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
		return -1;
	}

	public void error( String message ) {
		error( "Error", message, null );
	}

	public void error( Throwable throwable ) {
		error( "Error", null, throwable );
	}

	public void error( String message, Throwable throwable ) {
		error( "Error", message, throwable );
	}

	public String[] error( String title, String message, Throwable throwable ) {
		String[] messages = null;
		if( message == null && throwable != null ) message = throwable.getMessage();
		if( message != null ) {
			messages = new String[1];
			messages[0] = message;
		}

		StringWriter writer = new StringWriter();
		if( throwable != null ) throwable.printStackTrace( new PrintWriter( writer ) );

		// Show message on console.
		if( message != null ) Log.write( Log.ERROR, message );
		Log.write( Log.ERROR, writer.toString().trim() );

		return messages;
	}

	public UpdateManager.UpdateCheckTask getUpdateCheckTask() {
		return new ServiceUpdateTask();
	}

	protected void serviceRestart() {
		// Register a shutdown hook to restart the application.
		restartShutdownHook = new RestartShutdownHook( this );
		Runtime.getRuntime().addShutdownHook( restartShutdownHook );

		// Request the program stop.
		if( !requestStop() ) {
			Runtime.getRuntime().removeShutdownHook( restartShutdownHook );
			return;
		}

		// The shutdown hook should restart the application.
		Log.write( Log.INFO, "Restarting..." );
	}

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

		// Start the update manager.
		updateManager.startAndWait();

		startService( parameters );
		Log.write( getName() + " started." );

		if( updateManager.getCheckOption() == UpdateManager.CheckOption.STARTUP ) updateManager.checkForUpdates();
	}

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void stopAgent() throws Exception {
		Log.write( Log.DEBUG, getName() + " stopping..." );
		if( socket != null ) socket.close();
		stopService( parameters );

		updateManager.stopAndWait();

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

	protected abstract void startService( Parameters parameters ) throws Exception;

	protected abstract void process( Parameters parameters ) throws Exception;

	protected abstract void stopService( Parameters parameters ) throws Exception;

	private final synchronized void describe( Descriptor descriptor ) {
		if( this.descriptor != null ) return;
		this.descriptor = descriptor;

		pack = FeaturePack.load( descriptor );

		// Determine the program name.
		if( name == null ) setName( pack.getName() );
		setName( name == null ? pack.getName() : name );

		// Minimum Java runtime version.
		javaVersionMinimum = descriptor.getValue( "/pack/resources/java/@version", JAVA_VERSION_MINIMUM );
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

		try {
			// Initialize logging.
			Log.config( parameters );
			if( !parameters.isSet( LogFlag.LOG_FILE ) ) {
				try {
					File folder = getProgramDataFolder();
					String pattern = new File( folder, "program.log" ).getCanonicalPath().replace( '\\', '/');
					folder.mkdirs();

					FileHandler handler = new FileHandler( pattern, parameters.isTrue( LogFlag.LOG_FILE_APPEND ) );
					handler.setLevel( Log.INFO );
					if( parameters.isSet( LogFlag.LOG_FILE_LEVEL ) ) handler.setLevel( Log.parseLevel( parameters.get( LogFlag.LOG_FILE_LEVEL ) ) );
					handler.setFormatter( new DefaultFormatter() );
					Log.addHandler( handler );
				} catch( IOException exception ) {
					Log.write( exception );
				}
			}

			// Set the locale.
			if( parameters.isSet( LOCALE ) ) setLocale( parameters );

			// Print the program header.
			if( !isRunning() ) printHeader();

			// Verify Java environment.
			if( !checkJava( parameters ) ) return;

			Log.write( Log.DEBUG, "Processing parameters: " + parameters.toString() );

			configureOnce( parameters );

			// Check for existing peer.
			if( !peer && peerExists( parameters ) ) return;

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

			// The logic is somewhat complex, the nested if statements help clarify it.
			if( updateManager.getCheckOption() != UpdateManager.CheckOption.DISABLED ) {
				if( ( parameters.isSet( ServiceFlag.UPDATE ) & parameters.isTrue( ServiceFlag.UPDATE ) ) | ( !parameters.isSet( ServiceFlag.UPDATE ) & !peer ) ) {
					if( update() && !parameters.isSet( ServiceFlag.DEVELOPMENT ) ) {
						// The program should be allowed, but not forced, to exit at this point.
						Log.write( "Program exiting to apply updates." );
						return;
					}
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

	private static void setLocale( Parameters parameters ) {
		String locale = parameters.get( LOCALE );

		String[] parts = locale.split( "_" );

		String l = parts.length > 0 ? parts[0] : "";
		String c = parts.length > 1 ? parts[1] : "";
		String v = parts.length > 2 ? parts[2] : "";

		Locale.setDefault( new Locale( l, c, v ) );
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
			// If -home was specified on the command line use it.
			if( home == null && parameters.get( "home" ) != null ) {
				home = new File( parameters.get( "home" ) ).getCanonicalFile();
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

			// Check the development flag.
			if( home == null && parameters.isSet( ServiceFlag.DEVELOPMENT ) ) {
				home = new File( System.getProperty( "user.dir" ), "target/install" );
				home.mkdirs();
			}

			// Use the user directory as a last resort.
			if( home == null ) home = new File( System.getProperty( "user.dir" ) );

			// Canonicalize the home path.
			if( home != null ) home = home.getCanonicalFile();
		} catch( IOException exception ) {
			exception.printStackTrace();
		}

		Log.write( Log.TRACE, "Home: ", home );

		pack.setInstallFolder( home );
	}

	private final void configureArtifact( Parameters parameters ) {
		// Set the artifact name if specified.
		if( parameters.isSet( ServiceFlag.ARTIFACT ) ) {
			pack.setArtifact( parameters.get( ServiceFlag.ARTIFACT ) );
		}

		// Update the artifact if the development flag is set.
		if( parameters.isTrue( ServiceFlag.DEVELOPMENT ) && !pack.getArtifact().startsWith( DEVELOPMENT_PREFIX ) ) {
			pack.setArtifact( DEVELOPMENT_PREFIX + pack.getArtifact() );
		}

		Log.write( Log.TRACE, "Pack: ", pack.getKey() );
	}

	private final void configureSettings( Parameters parameters ) {
		try {
			String preferencesPath = "/" + pack.getGroup().replace( '.', '/' ) + "/" + pack.getArtifact();
			Preferences preferences = Preferences.userRoot().node( preferencesPath );

			settings.addProvider( new ParametersSettingProvider( parameters ) );
			if( preferences != null ) settings.addProvider( new PreferencesSettingProvider( preferences ) );
			if( parameters.isTrue( ServiceFlag.SETTINGS_RESET ) ) {
				Log.write( Log.WARN, "Resetting the program settings..." );
				settings.reset();
			}
		} catch( Exception exception ) {
			Log.write( exception );
		}

		settings.addSettingListener( "/network", new NetworkSettingsChangeHandler( this ) );
		configureNetworkSettings();
	}

	private final void configureServices( Parameters parameters ) {
		if( settings == null ) throw new RuntimeException( "Settings not initialized." );

		// Set proxy handlers.
		Authenticator.setDefault( new ServiceProxyAuthenticator( this ) );
		ProxySelector.setDefault( new ServiceProxySelector( this ) );

		updateManager.loadSettings( settings.getNode( "update" ) );
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
		String notice = getLicenseSummary();

		Log.write( Log.NONE, TextUtil.pad( 75, '-' ) );
		Log.write( Log.NONE, getName() + " " + getRelease().toHumanString() );
		Log.write( Log.NONE, getCopyright(), " ", getCopyrightNotice() );
		Log.write( Log.NONE );
		if( notice != null ) {
			Log.write( Log.NONE, TextUtil.reline( notice, 75 ) );
			Log.write( Log.NONE );
		}

		Log.write( Log.TRACE, "Java: " + System.getProperty( "java.runtime.version" ) );
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
				if( parameters.size() == 0 ) Log.write( getName() + " already running." );
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
	 * Apply updates. If updates are found then the method returns true, allowing
	 * the service to terminate cleanly.
	 * 
	 * @return True if updates were found, causing the service to terminate. False
	 *         otherwise.
	 */
	private final boolean update() {
		if( home == null && parameters.isSet( "update" ) && !parameters.isTrue( "update" ) ) {
			Log.write( Log.WARN, "Program not executed from updatable location." );
			return false;
		}

		Log.write( Log.DEBUG, "Checking for staged updates..." );

		// If updates are staged, apply them.
		if( updateManager.areUpdatesStaged() ) {
			Log.write( "Staged updates detected." );
			boolean result = false;
			try {
				result = updateManager.applyStagedUpdates();
			} catch( Exception exception ) {
				Log.write( exception );
			}
			return result;
		} else {
			Log.write( Log.TRACE, "No staged updates detected." );
			return false;
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

	private class ServiceUpdateTask extends UpdateManager.UpdateCheckTask {

		public ServiceUpdateTask() {
			super( Service.this );
		}

		@Override
		public void execute() {
			try {
				Log.write( Log.TRACE, "Checking for updates..." );
				if( getService().getUpdateManager().stagePostedUpdates() ) {
					Log.write( Log.TRACE, "Updates staged, restarting..." );
					serviceRestart();
				}
			} catch( Exception exception ) {
				Log.write( exception );
			}
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

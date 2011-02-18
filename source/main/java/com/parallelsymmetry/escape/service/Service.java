package com.parallelsymmetry.escape.service;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.parallelsymmetry.escape.service.task.TaskManager;
import com.parallelsymmetry.escape.service.update.UpdateManager;
import com.parallelsymmetry.escape.service.update.UpdatePack;
import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.JavaUtil;
import com.parallelsymmetry.escape.utility.OperatingSystem;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.agent.ServerAgent;
import com.parallelsymmetry.escape.utility.agent.Worker;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.DescriptorSettingProvider;
import com.parallelsymmetry.escape.utility.setting.ParametersSettingProvider;
import com.parallelsymmetry.escape.utility.setting.PreferencesSettingProvider;
import com.parallelsymmetry.escape.utility.setting.Settings;

public abstract class Service extends Agent {

	private static final String PEER_LOGGER_NAME = "peer";

	private static final String DEFAULT_DESCRIPTOR_PATH = "/META-INF/program.xml";

	private static final String DEFAULT_SETTINGS_PATH = "/META-INF/settings.xml";

	private static final String JAVA_VERSION_MINIMUM = "1.6";

	private static final String LOCALHOST = "127.0.0.1";

	private static final String COPYRIGHT = "(C)";

	private Thread shutdownHook = new ShutdownHook( this );

	private Parameters parameters;

	private Settings settings;

	private UpdatePack pack;

	private Descriptor descriptor;

	private int inceptionYear = DateUtil.getCurrentYear();

	private String copyrightHolder = "Unknown";

	private String licenseSummary;

	private String javaVersionMinimum = JAVA_VERSION_MINIMUM;

	private Socket socket;

	private String name;

	private File home;

	private UpdateManager updateManager;

	private PeerServer peerServer;

	private TaskManager taskManager;

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

		peerServer = new PeerServer( this );
		taskManager = new TaskManager();
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

		Log.init( parameters );
		configureHome( parameters );
		configureSettings( parameters );
		configureServices();
		processParameters( parameters, false );

		Log.write( Log.TRACE, "Method call() complete." );
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
		return getCopyright( Locale.getDefault() );
	}

	public String getCopyright( Locale locale ) {
		int currentYear = DateUtil.getCurrentYear();
		return COPYRIGHT + " " + ( currentYear == inceptionYear ? currentYear : inceptionYear + "-" + currentYear ) + " " + copyrightHolder;
	}

	public String getCopyrightNotice() {
		return getCopyrightNotice( Locale.getDefault() );
	}

	public String getCopyrightNotice( Locale locale ) {
		ResourceBundle bundle = ResourceBundle.getBundle( "copyright", locale );
		return bundle.getString( "notice" );
	}

	public String getLicenseSummary() {
		return licenseSummary;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public Settings getSettings() {
		return settings;
	}

	public UpdatePack getPack() {
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

	public UpdateManager getUpdateManager() {
		return updateManager;
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public File getProgramDataFolder() {
		return OperatingSystem.getProgramDataFolder( getArtifact(), getName() );
	}

	public void printHelp( String topic ) {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		if( "true".equals( topic ) ) {
			Log.write( Log.NONE, "Usage: java -jar <jar file name> [<option>...]" );
			Log.write( Log.NONE );
			printHelpCommands();
			printHelpOptions();
		}
	}

	public void printHelpCommands() {
		Log.write( Log.NONE, "Commands:" );
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
		Log.write( Log.NONE, "Options:" );
		Log.write( Log.NONE, "  -log.color           Use ANSI color in the console output." );
		Log.write( Log.NONE, "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( Log.NONE, "                       none, error, warn, info, trace, debug, all" );
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

	protected String[] error( String title, String message, Throwable throwable ) {
		List<String> elements = new ArrayList<String>();
		if( throwable != null ) {
			throwable.printStackTrace();

			Throwable cause = throwable;
			while( cause.getCause() != null ) {
				cause = cause.getCause();
			}
			elements.add( cause.getClass().getName() + ": " + cause.getMessage() );
		}

		String[] messages = null;
		if( message == null ) {
			messages = new String[elements.size()];
			System.arraycopy( elements.toArray( new String[elements.size()] ), 0, messages, 0, elements.size() );
		} else {
			messages = new String[elements.size() + 1];
			messages[0] = message;
			System.arraycopy( elements.toArray( new String[elements.size()] ), 0, messages, 1, elements.size() );
		}

		// Show message on console.
		Log.write( Log.ERROR, message );

		return messages;
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

		// Create and start the task manager.
		taskManager.loadSettings( settings.getNode( "services/tasks" ) );
		taskManager.startAndWait();

		startService( parameters );
		Log.write( getName() + " started." );
	}

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void stopAgent() throws Exception {
		Log.write( Log.DEBUG, getName() + " stopping..." );
		if( socket != null ) socket.close();
		stopService( parameters );
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

		pack = UpdatePack.load( descriptor );

		// Determine the program name.
		setName( name == null ? pack.getName() : name );

		// Determine the program copyright information.
		try {
			inceptionYear = Integer.parseInt( descriptor.getValue( "/pack/inception" ) );
		} catch( NumberFormatException exception ) {
			inceptionYear = Calendar.getInstance().get( Calendar.YEAR );
		}
		copyrightHolder = descriptor.getValue( "/pack/provider", copyrightHolder );

		licenseSummary = descriptor.getValue( "/pack/license/summary", licenseSummary );
		if( licenseSummary != null ) licenseSummary = TextUtil.reline( licenseSummary, 72 );

		// Minimum Java runtime version.
		javaVersionMinimum = descriptor.getValue( "/pack/resources/java/version", JAVA_VERSION_MINIMUM );
	}

	private final boolean checkJava( Parameters parameters ) {
		String javaRuntimeVersion = System.getProperty( "java.runtime.version" );
		if( javaVersionMinimum.compareTo( javaRuntimeVersion ) > 0 ) {
			error( "Java " + javaVersionMinimum + " or higher is required, found: " + javaRuntimeVersion );
			return false;
		}
		return true;
	}

	private final void configureOnce( Parameters parameters ) {
		if( isRunning() ) return;

		// Update the artifact if the development flag is set.
		if( parameters.isTrue( "development" ) ) {
			pack.setArtifact( pack.getArtifact() + "-dev" );
			Log.write( Log.TRACE, "Updated artifact to: " + pack.getArtifact() );
		}
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
		if( isRunning() ) return;

		try {
			// If -home was specified on the command line use it.
			if( home == null && parameters.get( "home" ) != null ) {
				home = new File( parameters.get( "home" ) ).getCanonicalFile();
			}

			// Check the class path.
			if( home == null ) {
				try {
					List<URI> uris = JavaUtil.parseSystemClasspath( System.getProperty( "java.class.path" ) );
					for( URI uri : uris ) {
						if( "file".equals( uri.getScheme() ) && uri.getPath().endsWith( ".jar" ) ) {
							// The following line assumes that the jar is in the home folder.
							home = new File( uri ).getParentFile();
							break;
						}
					}
				} catch( URISyntaxException exception ) {
					Log.write( exception );
				}
			}

			if( home != null ) home = home.getCanonicalFile();
		} catch( IOException exception ) {
			exception.printStackTrace();
		}
	}

	private final void configureSettings( Parameters parameters ) {
		if( isRunning() ) return;

		settings = new Settings();

		try {
			Descriptor defaultSettingDescriptor = null;
			InputStream input = getClass().getResourceAsStream( DEFAULT_SETTINGS_PATH );
			if( input != null ) defaultSettingDescriptor = new Descriptor( input );

			String preferencesPath = "/" + pack.getGroup().replace( '.', '/' ) + "/" + pack.getArtifact();

			if( parameters.isTrue( "preferences.reset" ) ) resetPreferences( Preferences.userRoot().node( preferencesPath ) );
			Preferences preferences = Preferences.userRoot().node( preferencesPath );

			settings.addProvider( new ParametersSettingProvider( parameters ) );
			if( preferences != null ) settings.addProvider( new PreferencesSettingProvider( preferences ) );
			if( defaultSettingDescriptor != null ) settings.setDefaultProvider( new DescriptorSettingProvider( defaultSettingDescriptor ) );
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	private final void resetPreferences( Preferences preferences ) {
		String path = preferences.absolutePath();
		try {
			preferences.removeNode();
		} catch( BackingStoreException exception ) {
			Log.write( exception );
		}
		preferences = Preferences.userRoot().node( path );
	}

	private final void configureServices() {
		if( isRunning() ) return;

		if( settings == null ) throw new RuntimeException( "Settings not initialized." );

		// Create the update manager.
		updateManager = new UpdateManager( this );
		updateManager.loadSettings( settings.getNode( "services/update" ) );
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
			if( !isRunning() ) printHeader();

			// Verify Java environment.
			if( !checkJava( parameters ) ) return;

			Log.write( Log.DEBUG, "Processing parameters: " + parameters.toString() );

			configureOnce( parameters );

			// Check for existing peer.
			if( !peer && peerExists( parameters ) ) return;

			// If the watch parameter is set then exit before doing anything else.
			if( parameters.isTrue( "watch" ) ) return;

			if( parameters.isTrue( "stop" ) ) {
				stopAndWait();
				return;
			} else if( parameters.isTrue( "restart" ) ) {
				restart();
				return;
			} else if( parameters.isTrue( "status" ) ) {
				printStatus();
				return;
			} else if( parameters.isTrue( "version" ) ) {
				return;
			} else if( parameters.isTrue( "help" ) ) {
				printHelp( parameters.get( "help" ) );
				return;
			}

			// Update if necessary.
			if( ( parameters.isSet( "update" ) & parameters.isTrue( "update" ) ) | ( !parameters.isSet( "update" ) & !peer ) ) if( update() ) {
				// The program should be allowed, but not forced, to exit at this point.
				Log.write( "Program exiting to apply updates." );
				return;
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

	private final void printHeader() {
		String notice = getLicenseSummary();

		Log.write( Log.NONE, getName() + " " + getRelease().toHumanString() );
		Log.write( Log.NONE, getCopyright( Locale.ENGLISH ) + " " + getCopyrightNotice( Locale.ENGLISH ) );
		Log.write( Log.NONE );
		if( notice != null ) {
			Log.write( Log.NONE, notice );
			Log.write( Log.NONE );
		}

		Log.write( Log.TRACE, "Java: " + System.getProperty( "java.runtime.version" ) );
		Log.write( Log.TRACE, "Home: " + getHomeFolder() );
	}

	private final void printStatus() {
		Log.write( getName() + " status: " + getStatus() );
	}

	private final boolean peerExists( Parameters parameters ) {
		boolean exists = false;
		String peer = null;
		String host = parameters.get( "host", LOCALHOST );
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

	private final boolean update() {
		if( home == null && parameters.isSet( "update" ) && !parameters.isTrue( "update" ) ) {
			Log.write( Log.WARN, "Program not executed from libraries; updates disabled." );
			return false;
		}

		Log.write( Log.DEBUG, "Checking for staged updates..." );

		// Detect updates.
		boolean found = updateManager.areUpdatesStaged();

		if( found ) {
			Log.write( "Staged updates detected." );
			try {
				updateManager.applyUpdates();
			} catch( Exception exception ) {
				Log.write( exception );
			}
		} else {
			Log.write( Log.TRACE, "No staged updates detected." );
		}

		return found;
	}

	private static final class PeerServer extends ServerAgent {

		private Service service;

		private List<PeerHandler> handlers = new CopyOnWriteArrayList<PeerHandler>();

		public PeerServer( Service service ) {
			super( "Peer Server", LOCALHOST );
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
			return service.getSettings().getInt( "port", 0 );
		}

		private final void storeServicePortNumber() {
			service.getSettings().putInt( "port", getLocalPort() );
			service.getSettings().flush();
		}

		private final void resetServicePortNumber() {
			service.getSettings().put( "port", null );
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

	private static final class ShutdownHook extends Thread {

		private Service service;

		public ShutdownHook( Service daemon ) {
			this.service = daemon;
		}

		public void run() {
			try {
				service.stop();
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}

	}

}

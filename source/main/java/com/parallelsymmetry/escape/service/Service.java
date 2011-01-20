package com.parallelsymmetry.escape.service;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.agent.ServerAgent;
import com.parallelsymmetry.escape.utility.agent.Worker;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.DescriptorSettingProvider;
import com.parallelsymmetry.escape.utility.setting.ParametersSettingProvider;
import com.parallelsymmetry.escape.utility.setting.PreferencesSettingProvider;
import com.parallelsymmetry.escape.utility.setting.Settings;

public abstract class Service extends Agent {

	private static final String DEFAULT_NAMESPACE = "com.parallelsymmetry";

	private static final String DEFAULT_IDENTIFIER = "service";

	private static final String PEER_LOGGER_NAME = "peer";

	private static final String DEFAULT_DESCRIPTOR_PATH = "/META-INF/program.xml";

	private static final String DEFAULT_SETTINGS_PATH = "/META-INF/settings.xml";

	private static final String DESCRIPTOR_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final String JAVA_VERSION_MINIMUM = "1.6";

	private static final String LOCALHOST = "127.0.0.1";

	private static final String COPYRIGHT = "(C)";

	private Parameters parameters;

	private Settings settings;

	private Thread shutdownHook = new ShutdownHook( this );

	private PeerServer peerServer;

	private String descriptorPath;

	private Descriptor descriptor;

	private String group = DEFAULT_NAMESPACE;

	private String artifact = DEFAULT_IDENTIFIER;

	private Release release = new Release( new Version() );

	private int inceptionYear = DateUtil.getCurrentYear();

	private String copyrightHolder = "Unknown";

	private String licenseSummary;

	private String javaVersionMinimum = JAVA_VERSION_MINIMUM;

	private Socket socket;

	private String name;

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
	 * Construct the service with the specified name and descriptor path. The
	 * descriptor path must be able to be found on the class path and must begin
	 * with a '/' slash.
	 * 
	 * @param name
	 * @param descriptorPath
	 */
	public Service( String name, String descriptorPath ) {
		super( name );
		this.name = name;
		this.descriptorPath = descriptorPath == null ? DEFAULT_DESCRIPTOR_PATH : descriptorPath;

		describe( Parameters.parse( new String[] {} ) );
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

		// Set log level.
		configureLogging( parameters );

		// Load description.
		describe( parameters );

		// Verify Java environment.
		if( !checkJava( parameters ) ) return;

		processParameters( parameters, false );
	}

	public String getGroup() {
		return this.group;
	}

	public String getArtifact() {
		return this.artifact;
	}

	public Release getRelease() {
		return release;
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

	public void printHelp( String topic ) {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		if( "true".equals( topic ) ) {
			Log.write( "Usage: java -jar <jar file name> [<option>...]" );
			Log.write();
			printHelpCommands();
			printHelpOptions();
		}
	}

	public void printHelpCommands() {
		Log.write( "Commands:" );
		Log.write( "  If no command is specified the program is started." );
		Log.write();
		Log.write( "  -help [topic]    Show help information." );
		Log.write( "  -version         Show version and copyright information only." );
		Log.write();
		Log.write( "  -stop            Stop the program and exit the VM." );
		Log.write( "  -status          Print the program status." );
		Log.write( "  -restart         Restart the program without exiting VM." );
		Log.write( "  -watch           Watch an already running program." );
		Log.write();
	}

	public void printHelpOptions() {
		Log.write( "Options:" );
		Log.write( "  -log.color           Use ANSI color in the console output." );
		Log.write( "  -log.level <level>   Change the output log level. Levels are:" );
		Log.write( "                       none, error, warn, info, trace, debug, all" );
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

	protected abstract void startService( Parameters parameters ) throws Exception;

	protected abstract void process( Parameters parameters ) throws Exception;

	protected abstract void stopService( Parameters parameters ) throws Exception;

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void startAgent() throws Exception {
		Log.write( Log.TRACE, getName() + " starting..." );
		Runtime.getRuntime().addShutdownHook( shutdownHook );
		peerServer = new PeerServer( this );
		peerServer.startAndWait();
		storeServicePortNumber();
		startService( parameters );
		Log.write( getName() + " started." );
	}

	/**
	 * This method should only be called through the processParameters() method.
	 */
	@Override
	protected final void stopAgent() throws Exception {
		Log.write( Log.TRACE, getName() + " stopping..." );
		if( socket != null ) socket.close();
		stopService( parameters );
		resetServicePortNumber();
		peerServer.stopAndWait();
		try {
			Runtime.getRuntime().removeShutdownHook( shutdownHook );
		} catch( IllegalStateException exception ) {
			// Intentionally ignore exception.
		}
		Log.write( getName() + " stopped." );
	}

	private final void describe( Parameters parameters ) {
		settings = new Settings();
		descriptor = getApplicationDescriptor();

		// Determine the program name.
		setName( name == null ? descriptor.getValue( "/program/information/name" ) : name );

		// Determine the program namespace.
		group = descriptor.getValue( "/program/information/group", group );
		group = parameters.get( "namespace", group );

		// Determine the program identifier.
		artifact = descriptor.getValue( "/program/information/artifact", artifact );
		artifact = parameters.get( "identifier", artifact );
		if( parameters.isSet( "development" ) ) artifact += "-dev";
		if( TextUtil.isEmpty( this.artifact ) ) artifact = getName().replace( ' ', '-' ).toLowerCase();

		// Determine the program release.
		Version version = new Version( descriptor.getValue( "/program/information/version", null ) );
		Date timestamp = DateUtil.parse( descriptor.getValue( "/program/information/timestamp", null ), DESCRIPTOR_DATE_FORMAT );
		release = new Release( version, timestamp );

		// Determine the program copyright information.
		try {
			inceptionYear = Integer.parseInt( descriptor.getValue( "/program/information/inception" ) );
		} catch( NumberFormatException exception ) {
			inceptionYear = Calendar.getInstance().get( Calendar.YEAR );
		}
		copyrightHolder = descriptor.getValue( "/program/information/vendor", copyrightHolder );

		licenseSummary = descriptor.getValue( "/program/information/license/summary", licenseSummary );
		if( licenseSummary != null ) licenseSummary = TextUtil.reline( licenseSummary, 72 );

		// Minimum Java runtime version.
		javaVersionMinimum = descriptor.getValue( "/program/resources/java/@version", JAVA_VERSION_MINIMUM );

		// Add the setting providers.
		try {
			Descriptor defaultSettingDescriptor = null;
			defaultSettingDescriptor = new Descriptor( getClass().getResourceAsStream( DEFAULT_SETTINGS_PATH ) );
			Preferences preferences = Preferences.userRoot().node( "/" + group.replace( '.', '/' ) + "/" + artifact );

			settings.addProvider( new ParametersSettingProvider( parameters ) );
			if( preferences != null ) settings.addProvider( new PreferencesSettingProvider( preferences ) );
			if( defaultSettingDescriptor != null ) settings.setDefaultProvider( new DescriptorSettingProvider( defaultSettingDescriptor ) );
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	private final boolean checkJava( Parameters parameters ) {
		String javaRuntimeVersion = System.getProperty( "java.runtime.version" );
		if( javaVersionMinimum.compareTo( javaRuntimeVersion ) > 0 ) {
			error( "Java " + javaVersionMinimum + " or higher is required, found: " + javaRuntimeVersion );
			return false;
		}
		return true;
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
			if( !isRunning() ) printHeader();

			// Check for existing peer.
			if( !peer && peerExists( parameters ) ) return;

			// If the watch parameter is set then exit before doing anything else.
			if( parameters.isSet( "watch" ) ) return;

			// Update if necessary.
			if( !parameters.isSet( "noupdate" ) ) if( update() ) return;

			if( parameters.isSet( "stop" ) ) {
				stopAndWait();
				return;
			} else if( parameters.isSet( "restart" ) ) {
				restart();
				return;
			} else if( parameters.isSet( "status" ) ) {
				printStatus();
				return;
			} else if( parameters.isSet( "version" ) ) {
				return;
			} else if( parameters.isSet( "help" ) ) {
				printHelp( parameters.get( "help" ) );
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

	private final boolean update() {
		Log.write( Log.DEBUG, "Checking for updates..." );

		// Check if updates exists. If not, just return.
		boolean found = false;

		// TODO Detect updates.

		Log.write( Log.TRACE, found ? "Updates detected." : "No updates detected." );

		// Call the updater.
		if( found ) {

		}

		return found;
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
	}

	private final void printStatus() {
		Log.write( getName() + " status: " + getStatus() );
	}

	private final boolean peerExists( Parameters parameters ) {
		boolean exists = false;
		String peer = null;
		String host = parameters.get( "host", LOCALHOST );
		int port = getSettings().getInt( "/port", 0 );

		if( port != 0 ) {
			// Connect to the peer, if possible, and pass the parameters.
			try {
				// FIXME Convert the Socket code to use SocketAgent.
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
				try {
					resetServicePortNumber();
				} catch( BackingStoreException resetException ) {
					Log.write( resetException );
				}
				Log.write( exception );
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

	private final void storeServicePortNumber() throws IOException, BackingStoreException {
		getSettings().putInt( "/port", peerServer.getLocalPort() );
	}

	private final void resetServicePortNumber() throws BackingStoreException {
		getSettings().put( "/port", null );
	}

	private final void configureLogging( Parameters parameters ) {
		Log.setLevel( Log.parseLevel( parameters.get( "log.level" ) ) );
		Log.setShowColor( parameters.isSet( "log.color" ) );
	}

	private final Descriptor getApplicationDescriptor() {
		if( descriptor == null ) {
			try {
				InputStream input = getClass().getResourceAsStream( descriptorPath );
				if( input != null ) Log.write( Log.DEBUG, "Application descriptor found: " + descriptorPath );
				descriptor = new Descriptor( input );
			} catch( Exception exception ) {
				Log.write( exception );
			}
		}

		return descriptor;
	}

	private static final class PeerServer extends ServerAgent {

		private Service service;

		private List<PeerHandler> handlers = new CopyOnWriteArrayList<PeerHandler>();

		public PeerServer( Service service ) {
			super( "Peer Server", LOCALHOST );
			this.service = service;
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

				Parameters parameters = null;
				try {
					parameters = Parameters.parse( (String[])input.readObject() );
				} catch( InvalidParameterException exception ) {
					Log.write( exception );
				}

				// Set up the peer log handler.
				logHandler = new PeerLogHandler( this, socket.getOutputStream() );
				Level level = Log.parseLevel( parameters.get( "log.level" ) );
				logHandler.setLevel( level == null ? Log.INFO : level );
				Log.addHandler( logHandler );

				// Process the parameters.
				service.processParameters( parameters, true );

				// If the watch flag is set then just watch.
				if( "true".equals( parameters.get( "watch" ) ) ) watch();
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
				// FIXME Change out ObjectOutputStream for other implementation.
				output.writeObject( record );
				output.flush();
			} catch( Exception exception ) {
				peer.stop();
			}
		}

		@Override
		public void flush() {
			try {
				output.flush();
			} catch( Exception exception ) {
				peer.stop();
			}
		}

		@Override
		public void close() throws SecurityException {
			if( closed ) return;
			closed = true;

			try {
				output.close();
			} catch( Exception exception ) {
				peer.stop();
			} finally {

				Log.removeHandler( this );
			}
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

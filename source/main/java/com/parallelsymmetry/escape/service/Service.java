package com.parallelsymmetry.escape.service;

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
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.JavaUtil;
import com.parallelsymmetry.escape.utility.Parameters;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.agent.ServerAgent;
import com.parallelsymmetry.escape.utility.agent.Worker;
import com.parallelsymmetry.escape.utility.log.Log;

public abstract class Service extends Agent {

	public static final String COPYRIGHT = "(C)";

	private static final String DEFAULT_NAMESPACE = "com.parallelsymmetry";

	private static final String DEFAULT_IDENTIFIER = "program";

	private static final String HOME_PARAMETER_NAME = "home";

	private static final String PEER_LOGGER_NAME = "peer";

	private static final String APPLICATION_DESCRIPTOR_PATH = "/META-INF/program.xml";

	private static final String REGULAR_PREFERENCE_FILE_PATH = "/META-INF/preferences.xml";

	private static final String DEFAULT_PREFERENCE_FILE_PATH = "/META-INF/preferences.default.xml";

	private static final String DESCRIPTOR_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final String JAVA_VERSION_MINIMUM = "1.6";

	private static final String LOCALHOST = "127.0.0.1";

	private Parameters parameters = Parameters.parse( new String[0] );

	private Thread shutdownHook = new ShutdownHook( this );

	private PeerServer peerServer;

	private Descriptor descriptor;

	private String namespace = DEFAULT_NAMESPACE;

	private String identifier = DEFAULT_IDENTIFIER;

	private String javaVersionMinimum = JAVA_VERSION_MINIMUM;

	private String copyrightHolder = "Unknown";

	private String licenseSummary;

	private int inceptionYear;

	private Release release;

	private Socket socket;

	private File home;

	private boolean process;

	/**
	 * Process the command line parameters. This method is the entry point for the
	 * service and is normally called from the main() method of the implementing
	 * class.
	 * 
	 * @param commands
	 */
	public synchronized void call( String[] commands ) {
		try {
			parameters = Parameters.parse( commands, getValidCommandLineFlags() );
		} catch( InvalidParameterException exception ) {
			Log.write( exception );

			// FIXME Print the message then print the help then exit.
		}

		// Set log level.
		setLogLevel( parameters );

		// Load description.
		describe( parameters );

		// Find home.
		home = findHome( parameters );

		// Verify Java environment.
		if( !verifyJavaEnvironment( parameters ) ) return;

		processParameters( parameters, false );
	}

	public Release getRelease() {
		return release;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public String getCopyright() {
		return getCopyright( Locale.getDefault() );
	}

	public String getCopyright( Locale locale ) {
		int currentYear = Calendar.getInstance().get( Calendar.YEAR );
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

	public Preferences getPreferences() {
		return com.parallelsymmetry.escape.utility.Preferences.getApplicationRoot( getNamespace(), getIdentifier() );
	}

	public File getHome() {
		return home;
	}

	public void help( String topic ) {
		// ---------0--------1---------2---------3---------4---------5---------6---------7---------8
		// ---------12345678901234567890123456789012345678901234567890123456789012345678901234567890
		Log.write( "Usage: java -jar <jar file name> [<option>...]" );
		Log.write();
		Log.write( "Options:" );
		helpOptions();
	}

	public void helpOptions() {
		Log.write( "  -help [topic]    Show help information." );
		Log.write( "  -version         Show version and copyright information only." );
		Log.write();
		Log.write( "  -stop            Stop the application and exit the VM." );
		Log.write( "  -start           Start the application." );
		Log.write( "  -status          Print the application status." );
		Log.write( "  -restart         Restart the application without exiting VM." );
		Log.write( "  -watch           Watch an already running application." );
		Log.write();
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
	 * returns null and allows all command line flags.
	 * 
	 * @return
	 */
	protected Set<String> getValidCommandLineFlags() {
		return null;
	}

	protected abstract void startService( Parameters parameters ) throws Exception;

	protected abstract void process( Parameters parameters ) throws Exception;

	protected abstract void stopService( Parameters parameters ) throws Exception;

	@Override
	protected final void startAgent() throws Exception {
		if( !process ) throw new RuntimeException( "Start should only be called from the Service.call() method." );

		Log.write( Log.TRACE, getName() + " starting..." );
		Runtime.getRuntime().addShutdownHook( shutdownHook );
		peerServer = new PeerServer( this );
		peerServer.startAndWait();
		storeServicePortNumber();
		startService( parameters );
		Log.write( getName() + " started." );
	}

	@Override
	protected final void stopAgent() throws Exception {
		if( !process ) throw new RuntimeException( "Start should only be called from the Service.call() method." );

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

	/**
	 * Process the program parameters. This method is called from both the
	 * process() method if another instance of the program is not running or the
	 * peerExists() method if another instance is running.
	 * 
	 * @param parameters
	 */
	private final void processParameters( Parameters parameters, boolean peer ) {
		process = true;
		try {
			if( this.parameters == null ) this.parameters = parameters;

			Log.write( Log.DEBUG, "Processing parameters: " + parameters.toString() );

			try {
				if( !isRunning() ) printHeader();

				// Check for existing peer.
				if( !peer && peerExists( parameters ) ) return;

				// If the watch parameter is set then exit before doing anything else.
				if( parameters.isSet( "watch" ) ) return;

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
					help( parameters.get( "help" ) );
					return;
				}

				// Start the application.
				startAndWait();

				// Process parameters.
				process( parameters );
			} catch( Exception exception ) {
				Log.write( exception );
				return;
			}
		} finally {
			process = false;
		}
	}

	private final boolean peerExists( Parameters parameters ) {
		boolean exists = false;
		String host = parameters.get( "host", LOCALHOST );
		int port = getServicePortNumber( parameters );

		if( port != 0 ) {
			// Connect to the peer, if possible, and pass the parameters.
			try {
				socket = new Socket( host, port );
				Log.write( "Connected to peer: " + socket.getInetAddress() + ":" + socket.getPort() );
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
				ObjectInputStream objectInput = new ObjectInputStream( socket.getInputStream() );
				while( ( object = objectInput.readObject() ) != null ) {
					LogRecord entry = (LogRecord)object;
					Log.writeTo( PEER_LOGGER_NAME, entry );
				}
				Log.write( Log.INFO, "Peer connection terminated." );
			} catch( EOFException exception ) {
				Log.write( Log.INFO, "Peer connection terminated." );
			} catch( Exception exception ) {
				Log.write( Log.INFO, "Peer connection forcefully terminated." );
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

	private final int getServicePortNumber( Parameters parameters ) {
		int port = 0;

		// Find the port from the parameters.
		if( port == 0 ) {
			String portString = parameters.get( "port" );
			if( portString != null ) {
				try {
					port = Integer.parseInt( portString );
				} catch( NumberFormatException exception ) {
					throw new IllegalArgumentException( "Invalid port number: " + portString );
				}
			}
		}

		// Find the port from the preferences.
		if( port == 0 ) {
			port = getServicePortPreferenceNode().getInt( getServicePortKey(), port );
		}

		return port;
	}

	/**
	 * This method returns the class name as an MD5 hash code hex encoded. This is
	 * intended to give a small amount of security to the program.
	 * 
	 * @return
	 */
	private final String getServicePortKey() {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance( "MD5" );
		} catch( NoSuchAlgorithmException exception ) {
			return null;
		}
		digest.reset();
		return TextUtil.toHexEncodedString( digest.digest( getClass().getName().getBytes() ) );
	}

	private final void storeServicePortNumber() throws IOException, BackingStoreException {
		Preferences preferences = getServicePortPreferenceNode();
		if( !preferences.isUserNode() ) return;
		preferences.putInt( getServicePortKey(), peerServer.getLocalPort() );
		preferences.flush();
	}

	private final void resetServicePortNumber() throws BackingStoreException {
		Preferences preferences = getServicePortPreferenceNode();
		if( !preferences.isUserNode() ) return;
		preferences.remove( getServicePortKey() );
		preferences.flush();
	}

	private final Preferences getServicePortPreferenceNode() {
		Preferences node = null;
		try {
			node = getPreferences().node( "port" );
			node.sync();
		} catch( BackingStoreException userNodeException ) {
			// Allow method to continue.
		}
		return node;
	}

	private final boolean verifyJavaEnvironment( Parameters parameters ) {
		String javaRuntimeVersion = System.getProperty( "java.runtime.version" );
		if( javaVersionMinimum.compareTo( javaRuntimeVersion ) > 0 ) {
			error( "Java " + javaVersionMinimum + " or higher is required, found: " + javaRuntimeVersion );
			return false;
		}
		return true;
	}

	private final void setLogLevel( Parameters parameters ) {
		Log.setLevel( Log.parseLevel( parameters.get( "log.level" ) ) );
	}

	private final void describe( Parameters parameters ) {
		descriptor = findApplicationDescriptor( parameters );

		// Get the application attributes.
		setName( descriptor.getValue( "/program/information/title", getName() ) );
		namespace = descriptor.getValue( "/program/information/namespace", namespace );
		identifier = descriptor.getValue( "/program/information/identifier", identifier );
		Version version = Version.parse( descriptor.getValue( "/program/information/version", null ) );
		Date timestamp = DateUtil.parse( descriptor.getValue( "/program/information/timestamp", null ), DESCRIPTOR_DATE_FORMAT );
		javaVersionMinimum = descriptor.getValue( "/program/resources/java/@version", JAVA_VERSION_MINIMUM );
		release = new Release( version, timestamp );

		identifier = parameters.get( "identifier", identifier );
		if( parameters.isSet( "development" ) ) identifier += "-dev";

		try {
			com.parallelsymmetry.escape.utility.Preferences preferences = (com.parallelsymmetry.escape.utility.Preferences)getPreferences();
			// Only reset the preferences when you need to start with a clean slate.
			if( parameters.isSet( "preferences.reset" ) ) preferences.reset();

			InputStream defaultPreferencesStream = getClass().getResourceAsStream( DEFAULT_PREFERENCE_FILE_PATH );
			if( defaultPreferencesStream != null ) {
				preferences.loadDefaults( defaultPreferencesStream );
			} else {
				Log.write( Log.DEBUG, "Default preferences not found." );
			}

			InputStream regularPreferencesStream = getClass().getResourceAsStream( REGULAR_PREFERENCE_FILE_PATH );
			if( regularPreferencesStream != null ) {
				preferences.loadDefaults( regularPreferencesStream );
			} else {
				Log.write( Log.DEBUG, "Regular preferences not found." );
			}
		} catch( IOException exception ) {
			error( exception );
		}

		try {
			inceptionYear = Integer.parseInt( descriptor.getValue( "/jnlp/information/inception" ) );
		} catch( NumberFormatException exception ) {
			inceptionYear = Calendar.getInstance().get( Calendar.YEAR );
		}
		copyrightHolder = descriptor.getValue( "/jnlp/information/vendor", copyrightHolder );
		licenseSummary = descriptor.getValue( "/jnlp/information/license/summary", licenseSummary );

		// Create the identifier from the name if it is not set.
		if( TextUtil.isEmpty( this.identifier ) ) identifier = getName().replace( ' ', '-' ).toLowerCase();
	}

	/**
	 * Find the home directory.
	 * 
	 * @param parameters
	 * @return
	 */
	private final File findHome( Parameters parameters ) {
		File home = null;

		try {
			// Check the class path.
			List<File> entries = JavaUtil.parseSystemClasspath( System.getProperty( "java.class.path" ) );
			if( entries.size() > 0 && entries.get( 0 ).getName().endsWith( ".jar" ) ) {
				home = entries.get( 0 ).getParentFile().getParentFile();
			}

			// If -home was specified on the command line use it.
			if( home == null && parameters.get( HOME_PARAMETER_NAME ) != null ) {
				home = new File( parameters.get( HOME_PARAMETER_NAME ) ).getCanonicalFile();
			}

			// If no home is found, use the current working directory.
			if( home == null ) {
				home = new File( System.getProperty( "user.dir" ) );
			}

			return home.getCanonicalFile();
		} catch( IOException exception ) {
			exception.printStackTrace();
		}

		return home;
	}

	private final Descriptor findApplicationDescriptor( Parameters parameters ) {
		Descriptor descriptor = null;

		try {
			InputStream input = getClass().getResourceAsStream( APPLICATION_DESCRIPTOR_PATH );
			if( input != null ) Log.write( Log.DEBUG, "Application descriptor found: " + APPLICATION_DESCRIPTOR_PATH );
			descriptor = new Descriptor( input );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		return descriptor;
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
		Log.write( Log.TRACE, "Home: " + getHome() );
	}

	private final void printStatus() {
		Log.write( getName() + " status: " + getStatus() );
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

		private Handler handler;

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
				handler = new PeerLogHandler( this, socket.getOutputStream() );
				handler.setLevel( Log.parseLevel( parameters.get( "log.level" ) ) );
				Log.addHandler( handler );

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
				if( handler != null ) handler.close();
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
			if( handler != null ) handler.close();
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

package com.parallelsymmetry.service.systest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.zip.ZipFile;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.parallelsymmetry.service.ServiceFlag;
import com.parallelsymmetry.service.ServiceFlagValue;
import com.parallelsymmetry.utility.ConsoleReader;
import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.IoUtil;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.XmlUtil;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;
import com.parallelsymmetry.utility.product.ProductCard;

public abstract class BaseTestCase extends TestCase {

	protected static final String TIMESTAMP = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9]";

	protected static final String PROGRAM_EXITING = "\\[I\\] " + TIMESTAMP + " Program exiting to apply updates.";

	protected static final String UPDATES_DETECTED = "\\[I\\] " + TIMESTAMP + " Staged updates detected: 1";

	protected static final String SERVICE_STARTED = "\\[I\\] " + TIMESTAMP + " Verify Service started.";

	protected static final String SERVICE_STOPPED = "\\[I\\] " + TIMESTAMP + " Verify Service stopped.";

	protected static final String AUTO_TERMINATION = "\\[I\\] " + TIMESTAMP + " " + VerifyService.AUTO_TERMINATION_MESSAGE;

	protected static final String VERIFY_TXT = "verify.txt";

	protected static final File TARGET = new File( "target" );

	protected static final File DEPENDENCY = new File( TARGET, "dependency" );

	protected static final File SANDBOX = new File( TARGET, "sandbox" );

	protected static final File INSTALL = new File( SANDBOX, "install" );

	protected static final File UPDATE = new File( SANDBOX, "update" );

	protected static final String PROGRAM_JAR_NAME = "verify.jar";

	protected static final String SERVICE_JAR_NAME = "service.jar";

	protected static final String UTILITY_JAR_NAME = "utility.jar";

	protected static final String UPDATER_JAR_NAME = "updater.jar";

	protected static final String UPDATE_PACK_NAME = "update.jar";

	protected static final String UPDATE_PACK_DESCRIPTOR_NAME = "update.xml";

	protected static final Random random = new Random();

	/**
	 * The amount of time given for all the processes to complete. Note that this
	 * must be enough time for the build computer to complete all test processes.
	 * The build computer is generally not as powerful as developer computers so
	 * the delay may need to be longer than what is necessary for a developer.
	 */
	protected static final int PROCESS_EXECUTE_TIME = 9000;

	private static final int TIMESTAMP_OFFSET = 61174;

	protected List<String> libraries = new ArrayList<String>();

	protected Map<String, Long> codes = new HashMap<String, Long>();

	@Override
	public void setUp() throws Exception {
		Log.setLevel( Log.NONE );
	}

	protected void clean() throws Exception {
		assertTrue( FileUtil.delete( SANDBOX ) );
	}

	protected void stage() throws Exception {
		// Set up a deployed project layout.
		INSTALL.mkdirs();
		FileUtil.copy( new File( TARGET, "verify.jar" ), new File( INSTALL, "verify.jar" ) );
		FileUtil.copy( new File( DEPENDENCY, "service.jar" ), new File( INSTALL, "service.jar" ) );
		FileUtil.copy( new File( DEPENDENCY, "utility.jar" ), new File( INSTALL, "utility.jar" ) );
		FileUtil.copy( new File( DEPENDENCY, "updater.jar" ), new File( INSTALL, "updater.jar" ) );

		libraries.add( PROGRAM_JAR_NAME );
		libraries.add( SERVICE_JAR_NAME );
		libraries.add( UTILITY_JAR_NAME );
		libraries.add( UPDATER_JAR_NAME );

		// Modify the service library to have the correct URIs.
		changeLibrary( new File( INSTALL, "verify.jar" ), new ModifyProductDescriptor( SANDBOX.toURI() ) );

		// Create an update for the deployed project.
		UPDATE.mkdirs();

		// Create new libraries in the UPDATE_LIB folder.
		for( String library : libraries ) {
			File source = new File( INSTALL, library );
			File target = new File( UPDATE, library );
			assertTrue( "Missing library. Run 'mvn package' to solve this issue: " + source, source.exists() );

			long code = random.nextLong();
			FileUtil.copy( source, target );
			changeLibrary( target, new ModifyReleaseTimestamp(), new ModifyAddVerifyCode( code ) );
			codes.put( library, code );
		}

		// Zip up the update folder to make the update pack.
		FileUtil.zip( UPDATE, new File( SANDBOX, UPDATE_PACK_NAME ) );

		// Create the product update descriptor.
		File updatePackDescriptor = new File( SANDBOX, UPDATE_PACK_DESCRIPTOR_NAME );
		FileUtil.copy( new File( "target/main/java/META-INF/product.xml" ), updatePackDescriptor );
		changeProductSourceUri( updatePackDescriptor, SANDBOX.toURI() );

		// This implementation, while elegant, leaves the file locked.
		//		URL url = new URL( "jar:" + new File( INSTALL, "verify.jar" ).toURI().toString() + "!" + ProductManager.PRODUCT_DESCRIPTOR_PATH );
		//		URLConnection c = url.openConnection();
		//		InputStream input = url.openStream();
		//		IoUtil.copy( input, new FileOutputStream( updatePackDescriptor ) );
		//		input.close();

		// Load the product update descriptor.
		Document programDescriptor = XmlUtil.loadXmlDocument( updatePackDescriptor );

		// Change the timestamp.
		XPath xpath = XPathFactory.newInstance().newXPath();
		Node node = (Node)xpath.evaluate( ProductCard.TIMESTAMP_PATH, programDescriptor, XPathConstants.NODE );
		long timestamp = Long.parseLong( node.getTextContent() );
		node.setTextContent( String.valueOf( timestamp + TIMESTAMP_OFFSET ) );

		// Save the product update descriptor.
		XmlUtil.save( programDescriptor, updatePackDescriptor );
	}

	protected void changeLibrary( File source, Modification... modifications ) throws Exception {
		// Create a temporary folder to work in.
		File folder = FileUtil.createTempFolder( "service", "test" );
		folder.deleteOnExit();

		// Unzip the source file into a temporary folder.
		FileUtil.unzip( source, folder );

		for( Modification modification : modifications ) {
			modification.modify( folder );
		}

		// Rezip the temporary folder to the source file.
		FileUtil.zip( folder, source );
	}

	protected void executeUpdate() throws Exception {
		// Build the classpath.
		StringBuilder classpath = new StringBuilder();
		for( String library : libraries ) {
			if( classpath.length() > 0 ) classpath.append( File.pathSeparator );
			classpath.append( INSTALL.getName() );
			classpath.append( File.separator );
			classpath.append( library );
		}

		// Create the process builder.
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory( INSTALL );
		builder.redirectErrorStream( true );

		// This has to stay at INFO or lower(TRACE, DEBUG, etc.) in order to work correctly.
		Level logLevel = Log.INFO;

		// Add the process commands.
		builder.command().add( "java" );
		builder.command().add( "-jar" );
		builder.command().add( "verify.jar" );
		builder.command().add( ServiceFlag.EXECMODE );
		builder.command().add( ServiceFlagValue.TEST );
		builder.command().add( LogFlag.LOG_LEVEL );
		builder.command().add( logLevel.getName() );
		builder.command().add( LogFlag.LOG_DATE );
		builder.command().add( LogFlag.LOG_FILE );
		builder.command().add( "verify.log" );
		builder.command().add( LogFlag.LOG_FILE_LEVEL );
		builder.command().add( logLevel.getName() );
		builder.command().add( LogFlag.LOG_FILE_APPEND );

		Log.write( Log.INFO, "Launching: " + TextUtil.toString( builder.command(), " " ) );

		// Start the process.
		Process process = builder.start();

		// Need to read the process output or the buffer fills and hangs the process.
		new ConsoleReader( process ).start();

		assertEquals( "Verify Service process exit code", 0, process.waitFor() );
	}

	protected Long getVerifyCode( File file ) throws Exception {
		ZipFile zip = null;
		try {
			zip = new ZipFile( file );
			InputStream input = zip.getInputStream( zip.getEntry( VERIFY_TXT ) );
			return Long.parseLong( IoUtil.load( input ) );
		} catch( Throwable throwable ) {
			return null;
		} finally {
			if( zip != null ) zip.close();
		}
	}

	private static interface Modification {

		void modify( File folder ) throws Exception;

	}

	private static class ModifyReleaseTimestamp implements Modification {

		@Override
		public void modify( File folder ) throws Exception {
			// Update the timestamp in the product.xml file.
			File programDescriptorFile = new File( folder, "META-INF/product.xml" );
			if( programDescriptorFile.exists() ) {
				Document programDescriptor = XmlUtil.loadXmlDocument( programDescriptorFile );
				XPath xpath = XPathFactory.newInstance().newXPath();
				Node node = (Node)xpath.evaluate( ProductCard.TIMESTAMP_PATH, programDescriptor, XPathConstants.NODE );

				if( node != null ) {
					long timestamp = Long.parseLong( node.getTextContent() );
					node.setTextContent( String.valueOf( timestamp + TIMESTAMP_OFFSET ) );

					XmlUtil.save( programDescriptor, programDescriptorFile );
				}
			}
		}

	}

	private static class ModifyAddVerifyCode implements Modification {

		private long code;

		public ModifyAddVerifyCode( long code ) {
			this.code = code;
		}

		@Override
		public void modify( File folder ) throws Exception {
			// Add the verify file.
			File verify = new File( folder, VERIFY_TXT );
			PrintStream output = new PrintStream( new FileOutputStream( verify ) );
			try {
				IoUtil.save( String.valueOf( code ), output );
			} finally {
				if( output != null ) output.close();
			}
		}

	}

	private static class ModifyProductDescriptor implements Modification {

		private URI sourceUri;

		public ModifyProductDescriptor( URI sourceUri ) {
			this.sourceUri = sourceUri;
		}

		@Override
		public void modify( File folder ) throws Exception {
			File programDescriptorFile = new File( folder, "META-INF/product.xml" );
			changeProductSourceUri( programDescriptorFile, sourceUri );
		}

	}

	private static void changeProductSourceUri( File programDescriptorFile, URI sourceUri ) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		Document programDescriptor = XmlUtil.loadXmlDocument( programDescriptorFile );

		// Fix the pack URI.
		Node packUriNode = (Node)xpath.evaluate( ProductCard.RESOURCES_PATH + "/pack/@uri", programDescriptor, XPathConstants.NODE );
		packUriNode.setTextContent( sourceUri.resolve( "update.jar" ).toASCIIString() );

		// Fix the source URI.
		Node sourceUriNode = (Node)xpath.evaluate( ProductCard.SOURCE_URI_PATH, programDescriptor, XPathConstants.NODE );
		sourceUriNode.setTextContent( sourceUri.resolve( "update.xml" ).toASCIIString() );

		// Save the pack descriptor.
		XmlUtil.save( programDescriptor, programDescriptorFile );
	}

}

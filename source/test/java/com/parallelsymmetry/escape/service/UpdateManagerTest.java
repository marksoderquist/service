package com.parallelsymmetry.escape.service;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.update.UpdateManager;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.XmlUtil;
import com.parallelsymmetry.escape.utility.log.Log;

public class UpdateManagerTest extends BaseTestCase {

	protected static final File SOURCE = new File( "source" );

	protected static final File TARGET = new File( "target" );

	protected static final File SANDBOX = new File( TARGET, "sandbox" );

	protected static final File UPDATE = new File( SANDBOX, "update" );

	protected static final String UPDATE_PACK_NAME = "update.jar";

	protected static final String UPDATE_PACK_DESCRIPTOR_NAME = "update.xml";

	@Override
	public void setUp() {
		Log.setLevel( Log.DEBUG );
	}

	public void testStagePostedUpdates() throws Exception {
		stageUpdate();

		Service service = new MockService();

		// Reset the preferences but don't start the program.
		service.call( "-settings.reset", "-stop" );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning() );

		// Start the task manager.
		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning() );

		// Stage the posted updates.
		UpdateManager manager = service.getUpdateManager();
		manager.stagePostedUpdates();

		File stageFolder = new File( service.getProgramDataFolder(), "stage" );
		File updateFile = new File( stageFolder, service.getPack().getKey() + ".jar" );

		System.err.println( updateFile.toString() );
		assertTrue( updateFile.exists() );

		// Shutdown the service.
		service.call( "-stop" );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning() );
	}

	private void stageUpdate() throws Exception {
		UPDATE.mkdirs();

		// Create an update for the deployed project.
		UPDATE.mkdirs();

		// Zip up the update folder to make the update.
		FileUtil.copy( new File( SOURCE, "test/update.jar" ), new File( SANDBOX, UPDATE_PACK_NAME ) );

		// Copy the pack descriptor.
		File programDescriptorFile = new File( TARGET, "test/java/META-INF/program.xml" );
		Document programDescriptor = XmlUtil.loadXmlDocument( programDescriptorFile );

		// Change the timestamp.
		XPath xpath = XPathFactory.newInstance().newXPath();
		Node node = (Node)xpath.evaluate( "/pack/timestamp", programDescriptor, XPathConstants.NODE );

		long timestamp = Long.parseLong( node.getTextContent() );
		node.setTextContent( String.valueOf( timestamp + 1000 ) );

		// Save the pack descriptor.
		File updatePackDescriptor = new File( SANDBOX, UPDATE_PACK_DESCRIPTOR_NAME );
		XmlUtil.save( programDescriptor, updatePackDescriptor );
	}

}

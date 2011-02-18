package com.parallelsymmetry.escape.service;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.update.UpdateManager;
import com.parallelsymmetry.escape.service.update.UpdateSite;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.XmlUtil;
import com.parallelsymmetry.escape.utility.log.Log;

import junit.framework.TestCase;

public class UpdateManagerTest extends TestCase {

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
		// Stop any running test.
		service.call( "-stop" );

		// Reset the preferences but don't start the program.
		service.call( "-preferences.reset", "-stop" );

		// Add test update site.
		UpdateManager manager = service.getUpdateManager();
		UpdateSite site = new UpdateSite( UPDATE.toURI() );
		manager.addSite( site );
		
		manager.stagePostedUpdates();

		File stageFolder = new File( service.getProgramDataFolder(), "stage" );
		File updateFile = new File( stageFolder, "update.jar" );
		
		//assertTrue( updateFile.exists() );
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

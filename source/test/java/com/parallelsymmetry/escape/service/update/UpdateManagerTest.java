package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.service.MockService;
import com.parallelsymmetry.escape.service.update.UpdateManager.ApplyOption;
import com.parallelsymmetry.escape.service.update.UpdateManager.CheckOption;
import com.parallelsymmetry.escape.service.update.UpdateManager.FoundOption;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.XmlUtil;

public class UpdateManagerTest extends BaseTestCase {

	private static final int TIMESTAMP_OFFSET = 61174;

	private MockService service;

	private UpdateManager updateManager;

	@Override
	public void setUp() {
		super.setUp();
		service = new MockService();
		updateManager = service.getUpdateManager();
	}

	public void testGetInstalledPacks() {
		assertEquals( service.getCard(), updateManager.getInstalledPacks().values().iterator().next() );
	}

	public void testGetUpdaterPath() {
		assertEquals( new File( service.getHomeFolder(), "updater.jar" ), updateManager.getUpdaterPath() );
	}

	public void testSetUpdaterPath() {
		String updater = "testupdater.jar";
		updateManager.setUpdaterPath( new File( service.getHomeFolder(), updater ) );
		assertEquals( new File( service.getHomeFolder(), updater ), updateManager.getUpdaterPath() );
	}

	public void testGetCheckOption() {
		assertEquals( CheckOption.DISABLED, updateManager.getCheckOption() );
	}

	public void testSetCheckOption() {
		updateManager.setCheckOption( CheckOption.MANUAL );
		assertEquals( CheckOption.MANUAL, updateManager.getCheckOption() );
	}

	public void testGetFoundOption() {
		assertEquals( FoundOption.STAGE, updateManager.getFoundOption() );
	}

	public void testSetFoundOption() {
		updateManager.setFoundOption( FoundOption.SELECT );
		assertEquals( FoundOption.SELECT, updateManager.getFoundOption() );
	}

	public void testGetApplyOption() {
		assertEquals( ApplyOption.RESTART, updateManager.getApplyOption() );
	}

	public void testSetApplyOption() {
		updateManager.setApplyOption( ApplyOption.VERIFY );
		assertEquals( ApplyOption.VERIFY, updateManager.getApplyOption() );
	}

	public void testGetPostedUpdates() throws Exception {
		File packDescriptor = new File( "target/sandbox/update.xml" );

		service.getTaskManager().start();
		updateManager.setCheckOption( CheckOption.MANUAL );

		FileUtil.delete( packDescriptor );
		try {
			updateManager.getPostedUpdates().size();
			fail( "UpdateManager should throw an exception when the pack descriptor cannot be found." );
		} catch( ExecutionException exception ) {
			// Intentionally ignore exception.
		}

		FileUtil.copy( new File( "source/test/resources/META-INF/program.xml" ), packDescriptor );
		updatePackDescriptorTimestamp( packDescriptor );
		assertEquals( 1, updateManager.getPostedUpdates().size() );
	}

	private void updatePackDescriptorTimestamp( File descriptor ) throws Exception {
		// Update the timestamp in the descriptor file.
		if( descriptor.exists() ) {
			Document programDescriptor = XmlUtil.loadXmlDocument( descriptor );
			XPath xpath = XPathFactory.newInstance().newXPath();
			Node node = (Node)xpath.evaluate( "/pack/timestamp", programDescriptor, XPathConstants.NODE );

			if( node != null ) {
				long timestamp = Long.parseLong( node.getTextContent() );
				node.setTextContent( String.valueOf( timestamp + TIMESTAMP_OFFSET ) );

				XmlUtil.save( programDescriptor, descriptor );
			}
		}

	}
}

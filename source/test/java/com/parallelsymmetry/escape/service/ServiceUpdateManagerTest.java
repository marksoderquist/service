package com.parallelsymmetry.escape.service;

import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.parallelsymmetry.escape.product.ProductCard;
import com.parallelsymmetry.escape.service.ServiceProductManager.ApplyOption;
import com.parallelsymmetry.escape.service.ServiceProductManager.CheckOption;
import com.parallelsymmetry.escape.service.ServiceProductManager.FoundOption;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.XmlUtil;

public class ServiceUpdateManagerTest extends BaseTestCase {

	protected static final File SOURCE = new File( "source" );

	protected static final File TARGET = new File( "target" );

	protected static final File SANDBOX = new File( TARGET, "sandbox" );

	protected static final File UPDATE = new File( SANDBOX, "update" );

	protected static final String UPDATE_PACK_NAME = "update.jar";

	protected static final String UPDATE_CARD_NAME = "update.xml";

	private static final File SOURCE_PRODUCT_CARD = new File( SOURCE, "test/resources/META-INF/product.xml" );

	private static final File SOURCE_PRODUCT_PACK = new File( SOURCE, "test/update.jar" );

	private static final File TARGET_SERVICE_CARD = new File( TARGET, "test/java/META-INF/product.xml" );

	private static final File TARGET_UPDATE_PACK = new File( SANDBOX, UPDATE_PACK_NAME );

	private static final File TARGET_UPDATE_CARD = new File( SANDBOX, UPDATE_CARD_NAME );

	private static final int TIMESTAMP_OFFSET = 61174;

	private MockService service;

	private ServiceProductManager updateManager;

	@Override
	public void setUp() {
		super.setUp();
		service = new MockService();
		updateManager = service.getProductManager();
	}

	public void testGetInstalledPacks() {
		assertEquals( service.getCard(), updateManager.getProducts().iterator().next() );
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
		// Initialize the product card before starting the service.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_SERVICE_CARD ) );

		// Start the service set the update manager for manual checks.
		service.getTaskManager().start();
		updateManager.setCheckOption( CheckOption.MANUAL );

		// Remove the old update card if it exists.
		FileUtil.delete( TARGET_UPDATE_CARD );

		// Ensure there are no posted updates.
		try {
			updateManager.getPostedUpdates().size();
			fail( "UpdateManager should throw an exception when the pack descriptor cannot be found." );
		} catch( ExecutionException exception ) {
			// Intentionally ignore exception.
		}

		// Create the update card and modify the timestamp.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_UPDATE_CARD ) );
		fixProductCardData( TARGET_UPDATE_CARD );

		// Ensure there are posted updates.
		assertEquals( 1, updateManager.getPostedUpdates().size() );
	}

	public void testStagePostedUpdates() throws Exception {
		stageUpdate();

		File stageFolder = new File( service.getProgramDataFolder(), ServiceProductManager.UPDATE_FOLDER_NAME );
		File updateFile = new File( stageFolder, updateManager.getStagedUpdateFileName( service.getCard() ) );

		// Cleanup from previous run.
		FileUtil.delete( stageFolder );
		assertFalse( stageFolder.exists() );

		// Reset the preferences but don't start the program.
		service.call( ServiceFlag.SETTINGS_RESET, ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning() );

		// Start the service.
		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning() );

		ServiceProductManager manager = service.getProductManager();
		try {
			// Enable the update manager temporarily.
			manager.setCheckOption( ServiceProductManager.CheckOption.STARTUP );
			manager.stagePostedUpdates();
			assertTrue( updateFile.exists() );
		} finally {
			// Disable the update manager.
			manager.setCheckOption( ServiceProductManager.CheckOption.DISABLED );

			// Shutdown the service.
			service.call( ServiceFlag.STOP );
			service.waitForShutdown( TIMEOUT, TIMEUNIT );
		}
		assertFalse( service.isRunning() );
	}

	private void stageUpdate() throws Exception {
		// Create an update for the deployed project.
		UPDATE.mkdirs();

		// Create the update pack.
		FileUtil.copy( SOURCE_PRODUCT_PACK, TARGET_UPDATE_PACK );

		// Create the update card and modify the timestamp.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_UPDATE_CARD ) );
		fixProductCardData( TARGET_UPDATE_CARD );
	}

	private void fixProductCardData( File descriptor ) throws Exception {
		// Update the timestamp in the descriptor file.
		if( descriptor.exists() ) {
			Document programDescriptor = XmlUtil.loadXmlDocument( descriptor );
			XPath xpath = XPathFactory.newInstance().newXPath();

			// Change the timestamp to a time in the future.
			Node timestampNode = (Node)xpath.evaluate( ProductCard.TIMESTAMP_PATH, programDescriptor, XPathConstants.NODE );
			long timestamp = Long.parseLong( timestampNode.getTextContent() );
			timestampNode.setTextContent( String.valueOf( timestamp + TIMESTAMP_OFFSET ) );

			// Fix the icon URI.
			Node iconUriNode = (Node)xpath.evaluate( ProductCard.ICON_PATH, programDescriptor, XPathConstants.NODE );
			iconUriNode.setTextContent( "icon.png" );

			// Fix the pack URI.
			Node packUriNode = (Node)xpath.evaluate( ProductCard.RESOURCES_PATH + "/pack/@uri", programDescriptor, XPathConstants.NODE );
			packUriNode.setTextContent( "update.jar" );

			// Fix the source URI.
			Node sourceUriNode = (Node)xpath.evaluate( ProductCard.SOURCE_URI_PATH, programDescriptor, XPathConstants.NODE );
			sourceUriNode.setTextContent( "update.xml" );

			XmlUtil.save( programDescriptor, descriptor );
		}

	}

}

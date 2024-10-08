package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.BaseServiceTest;
import com.parallelsymmetry.service.product.ProductManager.*;
import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.XmlUtil;
import com.parallelsymmetry.utility.product.ProductCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class ProductManagerTest extends BaseServiceTest {

	protected static final File SOURCE = new File( "source" );

	protected static final File TARGET = new File( "target" );

	protected static final File SANDBOX = new File( TARGET, "sandbox" );

	protected static final File UPDATE = new File( SANDBOX, "update" );

	protected static final String UPDATE_CARD_NAME = "update.xml";

	protected static final String UPDATE_PACK_NAME = "update.jar";

	private static final File SOURCE_PRODUCT_CARD = new File( SOURCE, "test/resources/META-INF/product.xml" );

	private static final File TARGET_PRODUCT_CARD = new File( TARGET, "test/java/META-INF/product.xml" );

	private static final File SOURCE_UPDATE_PACK = new File( SOURCE, "test/update.jar" );

	private static final File TARGET_UPDATE_CARD = new File( SANDBOX, UPDATE_CARD_NAME );

	private static final File TARGET_UPDATE_PACK = new File( SANDBOX, UPDATE_PACK_NAME );

	private static final int TIMESTAMP_OFFSET = 31174;

	private static final String TEST_PRODUCT = "/META-INF/product.test.xml";

	private ProductManager manager;

	@BeforeEach
	@Override
	public void setup() throws Exception {
		super.setup();
		manager = service.getProductManager();
	}

	@Test
	public void testGetInstalledPacks() {
		assertEquals( service.getCard(), manager.getProductCards().iterator().next() );
	}

	@Test
	public void testGetUpdaterPath() throws Exception {
		assertEquals( new File( service.getHomeFolder(), "updater.jar" ).getCanonicalPath(), manager.getUpdaterPath().getCanonicalPath() );
	}

	@Test
	public void testSetUpdaterPath() {
		String updater = "testupdater.jar";
		manager.setUpdaterPath( new File( service.getHomeFolder(), updater ) );
		assertEquals( new File( service.getHomeFolder(), updater ), manager.getUpdaterPath() );
	}

	@Test
	public void testGetCheckOption() {
		assertEquals( CheckOption.MANUAL, manager.getCheckOption() );
	}

	@Test
	public void testGetFoundOption() {
		assertEquals( FoundOption.STAGE, manager.getFoundOption() );
	}

	@Test
	public void testSetFoundOption() {
		manager.setFoundOption( FoundOption.SELECT );
		assertEquals( FoundOption.SELECT, manager.getFoundOption() );
	}

	@Test
	public void testGetApplyOption() {
		assertEquals( ApplyOption.RESTART, manager.getApplyOption() );
	}

	@Test
	public void testSetApplyOption() {
		manager.setApplyOption( ApplyOption.VERIFY );
		assertEquals( ApplyOption.VERIFY, manager.getApplyOption() );
	}

	@Test
	public void testGetPostedUpdatesWithMissingSource() throws Exception {
		// Initialize the product card before starting the service.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_PRODUCT_CARD ) );

		// Remove the old update card if it exists.
		assertTrue( FileUtil.delete( TARGET_UPDATE_CARD ) );

		// Start the service set the update manager for manual checks.
		service.getTaskManager().start();
		manager.setCheckOption( CheckOption.MANUAL );

		// Ensure there are no posted updates.
		try {
			manager.getPostedUpdates( true );
			fail( "UpdateManager should throw an exception when the pack descriptor cannot be found." );
		} catch( ExecutionException exception ) {
			// Intentionally ignore exception.
		}
	}

	@Test
	public void testGetPostedUpdates() throws Exception {
		// Initialize the product card before starting the service.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_PRODUCT_CARD ) );

		URI uri = TARGET_PRODUCT_CARD.toURI();
		ProductCard card = new ProductCard( uri, new Descriptor( uri ) );
		MockModule product = new MockModule( service, card );
		manager.registerProduct( product );
		manager.setUpdatable( card, true );

		// Start the service set the update manager for manual checks.
		service.getTaskManager().start();
		manager.setCheckOption( CheckOption.MANUAL );

		// Create the update card and modify the timestamp.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_UPDATE_CARD ) );
		fixProductCardData( TARGET_UPDATE_CARD );

		// Ensure there are posted updates.
		assertEquals( 1, manager.getPostedUpdates( true ).size() );
	}

	@Test
	public void testStagePostedUpdates() throws Exception {
		File stageFolder = new File( service.getDataFolder(), ProductManager.UPDATE_FOLDER_NAME );
		File updateFile = new File( stageFolder, manager.getStagedUpdateFileName( service.getCard() ) );

		// Cleanup from previous run.
		FileUtil.delete( stageFolder );
		assertFalse( stageFolder.exists() );

		// Create an update for the deployed project.
		UPDATE.mkdirs();

		// Create the update pack.
		FileUtil.copy( SOURCE_UPDATE_PACK, TARGET_UPDATE_PACK );

		// Create the update card.
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_PRODUCT_CARD ) );
		assertTrue( FileUtil.copy( SOURCE_PRODUCT_CARD, TARGET_UPDATE_CARD ) );

		URI uri = TARGET_PRODUCT_CARD.toURI();
		ProductCard card = new ProductCard( uri, new Descriptor( uri ) );
		card.setInstallFolder( SANDBOX );
		MockModule product = new MockModule( service, card );
		manager.registerProduct( product );
		manager.setUpdatable( card, true );

		// Modify the update card timestamp.
		fixProductCardData( TARGET_UPDATE_CARD );

		ProductManagerWatcher watcher = new ProductManagerWatcher();
		manager.addProductManagerListener( watcher );

		try {
			// Enable the update manager temporarily.
			manager.setCheckOption( ProductManager.CheckOption.MANUAL );
			assertEquals( 1, manager.stagePostedUpdates() );
			assertTrue( updateFile.exists() );
			assertEquals( 1, watcher.getEvents().size() );
			assertEquals( ProductManagerEvent.Type.PRODUCT_STAGED, watcher.getEvents().get( 0 ).getType() );
		} finally {
			// Disable the update manager.
			manager.setCheckOption( ProductManager.CheckOption.MANUAL );
		}
	}

	@Test
	public void testGetStagedUpdates() throws Exception {
		testStagePostedUpdates();
		URI uri = TARGET_UPDATE_CARD.toURI();
		ProductCard card = new ProductCard( uri, new Descriptor( uri ) );

		Set<ProductCard> cards = manager.getStagedUpdates();
		assertEquals( 1, cards.size() );

		assertTrue( card.deepEquals( cards.iterator().next() ) );
	}

	@Test
	public void testProductEnabled() throws Exception {
		URL url = getClass().getResource( TEST_PRODUCT );
		Descriptor descriptor = new Descriptor( url );
		ProductCard card = new ProductCard( url.toURI(), descriptor );

		ProductManagerWatcher watcher = new ProductManagerWatcher();
		manager.addProductManagerListener( watcher );
		assertEquals( 0, watcher.getEvents().size() );

		// Check the behavior before adding the product.
		int eventIndex = 0;
		manager.setEnabled( card, true );
		assertTrue( manager.isEnabled( card ) );
		assertEquals( 1, watcher.getEvents().size() );
		assertEquals( ProductManagerEvent.Type.PRODUCT_ENABLED, watcher.getEvents().get( eventIndex++ ).getType() );

		MockModule product = new MockModule( service, card );
		manager.registerProduct( product );
		assertTrue( manager.isEnabled( card ) );

		manager.setEnabled( card, false );
		assertFalse( manager.isEnabled( card ) );
		assertEquals( 2, watcher.getEvents().size() );
		assertEquals( ProductManagerEvent.Type.PRODUCT_DISABLED, watcher.getEvents().get( eventIndex++ ).getType() );

		manager.setEnabled( card, true );
		assertTrue( manager.isEnabled( card ) );
		assertEquals( 3, watcher.getEvents().size() );
		assertEquals( ProductManagerEvent.Type.PRODUCT_ENABLED, watcher.getEvents().get( eventIndex++ ).getType() );
	}

	@Test
	public void testProductUpdatable() throws Exception {
		URL url = getClass().getResource( TEST_PRODUCT );
		Descriptor descriptor = new Descriptor( url );
		ProductCard card = new ProductCard( url.toURI(), descriptor );

		// Check the behavior before adding the product.
		manager.setUpdatable( card, false );
		assertFalse( manager.isUpdatable( card ) );

		MockModule product = new MockModule( service, card );
		manager.registerProduct( product );
		assertFalse( manager.isUpdatable( card ) );

		manager.setUpdatable( card, true );
		assertTrue( manager.isUpdatable( card ) );

		manager.setUpdatable( card, false );
		assertFalse( manager.isUpdatable( card ) );
	}

	@Test
	public void testProductRemovable() throws Exception {
		URL url = getClass().getResource( TEST_PRODUCT );
		Descriptor descriptor = new Descriptor( url );
		ProductCard card = new ProductCard( url.toURI(), descriptor );

		assertFalse( manager.isRemovable( card ) );

		MockModule product = new MockModule( service, card );
		manager.registerProduct( product );
		assertFalse( manager.isRemovable( card ) );

		manager.setRemovable( card, true );
		assertTrue( manager.isRemovable( card ) );

		manager.setRemovable( card, false );
		assertFalse( manager.isRemovable( card ) );
	}

	@Test
	public void testProductInstalled() throws Exception {
		URL url = getClass().getResource( TEST_PRODUCT );
		Descriptor descriptor = new Descriptor( url );
		ProductCard card = new ProductCard( url.toURI(), descriptor );

		assertFalse( manager.isInstalled( card ) );

		MockModule product = new MockModule( service, card );
		manager.registerProduct( product );
		assertTrue( manager.isInstalled( card ) );

		manager.uninstallProducts( card );
		assertFalse( manager.isInstalled( card ) );
	}

	@Test
	public void testInstalledProductClass() {
		File file = new File( "." );

		ProductManager.InstalledProduct installedProductA = new ProductManager.InstalledProduct( file );
		ProductManager.InstalledProduct installedProductB = new ProductManager.InstalledProduct( file );

		assertEquals( installedProductA, installedProductB );
		assertEquals( installedProductB, installedProductA );
		assertEquals( installedProductA.hashCode(), installedProductB.hashCode() );
		assertEquals( installedProductB.hashCode(), installedProductA.hashCode() );
	}

	@Test
	public void testGetNextIntervalTime() {
		long hour = 3600000;
		long day = 24 * hour;

		assertEquals( hour, ProductManager.getNextIntervalTime( 0, CheckInterval.HOUR, 0, 0 ) );
		assertEquals( day, ProductManager.getNextIntervalTime( 0, CheckInterval.DAY, 0, 0 ) );
		assertEquals( 7 * day, ProductManager.getNextIntervalTime( 0, CheckInterval.WEEK, 0, 0 ) );
		assertEquals( 30 * day, ProductManager.getNextIntervalTime( 0, CheckInterval.MONTH, 0, 0 ) );
	}

	@Test
	public void testGetNextScheuleTime() {
		long hour = 3600000;
		long day = 24 * hour;

		// Offset to get to nearest Sunday after epoch(on Thursday).
		long offset = 3 * day;

		// Check the daily values.
		for( int index = 0; index < 24; index++ ) {
			assertEquals( index * hour, ProductManager.getNextScheduleTime( 0, CheckWhen.DAILY, index ) );
		}

		for( CheckWhen when : CheckWhen.values() ) {
			if( when == CheckWhen.DAILY ) continue;
			for( int index = 0; index < 24; index++ ) {
				assertEquals( ((when.ordinal() - 1) * day) + (index * hour), ProductManager.getNextScheduleTime( offset, when, index ) );
			}
		}
	}

	private void fixProductCardData( File descriptor ) throws Exception {
		// Update the timestamp in the descriptor file.
		if( descriptor.exists() ) {
			Document productDescriptor = XmlUtil.loadXmlDocument( descriptor );
			XPath xpath = XPathFactory.newInstance().newXPath();

			// Change the timestamp to a time in the future.
			Node timestampNode = (Node)xpath.evaluate( ProductCard.TIMESTAMP_PATH, productDescriptor, XPathConstants.NODE );
			long timestamp = Long.parseLong( timestampNode.getTextContent() );
			timestampNode.setTextContent( String.valueOf( timestamp + TIMESTAMP_OFFSET ) );

			// Fix the icon URI.
			Node iconUriNode = (Node)xpath.evaluate( ProductCard.ICON_PATH, productDescriptor, XPathConstants.NODE );
			iconUriNode.setTextContent( "icon.png" );

			// Fix the pack URI.
			Node packUriNode = (Node)xpath.evaluate( ProductCard.RESOURCES_PATH + "/pack/@uri", productDescriptor, XPathConstants.NODE );
			packUriNode.setTextContent( "update.jar" );

			// Fix the source URI.
			Node sourceUriNode = (Node)xpath.evaluate( ProductCard.SOURCE_URI_PATH, productDescriptor, XPathConstants.NODE );
			sourceUriNode.setTextContent( "update.xml" );

			XmlUtil.save( productDescriptor, descriptor );
		}

	}

	private static class ProductManagerWatcher implements ProductManagerListener {

		private List<ProductManagerEvent> events;

		public ProductManagerWatcher() {
			reset();
		}

		public void reset() {
			events = new CopyOnWriteArrayList<>();
		}

		@Override
		public void eventOccurred( ProductManagerEvent event ) {
			events.add( event );
		}

		public List<ProductManagerEvent> getEvents() {
			return new ArrayList<>( events );
		}

	}

}

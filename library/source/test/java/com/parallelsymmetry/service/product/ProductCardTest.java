package com.parallelsymmetry.service.product;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.parallelsymmetry.service.BaseTestCase;
import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.Release;
import com.parallelsymmetry.utility.UriUtil;
import com.parallelsymmetry.utility.Version;
import com.parallelsymmetry.utility.mock.MockWritableSettingProvider;
import com.parallelsymmetry.utility.setting.Settings;

public class ProductCardTest extends BaseTestCase {

	private static final String COMPLETE_CARD = "/META-INF/product.card.complete.xml";

	private static final String MINIMAL_CARD = "/META-INF/product.card.minimum.xml";

	public void testAssertDescriptorPaths() {
		assertEquals( "/product", ProductCard.PRODUCT_PATH );
		assertEquals( "/product/group", ProductCard.GROUP_PATH );
		assertEquals( "/product/artifact", ProductCard.ARTIFACT_PATH );
	}

	public void testUriMethods() throws Exception {
		URL url = new URL( "jar:file:/C:/Program%20Files/Escape/program.jar!/META-INF/product.xml" );
		URI uri = url.toURI();

		assertTrue( uri.isOpaque() );
		assertEquals( "jar:file:/C:/Program%20Files/Escape/program.jar!/META-INF/product.xml", uri.toString() );
		assertEquals( "jar:file:/C:/Program%20Files/Escape/program.jar!/META-INF/otherfile.txt", UriUtil.resolve( uri, URI.create( "otherfile.txt" ) ).toString() );
		assertEquals( "jar:file:/C:/Program%20Files/Escape/program.jar!/META-INF/product.xml", URI.create( ".." ).resolve( uri ).toString() );
	}

	public void testGetKey() throws Exception {
		assertEquals( "com.parallelsymmetry.mock", loadCard( COMPLETE_CARD ).getProductKey() );
	}

	public void testGetGroup() throws Exception {
		assertEquals( "com.parallelsymmetry", loadCard( COMPLETE_CARD ).getGroup() );
	}

	public void testSetGroup() throws Exception {
		ProductCard card = loadCard( COMPLETE_CARD );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );

		card.setGroup( "com.parallelsymmetry.test" );
		assertEquals( "com.parallelsymmetry.test", card.getGroup() );
		assertEquals( "com.parallelsymmetry.test.mock", card.getProductKey() );
	}

	public void testGetArtifact() throws Exception {
		assertEquals( "mock", loadCard( COMPLETE_CARD ).getArtifact() );
	}

	public void testSetArtifact() throws Exception {
		ProductCard card = loadCard( COMPLETE_CARD );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );

		card.setArtifact( "test-mock" );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.test-mock", card.getProductKey() );
	}

	public void testGetRelease() throws Exception {
		assertEquals( "1.0.0 Alpha 00  1973-08-14 22:29:00", loadCard( COMPLETE_CARD ).getRelease().toHumanString() );
	}

	public void testSetRelease() throws Exception {
		ProductCard card = loadCard( COMPLETE_CARD );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );

		card.setRelease( new Release( new Version( "1.0.0-a-01" ), new Date( 114215340000L ) ) );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );
	}

	public void testGetIcon() throws Exception {
		assertEquals( new File( "target/sandbox/icon.png" ).toURI(), loadCard( COMPLETE_CARD ).getIconUri() );
	}

	public void testGetName() throws Exception {
		assertEquals( "Mock Service", loadCard( COMPLETE_CARD ).getName() );
	}

	public void testGetProvider() throws Exception {
		assertEquals( "Parallel Symmetry", loadCard( COMPLETE_CARD ).getProvider() );
	}

	public void testContributors() throws Exception {
		List<String> contributors = loadCard( COMPLETE_CARD ).getContributors();
		assertEquals( 3, contributors.size() );
		assertEquals( "Mark", contributors.get( 0 ) );
		assertEquals( "John", contributors.get( 1 ) );
		assertEquals( "Mike", contributors.get( 2 ) );
	}

	public void testGetInceptionYear() throws Exception {
		assertEquals( 1973, loadCard( COMPLETE_CARD ).getInceptionYear() );
	}

	public void testGetSummary() throws Exception {
		assertEquals( "Mock service for testing", loadCard( COMPLETE_CARD ).getSummary() );
	}

	public void testGetDescription() throws Exception {
		assertEquals( "The Mock Service is used for product development and testing.", loadCard( COMPLETE_CARD ).getDescription() );
	}

	public void testGetCopyrightHolder() throws Exception {
		assertEquals( "Parallel Symmetry", loadCard( COMPLETE_CARD ).getCopyrightHolder() );
	}

	public void testGetCopyrightNotice() throws Exception {
		assertEquals( "All rights reserved.", loadCard( COMPLETE_CARD ).getCopyrightNotice() );
	}

	public void testGetLicenseUri() throws Exception {
		assertEquals( URI.create( "http://www.parallelsymmetry.com/legal/software.license.html" ), loadCard( COMPLETE_CARD ).getLicenseUri() );
	}

	public void testGetLicenseSummary() throws Exception {
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY. This is open software, and you are welcome to redistribute it under certain conditions.", loadCard( COMPLETE_CARD ).getLicenseSummary() );
	}

	public void testGetSourceUri() throws Exception {
		assertEquals( new File( "target/sandbox/update.xml" ).toURI(), loadCard( COMPLETE_CARD ).getSourceUri() );
	}

	public void testGetInstallFolder() throws Exception {
		ProductCard card = loadCard( COMPLETE_CARD );
		assertNull( card.getInstallFolder() );

		card.setInstallFolder( new File( "." ) );
		assertEquals( new File( "." ), card.getInstallFolder() );
	}

	public void testEquals() throws Exception {
		URL url = getClass().getResource( COMPLETE_CARD );
		Descriptor descriptor = new Descriptor( url );

		ProductCard card1 = new ProductCard( url.toURI(), descriptor );
		ProductCard card2 = new ProductCard( url.toURI(), descriptor );
		assertTrue( card1.equals( card2 ) );

		card1.setRelease( new Release( new Version( "1" ) ) );
		card2.setRelease( new Release( new Version( "2" ) ) );
		assertTrue( card1.equals( card2 ) );

		card1.setRelease( new Release( new Version( "1" ) ) );
		card2.setRelease( new Release( new Version( "1" ) ) );
		card1.setArtifact( "card1" );
		card2.setArtifact( "card2" );
		assertFalse( card1.equals( card2 ) );
	}

	public void testDeepEquals() throws Exception {
		URL url = getClass().getResource( COMPLETE_CARD );
		Descriptor descriptor = new Descriptor( url );

		ProductCard card1 = new ProductCard( url.toURI(), descriptor );
		ProductCard card2 = new ProductCard( url.toURI(), descriptor );
		assertTrue( card1.deepEquals( card2 ) );
	}

	public void testHashCode() throws Exception {
		URL url = getClass().getResource( COMPLETE_CARD );
		Descriptor descriptor = new Descriptor( url );

		ProductCard card1 = new ProductCard( url.toURI(), descriptor );
		ProductCard card2 = new ProductCard( url.toURI(), descriptor );
		assertTrue( card1.hashCode() == card2.hashCode() );

		card1.setRelease( new Release( new Version( "1" ) ) );
		card2.setRelease( new Release( new Version( "2" ) ) );
		assertTrue( card1.hashCode() == card2.hashCode() );

		card1.setRelease( new Release( new Version( "1" ) ) );
		card2.setRelease( new Release( new Version( "1" ) ) );
		card1.setArtifact( "card1" );
		card2.setArtifact( "card2" );
		assertFalse( card1.hashCode() == card2.hashCode() );
	}

	public void testMinimalProductInfo() throws Exception {
		ProductCard card = loadCard( MINIMAL_CARD );

		// Check the required information.
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "mock", card.getArtifact() );
		assertEquals( new Release( new Version() ), card.getRelease() );

		// Check the human oriented information.
		assertEquals( null, card.getIconUri() );
		assertEquals( "com.parallelsymmetry", card.getProvider() );
		assertEquals( "mock", card.getName() );
		assertEquals( null, card.getSummary() );
		assertEquals( null, card.getDescription() );
		assertEquals( 0, card.getContributors().size() );

		// Check the copyright information.
		assertEquals( DateUtil.getCurrentYear(), card.getInceptionYear() );
		assertEquals( null, card.getCopyrightHolder() );
		assertEquals( null, card.getCopyrightNotice() );

		// Check the license information.
		assertEquals( null, card.getLicenseUri() );
		assertEquals( null, card.getLicenseSummary() );

		// Check the update information.
		assertEquals( getClass().getResource( MINIMAL_CARD ).toURI(), card.getSourceUri() );
	}

	public void testSaveLoadSaveSettings() throws Exception {
		ProductCard standard = loadCard( COMPLETE_CARD );
		MockWritableSettingProvider provider = new MockWritableSettingProvider();
		Settings settings = new Settings();
		settings.addProvider( provider );
		standard.saveSettings( settings );

		ProductCard test = new ProductCard( settings );
		assertEquals( standard, test );
	}

	public static final void assertEquals( ProductCard standard, ProductCard test ) {
		assertEquals( standard.getProductKey(), test.getProductKey() );
		assertEquals( standard.getGroup(), test.getGroup() );
		assertEquals( standard.getArtifact(), test.getArtifact() );
		assertEquals( standard.getRelease(), test.getRelease() );
		assertEquals( standard.getIconUri(), test.getIconUri() );
		assertEquals( standard.getName(), test.getName() );
		assertEquals( standard.getProvider(), test.getProvider() );
		assertEquals( standard.getInceptionYear(), test.getInceptionYear() );
		assertEquals( standard.getSummary(), test.getSummary() );
		assertEquals( standard.getDescription(), test.getDescription() );
		assertEquals( standard.getCopyrightHolder(), test.getCopyrightHolder() );
		assertEquals( standard.getCopyrightNotice(), test.getCopyrightNotice() );
		assertEquals( standard.getLicenseUri(), test.getLicenseUri() );
		assertEquals( standard.getLicenseSummary(), test.getLicenseSummary() );
		assertEquals( standard.getSourceUri(), test.getSourceUri() );
	}

	private ProductCard loadCard( String path ) throws Exception {
		URL url = getClass().getResource( path );
		Descriptor descriptor = new Descriptor( url );
		return new ProductCard( url.toURI(), descriptor );
	}

}

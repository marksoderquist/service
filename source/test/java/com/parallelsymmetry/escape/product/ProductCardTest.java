package com.parallelsymmetry.escape.product;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Date;

import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.UriUtil;
import com.parallelsymmetry.escape.utility.Version;

public class ProductCardTest extends BaseTestCase {

	private static final String MOCK_SERVICE = "/META-INF/product.mock.service.xml";

	private static final String MINIMAL_CARD = "/META-INF/product.minimal.info.xml";

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
		assertEquals( "com.parallelsymmetry.mock", loadCard( MOCK_SERVICE ).getProductKey() );
	}

	public void testGetGroup() throws Exception {
		assertEquals( "com.parallelsymmetry", loadCard( MOCK_SERVICE ).getGroup() );
	}

	public void testSetGroup() throws Exception {
		ProductCard card = loadCard( MOCK_SERVICE );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );

		card.setGroup( "com.parallelsymmetry.test" );
		assertEquals( "com.parallelsymmetry.test", card.getGroup() );
		assertEquals( "com.parallelsymmetry.test.mock", card.getProductKey() );
	}

	public void testGetArtifact() throws Exception {
		assertEquals( "mock", loadCard( MOCK_SERVICE ).getArtifact() );
	}

	public void testSetArtifact() throws Exception {
		ProductCard card = loadCard( MOCK_SERVICE );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );

		card.setArtifact( "test-mock" );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.test-mock", card.getProductKey() );
	}

	public void testGetRelease() throws Exception {
		assertEquals( "1.0.0 Alpha 00  1973-08-14 22:29:00", loadCard( MOCK_SERVICE ).getRelease().toHumanString() );
	}

	public void testSetRelease() throws Exception {
		ProductCard card = loadCard( MOCK_SERVICE );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );

		card.setRelease( new Release( new Version( "1.0.0-a-01" ), new Date( 114215340000L ) ) );
		assertEquals( "com.parallelsymmetry", card.getGroup() );
		assertEquals( "com.parallelsymmetry.mock", card.getProductKey() );
	}

	public void testGetIcon() throws Exception {
		assertEquals( new File( "target/sandbox/icon.png" ).toURI(), loadCard( MOCK_SERVICE ).getIconUri() );
	}

	public void testGetName() throws Exception {
		assertEquals( "Mock Service", loadCard( MOCK_SERVICE ).getName() );
	}

	public void testGetProvider() throws Exception {
		assertEquals( "Parallel Symmetry", loadCard( MOCK_SERVICE ).getProvider() );
	}

	public void testGetInceptionYear() throws Exception {
		assertEquals( 1973, loadCard( MOCK_SERVICE ).getInceptionYear() );
	}

	public void testGetSummary() throws Exception {
		assertEquals( "Mock service for testing", loadCard( MOCK_SERVICE ).getSummary() );
	}

	public void testGetDescription() throws Exception {
		assertEquals( "The Mock Service is used for product development and testing.", loadCard( MOCK_SERVICE ).getDescription() );
	}

	public void testGetCopyrightHolder() throws Exception {
		assertEquals( "Parallel Symmetry", loadCard( MOCK_SERVICE ).getCopyrightHolder() );
	}

	public void testGetCopyrightNotice() throws Exception {
		assertEquals( "All rights reserved.", loadCard( MOCK_SERVICE ).getCopyrightNotice() );
	}

	public void testGetLicenseUri() throws Exception {
		assertEquals( URI.create( "http://www.parallelsymmetry.com/legal/software.license.html" ), loadCard( MOCK_SERVICE ).getLicenseUri() );
	}

	public void testGetLicenseSummary() throws Exception {
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY. This is open software, and you are welcome to redistribute it under certain conditions.", loadCard( MOCK_SERVICE ).getLicenseSummary() );
	}

	public void testGetSourceUri() throws Exception {
		assertEquals( new File( "target/sandbox/update.xml" ).toURI(), loadCard( MOCK_SERVICE ).getSourceUri() );
	}

	public void testGetInstallFolder() throws Exception {
		ProductCard card = loadCard( MOCK_SERVICE );
		assertNull( card.getInstallFolder() );

		card.setInstallFolder( new File( "." ) );
		assertEquals( new File( "." ), card.getInstallFolder() );
	}

	public void testEquals() throws Exception {
		URL url = getClass().getResource( MOCK_SERVICE );
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

	public void testHashCode() throws Exception {
		URL url = getClass().getResource( MOCK_SERVICE );
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

	private ProductCard loadCard( String path ) throws Exception {
		URL url = getClass().getResource( path );
		Descriptor descriptor = new Descriptor( url );
		return new ProductCard( url.toURI(), descriptor );
	}

}

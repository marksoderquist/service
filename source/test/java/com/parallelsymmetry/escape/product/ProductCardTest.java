package com.parallelsymmetry.escape.product;

import java.io.InputStream;
import java.net.URI;

import org.junit.Before;

import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.Version;

public class ProductCardTest extends BaseTestCase {

	private Descriptor descriptor;

	private ProductCard card;

	@Override
	@Before
	public void setUp() {
		try {
			InputStream input = getClass().getResourceAsStream( "/META-INF/program.xml" );
			descriptor = new Descriptor( input );
			card = new ProductCard( descriptor );
		} catch( Exception exception ) {
			fail( exception.getMessage() );
		}
	}
	
	public void testAssertDescriptorPaths() {
		assertEquals( "/product", ProductCard.PRODUCT_PATH );
		assertEquals( "/product/group", ProductCard.GROUP_PATH );
		assertEquals( "/product/artifact", ProductCard.ARTIFACT_PATH );
	}

	public void testGetKey() throws Exception {
		assertEquals( "com.parallelsymmetry.escape.service.mock", card.getKey() );
	}

	public void testGetGroup() throws Exception {
		assertEquals( "com.parallelsymmetry.escape.service", card.getGroup() );
	}

	public void testGetArtifact() throws Exception {
		assertEquals( "mock", card.getArtifact() );
	}

	public void testGetRelease() throws Exception {
		assertEquals( "1.0.0 Alpha 00  1973-08-14 22:29:00", card.getRelease().toHumanString() );
	}

	public void testGetIcon() throws Exception {
		assertEquals( URI.create( "target/sandbox/icon.png" ), card.getIconUri() );
	}

	public void testGetName() throws Exception {
		assertEquals( "Mock Service", card.getName() );
	}

	public void testGetProvider() throws Exception {
		assertEquals( "Parallel Symmetry", card.getProvider() );
	}

	public void testGetInceptionYear() throws Exception {
		assertEquals( 1973, card.getInceptionYear() );
	}

	public void testGetSummary() throws Exception {
		assertEquals( "No summary.", card.getSummary() );
	}

	public void testGetCopyrightHolder() throws Exception {
		assertEquals( "Parallel Symmetry", card.getCopyrightHolder() );
	}

	public void testGetCopyrightNotice() throws Exception {
		assertEquals( "All rights reserved.", card.getCopyrightNotice() );
	}

	public void testGetLicenseSummary() throws Exception {
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY. This is open software, and you are welcome to redistribute it under certain conditions.", card.getLicenseSummary() );
	}

	public void testGetUpdateUri() throws Exception {
		assertEquals( "target/sandbox/update.xml", card.getSourceUri().toString() );
	}

	public void testEquals() throws Exception {
		ProductCard card1 = new ProductCard( descriptor );
		ProductCard card2 = new ProductCard( descriptor );
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
		ProductCard card1 = new ProductCard( descriptor );
		ProductCard card2 = new ProductCard( descriptor );
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
}

package com.parallelsymmetry.escape.product;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

import org.junit.Before;

import com.parallelsymmetry.escape.product.ProductCard;
import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.utility.Descriptor;

public class ProductCardTest extends BaseTestCase {

	private ProductCard card;

	@Override
	@Before
	public void setUp() {
		try {
			InputStream input = getClass().getResourceAsStream( "/META-INF/program.xml" );
			Descriptor descriptor = new Descriptor( input );
			card = ProductCard.load( descriptor );
		} catch( Exception exception ) {
			fail( exception.getMessage() );
		}
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
		assertEquals( URI.create( "target/sandbox/icon.png" ), card.getIcon() );
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
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY. This is open software,\nand you are welcome to redistribute it under certain conditions.", card.getLicenseSummary() );
	}
	
	public void testGetUpdateUri() throws Exception {
		assertEquals( "target/sandbox/update.xml", card.getUpdateUri().toString() );
	}

	public void testIsInstallFolderValid() throws Exception {
		assertFalse( card.isInstallFolderValid() );
		card.setInstallFolder( new File( "." ) );
		assertTrue( card.isInstallFolderValid() );
	}

}

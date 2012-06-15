package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.io.InputStream;

import org.junit.Before;

import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.utility.Descriptor;

public class FeaturePackTest extends BaseTestCase {

	private FeaturePack pack;

	@Before
	public void setUp() {
		try {
			InputStream input = getClass().getResourceAsStream( "/META-INF/program.xml" );
			Descriptor descriptor = new Descriptor( input );
			pack = FeaturePack.load( descriptor );
		} catch( Exception exception ) {
			fail( exception.getMessage() );
		}
	}

	public void testGetKey() throws Exception {
		assertEquals( "com.parallelsymmetry.escape.service.mock", pack.getKey() );
	}

	public void testGetGroup() throws Exception {
		assertEquals( "com.parallelsymmetry.escape.service", pack.getGroup() );
	}

	public void testGetArtifact() throws Exception {
		assertEquals( "mock", pack.getArtifact() );
	}

	public void testGetSummary() throws Exception {
		assertEquals( "No summary.", pack.getSummary() );
	}

	public void testIsInstallFolderValid() throws Exception {
		assertFalse( pack.isInstallFolderValid() );
		pack.setInstallFolder( new File( "." ) );
		assertTrue( pack.isInstallFolderValid() );
	}

}

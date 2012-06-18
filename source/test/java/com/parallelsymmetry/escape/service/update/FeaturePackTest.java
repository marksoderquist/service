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

	public void testGetRelease() throws Exception {
		assertEquals( "1.0.0 Alpha 00  1973-08-14 22:29:00", pack.getRelease().toHumanString() );
	}

	public void testGetName() throws Exception {
		assertEquals( "Mock Service", pack.getName() );
	}

	public void testGetProvider() throws Exception {
		assertEquals( "Parallel Symmetry", pack.getProvider() );
	}

	public void testGetInceptionYear() throws Exception {
		assertEquals( 1973, pack.getInceptionYear() );
	}

	public void testGetSummary() throws Exception {
		assertEquals( "No summary.", pack.getSummary() );
	}

	public void testGetCopyrightHolder() throws Exception {
		assertEquals( "Parallel Symmetry", pack.getCopyrightHolder() );
	}

	public void testGetCopyrightNotice() throws Exception {
		assertEquals( "All rights reserved.", pack.getCopyrightNotice() );
	}

	public void testGetLicenseSummary() throws Exception {
		assertEquals( "Mock Service comes with ABSOLUTELY NO WARRANTY. This is open software,\nand you are welcome to redistribute it under certain conditions.", pack.getLicenseSummary() );
	}
	
	public void testGetUpdateUri() throws Exception {
		assertEquals( "target/sandbox/update.xml", pack.getUpdateUri().toString() );
	}

	public void testIsInstallFolderValid() throws Exception {
		assertFalse( pack.isInstallFolderValid() );
		pack.setInstallFolder( new File( "." ) );
		assertTrue( pack.isInstallFolderValid() );
	}

}

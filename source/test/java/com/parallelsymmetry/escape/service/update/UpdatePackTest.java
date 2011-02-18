package com.parallelsymmetry.escape.service.update;

import java.io.InputStream;

import junit.framework.TestCase;

import com.parallelsymmetry.escape.utility.Descriptor;

public class UpdatePackTest extends TestCase {
	
	public void testGetKey() throws Exception {
		InputStream input = getClass().getResourceAsStream( "/META-INF/program.xml" );
		Descriptor descriptor = new Descriptor( input);
		UpdatePack pack = UpdatePack.load(descriptor);
		
		assertEquals( "com.parallelsymmetry.escape.service.mock", pack.getKey() );
		assertEquals( "com.parallelsymmetry.escape.service", pack.getGroup() );
		assertEquals( "mock", pack.getArtifact() );
	}

}

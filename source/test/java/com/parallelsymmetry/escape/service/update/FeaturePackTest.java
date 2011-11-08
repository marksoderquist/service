package com.parallelsymmetry.escape.service.update;

import java.io.InputStream;

import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.utility.Descriptor;

public class FeaturePackTest extends BaseTestCase {
	
	public void testGetKey() throws Exception {
		InputStream input = getClass().getResourceAsStream( "/META-INF/program.xml" );
		Descriptor descriptor = new Descriptor( input);
		FeaturePack pack = FeaturePack.load(descriptor);
		
		assertEquals( "com.parallelsymmetry.escape.service.mock", pack.getKey() );
		assertEquals( "com.parallelsymmetry.escape.service", pack.getGroup() );
		assertEquals( "mock", pack.getArtifact() );
	}

}

package com.parallelsymmetry.escape.product;

import java.net.URL;

import org.junit.Test;

import com.parallelsymmetry.escape.service.BaseTestCase;
import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.Version;

public class ModuleTest extends BaseTestCase {

	private static final String TEST_MODULE_DESCRIPTOR_PATH = "/META-INF/product.mock.module.xml";

	private Descriptor descriptor;

	private Module module;

	@Override
	public void setUp() throws Exception {
		URL url = getClass().getResource( TEST_MODULE_DESCRIPTOR_PATH );
		descriptor = new Descriptor( url );
		module = new MockModule( new ProductCard( url.toURI(), descriptor ) );
	}

	@Test
	public void testGetIdentifier() throws Exception {
		assertEquals( "mock", module.getCard().getArtifact() );
	}

	@Test
	public void testGetRelease() throws Exception {
		Release release = new Release( new Version( "3.2.1" ), DateUtil.parse( Release.DATE_FORMAT, "2011-09-01 11:27:54" ) );
		assertEquals( release, module.getCard().getRelease() );
	}

	@Test
	public void testGetName() throws Exception {
		assertEquals( "Mock Module", module.getName() );
	}

	@Test
	public void testGetClassLoader() throws Exception {
		assertEquals( getClass().getClassLoader(), module.getClass().getClassLoader() );
	}

	@Test
	public void testGetPack() throws Exception {
		assertEquals( descriptor, module.getCard().getDescriptor() );
	}

}

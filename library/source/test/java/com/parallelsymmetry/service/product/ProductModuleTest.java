package com.parallelsymmetry.service.product;

import java.net.URL;

import org.junit.Test;

import com.parallelsymmetry.service.BaseTestCase;
import com.parallelsymmetry.service.MockService;
import com.parallelsymmetry.service.product.ProductCard;
import com.parallelsymmetry.service.product.ProductModule;
import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.Release;
import com.parallelsymmetry.utility.Version;

public class ProductModuleTest extends BaseTestCase {

	private static final String TEST_MODULE_DESCRIPTOR_PATH = "/META-INF/product.mock.module.xml";

	private ProductModule module;

	@Override
	public void setUp() throws Exception {
		MockService service = new MockService();
		URL url = getClass().getResource( TEST_MODULE_DESCRIPTOR_PATH );
		module = new MockModule( service, new ProductCard( url.toURI(), new Descriptor( url ) ) );
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
		assertEquals( "Mock Module", module.getCard().getName() );
	}

	@Test
	public void testGetClassLoader() throws Exception {
		assertEquals( getClass().getClassLoader(), module.getClass().getClassLoader() );
	}

}

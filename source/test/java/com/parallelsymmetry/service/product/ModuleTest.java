package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.BaseTestCase;
import com.parallelsymmetry.service.MockService;
import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.Release;
import com.parallelsymmetry.utility.Version;
import com.parallelsymmetry.utility.product.ProductCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModuleTest extends BaseTestCase {

	private static final String TEST_MODULE_DESCRIPTOR_PATH = "/META-INF/product.mock.module.xml";

	private ServiceModule module;

	@BeforeEach
	@Override
	public void setup() throws Exception {
		MockService service = new MockService();
		URL url = getClass().getResource( TEST_MODULE_DESCRIPTOR_PATH );
		assertNotNull( url );
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

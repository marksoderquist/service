package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.BaseTestCase;
import com.parallelsymmetry.utility.product.Product;
import com.parallelsymmetry.utility.product.ProductCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductUtilTest extends BaseTestCase {

	@Test
	public void testGetSettingsPathWithProduct() {
		Product product = new MockProduct();
		assertEquals( ProductUtil.PRODUCT_SETTINGS_KEY + "/com.parallelsymmetry.test.mock", ProductUtil.getSettingsPath( product ) );
	}

	@Test
	public void testGetSettingsPathWithProductCard() throws Exception {
		ProductCard card = new ProductCard( "com.parallelsymmetry.test", "mock" );
		assertEquals( ProductUtil.PRODUCT_SETTINGS_KEY + "/com.parallelsymmetry.test.mock", ProductUtil.getSettingsPath( card ) );
	}

}

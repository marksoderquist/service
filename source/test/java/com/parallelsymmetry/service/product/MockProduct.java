package com.parallelsymmetry.service.product;

import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.product.Product;
import com.parallelsymmetry.utility.product.ProductCard;
import com.parallelsymmetry.utility.product.ProductCardException;

import java.io.File;

class MockProduct implements Product {

	private ProductCard card;

	public MockProduct() {
		try {
			card = new ProductCard( "com.parallelsymmetry.test", "mock" );
		} catch( ProductCardException exception ) {
			Log.write( exception );
		}
	}

	@Override
	public ProductCard getCard() {
		return card;
	}

	@Override
	public File getDataFolder() {
		return null;
	}

}

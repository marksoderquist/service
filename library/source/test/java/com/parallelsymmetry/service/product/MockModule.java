package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.service.product.ProductCard;
import com.parallelsymmetry.service.product.ProductModule;

public class MockModule extends ProductModule {

	public MockModule( Service service, ProductCard card ) {
		super( service, card );
	}

	@Override
	public void register() {}

	@Override
	public void create() {}

	@Override
	public void destroy() {}

	@Override
	public void unregister() {}

}

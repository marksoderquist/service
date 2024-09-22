package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.product.ProductCard;

public class MockModule extends ServiceModule {

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

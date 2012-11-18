package com.parallelsymmetry.escape.product;

import com.parallelsymmetry.escape.service.Service;

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

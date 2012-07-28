package com.parallelsymmetry.escape.product;

public class MockModule extends ProductModule {

	public MockModule( ProductCard card ) {
		super( card );
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

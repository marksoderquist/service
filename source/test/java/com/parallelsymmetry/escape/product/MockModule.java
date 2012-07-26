package com.parallelsymmetry.escape.product;

import com.parallelsymmetry.escape.product.Module;
import com.parallelsymmetry.escape.product.ProductCard;

public class MockModule extends Module {

	public MockModule( ProductCard card ) {
		super( card );
	}

	@Override
	public String getName() {
		return "Mock Module";
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

package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.product.ProductManagerEvent;

public interface ProductManagerListener {

	void eventOccurred( ProductManagerEvent event );

}

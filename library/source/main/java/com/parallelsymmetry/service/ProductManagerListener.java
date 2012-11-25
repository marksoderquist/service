package com.parallelsymmetry.service;

import com.parallelsymmetry.service.product.ProductManagerEvent;

public interface ProductManagerListener {

	void eventOccurred( ProductManagerEvent event );

}

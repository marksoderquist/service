package com.parallelsymmetry.escape.service;

import java.util.EventObject;

import com.parallelsymmetry.escape.product.ProductCard;

public class ProductManagerEvent extends EventObject {

	public static enum Type {
		PRODUCT_CHANGED, PRODUCT_ENABLED, PRODUCT_DISABLED, PRODUCT_INSTALLED, PRODUCT_REMOVED, PRODUCT_UPDATED
	}

	private static final long serialVersionUID = 140916727289958624L;

	private Type type;

	private ProductCard card;

	public ProductManagerEvent( ServiceProductManager manager, Type type, ProductCard card ) {
		super( manager );
		this.type = type;
		this.card = card;
	}

	public Type getType() {
		return type;
	}

	public ProductCard getCard() {
		return card;
	}

}

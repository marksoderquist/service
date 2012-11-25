package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.Service;

public abstract class ProductModule implements Product, Comparable<ProductModule> {
	
	protected Service program;

	protected ProductCard card;

	public ProductModule( Service program, ProductCard card ) {
		this.program = program;
		this.card = card;
	}

	/**
	 * Get the product card.
	 * 
	 * @return
	 */
	@Override
	public ProductCard getCard() {
		return card;
	}

	/**
	 * Called by the program to register a module instance. This method is called
	 * before the program frame and workspaces are available.
	 */
	public abstract void register();

	/**
	 * Called by the program to create a module instance. This method is called
	 * after the program frame and workspaces are available.
	 */
	public abstract void create();

	/**
	 * Called by the program to destroy a module instance. This method is called
	 * before the program frame and workspaces are unavailable.
	 */
	public abstract void destroy();

	/**
	 * Called by the program to unregister a module instance. This method is
	 * called after the program frame and workspaces are unavailable.
	 */
	public abstract void unregister();

	@Override
	public int compareTo( ProductModule that ) {
		return this.card.getArtifact().compareTo( that.card.getArtifact() );
	}

	@Override
	public String toString() {
		return card.getName();
	}

}

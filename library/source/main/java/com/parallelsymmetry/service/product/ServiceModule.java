package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.product.ProductCard;

public abstract class ServiceModule implements ServiceProduct, Comparable<ServiceModule> {
	
	protected Service service;

	protected ProductCard card;

	public ServiceModule( Service service, ProductCard card ) {
		this.service = service;
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
	
	@Override
	public Service getService() {
		return service;
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
	public int compareTo( ServiceModule that ) {
		return this.card.getArtifact().compareTo( that.card.getArtifact() );
	}

	@Override
	public String toString() {
		return card.getName();
	}

}

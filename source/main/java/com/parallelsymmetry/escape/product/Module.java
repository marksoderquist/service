package com.parallelsymmetry.escape.product;

import java.net.URI;

import javax.swing.Icon;


public abstract class Module implements Product, Comparable<Module> {

	private ProductCard card;
	
	private URI codebase;

	public Module( ProductCard card ) {
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

	public Icon getIcon() {
		// TODO Get the icon image somehow.
		return null;
	}

	/**
	 * Get the human readable name for this module.
	 * 
	 * @return The module name.
	 */
	public String getName() {
		return card.getName();
	}

	public URI getCodebase() {
		return codebase;
	}

	public void setCodebase( URI uri ) {
		codebase = uri;
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
	public int compareTo( Module that ) {
		return this.card.getArtifact().compareTo( that.card.getArtifact() );
	}

	@Override
	public String toString() {
		return getName();
	}

}

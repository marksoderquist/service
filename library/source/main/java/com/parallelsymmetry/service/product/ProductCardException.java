package com.parallelsymmetry.service.product;

public class ProductCardException extends Exception {

	private static final long serialVersionUID = -3243845306071117161L;

	public ProductCardException() {}

	public ProductCardException( String message ) {
		super( message );
	}

	public ProductCardException( Throwable cause ) {
		super( cause );
	}

	public ProductCardException( String message, Throwable cause ) {
		super( message, cause );
	}

}

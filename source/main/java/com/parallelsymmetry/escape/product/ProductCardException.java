package com.parallelsymmetry.escape.product;

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

	public ProductCardException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
		super( message, cause, enableSuppression, writableStackTrace );
	}

}

package com.parallelsymmetry.service.product;

import java.util.Comparator;

import com.parallelsymmetry.service.Service;

public class ProductCardComparator implements Comparator<ProductCard> {

	public static enum Field {
		KEY, NAME, GROUP, ARTIFACT, RELEASE
	}

	private Service service;

	private Field field;

	public ProductCardComparator( Service service, Field field ) {
		this.service = service;
		this.field = field;
	}

	@Override
	public int compare( ProductCard card1, ProductCard card2 ) {
		if( card1.equals( service.getCard() ) ) return -1;
		if( card2.equals( service.getCard() ) ) return 1;

		switch( field ) {
			case KEY: {
				return card1.getProductKey().compareTo( card2.getProductKey() );
			}
			case RELEASE: {
				return card1.getRelease().compareTo( card2.getRelease() );
			}
			case ARTIFACT: {
				return card1.getArtifact().compareTo( card2.getArtifact() );
			}
			case GROUP: {
				return card1.getGroup().compareTo( card2.getGroup() );
			}
			case NAME: {
				return card1.getName().compareTo( card2.getName() );
			}
			default: {
				return card1.getName().compareTo( card2.getName() );
			}
		}
	}

}

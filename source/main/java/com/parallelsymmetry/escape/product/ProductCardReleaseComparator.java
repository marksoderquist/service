package com.parallelsymmetry.escape.product;

import java.util.Comparator;

public class ProductCardReleaseComparator implements Comparator<ProductCard> {

	@Override
	public int compare( ProductCard card1, ProductCard card2 ) {
		return card1.getRelease().compareTo( card2.getRelease() );
	}

}

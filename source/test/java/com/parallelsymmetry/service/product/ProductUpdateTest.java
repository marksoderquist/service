package com.parallelsymmetry.service.product;

import com.parallelsymmetry.service.BaseServiceTest;
import com.parallelsymmetry.utility.product.ProductCard;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductUpdateTest extends BaseServiceTest {

	@Test
	public void testHashCode() throws Exception {
		ProductCard card1 = new ProductCard( "group", "artifact" );
		File source1 = new File( "source" );
		File target1 = new File( "target" );
		ProductUpdate update1 = new ProductUpdate( card1, source1, target1 );

		ProductCard card2 = new ProductCard( "group", "artifact" );
		File source2 = new File( "source" );
		File target2 = new File( "target" );
		ProductUpdate update2 = new ProductUpdate( card2, source2, target2 );

		assertEquals( update1.hashCode(), update2.hashCode() );
	}

	@Test
	public void testEquals() throws Exception {
		ProductCard card1 = new ProductCard( "group", "artifact" );
		File source1 = new File( "source" );
		File target1 = new File( "target" );
		ProductUpdate update1 = new ProductUpdate( card1, source1, target1 );

		ProductCard card2 = new ProductCard( "group", "artifact" );
		File source2 = new File( "source" );
		File target2 = new File( "target" );
		ProductUpdate update2 = new ProductUpdate( card2, source2, target2 );

		assertEquals( update1, update2 );
		assertTrue( update1.equals( update2 ) );
	}

	@Test
	public void testProductUpdateSet() throws Exception {
		ProductCard card1 = new ProductCard( "group", "artifact" );
		File source1 = new File( "source" );
		File target1 = new File( "target" );
		ProductUpdate update1 = new ProductUpdate( card1, source1, target1 );

		ProductCard card2 = new ProductCard( "group", "artifact" );
		File source2 = new File( "source" );
		File target2 = new File( "target" );
		ProductUpdate update2 = new ProductUpdate( card2, source2, target2 );

		Set<ProductUpdate> updates = new HashSet<ProductUpdate>();
		updates.add( update1 );
		updates.add( update2 );
		assertEquals( 1, updates.size() );
	}

}

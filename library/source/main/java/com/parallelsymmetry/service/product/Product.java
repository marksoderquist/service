package com.parallelsymmetry.service.product;

import java.io.File;

import com.parallelsymmetry.utility.product.ProductCard;

public interface Product {

	ProductCard getCard();
	
	/**
	 * Get the product data folder. This is the location where the product should
	 * be able to store files that are specific to the product. This path is
	 * operating system specific and can be different between different versions
	 * of operating system.
	 * <p>
	 * Note: This folder is shared by multiple instances of the product.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>Windows 7: C:\Users\&lt;user&gt;\AppData\Roaming\&lt;program name&gt;</li>
	 * <li>Linux: /home/&lt;user&gt;/.&lt;program id&gt;</li>
	 * </ul>
	 * 
	 * @return The program data folder.
	 */
	File getProductDataFolder();
	
}

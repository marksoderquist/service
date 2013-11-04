package com.parallelsymmetry.service.product;

import java.text.MessageFormat;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.Bundles;
import com.parallelsymmetry.utility.product.ProductCard;
import com.parallelsymmetry.utility.setting.SettingProvider;
import com.parallelsymmetry.utility.setting.Settings;

public class ProductUtil {

	private static final String PRODUCT_SETTINGS_KEY = "products";

	public static final String getString( Product product, String path, String name ) {
		return Bundles.getString( product.getClass().getClassLoader(), path, name );
	}

	public static final String getString( Product product, String path, String name, Object... arguments ) {
		String pattern = Bundles.getString( product.getClass().getClassLoader(), path, name );
		return MessageFormat.format( pattern, arguments );
	}

	public static final Settings getSettings( Service service, Product product ) {
		return service.getSettings().getNode( getSettingsPath( product ) );
	}

	public static final Settings getSettings( Service service, ProductCard card ) {
		return service.getSettings().getNode( getSettingsPath( card ) );
	}

	public static final void addSettingsProvider( Service service, Product product, SettingProvider provider ) {
		service.getSettings().addProvider( provider, getSettingsPath( product ) );
	}
	
	public static final void removeSettingsProvider( Service service, Product product, SettingProvider provider ) {
		service.getSettings().removeProvider( provider );
	}

	private static final String getSettingsPath( Product product ) {
		return getSettingsPath( product.getCard() );
	}

	private static final String getSettingsPath( ProductCard card ) {
		return PRODUCT_SETTINGS_KEY + "/" + card.getProductKey();
	}

}

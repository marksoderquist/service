package com.parallelsymmetry.service;

public interface ServiceSettingsPath {

	// Manager paths.
	public static final String MANAGER_SETTINGS_ROOT = "/manager";

	public static final String TASK_MANAGER_SETTINGS_PATH = MANAGER_SETTINGS_ROOT + "/task";

	public static final String PRODUCT_MANAGER_SETTINGS_PATH = MANAGER_SETTINGS_ROOT + "/product";
	
	public static final String UPDATE_SETTINGS_PATH = PRODUCT_MANAGER_SETTINGS_PATH + "/update";

}

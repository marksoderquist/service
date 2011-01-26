package com.parallelsymmetry.escape.service;

import com.parallelsymmetry.escape.utility.setting.Settings;

public interface Persistent<T> {

	T loadSettings( Settings settings );

	T saveSettings( Settings settings );

}

package com.parallelsymmetry.service;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.parallelsymmetry.utility.log.Log;

public abstract class BaseTestCase extends TestCase {

	protected static final int TIMEOUT = 1;

	protected static final TimeUnit TIMEUNIT = TimeUnit.SECONDS;

	@Override
	public void setUp() throws Exception {
		Log.setLevel( Log.NONE );
	}

}
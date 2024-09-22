package com.parallelsymmetry.service;

import java.util.concurrent.TimeUnit;

import com.parallelsymmetry.utility.log.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTestCase {

	protected static final int TIMEOUT = 1;

	protected static final TimeUnit TIMEUNIT = TimeUnit.SECONDS;

	@BeforeEach
	public void setup() throws Exception {
		Log.setLevel( Log.NONE );
	}

	@AfterEach
	public void teardown() throws Exception {
		Log.setLevel( Log.NONE );
	}

}

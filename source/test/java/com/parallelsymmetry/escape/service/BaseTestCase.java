package com.parallelsymmetry.escape.service;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public abstract class BaseTestCase extends TestCase {

	protected static final int TIMEOUT = 1;

	protected static final TimeUnit TIMEUNIT = TimeUnit.SECONDS;

}

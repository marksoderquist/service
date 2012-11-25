package com.parallelsymmetry.service;

import com.parallelsymmetry.service.ServiceFlag;
import com.parallelsymmetry.service.ServiceFlagValue;

public abstract class BaseServiceTest extends BaseTestCase {

	protected MockService service;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		service = new MockService();
		service.call( new String[] { ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.SETTINGS_RESET } );
		service.waitForStartup();
		assertTrue( service.isRunning() );
	}

	@Override
	public void tearDown() throws Exception {
		service.call( new String[] { ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.STOP } );
		service.waitForShutdown();
		assertFalse( service.isRunning() );
	}

}

package com.parallelsymmetry.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseServiceTest extends BaseTestCase {

	protected MockService service;

	@Override
	public void setup() throws Exception {
		super.setup();
		service = new MockService();
		service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.SETTINGS_RESET );
		service.waitForStartup();
		assertTrue( service.isRunning() );
	}

	@Override
	public void teardown() throws Exception {
		service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.STOP );
		service.waitForShutdown();
		assertFalse( service.isRunning() );
		super.teardown();
	}

}

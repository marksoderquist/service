package com.parallelsymmetry.escape.service;

import junit.framework.TestCase;

public class ServiceTest extends TestCase {

	private MockService service = new MockService();

	public void testStartStop() {
		service.process( new String[] { "-start" } );
	}

}

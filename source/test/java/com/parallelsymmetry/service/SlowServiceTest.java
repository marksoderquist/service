package com.parallelsymmetry.service;

public class SlowServiceTest extends ServiceTest {

	@Override
	public void setUp() {
		service = new CountingService( 10, 10 );
		super.setUp();
	}

}

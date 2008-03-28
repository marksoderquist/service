package com.parallelsymmetry.service;

public class ExceptionServiceTest extends ServiceTest {

	@Override
	public void setUp() {
		service = new CountingService( 0, 0 );
		super.setUp();
	}

}

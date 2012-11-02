package com.parallelsymmetry.escape.service;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import com.parallelsymmetry.escape.utility.setting.Settings;

public class ServiceProxyAuthenticator extends Authenticator {

	private Service service;

	public ServiceProxyAuthenticator( Service service ) {
		this.service = service;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		String username = null;
		String password = null;

		Settings settings = service.getSettings().getNode( "/network/proxy" );

		boolean all = settings.getBoolean( "all", true );
		String scheme = all ? "http" : getRequestingScheme();

		// First try by specific protocol.
		username = settings.get( scheme + "-username", null );
		password = settings.get( scheme + "-password", null );

		// Second try just general credentials.
		if( username == null ) username = settings.get( "username", null );
		if( password == null ) password = settings.get( "password", null );

		if( username == null || password == null ) return new PasswordAuthentication( username, password.toCharArray() );

		return null;
	}

}

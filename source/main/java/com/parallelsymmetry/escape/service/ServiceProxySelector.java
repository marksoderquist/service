package com.parallelsymmetry.escape.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Settings;

public class ServiceProxySelector extends ProxySelector {

	private Service service;

	public ServiceProxySelector( Service service ) {
		this.service = service;
	}

	@Override
	public List<Proxy> select( URI uri ) {
		Log.write( Log.DEBUG, "Select proxy for: " + uri );

		List<Proxy> proxies = new ArrayList<Proxy>();

		Settings settings = service.getSettings().getNode( "/network/proxy" );

		String scheme = uri.getScheme();
		String mode = settings.get( "mode", "direct" );

		Set<String> exclusions = parseExclusions( settings.get( "exclude", null ) );

		if( "direct".equals( mode ) ) {
			proxies.add( Proxy.NO_PROXY );
		} else {
			if( !exclusions.contains( uri.getHost() ) ) proxies.add( getProxy( scheme ) );
		}

		return proxies;
	}

	@Override
	public void connectFailed( URI uri, SocketAddress address, IOException exception ) {
		if( "socket".equals( uri.getScheme() ) ) return;

		// FIXME Determine a cleaner way to handle i18n.
		//service.error( MessageFormat.format( Bundles.getString( Bundles.MESSAGES, "proxy.connect.failed" ), address.toString() ) );
	}

	private Proxy getProxy( String scheme ) {
		if( scheme == null ) return Proxy.NO_PROXY;

		Proxy.Type type = Proxy.Type.DIRECT;
		if( "http".equals( scheme ) ) {
			type = Proxy.Type.HTTP;
		} else if( "https".equals( scheme ) ) {
			type = Proxy.Type.HTTP;
		} else if( "ftp".equals( scheme ) ) {
			type = Proxy.Type.HTTP;
		} else {
			type = Proxy.Type.SOCKS;
		}

		SocketAddress address = getProxyAddress( scheme );

		return address == null ? Proxy.NO_PROXY : new Proxy( type, address );
	}

	private SocketAddress getProxyAddress( String scheme ) {
		Settings settings = service.getSettings().getNode( "/network/proxy" );
		boolean all = settings.getBoolean( "all", true );

		String host = null;
		String port = null;
		String address = settings.get( all ? "http" : scheme, null );

		if( !TextUtil.isEmpty( address ) ) {
			int index = address.indexOf( ':' );
			if( index < 0 ) {
				host = address;
			} else {
				host = address.substring( 0, index );
				port = address.substring( index + 1 );
			}
		}

		if( host == null ) return null;
		if( port == null ) port = "80";

		return InetSocketAddress.createUnresolved( host, Integer.parseInt( port ) );
	}

	private Set<String> parseExclusions( String exclusions ) {
		Set<String> set = new HashSet<String>();

		if( exclusions != null ) {
			StringTokenizer tokenizer = new StringTokenizer( exclusions, " |," );
			while( tokenizer.hasMoreTokens() ) {
				set.add( tokenizer.nextToken() );
			}
		}

		return set;
	}

}

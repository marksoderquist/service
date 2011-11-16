package com.parallelsymmetry.escape.service.task;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.service.ServiceTask;
import com.parallelsymmetry.escape.utility.BundleKey;
import com.parallelsymmetry.escape.utility.Bundles;
import com.parallelsymmetry.escape.utility.log.Log;

public class DownloadTask extends ServiceTask<Download> {

	private URI uri;

	private File target;

	private Set<DownloadListener> listeners;

	public DownloadTask( Service service, URI uri ) {
		this( service, uri, null );
		listeners = new CopyOnWriteArraySet<DownloadListener>();
	}

	public DownloadTask( Service service, URI uri, File target ) {
		super( service, Bundles.getString( BundleKey.PROMPTS, "download" ) + " " + uri.toString() );
		this.uri = uri;
		this.target = target;
	}

	@Override
	public Download execute() throws Exception {
		URLConnection connection = uri.toURL().openConnection();
		int length = connection.getContentLength();
		String encoding = connection.getContentEncoding();
		InputStream input = connection.getInputStream();

		setMinimum( 0 );
		setMaximum( length );

		byte[] buffer = new byte[8192];
		Download download = new Download( uri, length, encoding, target );

		try {
			int read = 0;
			int offset = 0;
			while( ( read = input.read( buffer ) ) > -1 ) {
				if( isCancelled() ) return null;
				download.write( buffer, 0, read );
				offset += read;
				setProgress( offset );
				fireEvent( new DownloadEvent( offset, length ) );
			}
			if( isCancelled() ) return null;
		} finally {
			download.close();
		}

		Log.write( Log.TRACE, "Resource downloaded: " + uri );
		Log.write( Log.DEBUG, "        to location: " + download.getTarget() );

		return download;
	}

	public void addListener( DownloadListener listener ) {
		listeners.add( listener );
	}

	public void removeListener( DownloadListener listener ) {
		listeners.remove( listener );
	}

	private void fireEvent( DownloadEvent event ) {
		for( DownloadListener listener : new HashSet<DownloadListener>( listeners ) ) {
			try {
				listener.update( event );
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}
	}

}

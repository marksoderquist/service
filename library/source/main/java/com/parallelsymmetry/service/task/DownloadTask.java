package com.parallelsymmetry.service.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.parallelsymmetry.utility.BundleKey;
import com.parallelsymmetry.utility.Bundles;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.task.Task;

public class DownloadTask extends Task<Download> {

	public static final int DEFAULT_CONNECT_TIMEOUT = 2000;

	public static final int DEFAULT_READ_TIMEOUT = 10000;

	private URI uri;

	private File target;

	private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	
	private int readTimeout = DEFAULT_READ_TIMEOUT;

	private Set<DownloadListener> listeners;

	public DownloadTask( URI uri ) {
		this( uri, null );
		listeners = new CopyOnWriteArraySet<DownloadListener>();
	}

	public DownloadTask( URI uri, File target ) {
		super( Bundles.getString( BundleKey.PROMPTS, "download" ) + " " + uri.toString() );
		this.uri = uri;
		this.target = target;
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public Download execute() throws IOException {
		return download();
	}

	private Download download() throws IOException {
		URLConnection connection = uri.toURL().openConnection();
		connection.setConnectTimeout( connectTimeout );
		connection.setReadTimeout( readTimeout );
		connection.setUseCaches( false );

		try {
			connection.connect();
		} catch( UnknownHostException exception ) {
			Log.write( Log.WARN, "Host not found: " + uri.toString() );
			return null;
		}

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

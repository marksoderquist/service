package com.parallelsymmetry.escape.service.task;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import com.parallelsymmetry.escape.utility.log.Log;

public class DownloadTask extends Task<Download> {

	private URI uri;

	private File target;

	public DownloadTask( URI uri ) {
		this( uri, null );
	}

	public DownloadTask( URI uri, File target ) {
		this.uri = uri;
		this.target = target;
	}

	@Override
	public Download execute() throws Exception {
		URLConnection connection = uri.toURL().openConnection();
		int length = connection.getContentLength();
		String encoding = connection.getContentEncoding();
		InputStream input = connection.getInputStream();

		byte[] buffer = new byte[8192];
		Download download = new Download( uri, length, encoding, target );

		try {
			int read = 0;
			int offset = 0;
			while( ( read = input.read( buffer ) ) > -1 ) {
				if( isCancelled() ) return null;
				download.write( buffer, 0, read );
				offset += read;

				// TODO Notify listeners of the new data.
			}
			if( isCancelled() ) return null;
		} finally {
			download.close();
		}

		Log.write( Log.TRACE, "Resource downloaded: " + uri );

		return download;
	}

}

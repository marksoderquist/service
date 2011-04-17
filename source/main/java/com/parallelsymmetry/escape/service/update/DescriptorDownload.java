package com.parallelsymmetry.escape.service.update;

import java.io.FileInputStream;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.parallelsymmetry.escape.service.task.DownloadTask;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.task.Task;

final class DescriptorDownload extends Task<Descriptor> {

	private DownloadTask task;

	public DescriptorDownload( URI uri ) {
		this.task = new DownloadTask( uri );
	}

	@Override
	public Descriptor execute() throws Exception {
		Descriptor descriptor = null;

		try {
			descriptor = new Descriptor( new FileInputStream( task.execute().getTarget() ) );
		} catch( ParserConfigurationException exception ) {
			Log.write( exception );
		} catch( SAXException exception ) {
			Log.write( exception );
		}

		return descriptor;
	}

}

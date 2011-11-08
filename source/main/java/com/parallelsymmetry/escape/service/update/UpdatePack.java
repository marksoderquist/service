package com.parallelsymmetry.escape.service.update;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.Version;
import com.parallelsymmetry.escape.utility.log.Log;

public class UpdatePack {

	public static final String GROUP_PATH = "/pack/group";

	public static final String ARTIFACT_PATH = "/pack/artifact";

	public static final String VERSION_PATH = "/pack/version";

	public static final String TIMESTAMP_PATH = "/pack/timestamp";

	public static final String NAME_PATH = "/pack/name";

	public static final String PROVIDER_PATH = "/pack/provider";

	public static final String COPYRIGHT_HOLDER_PATH = "/pack/copyright/holder";

	public static final String COPYRIGHT_NOTICE_PATH = "/pack/copyright/notice";

	public static final String UPDATE_URI_PATH = "/pack/update/uri";

	private static final String DEFAULT_ARTIFACT = "unknown";

	private static final String DEFAULT_GROUP = "com.parallelsymmetry";

	private Descriptor descriptor;

	private String group = DEFAULT_GROUP;

	private String artifact = DEFAULT_ARTIFACT;

	private Release release = new Release( new Version() );

	private String name = "Unknown";

	private String provider = "Unknown";

	private String copyrightHolder = "Unknown";

	private String copyrightNotice = "All rights reserved.";

	private URI uri;

	private File folder;

	private UpdatePack( Descriptor descriptor ) {
		this.descriptor = descriptor;
	}

	public Descriptor getDescriptor() {
		return descriptor;
	}

	public String getKey() {
		return group + "." + artifact;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup( String group ) {
		this.group = group;
	}

	public String getArtifact() {
		return artifact;
	}

	public void setArtifact( String artifact ) {
		this.artifact = artifact;
	}

	public Release getRelease() {
		return release;
	}

	public void setRelease( Release release ) {
		this.release = release;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider( String provider ) {
		this.provider = provider;
	}

	public void setCopyrightHolder( String holder ) {
		this.copyrightHolder = holder;
	}

	public String getCopyrightHolder() {
		return copyrightHolder;
	}

	public void setCopyrightNotice( String notice ) {
		this.copyrightNotice = notice;
	}

	public String getCopyrightNotice() {
		return copyrightNotice;
	}

	public URI getUpdateUri() {
		return uri;
	}

	public void setUpdateUri( URI uri ) {
		this.uri = uri;
	}

	public File getInstallFolder() {
		return folder;
	}

	public void setInstallFolder( File folder ) {
		this.folder = folder;
	}

	@Override
	public String toString() {
		return group + ":" + artifact;
	}

	public static final UpdatePack load( Descriptor descriptor ) {
		if( descriptor == null ) return null;

		String group = descriptor.getValue( GROUP_PATH );
		String artifact = descriptor.getValue( ARTIFACT_PATH );
		String version = descriptor.getValue( VERSION_PATH );
		String timestamp = descriptor.getValue( TIMESTAMP_PATH );
		String name = descriptor.getValue( NAME_PATH );
		String provider = descriptor.getValue( PROVIDER_PATH );
		String holder = descriptor.getValue( COPYRIGHT_HOLDER_PATH );
		String notice = descriptor.getValue( COPYRIGHT_NOTICE_PATH );
		String uri = descriptor.getValue( UPDATE_URI_PATH );

		UpdatePack pack = new UpdatePack( descriptor );
		
		Date releaseDate = null;
		try {
			releaseDate = new Date( Long.parseLong( timestamp ) );
		} catch( Throwable throwable ) {
			releaseDate = DateUtil.INCEPTION_DATE;
		}

		if( group != null) pack.group = group;
		if( artifact != null) pack.artifact = artifact;
		if( version != null ) pack.release = new Release( version, releaseDate );
		if( name != null ) pack.name = name;
		if( provider != null ) pack.provider = provider;

		pack.copyrightHolder = holder == null ? provider : holder;
		if( notice != null) pack.copyrightNotice = notice;

		try {
			if( uri != null ) pack.uri = new URI( uri );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}

		return pack;
	}

}

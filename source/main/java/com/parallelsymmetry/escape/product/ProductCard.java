package com.parallelsymmetry.escape.product;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.log.Log;

// FIXME Change name to ProductVoucher?
public class ProductCard {
	
	public static final String PRODUCT_PATH = "/pack";

	public static final String GROUP_PATH = PRODUCT_PATH + "/group";

	public static final String ARTIFACT_PATH = PRODUCT_PATH + "/artifact";

	public static final String VERSION_PATH = PRODUCT_PATH + "/version";

	public static final String TIMESTAMP_PATH = PRODUCT_PATH + "/timestamp";

	public static final String ICON_PATH = PRODUCT_PATH + "/icon/@uri";

	public static final String NAME_PATH = PRODUCT_PATH + "/name";

	public static final String PROVIDER_PATH = PRODUCT_PATH + "/provider";

	public static final String INCEPTION_YEAR_PATH = PRODUCT_PATH + "/inception";

	public static final String PRODUCT_SUMMARY_PATH = PRODUCT_PATH + "/summary";

	public static final String COPYRIGHT_HOLDER_PATH = PRODUCT_PATH + "/copyright/holder";

	public static final String COPYRIGHT_NOTICE_PATH = PRODUCT_PATH + "/copyright/notice";

	public static final String LICENSE_SUMMARY_PATH = PRODUCT_PATH + "/license/summary";

	public static final String SOURCE_URI_PATH = PRODUCT_PATH + "/source/@uri";

	private static final String COPYRIGHT = "(C)";

	private Descriptor descriptor;

	private String group;

	private String artifact;

	private Release release;

	private URI iconUri;

	private String name = "Unknown";

	private String provider = "Unknown";

	private int inceptionYear = DateUtil.getCurrentYear();

	private String summary = "No summary.";

	private String copyrightHolder = "Unknown";

	private String copyrightNotice = "All rights reserved.";

	private String licenseSummary;

	private URI sourceUri;

	private File folder;

	private ProductCard() {}

	public ProductCard( Descriptor descriptor ) throws ProductCardException {
		update( descriptor );
	}

	public static final ProductCard create( Descriptor descriptor ) throws ProductCardException {
		return new ProductCard().update( descriptor );
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

	public URI getIconUri() {
		// TODO ProductCard.getIcon() get the icon from the icon cache.
		return iconUri;
	}

	public void setIconUri( URI uri ) {
		// TODO ProductCard.setIcon() set the icon in the icon cache.
		this.iconUri = uri;
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

	public int getInceptionYear() {
		return inceptionYear;
	}

	public void setInceptionYear( int year ) {
		this.inceptionYear = year;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary( String summary ) {
		this.summary = summary;
	}

	public String getCopyright() {
		int currentYear = DateUtil.getCurrentYear();
		int inceptionYear = getInceptionYear();
		if( inceptionYear == 0 ) inceptionYear = Calendar.getInstance().get( Calendar.YEAR );

		return COPYRIGHT + " " + ( currentYear == inceptionYear ? currentYear : inceptionYear + "-" + currentYear ) + " " + getCopyrightHolder();
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

	public String getLicenseSummary() {
		return licenseSummary;
	}

	public void setLicenseSummary( String summary ) {
		this.licenseSummary = summary;
	}

	public URI getSourceUri() {
		return sourceUri;
	}

	public void setSourceUri( URI uri ) {
		this.sourceUri = uri;
	}

	public File getTargetFolder() {
		return folder;
	}

	public void setTargetFolder( File folder ) {
		this.folder = folder;
	}

	public boolean isTargetFolderValid() {
		return folder != null && folder.exists();
	}

	public ProductCard update( Descriptor descriptor ) throws ProductCardException {
		if( descriptor == null ) throw new ProductCardException( "Descriptor cannot be null." );
		this.descriptor = descriptor;

		String group = descriptor.getValue( GROUP_PATH );
		String artifact = descriptor.getValue( ARTIFACT_PATH );
		String version = descriptor.getValue( VERSION_PATH );
		String timestamp = descriptor.getValue( TIMESTAMP_PATH );
		String iconUri = descriptor.getValue( ICON_PATH );
		String name = descriptor.getValue( NAME_PATH );
		String provider = descriptor.getValue( PROVIDER_PATH );
		String inception = descriptor.getValue( INCEPTION_YEAR_PATH );
		String productSummary = descriptor.getValue( PRODUCT_SUMMARY_PATH );
		String holder = descriptor.getValue( COPYRIGHT_HOLDER_PATH );
		String notice = descriptor.getValue( COPYRIGHT_NOTICE_PATH );
		String licenseSummary = descriptor.getValue( LICENSE_SUMMARY_PATH );
		String sourceUri = descriptor.getValue( SOURCE_URI_PATH );

		// Determine the release date.
		Date releaseDate = null;
		try {
			releaseDate = new Date( Long.parseLong( timestamp ) );
		} catch( Throwable throwable ) {
			// Leave the date null.
		}

		// Determine the program inception year.
		int inceptionYear = 0;
		try {
			inceptionYear = Integer.parseInt( inception );
		} catch( NumberFormatException exception ) {
			// Leave the inception year zero.
		}

		if( group == null ) throw new ProductCardException( "Product group cannot be null." );
		if( artifact == null ) throw new ProductCardException( "Product artifact cannot be null." );
		this.group = group;
		this.artifact = artifact;
		this.release = new Release( version, releaseDate );

		try {
			if( iconUri != null ) this.iconUri = new URI( iconUri );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}

		if( name != null ) this.name = name;
		if( provider != null ) this.provider = provider;
		if( inceptionYear != 0 ) this.inceptionYear = inceptionYear;
		if( productSummary != null ) this.summary = productSummary;

		this.copyrightHolder = holder == null ? provider : holder;
		if( notice != null ) this.copyrightNotice = notice;

		if( licenseSummary != null ) this.licenseSummary = licenseSummary;

		try {
			if( sourceUri != null ) this.sourceUri = new URI( sourceUri );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}

		return this;
	}

	@Override
	public String toString() {
		return getKey();
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof ProductCard ) ) return false;
		ProductCard that = (ProductCard)object;
		return this.group.equals( that.group ) && this.artifact.equals( that.artifact ) && this.release.equals( that.release );
	}

	@Override
	public int hashCode() {
		return this.group.hashCode() + this.artifact.hashCode() + this.release.hashCode();
	}

}

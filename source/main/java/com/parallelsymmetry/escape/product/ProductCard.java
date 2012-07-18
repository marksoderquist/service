package com.parallelsymmetry.escape.product;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.Release;
import com.parallelsymmetry.escape.utility.UriUtil;
import com.parallelsymmetry.escape.utility.log.Log;

public class ProductCard {

	public static final String PRODUCT_PATH = "/product";

	public static final String GROUP_PATH = PRODUCT_PATH + "/group";

	public static final String ARTIFACT_PATH = PRODUCT_PATH + "/artifact";

	public static final String VERSION_PATH = PRODUCT_PATH + "/version";

	public static final String TIMESTAMP_PATH = PRODUCT_PATH + "/timestamp";

	public static final String ICON_PATH = PRODUCT_PATH + "/icon/@uri";

	public static final String NAME_PATH = PRODUCT_PATH + "/name";

	public static final String PROVIDER_PATH = PRODUCT_PATH + "/provider";

	public static final String INCEPTION_YEAR_PATH = PRODUCT_PATH + "/inception";

	public static final String SUMMARY_PATH = PRODUCT_PATH + "/summary";

	public static final String DESCRIPTION_PATH = PRODUCT_PATH + "/description";

	public static final String COPYRIGHT_HOLDER_PATH = PRODUCT_PATH + "/copyright/holder";

	public static final String COPYRIGHT_NOTICE_PATH = PRODUCT_PATH + "/copyright/notice";

	public static final String LICENSE_URI_PATH = PRODUCT_PATH + "/license/@uri";

	public static final String LICENSE_SUMMARY_PATH = PRODUCT_PATH + "/license/summary";

	public static final String RESOURCES_PATH = PRODUCT_PATH + "/resources";

	public static final String SOURCE_URI_PATH = PRODUCT_PATH + "/source/@uri";

	private static final String COPYRIGHT = "(C)";

	private Descriptor descriptor;

	private String group;

	private String artifact;

	private Release release;

	private URI iconUri;

	private String name;

	private String provider;

	private int inceptionYear;

	private String summary;

	private String description;

	private String copyrightHolder;

	private String copyrightNotice;

	private URI licenseUri;

	private String licenseSummary;

	private URI sourceUri;

	private File folder;

	private String productKey;

	private String releaseKey;

	private boolean enabled;

	private boolean installed;

	private boolean updatable;

	private boolean removable;

	public ProductCard( URI base, Descriptor descriptor ) throws ProductCardException {
		update( base, descriptor );
	}

	public ProductCard update( URI base, Descriptor descriptor ) throws ProductCardException {
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
		String summary = descriptor.getValue( SUMMARY_PATH );
		String description = descriptor.getValue( DESCRIPTION_PATH );
		String holder = descriptor.getValue( COPYRIGHT_HOLDER_PATH );
		String notice = descriptor.getValue( COPYRIGHT_NOTICE_PATH );
		String licenseUri = descriptor.getValue( LICENSE_URI_PATH );
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
		int inceptionYear = DateUtil.getCurrentYear();
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

		this.productKey = group + ":" + artifact;
		this.releaseKey = productKey + ":" + release.toString();

		try {
			if( iconUri != null ) this.iconUri = UriUtil.resolve( base, new URI( iconUri ) );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}

		this.name = name == null ? artifact : name;
		this.provider = provider == null ? group : provider;
		this.inceptionYear = inceptionYear;

		if( summary != null ) this.summary = summary;
		this.description = description;

		this.copyrightHolder = holder == null ? provider : holder;
		if( notice != null ) this.copyrightNotice = notice;

		try {
			if( licenseUri != null ) this.licenseUri = UriUtil.resolve( base, new URI( licenseUri ) );
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}
		if( licenseSummary != null ) this.licenseSummary = licenseSummary;

		try {
			if( sourceUri == null ) {
				this.sourceUri = base;
			} else {
				URI uri = new URI( sourceUri );
				if( uri.isAbsolute() ) {
					this.sourceUri = uri;
				} else {
					this.sourceUri = UriUtil.resolve( base, uri );
				}
			}
		} catch( URISyntaxException exception ) {
			Log.write( exception );
		}

		return this;
	}

	public Descriptor getDescriptor() {
		return descriptor;
	}

	public String getProductKey() {
		return productKey;
	}

	// TODO If not used, remove.
	public String getReleaseKey() {
		return releaseKey;
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

	public String getDescription() {
		return description;
	}

	public void setDescription( String description ) {
		this.description = description;
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

	public URI getLicenseUri() {
		return licenseUri;
	}

	public void setLicenseUri( URI uri ) {
		this.licenseUri = uri;
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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}

	public boolean isInstalled() {
		return installed;
	}

	public void setInstalled( boolean installed ) {
		this.installed = installed;
	}

	public boolean isRemovable() {
		return removable;
	}

	public void setRemovable( boolean removable ) {
		this.removable = removable;
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public void setUpdatable( boolean updatable ) {
		this.updatable = updatable;
	}

	@Override
	public String toString() {
		return getProductKey();
	}

	@Override
	public boolean equals( Object object ) {
		if( !( object instanceof ProductCard ) ) return false;
		ProductCard that = (ProductCard)object;
		return this.group.equals( that.group ) && this.artifact.equals( that.artifact );
	}

	@Override
	public int hashCode() {
		return this.group.hashCode() ^ this.artifact.hashCode();
	}

}

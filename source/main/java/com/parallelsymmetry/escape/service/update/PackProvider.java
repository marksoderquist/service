package com.parallelsymmetry.escape.service.update;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.utility.Descriptor;

public class PackProvider implements FeatureProvider {

	private Service service;

	private FeaturePack pack;

	public PackProvider( Service service, FeaturePack pack ) {
		this.service = service;
		this.pack = pack;
	}

	@Override
	public Set<FeatureResource> getResources() throws Exception {
		return getResources( pack );
	}

	private Set<FeatureResource> getResources( FeaturePack source ) throws Exception {
		URI codebase = source.getUpdateUri();
		Descriptor descriptor = source.getDescriptor();

		Set<FeatureResource> resources = new HashSet<FeatureResource>();

		// Resolve all the files to download.
		String[] files = getResources( descriptor, "file/@uri" );
		String[] packs = getResources( descriptor, "pack/@uri" );
		String[] jnlps = getResources( descriptor, "jnlp/@uri" );

		for( String file : files ) {
			URI uri = codebase.resolve( file );
			resources.add( new FeatureResource( FeatureResource.Type.FILE, uri ) );
		}
		for( String pack : packs ) {
			URI uri = codebase.resolve( pack );
			resources.add( new FeatureResource( FeatureResource.Type.PACK, uri ) );
		}
		for( String jnlp : jnlps ) {
			URI uri = codebase.resolve( jnlp );
			Future<Descriptor> future = service.getTaskManager().submit( new DescriptorDownload( uri ) );
			resources.addAll( new JnlpProvider( service, future.get() ).getResources() );
		}

		return resources;
	}

	private String[] getResources( Descriptor descriptor, String path ) {
		String os = System.getProperty( "os.name" );
		String arch = System.getProperty( "os.arch" );

		String[] uris = null;
		Set<String> resources = new HashSet<String>();

		// Determine the resources.
		Node[] nodes = descriptor.getNodes( "/pack/resources" );
		for( Node node : nodes ) {
			Descriptor resourcesDescriptor = new Descriptor( node );
			Node osNameNode = node.getAttributes().getNamedItem( "os" );
			Node osArchNode = node.getAttributes().getNamedItem( "arch" );

			String osName = osNameNode == null ? null : osNameNode.getTextContent();
			String osArch = osArchNode == null ? null : osArchNode.getTextContent();

			// Determine what resources should not be included.
			if( osName != null && !os.startsWith( osName ) ) continue;
			if( osArch != null && !arch.equals( osArch ) ) continue;

			uris = resourcesDescriptor.getValues( path );
			if( uris != null ) resources.addAll( Arrays.asList( uris ) );
		}

		return resources.toArray( new String[resources.size()] );
	}

}

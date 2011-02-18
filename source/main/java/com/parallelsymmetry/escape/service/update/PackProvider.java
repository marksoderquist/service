package com.parallelsymmetry.escape.service.update;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Node;

import com.parallelsymmetry.escape.utility.Descriptor;

public class PackProvider implements UpdateProvider {

	private UpdatePack pack;

	public PackProvider( UpdatePack pack ) {
		this.pack = pack;
	}

	@Override
	public Set<Resource> getResources() throws Exception {
		return getResources( pack );
	}

	private Set<Resource> getResources( UpdatePack source ) throws Exception {
		URI codebase = source.getUpdateUri();
		Descriptor descriptor = source.getDescriptor();

		Set<Resource> resources = new HashSet<Resource>();

		// Resolve all the files to download.
		String[] files = getResources( descriptor, "file/@uri" );
		String[] packs = getResources( descriptor, "pack/@uri" );
		String[] jnlps = getResources( descriptor, "jnlp/@uri" );

		for( String file : files ) {
			URI uri = codebase.resolve( file );
			resources.add( new Resource( Resource.Type.JAR, uri ) );
		}
		for( String pack : packs ) {
			URI uri = codebase.resolve( pack );
			resources.add( new Resource( Resource.Type.PACK, uri ) );
		}
		for( String jnlp : jnlps ) {
			URI uri = codebase.resolve( jnlp );
			resources.addAll( new JnlpProvider( UpdateManager.loadDescriptor( uri ) ).getResources() );
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

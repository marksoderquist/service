package com.parallelsymmetry.escape.service.update;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.UpdateManager;
import com.parallelsymmetry.escape.service.pack.UpdatePack;
import com.parallelsymmetry.escape.utility.Descriptor;

public class PackProvider implements UpdateProvider {

	private UpdatePack pack;

	public PackProvider( UpdatePack pack ) {
		this.pack = pack;
	}

	@Override
	public Set<Resource> getResources() {
		return getResources( pack );
	}

	private Set<Resource> getResources( UpdatePack pack ) {
		URI codebase = pack.getUpdateUri();
		Descriptor descriptor = pack.getDescriptor();

		Set<Resource> resources = new HashSet<Resource>();

		// Resolve all the files to download.
		String[] jars = getResources( descriptor, "jar/@uri" );
		String[] libs = getResources( descriptor, "lib/@uri" );
		String[] extensions = getResources( descriptor, "extension/@uri" );

		for( String jar : jars ) {
			URI uri = codebase.resolve( jar );
			resources.add( new Resource( Resource.Type.JAR, uri ) );
		}
		for( String lib : libs ) {
			URI uri = codebase.resolve( lib );
			resources.add( new Resource( Resource.Type.PACK, uri ) );
		}

		// TODO What about JNLP and other extensions.
		for( String extension : extensions ) {
			URI uri = codebase.resolve( extension );
			resources.addAll( getResources( UpdateManager.loadUpdatePack( uri ) ) );
		}

		return resources;
	}

	private String[] getResources( Descriptor descriptor, String path ) {
		String os = System.getProperty( "os.name" );
		String arch = System.getProperty( "os.arch" );

		String[] uris = null;
		Set<String> resources = new HashSet<String>();

		// Determine the resources.
		Node[] nodes = descriptor.getNodes( "/jnlp/resources" );
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

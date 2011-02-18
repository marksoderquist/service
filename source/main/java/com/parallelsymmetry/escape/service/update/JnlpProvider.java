package com.parallelsymmetry.escape.service.update;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.Service;
import com.parallelsymmetry.escape.utility.Descriptor;

public class JnlpProvider implements UpdateProvider {

	private Service service;

	private Descriptor descriptor;

	public JnlpProvider( Service service, Descriptor descriptor ) {
		this.service = service;
		this.descriptor = descriptor;
	}

	@Override
	public Set<Resource> getResources() throws Exception {
		return getResources( descriptor );
	}

	private Set<Resource> getResources( Descriptor descriptor ) throws Exception {
		URI codebase = new URI( descriptor.getValue( "/jnlp/@codebase" ) );

		Set<Resource> resources = new HashSet<Resource>();

		// Resolve all the files to download.
		String[] jars = getResources( descriptor, "jar/@href" );
		String[] libs = getResources( descriptor, "lib/@href" );
		String[] natives = getResources( descriptor, "nativelib/@href" );
		String[] extensions = getResources( descriptor, "extension/@href" );

		for( String jar : jars ) {
			URI uri = codebase.resolve( jar );
			resources.add( new Resource( Resource.Type.JAR, uri ) );
		}
		for( String lib : libs ) {
			URI uri = codebase.resolve( lib );
			resources.add( new Resource( Resource.Type.PACK, uri ) );
		}
		for( String lib : natives ) {
			URI uri = codebase.resolve( lib );
			resources.add( new Resource( Resource.Type.PACK, uri ) );
		}
		for( String extension : extensions ) {
			URI uri = codebase.resolve( extension );
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

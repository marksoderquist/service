package com.parallelsymmetry.escape.product;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.w3c.dom.Node;

import com.parallelsymmetry.escape.service.task.DescriptorDownloadTask;
import com.parallelsymmetry.escape.utility.Descriptor;
import com.parallelsymmetry.escape.utility.task.TaskManager;

public class CardProvider implements ProductResourceProvider {

	private TaskManager taskManager;

	private ProductCard card;

	public CardProvider( ProductCard card, TaskManager taskManager ) {
		this.card = card;
		this.taskManager = taskManager;
	}

	@Override
	public Set<ProductResource> getResources() throws Exception {
		URI codebase = card.getUpdateUri();
		Descriptor descriptor = card.getDescriptor();

		Set<ProductResource> resources = new HashSet<ProductResource>();

		// Resolve all the files to download.
		String[] files = getResources( descriptor, "file/@uri" );
		String[] packs = getResources( descriptor, "pack/@uri" );
		String[] jnlps = getResources( descriptor, "jnlp/@uri" );

		for( String file : files ) {
			URI uri = codebase.resolve( file );
			resources.add( new ProductResource( ProductResource.Type.FILE, uri ) );
		}
		for( String pack : packs ) {
			URI uri = codebase.resolve( pack );
			resources.add( new ProductResource( ProductResource.Type.PACK, uri ) );
		}
		for( String jnlp : jnlps ) {
			URI uri = codebase.resolve( jnlp );
			Future<Descriptor> future = taskManager.submit( new DescriptorDownloadTask( uri ) );
			resources.addAll( new JnlpProvider( future.get(), taskManager ).getResources() );
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

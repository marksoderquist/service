package com.parallelsymmetry.service.product;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import com.parallelsymmetry.service.task.DescriptorDownloadTask;
import com.parallelsymmetry.utility.Descriptor;
import com.parallelsymmetry.utility.product.ProductCard;
import com.parallelsymmetry.utility.task.TaskManager;

public class PackProvider implements ProductResourceProvider {

	private TaskManager taskManager;

	private ProductCard card;

	public PackProvider( ProductCard card, TaskManager taskManager ) {
		this.card = card;
		this.taskManager = taskManager;
	}

	@Override
	public Set<ProductResource> getResources() throws Exception {
		URI codebase = card.getSourceUri();
		Set<ProductResource> resources = new HashSet<ProductResource>();

		// Determine all the resources that need to be downloaded.
		String[] files = card.getResourceUris( "file" );
		String[] packs = card.getResourceUris( "pack" );
		String[] jnlps = card.getResourceUris( "jnlp" );

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

}

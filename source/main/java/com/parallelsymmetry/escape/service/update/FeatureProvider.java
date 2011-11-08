package com.parallelsymmetry.escape.service.update;

import java.util.Set;

public interface FeatureProvider {

	Set<FeatureResource> getResources() throws Exception;
	
}

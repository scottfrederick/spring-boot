/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jooq.tools.StringUtils;

import org.springframework.boot.context.config.LocationResourceLoader.ResourceType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataLocationResolver} for service bindings locations.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class ServiceBindingConfigDataLocationResolver
		implements ConfigDataLocationResolver<ServiceBindingConfigDataResource> {

	private static final String PREFIX = "bindings:";

	private static final String SERVICE_BINDING_ROOT = "SERVICE_BINDING_ROOT";

	private final LocationResourceLoader resourceLoader;

	public ServiceBindingConfigDataLocationResolver(ResourceLoader resourceLoader) {
		this.resourceLoader = new LocationResourceLoader(resourceLoader);
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix(PREFIX);
	}

	@Override
	public List<ServiceBindingConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) {
		try {
			String nonPrefixedLocation = location.getNonPrefixedValue(PREFIX);
			if (StringUtils.isEmpty(nonPrefixedLocation)) {
				nonPrefixedLocation = System.getenv(SERVICE_BINDING_ROOT);
				if (!nonPrefixedLocation.endsWith("/")) {
					nonPrefixedLocation = nonPrefixedLocation + "/";
				}
			}
			return resolve(nonPrefixedLocation);
		}
		catch (IOException ex) {
			throw new ConfigDataLocationNotFoundException(location, ex);
		}
	}

	private List<ServiceBindingConfigDataResource> resolve(String location) throws IOException {
		Assert.isTrue(location.endsWith("/"),
				() -> String.format("Service bindings location '%s' must end with '/'", location));
		if (!this.resourceLoader.isPattern(location)) {
			return Collections.singletonList(new ServiceBindingConfigDataResource(location));
		}
		Resource[] resources = this.resourceLoader.getResources(location, ResourceType.DIRECTORY);
		List<ServiceBindingConfigDataResource> resolved = new ArrayList<>(resources.length);
		for (Resource resource : resources) {
			resolved.add(new ServiceBindingConfigDataResource(resource.getFile().toPath()));
		}
		return resolved;
	}

}

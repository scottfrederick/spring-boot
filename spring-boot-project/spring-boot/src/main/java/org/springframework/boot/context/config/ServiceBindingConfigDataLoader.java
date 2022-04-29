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
import java.nio.file.Path;
import java.util.Collections;

import org.springframework.boot.env.FileTreePropertySource.Option;
import org.springframework.boot.env.ServiceBindingPropertySource;

/**
 * {@link ConfigDataLoader} for service bindings.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class ServiceBindingConfigDataLoader implements ConfigDataLoader<ServiceBindingConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, ServiceBindingConfigDataResource resource)
			throws IOException, ConfigDataResourceNotFoundException {
		Path path = resource.getPath();
		ConfigDataResourceNotFoundException.throwIfDoesNotExist(resource, path);
		String name = "Service bindings '" + path + "'";
		ServiceBindingPropertySource source = new ServiceBindingPropertySource(name, path,
				Option.AUTO_TRIM_TRAILING_NEW_LINE, Option.USE_LOWERCASE_NAMES);
		return new ConfigData(Collections.singletonList(source));
	}

}

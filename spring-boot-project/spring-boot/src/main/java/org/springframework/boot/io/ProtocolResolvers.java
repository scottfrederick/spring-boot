/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.io;

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.util.Assert;

/**
 * {@link ProtocolResolver} implementations that are loaded from a
 * {@code spring.factories} file.
 *
 * @author Scott Frederick
 */
final class ProtocolResolvers {

	private static List<ProtocolResolver> protocolResolvers;

	private ProtocolResolvers() {
	}

	/**
	 * Apply all discovered {@link ProtocolResolver}s to the provided
	 * {@link DefaultResourceLoader}.
	 * @param resourceLoader the resource loader to apply protocol resolvers to
	 * @param <T> a resource loader
	 */
	static <T extends DefaultResourceLoader> void applyTo(T resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		if (protocolResolvers != null) {
			resourceLoader.getProtocolResolvers().addAll(protocolResolvers);
		}
	}

	private static void initialize(ConfigurableApplicationContext context) {
		ArgumentResolver argumentResolver = getArgumentResolver(context);
		SpringFactoriesLoader loader = SpringFactoriesLoader
			.forDefaultResourceLocation((context != null) ? context.getClassLoader() : null);
		protocolResolvers = loader.load(ProtocolResolver.class, argumentResolver);
	}

	private static ArgumentResolver getArgumentResolver(ConfigurableApplicationContext context) {
		if (context == null) {
			return null;
		}
		return ArgumentResolver.of(BeanFactory.class, context.getBeanFactory())
			.and(Environment.class, context.getEnvironment());
	}

	static class ProtocolResolversInitializer implements ApplicationListener<ApplicationPreparedEvent> {

		@Override
		public void onApplicationEvent(ApplicationPreparedEvent event) {
			ConfigurableApplicationContext context = event.getApplicationContext();
			ProtocolResolvers.initialize(context);
		}

	}

}

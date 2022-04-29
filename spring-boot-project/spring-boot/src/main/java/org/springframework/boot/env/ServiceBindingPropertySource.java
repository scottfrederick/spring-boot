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

package org.springframework.boot.env;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.origin.Origin;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that maps service bindings to Spring Boot properties.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class ServiceBindingPropertySource extends FileTreePropertySource {

	private final ServiceBindings bindings;

	private final String[] names;

	/**
	 * Create a new {@link ServiceBindingPropertySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 */
	public ServiceBindingPropertySource(String name, Path sourceDirectory) {
		this(name, sourceDirectory, EnumSet.noneOf(Option.class));
	}

	/**
	 * Create a new {@link ServiceBindingPropertySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 * @param options the property source options
	 */
	public ServiceBindingPropertySource(String name, Path sourceDirectory, Option... options) {
		this(name, sourceDirectory, EnumSet.copyOf(Arrays.asList(options)));
	}

	private ServiceBindingPropertySource(String name, Path sourceDirectory, Set<Option> options) {
		super(name, sourceDirectory, options);
		this.bindings = ServiceBindings.findAll(sourceDirectory, options);
		this.names = StringUtils.toStringArray(this.bindings.getNames());
	}

	@Override
	public String[] getPropertyNames() {
		return this.names.clone();
	}

	@Override
	public Value getProperty(String name) {
		PropertyFile propertyFile = this.bindings.get(name);
		return (propertyFile != null) ? propertyFile.getContent() : null;
	}

	@Override
	public Origin getOrigin(String name) {
		PropertyFile propertyFile = this.bindings.get(name);
		return (propertyFile != null) ? propertyFile.getOrigin() : null;
	}

	@Override
	public boolean isImmutable() {
		return !this.options.contains(Option.ALWAYS_READ);
	}

	private static final class ServiceBindings {

		private final Map<String, ServiceBinding> bindings;

		private ServiceBindings(Map<String, ServiceBinding> bindings) {
			this.bindings = bindings;
		}

		static ServiceBindings findAll(Path bindingsRootPath, Set<Option> options) {
			if (!Files.exists(bindingsRootPath)) {
				return new ServiceBindings(Collections.emptyMap());
			}
			Assert.isTrue(Files.isDirectory(bindingsRootPath),
					() -> "The service binding root '" + bindingsRootPath + "' must be a directory");
			try (Stream<Path> files = Files.list(bindingsRootPath)) {
				return new ServiceBindings(files.collect(
						Collectors.toMap((p) -> p.getFileName().toString(), (p) -> new ServiceBinding(p, options))));
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						String.format("Unable to list bindings in service binding root '%s'", bindingsRootPath), ex);
			}
		}

		Collection<String> getNames() {
			Collection<String> names = new ArrayList<>();
			this.bindings.forEach((bindingName, binding) -> binding.getProperties().keySet()
					.forEach((propertyName) -> names.add(String.format("%s.%s", bindingName, propertyName))));
			return names;
		}

		PropertyFile get(String name) {
			String[] split = name.split("\\.");
			if (split.length != 2) {
				return null;
			}
			String bindingName = split[0];
			String propertyName = split[1];
			if (this.bindings.containsKey(bindingName)) {
				return this.bindings.get(bindingName).getProperties().get(propertyName);
			}
			return null;
		}

	}

	private static final class ServiceBinding {

		/**
		 * The key for the provider of a binding.
		 */
		public static final String PROVIDER = "provider";

		/**
		 * The key for the type of a binding.
		 */
		public static final String TYPE = "type";

		private final Set<Option> options;

		private final String name;

		private final String type;

		private final String provider;

		private final Map<String, PropertyFile> properties;

		private ServiceBinding(Path path, Set<Option> options) {
			this.name = path.getFileName().toString();
			this.options = options;
			this.properties = new HashMap<>();

			String type = null;
			String provider = null;
			Map<String, PropertyFile> foundProperties = findPropertyFiles(path);
			for (Map.Entry<String, PropertyFile> entry : foundProperties.entrySet()) {
				switch (entry.getKey()) {
					case TYPE:
						type = entry.getValue().getContent().toString();
						break;
					case PROVIDER:
						provider = entry.getValue().getContent().toString();
						break;
					default:
						this.properties.put(entry.getKey(), entry.getValue());
						break;
				}
			}

			if (type == null) {
				throw new IllegalArgumentException(String.format("Service binding '%s' must have a 'type'", path));
			}

			this.type = type;
			this.provider = provider;
		}

		private Map<String, PropertyFile> findPropertyFiles(Path path) {
			if (!Files.exists(path)) {
				return Collections.emptyMap();
			}
			try (Stream<Path> files = Files.list(path)) {
				return files.filter(this::isPropertyFile).collect(
						Collectors.toMap((p) -> p.getFileName().toString(), (p) -> new PropertyFile(p, this.options)));
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						String.format("Unable to list properties of service binding '%s'", path), ex);
			}
		}

		private boolean isPropertyFile(Path p) {
			try {
				return !Files.isDirectory(p) && !Files.isHidden(p);
			}
			catch (IOException ex) {
				throw new IllegalStateException(String.format("Unable to determine attributes of file '%s'", p), ex);
			}
		}

		String getName() {
			return this.name;
		}

		String getType() {
			return this.type;
		}

		String getProvider() {
			return this.provider;
		}

		Map<String, PropertyFile> getProperties() {
			return Collections.unmodifiableMap(this.properties);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ServiceBinding binding = (ServiceBinding) o;
			return this.name.equals(binding.name) && Objects.equals(this.provider, binding.provider)
					&& this.properties.equals(binding.properties) && Objects.equals(this.type, binding.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name, this.provider, this.properties, this.type);
		}

		@Override
		public String toString() {
			return "Binding{" + "name='" + this.name + '\'' + "provider='" + this.provider + '\'' + ", properties="
					+ new TreeSet<>(this.properties.keySet()) + ", type='" + this.type + '\'' + '}';
		}

	}

}

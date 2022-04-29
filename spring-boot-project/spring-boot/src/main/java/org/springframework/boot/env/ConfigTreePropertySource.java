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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.origin.Origin;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} backed by a directory tree that contains files for each value.
 * The {@link PropertySource} will recursively scan a given source directory and expose a
 * property for each file found. The property name will be the filename, and the property
 * value will be the contents of the file.
 * <p>
 * Directories are only scanned when the source is first created. The directory is not
 * monitored for updates, so files should not be added or removed. However, the contents
 * of a file can be updated as long as the property source was created with a
 * {@link Option#ALWAYS_READ} option. Nested directories are included in the source, but
 * with a {@code '.'} rather than {@code '/'} used as the path separator.
 * <p>
 * Property values are returned as {@link Value} instances which allows them to be treated
 * either as an {@link InputStreamSource} or as a {@link CharSequence}. In addition, if
 * used with an {@link Environment} configured with an
 * {@link ApplicationConversionService}, property values can be converted to a
 * {@code String} or {@code byte[]}.
 * <p>
 * This property source is typically used to read Kubernetes {@code configMap} volume
 * mounts.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.4.0
 */
public class ConfigTreePropertySource extends FileTreePropertySource {

	private final Map<String, PropertyFile> propertyFiles;

	private final String[] names;

	/**
	 * Create a new {@link ConfigTreePropertySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 */
	public ConfigTreePropertySource(String name, Path sourceDirectory) {
		this(name, sourceDirectory, EnumSet.noneOf(Option.class));
	}

	/**
	 * Create a new {@link ConfigTreePropertySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 * @param options the property source options
	 */
	public ConfigTreePropertySource(String name, Path sourceDirectory, Option... options) {
		this(name, sourceDirectory, EnumSet.copyOf(Arrays.asList(options)));
	}

	private ConfigTreePropertySource(String name, Path sourceDirectory, Set<Option> options) {
		super(name, sourceDirectory, options);
		Assert.isTrue(Files.exists(sourceDirectory), () -> "Directory '" + sourceDirectory + "' does not exist");
		Assert.isTrue(Files.isDirectory(sourceDirectory), () -> "File '" + sourceDirectory + "' is not a directory");
		this.propertyFiles = PropertyFiles.findAll(sourceDirectory, options);
		this.names = StringUtils.toStringArray(this.propertyFiles.keySet());
	}

	@Override
	public String[] getPropertyNames() {
		return this.names.clone();
	}

	@Override
	public Value getProperty(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getContent() : null;
	}

	@Override
	public Origin getOrigin(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getOrigin() : null;
	}

	protected static final class PropertyFiles {

		private static final int MAX_DEPTH = 100;

		static Map<String, PropertyFile> findAll(Path sourceDirectory, Set<Option> options) {
			Map<String, PropertyFile> propertyFiles = new TreeMap<>();
			try (Stream<Path> files = Files.find(sourceDirectory, MAX_DEPTH, PropertyFiles::isPropertyFile,
					FileVisitOption.FOLLOW_LINKS)) {
				files.forEach((path) -> {
					String name = getName(sourceDirectory.relativize(path));
					if (StringUtils.hasText(name)) {
						if (options.contains(Option.USE_LOWERCASE_NAMES)) {
							name = name.toLowerCase();
						}
						propertyFiles.put(name, new PropertyFile(path, options));
					}
				});
				return Collections.unmodifiableMap(propertyFiles);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to find files in '" + sourceDirectory + "'", ex);
			}
		}

		private static boolean isPropertyFile(Path path, BasicFileAttributes attributes) {
			return !hasHiddenPathElement(path) && (attributes.isRegularFile() || attributes.isSymbolicLink());
		}

		private static boolean hasHiddenPathElement(Path path) {
			for (Path value : path) {
				if (value.toString().startsWith("..")) {
					return true;
				}
			}
			return false;
		}

		private static String getName(Path relativePath) {
			int nameCount = relativePath.getNameCount();
			if (nameCount == 1) {
				return relativePath.toString();
			}
			StringBuilder name = new StringBuilder();
			for (int i = 0; i < nameCount; i++) {
				name.append((i != 0) ? "." : "");
				name.append(relativePath.getName(i));
			}
			return name.toString();
		}

	}

}

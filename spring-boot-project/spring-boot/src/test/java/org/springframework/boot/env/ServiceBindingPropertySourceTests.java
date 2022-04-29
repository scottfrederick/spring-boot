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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.env.FileTreePropertySource.Option;
import org.springframework.boot.env.FileTreePropertySource.Value;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ServiceBindingPropertySource}.
 *
 * @author Scott Frederick
 */
class ServiceBindingPropertySourceTests {

	@TempDir
	Path directory;

	@Test
	void createWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServiceBindingPropertySource(null, this.directory))
				.withMessageContaining("name must contain");
	}

	@Test
	void createWhenSourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServiceBindingPropertySource("test", null))
				.withMessage("Property source must not be null");
	}

	@Test
	void createWhenSourceDoesNotExistReturnsEmpty() {
		Path missing = this.directory.resolve("missing");
		ServiceBindingPropertySource propertySource = new ServiceBindingPropertySource("test", missing);
		assertThat(propertySource.getPropertyNames()).isEmpty();
	}

	@Test
	void createWhenSourceIsFileThrowsException() throws Exception {
		Path file = this.directory.resolve("file");
		FileCopyUtils.copy("test".getBytes(StandardCharsets.UTF_8), file.toFile());
		assertThatIllegalArgumentException().isThrownBy(() -> new ServiceBindingPropertySource("test", file))
				.withMessage("The service binding root '" + file + "' must be a directory");
	}

	@Test
	void getPropertyNamesFromBindingsReturnsPropertyNames() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		assertThat(propertySource.getPropertyNames()).containsExactlyInAnyOrder("db.host", "db.port");
	}

	@Test
	void getPropertyNamesFromBindingsWithSymlinksIgnoresHiddenFiles() throws Exception {
		ServiceBindingPropertySource propertySource = getSymlinkedServiceBindings();
		assertThat(propertySource.getPropertyNames()).containsExactlyInAnyOrder("db.host", "db.port");
	}

	@Test
	void getPropertyFromBindingsReturnsFileContent() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		assertThat(propertySource.getProperty("db.host")).hasToString("hostname");
		assertThat(propertySource.getProperty("db.port")).hasToString("9999");
	}

	@Test
	void getPropertyFromBindingsWhenMissingReturnsNull() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		assertThat(propertySource.getProperty("db.missing")).isNull();
	}

	@Test
	void getPropertyWhenBindingNameMissingReturnsNull() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		assertThat(propertySource.getProperty("missing")).isNull();
	}

	@Test
	void getPropertyFromBindingsWhenFileDeletedThrowsException() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		Path b = this.directory.resolve("db/host");
		Files.delete(b);
		assertThatIllegalStateException().isThrownBy(() -> propertySource.getProperty("db.host").toString())
				.withMessage("The property file '" + b + "' no longer exists");
	}

	@Test
	void getOriginFromBindingsReturnsOrigin() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		TextResourceOrigin origin = (TextResourceOrigin) propertySource.getOrigin("db.host");
		assertThat(origin.getResource().getFile()).isEqualTo(this.directory.resolve("db/host").toFile());
		assertThat(origin.getLocation().getLine()).isEqualTo(0);
		assertThat(origin.getLocation().getColumn()).isEqualTo(0);
	}

	@Test
	void getOriginFromBindingsWhenMissingReturnsNull() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		assertThat(propertySource.getOrigin("db.missing")).isNull();
	}

	@Test
	void getPropertyWhenMissingBindingTypeThrowsException() throws Exception {
		addProperty("db/host", "hostname");
		addProperty("db/port", "9999");
		assertThatIllegalArgumentException().isThrownBy(() -> new ServiceBindingPropertySource("test", this.directory))
				.withMessageContaining("db' must have a 'type");
	}

	@Test
	void getPropertyWhenNotAlwaysReadIgnoresUpdates() throws Exception {
		ServiceBindingPropertySource propertySource = getServiceBindings();
		Value v1 = propertySource.getProperty("db.port");
		Value v2 = propertySource.getProperty("db.port");
		assertThat(v1).isSameAs(v2);
		assertThat(v1).hasToString("9999");
		assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('9', '9', '9', '9');
		addProperty("db/port", "0000");
		assertThat(v1).hasToString("9999");
		assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('9', '9', '9', '9');
	}

	@Test
	void getPropertyWhenAlwaysReadReflectsUpdates() throws Exception {
		addBindings();
		ServiceBindingPropertySource propertySource = new ServiceBindingPropertySource("test", this.directory,
				Option.ALWAYS_READ);
		Value v1 = propertySource.getProperty("db.port");
		Value v2 = propertySource.getProperty("db.port");
		assertThat(v1).isNotSameAs(v2);
		assertThat(v1).hasToString("9999");
		assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('9', '9', '9', '9');
		addProperty("db/port", "0000");
		assertThat(v1).hasToString("0000");
		assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('0', '0', '0', '0');
		assertThat(propertySource.getProperty("db.port")).hasToString("0000");
	}

	private ServiceBindingPropertySource getServiceBindings() throws IOException {
		addBindings();
		return new ServiceBindingPropertySource("test", this.directory);
	}

	private void addBindings() throws IOException {
		addProperty("db/type", "relational");
		addProperty("db/provider", "vendor");
		addProperty("db/host", "hostname");
		addProperty("db/port", "9999");
	}

	private ServiceBindingPropertySource getSymlinkedServiceBindings() throws IOException {
		addProperty("db/..hidden-type", "relational");
		addProperty("db/..hidden-provider", "vendor");
		addProperty("db/..hidden-host", "hostname");
		addProperty("db/..hidden-port", "port");
		createSymbolicLink("db/type", "db/..hidden-type");
		createSymbolicLink("db/provider", "db/..hidden-provider");
		createSymbolicLink("db/host", "db/..hidden-host");
		createSymbolicLink("db/port", "db/..hidden-port");
		return new ServiceBindingPropertySource("test", this.directory);
	}

	private void addProperty(String path, String value) throws IOException {
		File file = this.directory.resolve(path).toFile();
		file.getParentFile().mkdirs();
		FileCopyUtils.copy(value.getBytes(StandardCharsets.UTF_8), file);
	}

	private void createSymbolicLink(String link, String target) throws IOException {
		Files.createSymbolicLink(this.directory.resolve(link).toAbsolutePath(),
				this.directory.resolve(target).toAbsolutePath());
	}

}

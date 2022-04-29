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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Base class for {@link PropertySource} implementations that read properties from a tree
 * of directories and files.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 3.1.0
 */
public abstract class FileTreePropertySource extends EnumerablePropertySource<Path> implements OriginLookup<String> {

	protected final Set<Option> options;

	protected FileTreePropertySource(String name, Path source, Set<Option> options) {
		super(name, source);
		this.options = options;
	}

	@Override
	public boolean isImmutable() {
		return !this.options.contains(Option.ALWAYS_READ);
	}

	/**
	 * Property source options.
	 */
	public enum Option {

		/**
		 * Always read the value of the file when accessing the property value. When this
		 * option is not set the property source will cache the value when it's first
		 * read.
		 */
		ALWAYS_READ,

		/**
		 * Convert file and directory names to lowercase.
		 */
		USE_LOWERCASE_NAMES,

		/**
		 * Automatically attempt trim trailing new-line characters.
		 */
		AUTO_TRIM_TRAILING_NEW_LINE

	}

	/**
	 * A value returned from the property source which exposes the contents of the
	 * property file. Values can either be treated as {@link CharSequence} or as an
	 * {@link InputStreamSource}.
	 */
	interface Value extends CharSequence, InputStreamSource {

	}

	/**
	 * A single property file that was found when the source was created.
	 */
	protected static final class PropertyFile {

		private static final TextResourceOrigin.Location START_OF_FILE = new TextResourceOrigin.Location(0, 0);

		private final Path path;

		private final PathResource resource;

		private final Origin origin;

		private final PropertyFileContent cachedContent;

		private final boolean autoTrimTrailingNewLine;

		PropertyFile(Path path, Set<Option> options) {
			this.path = path;
			this.resource = new PathResource(path);
			this.origin = new TextResourceOrigin(this.resource, START_OF_FILE);
			this.autoTrimTrailingNewLine = options.contains(Option.AUTO_TRIM_TRAILING_NEW_LINE);
			this.cachedContent = options.contains(Option.ALWAYS_READ) ? null
					: new PropertyFileContent(path, this.resource, this.origin, true, this.autoTrimTrailingNewLine);
		}

		PropertyFileContent getContent() {
			if (this.cachedContent != null) {
				return this.cachedContent;
			}
			return new PropertyFileContent(this.path, this.resource, this.origin, false, this.autoTrimTrailingNewLine);
		}

		Origin getOrigin() {
			return this.origin;
		}

	}

	/**
	 * The contents of a found property file.
	 */
	protected static final class PropertyFileContent implements Value, OriginProvider {

		private final Path path;

		private final Resource resource;

		private final Origin origin;

		private final boolean cacheContent;

		private final boolean autoTrimTrailingNewLine;

		private volatile byte[] content;

		private PropertyFileContent(Path path, Resource resource, Origin origin, boolean cacheContent,
				boolean autoTrimTrailingNewLine) {
			this.path = path;
			this.resource = resource;
			this.origin = origin;
			this.cacheContent = cacheContent;
			this.autoTrimTrailingNewLine = autoTrimTrailingNewLine;
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

		@Override
		public int length() {
			return toString().length();
		}

		@Override
		public char charAt(int index) {
			return toString().charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return toString().subSequence(start, end);
		}

		@Override
		public String toString() {
			String string = new String(getBytes());
			if (this.autoTrimTrailingNewLine) {
				string = autoTrimTrailingNewLine(string);
			}
			return string;
		}

		private String autoTrimTrailingNewLine(String string) {
			if (!string.endsWith("\n")) {
				return string;
			}
			int numberOfLines = 0;
			for (int i = 0; i < string.length(); i++) {
				char ch = string.charAt(i);
				if (ch == '\n') {
					numberOfLines++;
				}
			}
			if (numberOfLines > 1) {
				return string;
			}
			return (string.endsWith("\r\n")) ? string.substring(0, string.length() - 2)
					: string.substring(0, string.length() - 1);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (!this.cacheContent) {
				assertStillExists();
				return this.resource.getInputStream();
			}
			return new ByteArrayInputStream(getBytes());
		}

		private byte[] getBytes() {
			try {
				if (!this.cacheContent) {
					assertStillExists();
					return FileCopyUtils.copyToByteArray(this.resource.getInputStream());
				}
				if (this.content == null) {
					assertStillExists();
					synchronized (this.resource) {
						if (this.content == null) {
							this.content = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
						}
					}
				}
				return this.content;
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private void assertStillExists() {
			Assert.state(Files.exists(this.path), () -> "The property file '" + this.path + "' no longer exists");
		}

	}

}

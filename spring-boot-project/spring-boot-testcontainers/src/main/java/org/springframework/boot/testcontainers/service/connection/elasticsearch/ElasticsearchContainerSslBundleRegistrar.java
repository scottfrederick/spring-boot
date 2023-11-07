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

package org.springframework.boot.testcontainers.service.connection.elasticsearch;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;

public class ElasticsearchContainerSslBundleRegistrar implements SslBundleRegistrar {

	static final String SSL_BUNDLE_NAME = "elasticsearch-container";

	private final ElasticsearchContainer container;

	public ElasticsearchContainerSslBundleRegistrar(ElasticsearchContainer container) {
		this.container = container;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registry.registerBundle(SSL_BUNDLE_NAME,
				SslBundle.of(null, null, SslOptions.NONE, null, new ElasticsearchContainerSslManagerBundle()));
	}

	private final class ElasticsearchContainerSslManagerBundle implements SslManagerBundle {

		private ElasticsearchContainerSslManagerBundle() {
		}

		@Override
		public KeyManagerFactory getKeyManagerFactory() {
			return null;
		}

		@Override
		public TrustManagerFactory getTrustManagerFactory() {
			return null;
		}

		@Override
		public SSLContext createSslContext(String protocol) {
			return ElasticsearchContainerSslBundleRegistrar.this.container.createSslContextFromCa();
		}

	}

}

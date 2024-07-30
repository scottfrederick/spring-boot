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

package org.springframework.boot.ssl;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SslBundleKeyStore}.
 *
 * @author Scott Frederick
 */
class SslBundleKeyStoreTests {

	private static void assertExpectedPrivateKey(PrivateKey privateKey) {
		assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
	}

	private static void assertExpectedCertificate(X509Certificate certificate) {
		assertThat(certificate.getIssuerX500Principal().getName()).contains("ST=California,C=US");
	}

	@Nested
	class PemTests {

		@Test
		void getWithKeyAndCertificateReturnsKeyAndCertificate() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
				.withPrivateKey("classpath:test-key.pem");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores);
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertExpectedPrivateKey(bundleKeyStore.getPrivateKey());
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithKeyAndCertificateUsingAliasReturnsKeyAndCertificate() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
				.withPrivateKey("classpath:test-key.pem")
				.withPrivateKeyPassword("password")
				.withPassword("secret")
				.withAlias("test-alias");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("secret", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertExpectedPrivateKey(bundleKeyStore.getPrivateKey());
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithKeyAndCertificateUsingInvalidAliasFails() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
				.withPrivateKey("classpath:test-key.pem")
				.withPrivateKeyPassword("password")
				.withPassword("secret")
				.withAlias("test-alias");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("secret", "wrong-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining("Private key with alias 'wrong-alias' was not found in SSL bundle");
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getCertificate)
				.withMessageContaining("Certificate with alias 'wrong-alias' was not found in SSL bundle");
		}

		@Test
		void getWithKeyAndCertificateUsingInvalidKeyPasswordDoesNotFindPrivateKey() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
				.withPrivateKey("classpath:test-key.pem")
				.withPrivateKeyPassword("password")
				.withPassword("secret");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("invalid", null));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThat(bundleKeyStore.getPrivateKey()).isNull();
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithKeyAndCertificateUsingAliasAndInvalidKeyPasswordFailsGettingPrivateKey() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
				.withPrivateKey("classpath:test-key.pem")
				.withPrivateKeyPassword("password")
				.withPassword("secret")
				.withAlias("test-alias");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("invalid", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining(
						"Key with alias 'test-alias' could not be retrieved from the key store with the provided password");
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithCertificateReturnsCertificate() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores);
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThat(bundleKeyStore.getPrivateKey()).isNull();
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithCertificateUsingAliasReturnsCertificate() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
				.withPassword("secret")
				.withAlias("test-alias");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("secret", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining("Private key with alias 'test-alias' was not found in SSL bundle");
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithCertificateChainReturnsFirstCertificate() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert-chain.pem");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores);
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThat(bundleKeyStore.getPrivateKey()).isNull();
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithCertificateChainUsingAliasFails() {
			PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert-chain.pem")
				.withPassword("secret")
				.withAlias("test-alias");
			SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("secret", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining("Private key with alias 'test-alias' was not found in SSL bundle");
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getCertificate)
				.withMessageContaining("Certificate with alias 'test-alias' was not found in SSL bundle");
		}

	}

	@Nested
	class JksTests {

		@Test
		void getWithKeyAndCertificateReturnsKeyAndCertificate() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test.jks", "secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("password", null));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertExpectedPrivateKey(bundleKeyStore.getPrivateKey());
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithKeyAndCertificateUsingAliasReturnsKeyAndCertificate() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test.jks", "secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("password", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertExpectedPrivateKey(bundleKeyStore.getPrivateKey());
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithKeyAndCertificateUsingInvalidAliasFails() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test.jks", "secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("password", "wrong-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining("Private key with alias 'wrong-alias' was not found in SSL bundle");
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getCertificate)
				.withMessageContaining("Certificate with alias 'wrong-alias' was not found in SSL bundle");
		}

		@Test
		void getWithKeyAndCertificateUsingInvalidKeyPasswordDoesNotFindPrivateKey() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test.jks", "secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("invalid", null));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThat(bundleKeyStore.getPrivateKey()).isNull();
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithKeyAndCertificateUsingAliasAndInvalidKeyPasswordFailsGettingPrivateKey() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test.jks", "secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("invalid", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining(
						"Key with alias 'test-alias' could not be retrieved from the key store with the provided password");
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithCertificateReturnsCertificate() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test-cert.p12",
					"secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores);
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThat(bundleKeyStore.getPrivateKey()).isNull();
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

		@Test
		void getWithCertificateUsingAliasReturnsCertificate() {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, null, "classpath:test-cert.p12",
					"secret");
			SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, null);
			SslBundle bundle = SslBundle.of(stores, SslBundleKey.of("password", "test-alias"));
			SslBundleKeyStore bundleKeyStore = SslBundleKeyStore.from(bundle.getStores().getKeyStore(),
					bundle.getKey());
			assertThatIllegalArgumentException().isThrownBy(bundleKeyStore::getPrivateKey)
				.withMessageContaining("Private key with alias 'test-alias' was not found in SSL bundle");
			assertExpectedCertificate(bundleKeyStore.getCertificate());
		}

	}

}

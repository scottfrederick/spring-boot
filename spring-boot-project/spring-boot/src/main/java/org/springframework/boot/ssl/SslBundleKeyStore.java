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

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.springframework.util.Assert;

/**
 * Provides access to private keys and certificates from a {@link KeyStore} created from
 * an {@link SslBundle}.
 *
 * @author Scott Frederick
 * @since 3.4.0
 */
public final class SslBundleKeyStore {

	private final KeyStore keyStore;

	private final SslBundleKey sslBundleKey;

	private SslBundleKeyStore(KeyStore keyStore, SslBundleKey sslBundleKey) {
		this.sslBundleKey = sslBundleKey;
		this.keyStore = keyStore;
	}

	/**
	 * Get the private key with the alias provided by {@link SslBundleKey#getAlias()} if
	 * the alias is specified; otherwise get the first {@link PrivateKey} in the
	 * {@link KeyStore} that can be retrieved using {@link SslBundleKey#getPassword()}.
	 * @return the private key, or {@code null} if no alias is specified and there is no
	 * {@code PrivateKey} in the {@code KeyStore}
	 */
	public PrivateKey getPrivateKey() {
		String keyAlias = this.sslBundleKey.getAlias();
		String keyPassword = this.sslBundleKey.getPassword();
		try {
			if (keyAlias != null) {
				Key key = this.keyStore.getKey(keyAlias, keyPassword.toCharArray());
				Assert.notNull(key, "Private key with alias '" + keyAlias + "' was not found in SSL bundle");
				Assert.isInstanceOf(PrivateKey.class, key,
						"Key with alias '" + keyAlias + "' was expected to be a PrivateKey");
				return (PrivateKey) key;
			}
			return getFirstPrivateKey(this.keyStore, keyPassword);
		}
		catch (UnrecoverableKeyException kex) {
			throw new IllegalArgumentException("Key with alias '" + keyAlias
					+ "' could not be retrieved from the key store with the provided password", kex);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Error getting private key from SSL bundle", ex);
		}
	}

	/**
	 * Get the certificate with the alias provided by {@link SslBundleKey#getAlias()} if
	 * the alias is specified; otherwise get the first {@link X509Certificate} in the
	 * {@link KeyStore}.
	 * @return the certificate, or {@code null} if no alias is specified and there is no
	 * {@code X509Certificate} in the {@code KeyStore}
	 */
	public X509Certificate getCertificate() {
		String keyAlias = (this.sslBundleKey != null) ? this.sslBundleKey.getAlias() : null;
		try {
			if (keyAlias != null) {
				Certificate certificate = this.keyStore.getCertificate(keyAlias);
				Assert.notNull(certificate, "Certificate with alias '" + keyAlias + "' was not found in SSL bundle");
				Assert.isInstanceOf(X509Certificate.class, certificate,
						"Certificate with alias '" + keyAlias + "' was expected to be an X509Certificate");
				return (X509Certificate) certificate;
			}
			return getFirstCertificate(this.keyStore);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Error getting X509 certificate from SSL bundle", ex);
		}
	}

	private PrivateKey getFirstPrivateKey(KeyStore keyStore, String keyPassword) throws KeyStoreException {
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			if (keyStore.isKeyEntry(alias)) {
				try {
					Key key = keyStore.getKey(alias, (keyPassword != null) ? keyPassword.toCharArray() : null);
					if (key instanceof PrivateKey privateKey) {
						return privateKey;
					}
				}
				catch (UnrecoverableKeyException kex) {
					// password does not match this key, keep looking
				}
				catch (GeneralSecurityException ex) {
					throw new IllegalStateException("Error getting private key from SSL bundle", ex);
				}
			}
		}
		return null;
	}

	private X509Certificate getFirstCertificate(KeyStore keyStore) throws KeyStoreException {
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			Certificate certificate = keyStore.getCertificate(alias);
			if (certificate instanceof X509Certificate) {
				return (X509Certificate) certificate;
			}
		}
		return null;
	}

	/**
	 * Construct a new {@link SslBundleKeyStore} from the provided {@link KeyStore} and
	 * {@link SslBundleKey}.
	 * @param keyStore the {@code KeyStore}
	 * @param sslBundleKey the {@code SslBundleKey}
	 * @return an {@link SslBundleKeyStore}
	 */
	public static SslBundleKeyStore from(KeyStore keyStore, SslBundleKey sslBundleKey) {
		return new SslBundleKeyStore(keyStore, sslBundleKey);
	}

}

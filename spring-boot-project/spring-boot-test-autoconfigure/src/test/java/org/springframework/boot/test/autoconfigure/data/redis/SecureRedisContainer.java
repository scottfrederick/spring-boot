/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.redis;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.testsupport.testcontainers.RedisContainer;

/**
 * A {@link GenericContainer} for Redis with SSL configuration.
 *
 * @author Scott Frederick
 */
class SecureRedisContainer extends RedisContainer {

	public static final int TRUST_FILE_MODE = 0x664;

	SecureRedisContainer() {
		this.withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.crt", TRUST_FILE_MODE),
				"/ssl/server.crt")
			.withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.key", TRUST_FILE_MODE),
					"/ssl/server.key")
			.withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.crt", TRUST_FILE_MODE),
					"/ssl/ca.crt")
			.withCommand("redis-server --tls-port 6379 --port 0 "
					+ "--tls-cert-file /ssl/server.crt --tls-key-file /ssl/server.key --tls-ca-cert-file /ssl/ca.crt");
	}

}

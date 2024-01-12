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

package org.springframework.boot.test.web.client;

import java.util.Collection;
import java.util.Map;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.RestTemplate;

/**
 * Provides access to {@link MockRestServiceServer} instances that have been bound to
 * {@link RestTemplate} or {@link RestClient} instances.
 *
 * @author Scott Frederick
 * @since 3.3.0
 */
public class MockRestServiceServers {

	private final Map<RestTemplate, MockRestServiceServer> restTemplateServers;

	private final Map<Builder, MockRestServiceServer> restClientServers;

	/**
	 * Create a new {@code MockRestServiceServers} instance.
	 * @param restTemplateServers {@code MockRestServiceServers} bound to
	 * {@code RestTemplates}
	 * @param restClientServers {@code MockRestServiceServers} bound to
	 * {@code RestClients}
	 */
	public MockRestServiceServers(Map<RestTemplate, MockRestServiceServer> restTemplateServers,
			Map<RestClient.Builder, MockRestServiceServer> restClientServers) {
		this.restTemplateServers = restTemplateServers;
		this.restClientServers = restClientServers;
	}

	/**
	 * Get the {@code MockRestServiceServer} bound to the single {@code RestTemplate}.
	 * @return the {@code MockRestServiceServer}
	 */
	public MockRestServiceServer forRestTemplate() {
		return getServer(this.restTemplateServers.values(), "RestTemplate");
	}

	/**
	 * Get the {@code MockRestServiceServer} bound to the provided {@code RestTemplate}.
	 * @param restTemplate the {@code RestTemplate} to return the bound server for
	 * @return the {@code MockRestServiceServer}
	 */
	public MockRestServiceServer forRestTemplate(RestTemplate restTemplate) {
		return this.restTemplateServers.get(restTemplate);
	}

	/**
	 * Get the {@code MockRestServiceServer} bound to the single
	 * {@code RestClient.Builder}.
	 * @return the {@code MockRestServiceServer}
	 */
	public MockRestServiceServer forRestClient() {
		return getServer(this.restClientServers.values(), "RestClient");
	}

	/**
	 * Get the {@code MockRestServiceServer} bound to the provided
	 * {@code RestClient.Builder}.
	 * @param restClient the {@code RestClient.Builder} to return the bound server for
	 * @return the {@code MockRestServiceServer}
	 */
	public MockRestServiceServer forRestClient(RestClient.Builder restClient) {
		return this.restClientServers.get(restClient);
	}

	private MockRestServiceServer getServer(Collection<MockRestServiceServer> servers, String type) {
		Assert.state(!servers.isEmpty(),
				"Unable to return a single MockRestServiceServer since no " + type + " has been bound");
		Assert.state(servers.size() == 1,
				"Unable to return a single MockRestServiceServer since more than one " + type + " has been bound");
		return servers.iterator().next();
	}

}

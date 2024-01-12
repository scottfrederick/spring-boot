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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MockRestServiceServers}.
 *
 * @author Scott Frederick
 */
class MockRestServiceServersTests {

	private final Map<RestTemplate, MockRestServiceServer> restTemplateServers = new HashMap<>();

	private final Map<RestClient.Builder, MockRestServiceServer> restClientServers = new HashMap<>();

	private final MockRestServiceServers servers = new MockRestServiceServers(this.restTemplateServers,
			this.restClientServers);

	@Test
	void getRestTemplateServerWhenNoServersAreBoundThrowsException() {
		assertThatIllegalStateException().isThrownBy(this.servers::forRestTemplate)
			.withMessageContaining(
					"Unable to return a single MockRestServiceServer since no RestTemplate has been bound");
	}

	@Test
	void getRestTemplateServerWhenMultipleServersAreBoundThrowsException() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		this.restTemplateServers.put(template1, MockRestServiceServer.bindTo(template1).build());
		this.restTemplateServers.put(template2, MockRestServiceServer.bindTo(template2).build());
		assertThatIllegalStateException().isThrownBy(this.servers::forRestTemplate)
			.withMessageContaining("Unable to return a single MockRestServiceServer "
					+ "since more than one RestTemplate has been bound");
	}

	@Test
	void getRestTemplateServerWhenSingleServerIsBoundReturnsServer() {
		RestTemplate template = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
		this.restTemplateServers.put(template, server);
		assertThat(this.servers.forRestTemplate()).isSameAs(server);
	}

	@Test
	void getRestTemplateServerWhenRestTemplateIsFoundReturnsServer() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		MockRestServiceServer server1 = MockRestServiceServer.bindTo(template1).build();
		MockRestServiceServer server2 = MockRestServiceServer.bindTo(template2).build();
		this.restTemplateServers.put(template1, server1);
		this.restTemplateServers.put(template2, server2);
		assertThat(this.servers.forRestTemplate(template1)).isSameAs(server1);
		assertThat(this.servers.forRestTemplate(template2)).isSameAs(server2);
	}

	@Test
	void getRestTemplateServerWhenRestTemplateIsNotFoundReturnsNull() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		this.restTemplateServers.put(template1, MockRestServiceServer.bindTo(template1).build());
		this.restTemplateServers.put(template2, MockRestServiceServer.bindTo(template2).build());
		assertThat(this.servers.forRestTemplate(new RestTemplate())).isNull();
	}

	@Test
	void getRestClientServerWhenNoServersAreBoundThrowsException() {
		assertThatIllegalStateException().isThrownBy(this.servers::forRestClient)
			.withMessageContaining(
					"Unable to return a single MockRestServiceServer since no RestClient has been bound");
	}

	@Test
	void getRestClientServerWhenMultipleServersAreBoundThrowsException() {
		RestClient.Builder client1 = RestClient.builder();
		RestClient.Builder client2 = RestClient.builder();
		this.restClientServers.put(client1, MockRestServiceServer.bindTo(client1).build());
		this.restClientServers.put(client2, MockRestServiceServer.bindTo(client2).build());
		assertThatIllegalStateException().isThrownBy(this.servers::forRestClient)
			.withMessageContaining("Unable to return a single MockRestServiceServer "
					+ "since more than one RestClient has been bound");
	}

	@Test
	void getRestClientServerWhenSingleServerIsBoundReturnsServer() {
		RestClient.Builder client = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
		this.restClientServers.put(client, server);
		assertThat(this.servers.forRestClient()).isSameAs(server);
	}

	@Test
	void getRestClientServerWhenRestClientIsFoundReturnsServer() {
		RestClient.Builder client1 = RestClient.builder();
		RestClient.Builder client2 = RestClient.builder();
		MockRestServiceServer server1 = MockRestServiceServer.bindTo(client1).build();
		MockRestServiceServer server2 = MockRestServiceServer.bindTo(client2).build();
		this.restClientServers.put(client1, server1);
		this.restClientServers.put(client2, server2);
		assertThat(this.servers.forRestClient(client1)).isSameAs(server1);
		assertThat(this.servers.forRestClient(client2)).isSameAs(server2);
	}

	@Test
	void getRestClientServerWhenRestClientIsNotFoundReturnsNull() {
		RestClient.Builder client1 = RestClient.builder();
		RestClient.Builder client2 = RestClient.builder();
		this.restClientServers.put(client1, MockRestServiceServer.bindTo(client1).build());
		this.restClientServers.put(client2, MockRestServiceServer.bindTo(client2).build());
		assertThat(this.servers.forRestClient(RestClient.builder())).isNull();
	}

}

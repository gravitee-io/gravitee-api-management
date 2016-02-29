/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.reactor;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.BodyPart;
import io.gravitee.reporter.api.http.RequestMetrics;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HttpServerRequest implements Request {

	private final String id;

	private final Instant instant;

	private String remoteAddress;

	private String localAddress;

	private HttpMethod method;

	private URI requestURI;

	private boolean secure;

	private HttpVersion version;

	private Map<String, String> queryParameters = new LinkedHashMap<>();

	private HttpHeaders headers = new HttpHeaders();

	private final RequestMetrics metrics;

	public HttpServerRequest() {
		this.instant = Instant.now();
		this.id = UUID.randomUUID().toString();
		this.metrics = RequestMetrics.on(instant.toEpochMilli()).build();
	}

	public HttpMethod method() {
		return method;
	}

	@Override
	public HttpVersion version() {
		return version;
	}

	@Override
	public Instant timestamp() {
		return Instant.now();
	}

	@Override
	public String remoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public String localAddress() {
		return this.localAddress;
	}

	@Override
	public Request bodyHandler(Handler<BodyPart> handler) {
		return this;
	}

	private Handler<Void> endHandler;

	@Override
	public Request endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	public Handler<Void> endHandler() {
		return endHandler;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public String uri() {
		return requestURI.toString();
	}

	@Override
	public String path() {
		return requestURI.getPath();
	}

	public void setRequestURI(URI requestURI) {
		this.requestURI = requestURI;
	}

	public Map<String, String> parameters() {
		return queryParameters;
	}

	@Override
	public HttpHeaders headers() {
		return headers;
	}

	public String id() {
		return id;
	}

	@Override
	public RequestMetrics metrics() {
		return metrics;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("HttpServerRequest{");
		sb.append("id='").append(id).append('\'');
		sb.append(", requestURI=").append(requestURI);
		sb.append('}');
		return sb.toString();
	}
}

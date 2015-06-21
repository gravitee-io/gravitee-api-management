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
package io.gravitee.gateway.core.http;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.Request;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ServerRequest implements Request {

	private final String id;

	private String remoteAddr;

	private HttpMethod method;

	private URI requestURI;

	private boolean secure;

	private HttpVersion version;

	private Map<String, String> queryParameters = new LinkedHashMap();

	private Map<String, String> headers = new HashMap();

	public ServerRequest() {
		this.id = UUID.randomUUID().toString();
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public HttpMethod method() {
		return method;
	}

	@Override
	public HttpVersion version() {
		return version;
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

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public Map<String, String> parameters() {
		return queryParameters;
	}

	@Override
	public Map<String, String> headers() {
		return headers;
	}

	public String id() {
		return id;
	}

	public boolean hasContent() {
		return false;
	}
}

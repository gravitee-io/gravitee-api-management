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

import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HttpServerRequest implements Request {

	private final String id;

	private final Date date;

	private String remoteAddress;

	private String localAddress;

	private HttpMethod method;

	private URI requestURI;

	private boolean secure;

	private HttpVersion version;

	private long contentLength;

	private InputStream inputStream;

	private String contentType;

	private Map<String, String> queryParameters = new LinkedHashMap();

	private Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	public HttpServerRequest() {
		this.date = new Date();
		this.id = UUID.randomUUID().toString();
	}

	public HttpMethod method() {
		return method;
	}

	@Override
	public HttpVersion version() {
		return version;
	}

	@Override
	public long contentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public String contentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public Date timestamp() {
		return date;
	}

	@Override
	public InputStream inputStream() {
		return inputStream;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	@Override
	public String remoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public String localAddress() {
		return this.localAddress;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("HttpServerRequest{");
		sb.append("id='").append(id).append('\'');
		sb.append(", requestURI=").append(requestURI);
		sb.append('}');
		return sb.toString();
	}
}

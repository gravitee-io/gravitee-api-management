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
package io.gravitee.gateway.http.core.client;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.ClientRequest;
import io.gravitee.gateway.api.ClientResponse;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractHttpClient extends AbstractLifecycleComponent<HttpClient> implements HttpClient {

    protected static final Set<String> HOP_HEADERS;

    protected final Logger loggerDumpHttpClient = LoggerFactory.getLogger("io.gravitee.gateway.http.client");

    static {
        Set<String> hopHeaders = new HashSet<>();
        // Standard HTTP headers
        hopHeaders.add(HttpHeaders.CONNECTION);
        hopHeaders.add(HttpHeaders.KEEP_ALIVE);
        hopHeaders.add(HttpHeaders.PROXY_AUTHORIZATION);
        hopHeaders.add(HttpHeaders.PROXY_AUTHENTICATE);
        hopHeaders.add(HttpHeaders.PROXY_CONNECTION);
        hopHeaders.add(HttpHeaders.TRANSFER_ENCODING);
        hopHeaders.add(HttpHeaders.TE);
        hopHeaders.add(HttpHeaders.TRAILER);
        hopHeaders.add(HttpHeaders.UPGRADE);

        // Gravitee HTTP headers
        hopHeaders.add(GraviteeHttpHeader.X_GRAVITEE_API_NAME);

        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    @Resource
    protected Api api;

    protected boolean hasContent(Request request) {
        return request.headers().contentLength() > 0 ||
                request.headers().contentType() != null ||
                request.headers().getFirst(HttpHeaders.TRANSFER_ENCODING) != null;
    }

    protected boolean isDumpRequestEnabled() {
        return api.getProxy().getHttpClient().getOptions().isDumpRequest();
    }

    @Override
    public ClientRequest request(String host, int port, HttpMethod method, String requestURI, Request serverRequest, Handler<ClientResponse> responseHandler) {
        if (isDumpRequestEnabled()) {
            loggerDumpHttpClient.info("{} rewriting: {} -> {}", serverRequest.id(), serverRequest.uri(), requestURI);
        }

        return request0(host, port, method, requestURI, serverRequest, responseHandler);
    }

    protected abstract ClientRequest request0(String host, int port, HttpMethod method, String requestURI, Request serverRequest, Handler<ClientResponse> responseHandler);
}
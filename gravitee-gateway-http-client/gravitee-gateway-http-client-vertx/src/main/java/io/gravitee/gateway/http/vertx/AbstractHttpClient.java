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
package io.gravitee.gateway.http.vertx;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.client.HttpClient;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractHttpClient extends AbstractLifecycleComponent<HttpClient> implements HttpClient {

    protected static final Set<String> HOP_HEADERS;

    static {
        Set<String> hopHeaders = new HashSet<String>();
        hopHeaders.add("connection");
        hopHeaders.add("keep-alive");
        hopHeaders.add("proxy-authorization");
        hopHeaders.add("proxy-authenticate");
        hopHeaders.add("proxy-connection");
        hopHeaders.add("transfer-encoding");
        hopHeaders.add("te");
        hopHeaders.add("trailer");
        hopHeaders.add("upgrade");
        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    @Resource
    protected Api api;

    protected String rewriteTarget(Request request, URI endpointUri) {
        final StringBuilder requestURI =
                new StringBuilder(request.path())
                        .delete(0, api.getProxy().getContextPath().length())
                        .insert(0, endpointUri.toString());

        if (request.parameters() != null && ! request.parameters().isEmpty()) {
            requestURI.append('?');

            for(Map.Entry<String, String> queryParam : request.parameters().entrySet()) {
                requestURI.append(queryParam.getKey()).append('=').append(queryParam.getValue()).append('&');
            }

            // Removing latest & separator
            requestURI.deleteCharAt(requestURI.length() - 1);
        }

        return requestURI.toString();
    }

    protected boolean hasContent(Request request) {
        return request.headers().contentLength() > 0 ||
                request.headers().contentType() != null ||
                request.headers().getFirst(HttpHeaders.TRANSFER_ENCODING) != null;
    }

    protected URI rewriteURI(Request request, URI endpointUri) {
        String newTarget = rewriteTarget(request, endpointUri);
        return newTarget == null ? null : URI.create(newTarget);
    }

    protected boolean isDumpRequestEnabled() {
        return api.getProxy().getHttpClient().getOptions().isDumpRequest();
    }
}

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
package io.gravitee.gateway.core.http.client;

import io.gravitee.gateway.api.Request;
import io.gravitee.model.Api;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractHttpClient implements HttpClient {

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

    protected final Api api;

    protected AbstractHttpClient(Api api) {
        this.api = api;
    }

    protected String rewriteTarget(Request request) {
        final StringBuilder requestURI =
                new StringBuilder(request.path())
                        .delete(0, api.getPublicURI().getPath().length())
                        .insert(0, api.getTargetURI().toString());

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

    private static boolean hasContent(Request request) {
        return request.contentLength() > 0 ||
                request.contentType() != null ||
                // TODO: create an enum class for common HTTP headers
                request.headers().get("Transfer-Encoding") != null;
    }

    protected URI rewriteURI(Request request) {
        String newTarget = rewriteTarget(request);
        return newTarget == null ? null : URI.create(newTarget);
    }

}

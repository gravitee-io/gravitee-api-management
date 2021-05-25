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
package io.gravitee.gateway.core.endpoint.resolver;

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.builder.ProxyRequestBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class UserDefinedProxyEndpoint extends AbstractProxyEndpoint {

    private static final String QUERY_SEPARATOR = "?";

    private final String uri;

    UserDefinedProxyEndpoint(Endpoint endpoint, String uri) {
        super(endpoint);
        this.uri = uri;
    }

    @Override
    public ProxyRequest createProxyRequest(Request request, Function<ProxyRequestBuilder, ProxyRequestBuilder> mapper) {
        ProxyRequestBuilder builder = ProxyRequestBuilder
            .from(request)
            .uri(getTargetWithoutQueryParams(uri))
            .parameters(mergeQueryParameters(uri, request.parameters()));

        if (mapper != null) {
            builder = mapper.apply(builder);
        }

        return builder.build();
    }

    /**
     * Extract query parameters from the uri parameter and copy to the parameters map
     * @param uri
     * @param parameters
     */
    private MultiValueMap<String, String> mergeQueryParameters(String uri, MultiValueMap<String, String> parameters) {
        MultiValueMap<String, String> queryParameters = URIUtils.parameters(uri);

        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach(
                (name, values) -> {
                    for (String value : values) {
                        queryParameters.add(name, value);
                    }
                }
            );
        }

        return queryParameters;
    }

    private String getTargetWithoutQueryParams(String uri) {
        int targetQueryIndex = uri.indexOf(QUERY_SEPARATOR);
        if (targetQueryIndex > -1) {
            return uri.substring(0, targetQueryIndex);
        } else {
            return uri;
        }
    }
}

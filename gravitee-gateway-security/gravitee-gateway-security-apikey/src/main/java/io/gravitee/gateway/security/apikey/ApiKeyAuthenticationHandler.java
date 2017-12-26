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
package io.gravitee.gateway.security.apikey;

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;

/**
 * An api-key based {@link AuthenticationHandler}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyAuthenticationHandler implements AuthenticationHandler {

    private final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationHandler.class);

    static final String API_KEY_POLICY = "api-key";

    @Value("${policy.api-key.header:" + GraviteeHttpHeader.X_GRAVITEE_API_KEY + "}")
    private String apiKeyHeader = GraviteeHttpHeader.X_GRAVITEE_API_KEY;

    @Value("${policy.api-key.param:api-key}")
    private String apiKeyQueryParameter = "api-key";

    @Override
    public boolean canHandle(Request request) {
        final String apiKey = lookForApiKey(request);
        return apiKey != null;
    }

    @Override
    public String name() {
        return "api_key";
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        return Collections.singletonList(
                (PluginAuthenticationPolicy) () -> API_KEY_POLICY);
    }

    private String lookForApiKey(Request request) {
        logger.debug("Looking for an API Key from request header: {}", apiKeyHeader);
        // 1_ First, search in HTTP headers
        String apiKey = request.headers().getFirst(apiKeyHeader);

        if (apiKey == null || apiKey.isEmpty()) {
            logger.debug("Looking for an API Key from request query parameter: {}", apiKeyQueryParameter);
            // 2_ If not found, search in query parameters
            apiKey = request.parameters().getFirst(apiKeyQueryParameter);
        }

        return apiKey;
    }
}

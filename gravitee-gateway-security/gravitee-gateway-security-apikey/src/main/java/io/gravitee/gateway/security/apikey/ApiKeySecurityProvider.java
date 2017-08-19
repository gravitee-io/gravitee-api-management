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
import io.gravitee.gateway.security.core.SecurityPolicy;
import io.gravitee.gateway.security.core.SecurityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * An api-key based {@link SecurityProvider}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeySecurityProvider implements SecurityProvider {

    private final Logger logger = LoggerFactory.getLogger(ApiKeySecurityProvider.class);

    static final String API_KEY_POLICY = "api-key";
    static final String API_KEY_POLICY_CONFIGURATION = "{}";

    @Value("${policy.api-key.header:" + GraviteeHttpHeader.X_GRAVITEE_API_KEY + "}")
    private String apiKeyHeader = GraviteeHttpHeader.X_GRAVITEE_API_KEY;

    @Value("${policy.api-key.param:api-key}")
    private String apiKeyQueryParameter = "api-key";

    static final SecurityPolicy POLICY = new SecurityPolicy() {
        @Override
        public String policy() {
            return API_KEY_POLICY;
        }

        @Override
        public String configuration() {
            return API_KEY_POLICY_CONFIGURATION;
        }
    };

    @Override
    public boolean canHandle(Request request) {
        final String apiKey = lookForApiKey(request);

        if (apiKey == null) {
            logger.debug("No API Key has been found from the incoming requests. Skipping API Key authentication.");
            return false;
        }

        return true;
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
    public SecurityPolicy create(ExecutionContext executionContext) {
        return POLICY;
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

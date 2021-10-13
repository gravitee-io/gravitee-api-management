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

import static io.gravitee.reporter.api.http.SecurityType.API_KEY;

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.ComponentResolver;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * An api-key based {@link AuthenticationHandler}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyAuthenticationHandler implements AuthenticationHandler, ComponentResolver {

    private final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationHandler.class);

    static final String API_KEY_POLICY = "api-key";

    private static final String APIKEY_CONTEXT_ATTRIBUTE = "apikey";

    private static final List<AuthenticationPolicy> POLICIES = Collections.singletonList((PluginAuthenticationPolicy) () -> API_KEY_POLICY);

    private String apiKeyHeader;

    private String apiKeyQueryParameter;

    private Api api;

    private ApiKeyRepository apiKeyRepository;

    @Override
    public boolean canHandle(AuthenticationContext context) {
        final String apiKey = readApiKey(context.request());

        if (apiKey == null) {
            return false;
        }

        if (apiKeyRepository != null) {
            // Get the api-key from the repository if not present in the context
            if (context.get(APIKEY_CONTEXT_ATTRIBUTE) == null) {
                try {
                    final Optional<ApiKey> optApiKey = apiKeyRepository.findByKeyAndApi(apiKey, api.getId());
                    if (optApiKey.isPresent()) {
                        context.request().metrics().setSecurityType(API_KEY);
                        context.request().metrics().setSecurityToken(apiKey);
                    }
                    context.set(APIKEY_CONTEXT_ATTRIBUTE, optApiKey);
                } catch (TechnicalException e) {
                    // Any API key plan can be selected, the request will be rejected by the API Key policy whatsoever
                }
            }
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
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        return POLICIES;
    }

    private String readApiKey(Request request) {
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

    @Override
    public void resolve(ComponentProvider componentProvider) {
        apiKeyRepository = componentProvider.getComponent(ApiKeyRepository.class);
        api = componentProvider.getComponent(Api.class);

        Environment environment = componentProvider.getComponent(Environment.class);
        apiKeyHeader = environment.getProperty("policy.api-key.header", GraviteeHttpHeader.X_GRAVITEE_API_KEY);
        apiKeyQueryParameter = environment.getProperty("policy.api-key.param", "api-key");
    }
}

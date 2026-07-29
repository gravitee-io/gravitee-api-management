/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.http.attributes;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.AttributesToHeaderPolicy;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.gravitee.policy.mock.MockPolicy;
import io.gravitee.policy.mock.configuration.MockPolicyConfiguration;
import java.util.List;
import java.util.Map;

public class AbstractAttributesIntegrationTest extends AbstractGatewayTest {

    protected ApiKey apiKey;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("attributes-to-header", PolicyBuilder.build("attributes-to-header", AttributesToHeaderPolicy.class));
        policies.put(
            "api-key",
            PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
        );
        policies.put("mock", PolicyBuilder.build("mock", MockPolicy.class, MockPolicyConfiguration.class));
    }

    protected void addApiKeyPlan(ReactableApi<?> api) {
        apiKey = anApiKey(api);
        if (api.getDefinition() instanceof Api) {
            ((Api) api.getDefinition()).setPlans(
                List.of(
                    Plan.builder()
                        .id("default_plan")
                        .api(api.getId())
                        .security("key_less")
                        .status("PUBLISHED")
                        .securityDefinition("{\"propagateApiKey\":true}")
                        .build(),
                    Plan.builder()
                        .id("plan-id")
                        .api(api.getId())
                        .security("API_KEY")
                        .status("PUBLISHED")
                        .securityDefinition("{\"propagateApiKey\":true}")
                        .build()
                )
            );
        }
    }

    protected ApiKey anApiKey(ReactableApi<?> api) {
        final ApiKey apiKey = new ApiKey();
        apiKey.setApi(api.getId());
        apiKey.setApplication("application-id");
        apiKey.setSubscription("subscription-id");
        apiKey.setPlan("plan-id");
        apiKey.setKey("apiKeyValue");
        return apiKey;
    }

    protected Subscription aSubscription() {
        final Subscription subscription = new Subscription();
        subscription.setApplication("application-id");
        subscription.setId("subscription-id");
        subscription.setPlan("plan-id");
        return subscription;
    }
}

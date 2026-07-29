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
package io.gravitee.apim.integration.tests.plan.multiple;

import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @author GraviteeSource Team
 */
public class PlanKeylessApiKeyV4IntegrationTest {

    public static void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        final Api apiDefinition = (Api) api.getDefinition();
        configurePlans(apiDefinition, Set.of("key-less", "api-key"));
    }

    public static void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    public static void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    public static void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    public static Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v4-proxy-api", true), Arguments.of("v4-message-api", false));
    }

    @Nested
    @DeployApi(value = { "/apis/plan/v4-proxy-api.json", "/apis/plan/v4-message-api.json" })
    public class SelectApiKeyTest extends PlanKeylessApiKeyV4EmulationIntegrationTest.AbstractSelectApiKeyTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            PlanKeylessApiKeyV4IntegrationTest.configureApi(api, definitionClass);
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            PlanKeylessApiKeyV4IntegrationTest.configureEntrypoints(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            PlanKeylessApiKeyV4IntegrationTest.configureEndpoints(endpoints);
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            PlanKeylessApiKeyV4IntegrationTest.configureReactors(reactors);
        }

        @Override
        protected Stream<Arguments> provideApis() {
            return PlanKeylessApiKeyV4IntegrationTest.provideApis();
        }
    }

    @Nested
    @DeployApi(value = { "/apis/plan/v4-proxy-api.json", "/apis/plan/v4-message-api.json" })
    public class SelectKeylessTest extends PlanKeylessApiKeyV4EmulationIntegrationTest.AbstractSelectKeylessTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            PlanKeylessApiKeyV4IntegrationTest.configureApi(api, definitionClass);
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            PlanKeylessApiKeyV4IntegrationTest.configureEntrypoints(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            PlanKeylessApiKeyV4IntegrationTest.configureEndpoints(endpoints);
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            PlanKeylessApiKeyV4IntegrationTest.configureReactors(reactors);
        }

        @Override
        protected Stream<Arguments> provideApis() {
            return PlanKeylessApiKeyV4IntegrationTest.provideApis();
        }
    }
}

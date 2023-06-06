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
package io.gravitee.apim.integration.tests.http.bestmatch;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import java.util.Map;
import org.junit.jupiter.api.Nested;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BestMatchV4IntegrationTest extends BestMatchIntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/bestmatch/api.json")
    class StartsWithOperator extends BestMatchIntegrationTest.StartsWithOperator {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/bestmatch/api.json")
    class EqualsOperator extends BestMatchIntegrationTest.EqualsOperator {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                final Api definition = (Api) api.getDefinition();
                definition
                        .getFlows()
                        .stream()
                        .flatMap(flow -> flow.selectorByType(SelectorType.HTTP).stream())
                        .forEach(selector -> {
                            HttpSelector httpSelector = (HttpSelector) selector;
                            httpSelector.setPathOperator(Operator.EQUALS);
                        });
                definition
                        .getPlans()
                        .stream()
                        .flatMap(plan -> plan.getFlows().stream())
                        .flatMap(flow -> flow.selectorByType(SelectorType.HTTP).stream())
                        .forEach(selector -> {
                            HttpSelector httpSelector = (HttpSelector) selector;
                            httpSelector.setPathOperator(Operator.EQUALS);
                        });
            }
        }
    }
}

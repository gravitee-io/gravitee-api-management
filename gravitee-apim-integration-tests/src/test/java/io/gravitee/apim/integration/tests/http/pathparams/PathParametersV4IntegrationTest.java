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
package io.gravitee.apim.integration.tests.http.pathparams;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
public class PathParametersV4IntegrationTest extends PathParametersV3IntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    @Test
    @DeployApi("/apis/v4/http/pathparams/api-no-path-param.json")
    void should_not_add_path_param_to_headers_when_no_param(HttpClient httpClient) throws InterruptedException {
        super.should_not_add_path_param_to_headers_when_no_param(httpClient);
    }

    @Override
    @Test
    @DeployApi("/apis/v4/http/pathparams/api-no-path-param.json")
    void should_handle_mulitple_parallel_execution_when_path_param(HttpClient httpClient) throws InterruptedException {
        super.should_handle_mulitple_parallel_execution_when_path_param(httpClient);
    }

    @Override
    @ParameterizedTest
    @DeployApi("/apis/v4/http/pathparams/api-path-param.json")
    @MethodSource("io.gravitee.apim.integration.tests.http.pathparams.PathParametersV3IntegrationTest#provideParameters")
    void should_add_path_param_to_headers_when_no_param(
        String method,
        String path,
        Map<String, String> expectedHeaders,
        Set<String> excludedHeaders,
        HttpClient httpClient
    ) throws InterruptedException {
        super.should_add_path_param_to_headers_when_no_param(method, path, expectedHeaders, excludedHeaders, httpClient);
    }
}

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
package io.gravitee.apim.integration.tests.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganizations;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.AddHeader2Policy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author Gravitee Team
 */
@DeployOrganizations(
    {
        @DeployOrganization(
            organization = "/organizations/organization-1-add-header-1.json",
            apis = { "/organizations/apis/v4/api-1.json" }
        ),
        @DeployOrganization(
            organization = "/organizations/organization-2-add-header-2.json",
            apis = { "/organizations/apis/v4/api-2.json" }
        ),
        @DeployOrganization(organization = "/organizations/organization-3.json"),
    }
)
class HttpRequestOrganizationV4IntegrationTest extends HttpRequestOrganizationV4EmulationIntegrationTest {

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        super.configureEntrypoints(entrypoints);
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        super.configureEndpoints(endpoints);
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
    }

    private MessageStorage messageStorage;

    @BeforeEach
    void setUp() {
        messageStorage = getBean(MessageStorage.class);
    }

    @AfterEach
    void tearDown() {
        messageStorage.reset();
    }

    // Api 2 is a message api when testing V4
    @Override
    protected void assertOrganization2RequestHeaders() {
        messageStorage
            .subject()
            .take(1)
            .test()
            .assertValue(message -> {
                assertThat(message.headers().get(AddHeader2Policy.HEADER_NAME)).isEqualTo(AddHeader2Policy.REQUEST_HEADER);
                return true;
            });
    }
}

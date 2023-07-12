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
package io.gravitee.plugin.endpoint.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnectorConfiguration;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnector;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultEndpointConnectorPluginManagerTest {

    @Mock
    private DeploymentContext deploymentContext;

    private EndpointConnectorPluginManager cut;

    @BeforeEach
    public void beforeEach() {
        cut =
            new DefaultEndpointConnectorPluginManager(
                new DefaultEndpointConnectorClassLoaderFactory(),
                new ConnectorHelper(null, new ObjectMapper())
            );
    }

    @Test
    public void shouldRegisterNewEndpointPluginWithoutConfiguration() {
        DefaultEndpointConnectorPlugin endpointPlugin = new DefaultEndpointConnectorPlugin(
            new FakeEndpointConnectorPlugin(),
            FakeEndpointConnectorFactory.class,
            null
        );
        cut.register(endpointPlugin);
        final EndpointConnectorFactory<FakeEndpointConnector> fake = cut.getFactoryById("fake-endpoint");
        assertThat(fake).isNotNull();
        final EndpointConnector fakeConnector = fake.createConnector(deploymentContext, null);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    public void shouldRegisterNewEndpointPluginWithConfiguration() {
        DefaultEndpointConnectorPlugin endpointPlugin = new DefaultEndpointConnectorPlugin(
            new FakeEndpointConnectorPlugin(),
            FakeEndpointConnectorFactory.class,
            EndpointConnectorConfiguration.class
        );
        cut.register(endpointPlugin);
        final EndpointConnectorFactory<FakeEndpointConnector> fake = cut.getFactoryById("fake-endpoint");
        assertThat(fake).isNotNull();
        final FakeEndpointConnector fakeConnector = fake.createConnector(deploymentContext, "{\"info\":\"test\"}");
        assertThat(fakeConnector).isNotNull();
        assertThat(fakeConnector.getConfiguration().getInfo()).isEqualTo("test");
    }

    @Test
    public void shouldNotRetrieveUnRegisterPlugin() {
        final EndpointConnector factoryById = cut.getFactoryById("fake-endpoint");
        assertThat(factoryById).isNull();
    }
}

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

import io.gravitee.gateway.jupiter.api.connector.AbstractConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnectorConfiguration;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnector;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class DefaultEndpointConnectorPluginManagerTest {

    private EndpointConnectorPluginManager endpointConnectorPluginManager;

    @BeforeEach
    public void beforeEach() {
        endpointConnectorPluginManager = new DefaultEndpointConnectorPluginManager(new DefaultEndpointConnectorClassLoaderFactory());
    }

    @Test
    public void shouldRegisterNewEndpointPluginWithoutConfiguration() {
        DefaultEndpointConnectorPlugin endpointPlugin = new DefaultEndpointConnectorPlugin(
            new FakeEndpointConnectorPlugin(),
            FakeEndpointConnectorFactory.class,
            null
        );
        endpointConnectorPluginManager.register(endpointPlugin);
        final AbstractConnectorFactory<FakeEndpointConnector> fake = endpointConnectorPluginManager.getFactoryById("fake-endpoint");
        assertThat(fake).isNotNull();
        final FakeEndpointConnector fakeConnector = fake.createConnector(null);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    public void shouldRegisterNewEndpointPluginWithConfiguration() {
        DefaultEndpointConnectorPlugin endpointPlugin = new DefaultEndpointConnectorPlugin(
            new FakeEndpointConnectorPlugin(),
            FakeEndpointConnectorFactory.class,
            EndpointConnectorConfiguration.class
        );
        endpointConnectorPluginManager.register(endpointPlugin);
        final AbstractConnectorFactory<FakeEndpointConnector> fake = endpointConnectorPluginManager.getFactoryById("fake-endpoint");
        assertThat(fake).isNotNull();
        final FakeEndpointConnector fakeConnector = fake.createConnector("{\"info\":\"test\"}");
        assertThat(fakeConnector).isNotNull();
        assertThat(fakeConnector.getConfiguration().getInfo()).isEqualTo("test");
    }

    @Test
    public void shouldNotRetrieveUnRegisterPlugin() {
        final EndpointConnector factoryById = endpointConnectorPluginManager.getFactoryById("fake-endpoint");
        assertThat(factoryById).isNull();
    }
}

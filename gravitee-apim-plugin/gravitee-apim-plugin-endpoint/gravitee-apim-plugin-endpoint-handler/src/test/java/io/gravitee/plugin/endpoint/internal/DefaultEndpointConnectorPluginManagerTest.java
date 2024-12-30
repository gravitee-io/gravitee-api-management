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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorConfiguration;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnector;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorPlugin;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultEndpointConnectorPluginManagerTest {

    public static final String FAKE_ENDPOINT = "fake-endpoint";

    @Mock
    private DeploymentContext deploymentContext;

    private EndpointConnectorPluginManager cut;

    @BeforeEach
    void beforeEach() {
        cut =
            spy(
                new DefaultEndpointConnectorPluginManager(
                    new DefaultEndpointConnectorClassLoaderFactory(),
                    new PluginConfigurationHelper(null, new ObjectMapper())
                )
            );
    }

    @Test
    void shouldRegisterNewEndpointPluginWithoutConfiguration() {
        DefaultEndpointConnectorPlugin endpointPlugin = new DefaultEndpointConnectorPlugin(
            new FakeEndpointConnectorPlugin(),
            FakeEndpointConnectorFactory.class,
            null
        );
        cut.register(endpointPlugin);
        final EndpointConnectorFactory<FakeEndpointConnector> fake = cut.getFactoryById(FAKE_ENDPOINT);
        assertThat(fake).isNotNull();
        final EndpointConnector fakeConnector = fake.createConnector(deploymentContext, null, null);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    void shouldRegisterNewEndpointPluginWithConfiguration() {
        DefaultEndpointConnectorPlugin endpointPlugin = new DefaultEndpointConnectorPlugin(
            new FakeEndpointConnectorPlugin(),
            FakeEndpointConnectorFactory.class,
            EndpointConnectorConfiguration.class
        );
        cut.register(endpointPlugin);
        final EndpointConnectorFactory<FakeEndpointConnector> fake = cut.getFactoryById(FAKE_ENDPOINT);
        assertThat(fake).isNotNull();
        final FakeEndpointConnector fakeConnector = fake.createConnector(deploymentContext, "{\"info\":\"test\"}", null);
        assertThat(fakeConnector).isNotNull();
        assertThat(fakeConnector.getConfiguration().getInfo()).isEqualTo("test");
    }

    @Test
    void shouldNotRetrieveUnRegisterPlugin() {
        final EndpointConnector factoryById = cut.getFactoryById(FAKE_ENDPOINT);
        assertThat(factoryById).isNull();
    }

    @Test
    void shouldNotFindEndpointGroupSchemaFile() throws IOException {
        cut.register(FakeEndpointConnectorPlugin.createWithoutSharedConfigurationFile(true));
        final String schema = cut.getSharedConfigurationSchema(FAKE_ENDPOINT, false);
        assertThat(schema).isNull();
    }

    @Test
    void shouldGetFirstEndpointGroupSchemaFile() throws IOException {
        cut.register(FakeEndpointConnectorPlugin.createWithSharedConfigurationFile(true));
        final String schema = cut.getSharedConfigurationSchema(FAKE_ENDPOINT, true);
        assertThat(schema).isEqualTo("{\n  \"schema\": \"sharedConfiguration\"\n}");

        verify(cut, never()).getSchema(any(), any(), eq(true));
    }

    @Nested
    class DeprecatedForBackwardCompatibility {

        @Test
        void shouldNotFindEndpointGroupSchemaFile() throws IOException {
            cut.register(FakeEndpointConnectorPlugin.createLegacyWithoutSharedConfigurationFile(true));
            final String schema = cut.getSharedConfigurationSchema(FAKE_ENDPOINT, true);
            assertThat(schema).isNull();
        }

        @Test
        void shouldGetFirstEndpointGroupSchemaFile() throws IOException {
            cut.register(FakeEndpointConnectorPlugin.createLegacyWithSharedConfigurationFile(true));
            final String schema = cut.getSharedConfigurationSchema(FAKE_ENDPOINT, true);
            assertThat(schema).isEqualTo("{\n  \"schema\": \"sharedConfiguration\"\n}");
        }
    }

    @Test
    void shouldNotFindNotDeployedPlugin() {
        cut.register(FakeEndpointConnectorPlugin.createLegacyWithSharedConfigurationFile(false));
        final EndpointConnector factoryById = cut.getFactoryById(FAKE_ENDPOINT);
        assertThat(factoryById).isNull();
    }

    @Test
    void shouldFindNotDeployedPlugin() {
        cut.register(FakeEndpointConnectorPlugin.createLegacyWithSharedConfigurationFile(false));
        final EndpointConnectorFactory<FakeEndpointConnector> fake = cut.getFactoryById(FAKE_ENDPOINT, true);
        assertThat(fake).isNotNull();
    }
}

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
package io.gravitee.plugin.entrypoint.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.connector.entrypoint.BaseEntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.HttpEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnector;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnectorFactory;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnectorPlugin;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultEntrypointConnectorPluginManagerTest {

    private static final String FAKE_ENTRYPOINT = "fake-entrypoint";

    @Mock
    private DeploymentContext deploymentContext;

    private EntrypointConnectorPluginManager cut;

    @BeforeEach
    public void beforeEach() {
        cut =
            new DefaultEntrypointConnectorPluginManager(
                new DefaultEntrypointConnectorConnectorClassLoaderFactory(),
                new PluginConfigurationHelper(null, new ObjectMapper())
            );
    }

    @Test
    void shouldRegisterNewEntrypointPluginWithoutConfiguration() {
        DefaultEntrypointConnectorPlugin entrypointPlugin = new DefaultEntrypointConnectorPlugin(
            new FakeEntrypointConnectorPlugin(),
            FakeEntrypointConnectorFactory.class,
            null
        );
        cut.register(entrypointPlugin);
        EntrypointConnectorFactory<?> fake = cut.getFactoryById("fake-entrypoint");
        assertThat(fake).isNotNull();
        BaseEntrypointConnector fakeConnector = fake.createConnector(deploymentContext, null);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    void shouldRegisterNewEntrypointPluginWithConfiguration() {
        DefaultEntrypointConnectorPlugin<FakeEntrypointConnectorFactory, EntrypointConnectorConfiguration> entrypointPlugin =
            new DefaultEntrypointConnectorPlugin(
                new FakeEntrypointConnectorPlugin(),
                FakeEntrypointConnectorFactory.class,
                EntrypointConnectorConfiguration.class
            );
        cut.register(entrypointPlugin);
        EntrypointConnectorFactory<?> fake = cut.getFactoryById("fake-entrypoint");
        assertThat(fake).isNotNull();
        BaseEntrypointConnector fakeConnector = fake.createConnector(deploymentContext, "{\"info\":\"test\"}");
        assertThat(fakeConnector).isNotNull();
        assertThat(fakeConnector).isInstanceOf(FakeEntrypointConnector.class);
        FakeEntrypointConnector fakeEntrypointConnector = (FakeEntrypointConnector) fakeConnector;
        assertThat(fakeEntrypointConnector.getConfiguration().getInfo()).isEqualTo("test");
    }

    @Test
    void shouldNotRetrieveUnRegisterPlugin() {
        final HttpEntrypointConnector factoryById = cut.getFactoryById("fake-endpoint");
        assertThat(factoryById).isNull();
    }

    @Test
    void shouldNotFindSubscriptionSchemaFile() throws IOException {
        cut.register(new FakeEntrypointConnectorPlugin(true));
        final String schema = cut.getSubscriptionSchema(FAKE_ENTRYPOINT);
        assertThat(schema).isNull();
    }

    @Test
    void shouldGetFirstSubscriptionSchemaFile() throws IOException {
        cut.register(new FakeEntrypointConnectorPlugin());
        final String schema = cut.getSubscriptionSchema(FAKE_ENTRYPOINT);
        assertThat(schema).isEqualTo("{\n  \"schema\": \"subscription\"\n}");
    }

    @Test
    void shouldNotRetrieveNotDeployedPlugin() {
        cut.register(new FakeEntrypointConnectorPlugin(true, false));
        final HttpEntrypointConnector factoryById = cut.getFactoryById("fake-endpoint");
        assertThat(factoryById).isNull();
    }

    @Test
    void shouldRetrieveNotDeployedPlugin() {
        cut.register(new FakeEntrypointConnectorPlugin(true, false));
        final HttpEntrypointConnector factoryById = cut.getFactoryById("fake-endpoint", true);
        assertThat(factoryById).isNull();
    }

    @Test
    void shouldNotFindSubscriptionSchemaFileForNotDeployedPlugin() throws IOException {
        cut.register(new FakeEntrypointConnectorPlugin(true, false));
        final String schema = cut.getSubscriptionSchema(FAKE_ENTRYPOINT);
        assertThat(schema).isNull();
    }

    @Test
    void shouldFindSubscriptionSchemaFileForNotDeployedPlugin() throws IOException {
        cut.register(new FakeEntrypointConnectorPlugin(true, false));
        final String schema = cut.getSubscriptionSchema(FAKE_ENTRYPOINT, true);
        assertThat(schema).isNull();
    }
}

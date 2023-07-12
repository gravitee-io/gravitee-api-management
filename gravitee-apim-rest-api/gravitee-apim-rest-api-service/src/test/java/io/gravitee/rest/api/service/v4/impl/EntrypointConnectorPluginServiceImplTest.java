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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.fge.jsonschema.main.JsonSchema;
import com.google.errorprone.annotations.DoNotMock;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.Connector;
import io.gravitee.gateway.jupiter.api.connector.ConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrypointConnectorPluginServiceImplTest {

    private static final String CONNECTOR_ID = "connector-id";

    private EntrypointConnectorPluginService cut;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private EntrypointConnectorPluginManager pluginManager;

    @Before
    public void setUp() {
        cut = new EntrypointConnectorPluginServiceImpl(jsonSchemaService, pluginManager);
    }

    @Test
    public void shouldGetSubscriptionSchema() throws IOException {
        when(pluginManager.getSubscriptionSchema(CONNECTOR_ID)).thenReturn("subscriptionConfiguration");

        final String result = cut.getSubscriptionSchema(CONNECTOR_ID);

        assertThat(result).isEqualTo("subscriptionConfiguration");
    }

    @Test
    public void shouldNotGetSubscriptionSchemaBecauseOfIOException() throws IOException {
        when(pluginManager.getSubscriptionSchema(CONNECTOR_ID)).thenThrow(IOException.class);

        try {
            cut.getSubscriptionSchema(CONNECTOR_ID);
            fail("We should not go further because call should throw a TechnicalManagementException");
        } catch (TechnicalManagementException e) {
            assertThat(e.getMessage())
                .isEqualTo("An error occurs while trying to get entrypoint subscription schema for plugin " + CONNECTOR_ID);
        }
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldNotValidateSubscriptionConfigurationBecauseOfAbsentPlugin() {
        when(pluginManager.get(CONNECTOR_ID)).thenReturn(null);

        cut.validateEntrypointSubscriptionConfiguration(CONNECTOR_ID, "a configuration");
    }

    @Test
    public void shouldReturnNullWhenConfigurationNull() throws IOException {
        when(pluginManager.get(CONNECTOR_ID)).thenReturn(new FakePlugin());
        when(pluginManager.getFactoryById(CONNECTOR_ID)).thenReturn(new FakeConnectorFactory());

        assertThat(cut.validateEntrypointSubscriptionConfiguration(CONNECTOR_ID, null)).isNull();
    }

    @Test
    public void shouldValidateAndSanitize() throws IOException {
        final String configuration = "to_validate_and_sanitize";
        final String expectedConfiguration = "validated_and_sanitized";

        when(pluginManager.get(CONNECTOR_ID)).thenReturn(new FakePlugin());
        when(pluginManager.getFactoryById(CONNECTOR_ID)).thenReturn(new FakeConnectorFactory());
        when(pluginManager.getSubscriptionSchema(CONNECTOR_ID)).thenReturn("subscriptionConfiguration");
        when(jsonSchemaService.validate("subscriptionConfiguration", configuration)).thenReturn(expectedConfiguration);

        final String result = cut.validateEntrypointSubscriptionConfiguration(CONNECTOR_ID, configuration);
        assertThat(result).isEqualTo(expectedConfiguration);
    }

    private static class FakePlugin implements EntrypointConnectorPlugin {

        @Override
        public Class connectorFactory() {
            return FakeConnectorFactory.class;
        }

        @Override
        public Class configuration() {
            return null;
        }

        @Override
        public String id() {
            return CONNECTOR_ID;
        }

        @Override
        public String clazz() {
            return null;
        }

        @Override
        public Path path() {
            return null;
        }

        @Override
        public PluginManifest manifest() {
            return new PluginManifest() {
                @Override
                public String id() {
                    return CONNECTOR_ID;
                }

                @Override
                public String name() {
                    return null;
                }

                @Override
                public String description() {
                    return null;
                }

                @Override
                public String category() {
                    return null;
                }

                @Override
                public String version() {
                    return null;
                }

                @Override
                public String plugin() {
                    return null;
                }

                @Override
                public String type() {
                    return null;
                }
            };
        }

        @Override
        public URL[] dependencies() {
            return new URL[0];
        }
    }

    private static class FakeConnectorFactory implements EntrypointConnectorFactory {

        @Override
        public ApiType supportedApi() {
            return null;
        }

        @Override
        public Set<ConnectorMode> supportedModes() {
            return null;
        }

        @Override
        public Connector createConnector(DeploymentContext deploymentContext, String s) {
            return null;
        }
    }
}

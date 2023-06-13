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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorFactory;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EndpointConnectorPluginServiceImplTest {

    private static final String PLUGIN_ID = "my-test-plugin";
    private static final String CONFIGURATION = "my-test-configuration";
    private static final String SCHEMA = "my-test-schema";

    private EndpointConnectorPluginService cut;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private EndpointConnectorPluginManager pluginManager;

    @Mock
    private EndpointConnectorPlugin mockPlugin;

    @Mock
    private PluginManifest mockPluginManifest;

    @Mock
    private EndpointConnectorFactory<?> mockFactory;

    @Before
    public void setup() {
        cut = new EndpointConnectorPluginServiceImpl(jsonSchemaService, pluginManager);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(pluginManager.getFactoryById(PLUGIN_ID, true)).thenReturn(mockFactory);
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
        when(mockFactory.supportedApi()).thenReturn(ApiType.MESSAGE);
        when(mockFactory.supportedModes()).thenReturn(Set.of(io.gravitee.gateway.reactive.api.ConnectorMode.REQUEST_RESPONSE));
    }

    @Test
    public void shouldValidateConfiguration() throws IOException {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
        when(pluginManager.getSchema(PLUGIN_ID, true)).thenReturn(SCHEMA);
        when(jsonSchemaService.validate(SCHEMA, CONFIGURATION)).thenReturn("fixed-configuration");

        String resultConfiguration = cut.validateConnectorConfiguration(PLUGIN_ID, CONFIGURATION);

        assertEquals("fixed-configuration", resultConfiguration);
    }

    @Test
    public void shouldValidateConfigurationWhenNullConfiguration() throws IOException {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);

        String resultConfiguration = cut.validateConnectorConfiguration(PLUGIN_ID, null);

        assertNull(resultConfiguration);
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldFailToValidateConfigurationWhenNoPlugin() {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(null);

        cut.validateConnectorConfiguration(PLUGIN_ID, CONFIGURATION);
    }

    @Test
    public void shouldFindById() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);

        PlatformPluginEntity result = cut.findById(PLUGIN_ID);

        assertNotNull(result);
        assertEquals(PLUGIN_ID, result.getId());
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldNotFindById() {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(null);

        cut.findById(PLUGIN_ID);
    }

    @Test
    public void shouldFindAll() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));

        Set<ConnectorPluginEntity> result = cut.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PLUGIN_ID, result.iterator().next().getId());
    }

    @Test
    public void shouldGetEndpointSharedConfigurationSchema() throws IOException {
        when(pluginManager.getSharedConfigurationSchema(PLUGIN_ID, true)).thenReturn("sharedConfiguration");

        final String result = cut.getSharedConfigurationSchema(PLUGIN_ID);

        assertThat(result).isEqualTo("sharedConfiguration");
    }

    @Test
    public void shouldNotGetSharedConfigurationSchemaBecauseOfIOException() throws IOException {
        when(pluginManager.getSharedConfigurationSchema(PLUGIN_ID, true)).thenThrow(IOException.class);

        assertThatThrownBy(() -> cut.getSharedConfigurationSchema(PLUGIN_ID))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("An error occurs while trying to get endpoint shared configuration schema for plugin " + PLUGIN_ID);
    }
}

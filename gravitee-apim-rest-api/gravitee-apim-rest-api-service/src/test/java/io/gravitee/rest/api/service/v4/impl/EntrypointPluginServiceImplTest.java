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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class EntrypointPluginServiceImplTest {

    private static final String PLUGIN_ID = "my-test-plugin";
    private static final String CONFIGURATION = "my-test-configuration";
    private static final String SCHEMA = "my-test-schema";

    @InjectMocks
    private EntrypointConnectorPluginServiceImpl entrypointService;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private EntrypointConnectorPluginManager pluginManager;

    @Mock
    private EntrypointConnectorPlugin mockPlugin;

    @Mock
    private PluginManifest mockPluginManifest;

    @Mock
    private EntrypointConnectorFactory<?> mockFactory;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(entrypointService, "pluginManager", pluginManager);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(pluginManager.getFactoryById(PLUGIN_ID)).thenReturn(mockFactory);
        when(pluginManager.get(PLUGIN_ID)).thenReturn(mockPlugin);
        when(mockFactory.supportedApi()).thenReturn(ApiType.MESSAGE);
        when(mockFactory.supportedModes()).thenReturn(Set.of(io.gravitee.gateway.reactive.api.ConnectorMode.REQUEST_RESPONSE));
        when(mockFactory.supportedListenerType()).thenReturn(ListenerType.HTTP);
    }

    @Test
    public void shouldValidateConfiguration() throws IOException {
        when(pluginManager.getSchema(PLUGIN_ID)).thenReturn(SCHEMA);
        when(jsonSchemaService.validate(SCHEMA, CONFIGURATION)).thenReturn("fixed-configuration");

        String resultConfiguration = entrypointService.validateConnectorConfiguration(PLUGIN_ID, CONFIGURATION);

        assertEquals("fixed-configuration", resultConfiguration);
    }

    @Test
    public void shouldValidateConfigurationWhenNullConfiguration() {
        String resultConfiguration = entrypointService.validateConnectorConfiguration(PLUGIN_ID, null);

        assertNull(resultConfiguration);
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldFailToValidateConfigurationWhenNoPlugin() {
        when(pluginManager.get(PLUGIN_ID)).thenReturn(null);

        entrypointService.validateConnectorConfiguration(PLUGIN_ID, CONFIGURATION);
    }

    @Test
    public void shouldFindById() {
        ConnectorPluginEntity result = entrypointService.findById(PLUGIN_ID);

        assertNotNull(result);
        assertEquals(PLUGIN_ID, result.getId());
        assertEquals(io.gravitee.definition.model.v4.listener.ListenerType.HTTP, result.getSupportedListenerType());
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldNotFindById() {
        when(pluginManager.get(PLUGIN_ID)).thenReturn(null);

        entrypointService.findById(PLUGIN_ID);
    }

    @Test
    public void shouldFindAll() {
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll()).thenReturn(List.of(mockPlugin));

        Set<ConnectorPluginEntity> result = entrypointService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PLUGIN_ID, result.iterator().next().getId());
    }

    @Test
    public void shouldFindBySupportedApi() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll()).thenReturn(List.of(mockPlugin));
        when(pluginManager.getFactoryById(PLUGIN_ID)).thenReturn(mockFactory);

        Set<ConnectorPluginEntity> result = entrypointService.findBySupportedApi(io.gravitee.definition.model.v4.ApiType.MESSAGE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PLUGIN_ID, result.iterator().next().getId());
    }

    @Test
    public void shouldFindByConnectorMode() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll()).thenReturn(List.of(mockPlugin));
        when(pluginManager.getFactoryById(PLUGIN_ID)).thenReturn(mockFactory);

        Set<ConnectorPluginEntity> result = entrypointService.findByConnectorMode(ConnectorMode.REQUEST_RESPONSE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PLUGIN_ID, result.iterator().next().getId());
    }
}

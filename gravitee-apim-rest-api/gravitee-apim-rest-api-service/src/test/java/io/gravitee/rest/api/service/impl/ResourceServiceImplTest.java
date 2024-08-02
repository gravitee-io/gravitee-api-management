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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.plugin.resource.internal.ResourcePluginManagerImpl;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
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
public class ResourceServiceImplTest {

    private static final String PLUGIN_ID = "my-test-plugin";
    private static final String CONFIGURATION = "my-test-configuration";
    private static final String SCHEMA = "my-test-schema";

    @InjectMocks
    private ResourceServiceImpl resourceService;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private ResourcePluginManagerImpl pluginManager;

    @Mock
    private ResourcePlugin mockPlugin;

    @Mock
    private PluginManifest mockPluginManifest;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(resourceService, "pluginManager", pluginManager);
    }

    @Test
    public void shouldValidateConfigurationFromV3resource() throws IOException {
        when(pluginManager.getSchema(PLUGIN_ID, true)).thenReturn(SCHEMA);

        Resource resource = mock(Resource.class);
        when(resource.getType()).thenReturn(PLUGIN_ID);
        when(resource.getConfiguration()).thenReturn(CONFIGURATION);

        resourceService.validateResourceConfiguration(resource);

        verify(jsonSchemaService).validate(SCHEMA, CONFIGURATION);
    }

    @Test
    public void shouldValidateConfigurationFromV4resource() throws IOException {
        when(pluginManager.getSchema(PLUGIN_ID, true)).thenReturn(SCHEMA);

        io.gravitee.definition.model.v4.resource.Resource resource = mock(io.gravitee.definition.model.v4.resource.Resource.class);
        when(resource.getType()).thenReturn(PLUGIN_ID);
        when(resource.getConfiguration()).thenReturn(CONFIGURATION);

        resourceService.validateResourceConfiguration(resource);

        verify(jsonSchemaService).validate(SCHEMA, CONFIGURATION);
    }

    @Test
    public void shouldFindById() throws IOException {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
        when(pluginManager.getIcon(PLUGIN_ID, true)).thenReturn("icon");

        PlatformPluginEntity result = resourceService.findById(PLUGIN_ID);

        assertNotNull(result);
        assertEquals(PLUGIN_ID, result.getId());
        assertEquals("icon", result.getIcon());
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldNotFindById() {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(null);

        resourceService.findById(PLUGIN_ID);
    }

    @Test
    public void shouldFindAll() throws IOException {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
        when(pluginManager.getIcon(PLUGIN_ID, true)).thenReturn("icon");

        Set<PlatformPluginEntity> result = resourceService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PLUGIN_ID, result.iterator().next().getId());
        assertEquals("icon", result.iterator().next().getIcon());
    }
}

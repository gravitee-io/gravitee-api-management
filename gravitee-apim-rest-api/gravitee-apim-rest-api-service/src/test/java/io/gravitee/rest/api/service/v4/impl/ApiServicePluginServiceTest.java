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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServicePluginServiceTest {

    public static final String API_SERVICE_PLUGIN_ID = "api.service.plugin.id";
    public static final String API_SERVICE_PLUGIN_DESCRIPTION = "api.service.plugin.description";
    public static final String API_SERVICE_PLUGIN_NAME = "api.service.plugin.name";
    public static final String API_SERVICE_PLUGIN_VERSION = "api.service.plugin.version";
    public static final String API_SERVICE_PLUGIN_CATEGORY = "api.service.plugin.category";
    public static final String API_SERVICE_PLUGIN_SCHEMA = "api.service.plugin.schema";
    public static final String API_SERVICE_PLUGIN_CONFIG_FIXED = "api.service.plugin.config.fixed";

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private ApiServicePluginManager pluginManager;

    @Mock
    private ApiServicePlugin mockPlugin;

    @Mock
    private PluginManifest mockPluginManifest;

    @InjectMocks
    private ApiServicePluginServiceImpl cut;

    @Before
    public void setup() {
        when(mockPlugin.id()).thenReturn(API_SERVICE_PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(mockPluginManifest.description()).thenReturn(API_SERVICE_PLUGIN_DESCRIPTION);
        when(mockPluginManifest.name()).thenReturn(API_SERVICE_PLUGIN_NAME);
        when(mockPluginManifest.version()).thenReturn(API_SERVICE_PLUGIN_VERSION);
        when(mockPluginManifest.category()).thenReturn(API_SERVICE_PLUGIN_CATEGORY);
    }

    @Test
    public void shouldFindAll() {
        when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));

        var plugins = cut.findAll();

        assertThat(plugins)
            .isNotNull()
            .isNotEmpty()
            .allMatch(
                plugin ->
                    plugin.getId().equals(API_SERVICE_PLUGIN_ID) &&
                    plugin.getDescription().equals(API_SERVICE_PLUGIN_DESCRIPTION) &&
                    plugin.getName().equals(API_SERVICE_PLUGIN_NAME) &&
                    plugin.getVersion().equals(API_SERVICE_PLUGIN_VERSION) &&
                    plugin.getCategory().equals(API_SERVICE_PLUGIN_CATEGORY)
            );
    }

    @Test
    public void shouldFindById() {
        when(pluginManager.get(API_SERVICE_PLUGIN_ID, true)).thenReturn(mockPlugin);

        var plugin = cut.findById(API_SERVICE_PLUGIN_ID);

        assertThat(plugin)
            .isNotNull()
            .matches(
                p ->
                    p.getId().equals(API_SERVICE_PLUGIN_ID) &&
                    p.getDescription().equals(API_SERVICE_PLUGIN_DESCRIPTION) &&
                    p.getName().equals(API_SERVICE_PLUGIN_NAME) &&
                    p.getVersion().equals(API_SERVICE_PLUGIN_VERSION) &&
                    p.getCategory().equals(API_SERVICE_PLUGIN_CATEGORY)
            );
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldNotFindById() {
        when(pluginManager.get(API_SERVICE_PLUGIN_ID, true)).thenReturn(null);
        cut.findById(API_SERVICE_PLUGIN_ID);
    }

    @Test
    public void shouldValidateApiServiceConfiguration() throws Exception {
        final var config = "{}";
        when(pluginManager.get(API_SERVICE_PLUGIN_ID, true)).thenReturn(mockPlugin);
        when(pluginManager.getSchema(API_SERVICE_PLUGIN_ID, true)).thenReturn(API_SERVICE_PLUGIN_SCHEMA);
        when(jsonSchemaService.validate(API_SERVICE_PLUGIN_SCHEMA, config)).thenReturn(API_SERVICE_PLUGIN_CONFIG_FIXED);

        var fixedConfig = cut.validateApiServiceConfiguration(API_SERVICE_PLUGIN_ID, config);

        assertThat(fixedConfig).isNotNull().isEqualTo(API_SERVICE_PLUGIN_CONFIG_FIXED);
    }

    @Test
    public void shouldValidateApiServiceConfigurationWhenNullConfig() throws Exception {
        when(pluginManager.get(API_SERVICE_PLUGIN_ID, true)).thenReturn(mockPlugin);
        var fixedConfig = cut.validateApiServiceConfiguration(API_SERVICE_PLUGIN_ID, null);
        assertThat(fixedConfig).isNull();
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldFailToValidateWhenPluginNotFound() {
        when(pluginManager.get(API_SERVICE_PLUGIN_ID, true)).thenReturn(null);
        cut.validateApiServiceConfiguration(API_SERVICE_PLUGIN_ID, null);
    }
}

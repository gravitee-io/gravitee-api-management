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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.policy.FlowPhase;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PolicyPluginServiceImplTest {

    private static final String PLUGIN_ID = "my-test-plugin";
    private static final String CONFIGURATION = "my-test-configuration";
    private static final String SCHEMA = "my-test-schema";

    private PolicyPluginService cut;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private ConfigurablePluginManager<PolicyPlugin<?>> pluginManager;

    @Mock
    private PolicyPlugin mockPlugin;

    @Mock
    private PluginManifest mockPluginManifest;

    @Before
    public void setup() {
        cut = new PolicyPluginServiceImpl(jsonSchemaService, pluginManager);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
    }

    @Test
    public void shouldValidateConfiguration() throws IOException {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
        when(pluginManager.getSchema(PLUGIN_ID, true)).thenReturn(SCHEMA);
        when(jsonSchemaService.validate(SCHEMA, CONFIGURATION)).thenReturn("fixed-configuration");

        String resultConfiguration = cut.validatePolicyConfiguration(PLUGIN_ID, CONFIGURATION);

        assertEquals("fixed-configuration", resultConfiguration);
    }

    @Test
    public void shouldValidateConfigurationWhenNullConfiguration() throws IOException {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);

        String resultConfiguration = cut.validatePolicyConfiguration(PLUGIN_ID, null);

        assertNull(resultConfiguration);
    }

    @Test
    public void should_validateConfiguration_with_internal_policy() {
        var policyId = "shared-policy-group-policy";

        String resultConfiguration = cut.validatePolicyConfiguration(policyId, CONFIGURATION);

        assertEquals(CONFIGURATION, resultConfiguration);
        verify(pluginManager, times(0)).get(policyId, true);
    }

    @Test(expected = PluginNotFoundException.class)
    public void shouldFailToValidateConfigurationWhenNoPlugin() {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(null);

        cut.validatePolicyConfiguration(PLUGIN_ID, CONFIGURATION);
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
    public void should_find_all() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
        when(mockPluginManifest.properties()).thenReturn(Map.of("proxy", "REQUEST", "message", "PUBLISH"));
        Set<PolicyPluginEntity> result = cut.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        PolicyPluginEntity policyPlugin = result.iterator().next();
        assertEquals(PLUGIN_ID, policyPlugin.getId());
        assertEquals(Set.of(FlowPhase.REQUEST), policyPlugin.getProxy());
        assertEquals(Set.of(FlowPhase.PUBLISH), policyPlugin.getMessage());
    }

    @Test
    public void should_find_all_with_legacy_phase() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
        when(mockPluginManifest.properties()).thenReturn(Map.of("message", "MESSAGE_REQUEST, MESSAGE_RESPONSE"));
        Set<PolicyPluginEntity> result = cut.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        PolicyPluginEntity policyPlugin = result.iterator().next();
        assertEquals(PLUGIN_ID, policyPlugin.getId());
        assertEquals(Set.of(FlowPhase.PUBLISH, FlowPhase.SUBSCRIBE), policyPlugin.getMessage());
    }

    @Test
    public void shouldGetGvSchemaForm() throws IOException {
        when(pluginManager.getSchema("my-policy", "display-gv-schema-form", true)).thenReturn("gv-schema-form-config");

        String schema = cut.getSchema("my-policy", SchemaDisplayFormat.GV_SCHEMA_FORM);
        assertEquals("gv-schema-form-config", schema);
    }

    @Test
    public void shouldGetDefaultSchemaFormWhenIOException() throws IOException {
        when(pluginManager.getSchema("my-policy", "display-gv-schema-form", true)).thenThrow(new IOException());
        when(pluginManager.getSchema("my-policy", true)).thenReturn("default-configuration");

        String schema = cut.getSchema("my-policy", SchemaDisplayFormat.GV_SCHEMA_FORM);
        assertEquals("default-configuration", schema);
    }

    @Test
    public void shouldGetDefaultSchemaFormWhenNull() throws IOException {
        when(pluginManager.getSchema("my-policy", "display-gv-schema-form", true)).thenReturn(null);
        when(pluginManager.getSchema("my-policy", true)).thenReturn("default-configuration");

        String schema = cut.getSchema("my-policy", SchemaDisplayFormat.GV_SCHEMA_FORM);
        assertEquals("default-configuration", schema);
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.StepV2;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {

    private static final String POLICY_ID = "myPolicy";

    @InjectMocks
    private PolicyServiceImpl policyService;

    @Mock
    private ConfigurablePluginManager<PolicyPlugin> policyManager;

    @Mock
    private PolicyPlugin policyDefinition;

    @Mock
    private JsonSchemaService jsonSchemaService;

    class PolicyMock {

        @OnRequest
        public void onRequest() {}
    }

    @Before
    public void before() {
        when(policyDefinition.path()).thenReturn(mock(Path.class));
        when(policyDefinition.type()).thenReturn("");
        when(policyDefinition.policy()).thenReturn(PolicyMock.class);
    }

    @Mock
    private PluginManifest manifest;

    @Test
    public void shouldFindAll() throws TechnicalException {
        when(policyDefinition.id()).thenReturn(POLICY_ID);
        when(policyManager.findAll(true)).thenReturn(Collections.singletonList(policyDefinition));
        when(policyDefinition.manifest()).thenReturn(manifest);

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertEquals(POLICY_ID, policies.iterator().next().getId());
    }

    @Test
    public void shouldFilterMessagePolicies() {
        when(policyManager.findAll(true)).thenReturn(Collections.singletonList(policyDefinition));
        when(policyDefinition.manifest()).thenReturn(manifest);
        when(manifest.properties()).thenReturn(Map.of("message", "MESSAGE_REQUEST,MESSAGE_RESPONSE"));

        final Set<PolicyEntity> policies = policyService.findAll();

        assertThat(policies).isEmpty();
    }

    @Test
    public void shouldNotFilterPoliciesWithoutProxyOrMessageProperty() {
        when(policyDefinition.id()).thenReturn(POLICY_ID);
        when(policyManager.findAll(true)).thenReturn(Collections.singletonList(policyDefinition));
        when(policyDefinition.manifest()).thenReturn(manifest);

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertEquals(POLICY_ID, policies.iterator().next().getId());
    }

    @Test
    public void shouldNotFilterProxyPolicies() {
        when(policyDefinition.id()).thenReturn(POLICY_ID);
        when(policyManager.findAll(true)).thenReturn(Collections.singletonList(policyDefinition));
        when(policyDefinition.manifest()).thenReturn(manifest);
        when(manifest.properties()).thenReturn(Map.of("proxy", "REQUEST"));

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertEquals(POLICY_ID, policies.iterator().next().getId());
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJson() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ Invalid: \"test\", \"valid\": false }");
        when(jsonSchemaService.validate(any(), anyString())).thenThrow(InvalidDataException.class);
        policyService.validatePolicyConfiguration(policy);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJsonStep() throws Exception {
        StepV2 step = new StepV2();
        step.setPolicy("my-policy");
        step.setConfiguration("{ Invalid: \"test\", \"valid\": false }");
        when(jsonSchemaService.validate(any(), anyString())).thenThrow(InvalidDataException.class);
        policyService.validatePolicyConfiguration(step);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithNoSchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"test\", \"valid\": true }");

        when(policyManager.getSchema("my-policy", true)).thenReturn("");
        when(jsonSchemaService.validate(anyString(), anyString())).thenReturn(policy.getConfiguration());

        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithNoSchemaStep() throws Exception {
        StepV2 step = new StepV2();
        step.setPolicy("my-policy");
        step.setConfiguration("{ \"name\": \"test\", \"valid\": true }");

        when(policyManager.getSchema("my-policy", true)).thenReturn("");
        when(jsonSchemaService.validate(anyString(), anyString())).thenReturn(step.getConfiguration());

        policyService.validatePolicyConfiguration(step);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithEmptySchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"test\", \"valid\": true }");
        when(policyManager.getSchema("my-policy", true)).thenReturn("");
        when(jsonSchemaService.validate(anyString(), anyString())).thenReturn(policy.getConfiguration());

        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptNoConfiguration() {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration(null);

        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptNoConfigurationStep() {
        StepV2 step = new StepV2();
        step.setPolicy("my-policy");
        step.setConfiguration(null);

        policyService.validatePolicyConfiguration(step);
    }

    @Test
    public void shouldAcceptNullPolicy() {
        policyService.validatePolicyConfiguration((Policy) null);
    }

    @Test
    public void shouldAcceptNullStep() {
        policyService.validatePolicyConfiguration((StepV2) null);
    }

    @Test
    public void shouldGetGvSchemaForm() throws IOException {
        when(policyManager.getSchema("my-policy", "display-gv-schema-form", true)).thenReturn("gv-schema-form-config");

        String schema = policyService.getSchema("my-policy", SchemaDisplayFormat.GV_SCHEMA_FORM);
        assertEquals("gv-schema-form-config", schema);
    }

    @Test
    public void shouldGetDefaultSchemaFormWhenIOException() throws IOException {
        when(policyManager.getSchema("my-policy", "display-gv-schema-form", true)).thenThrow(new IOException());
        when(policyManager.getSchema("my-policy", true)).thenReturn("default-configuration");

        String schema = policyService.getSchema("my-policy", SchemaDisplayFormat.GV_SCHEMA_FORM);
        assertEquals("default-configuration", schema);
    }

    @Test
    public void shouldGetDefaultSchemaFormWhenNull() throws IOException {
        when(policyManager.getSchema("my-policy", "display-gv-schema-form", true)).thenReturn(null);
        when(policyManager.getSchema("my-policy", true)).thenReturn("default-configuration");

        String schema = policyService.getSchema("my-policy", SchemaDisplayFormat.GV_SCHEMA_FORM);
        assertEquals("default-configuration", schema);
    }
}
